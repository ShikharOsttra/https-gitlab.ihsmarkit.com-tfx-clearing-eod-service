package com.ihsmarkit.tfx.eod.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.eod.batch.AbstractSpringBatchTest;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import lombok.RequiredArgsConstructor;

class CacheConfigTest extends AbstractSpringBatchTest {

    private static final String TEST_STEP = "TEST_STEP";
    private static final String TEST_STEP_NEXT = "TEST_STEP_NEXT";
    private static final String TEST_JOB_NAME = "TEST_JOB_NAME";
    private static final CurrencyPairEntity USDJPY = CurrencyPairEntity.of(1L, "USD", "JPY");
    private static final LocalDate OCT_10 = LocalDate.of(2019, 10, 10);
    private static final LocalDate OCT_11 = OCT_10.plusDays(1);

    @Autowired
    private Job job;

    @MockBean
    private CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;


    @Test
    void shouldHaveSeparateCachesForJobs() throws Exception {

        when(calendarTradingSwapPointRepository.findNextTradingDate(any(), any())).thenReturn(Optional.of(OCT_11));

        assertThat(
            List.of(
                jobLauncherTestUtils.getJobLauncher().run(job, jobLauncherTestUtils.getUniqueJobParameters()).getStatus(),
                jobLauncherTestUtils.getJobLauncher().run(job, jobLauncherTestUtils.getUniqueJobParameters()).getStatus()
            )
        ).containsExactly(BatchStatus.COMPLETED, BatchStatus.COMPLETED);

        verify(calendarTradingSwapPointRepository, times(2)).findNextTradingDate(OCT_10, USDJPY);
    };

    @Component
    @RequiredArgsConstructor
    static class TestTasklet implements Tasklet {

        private final TradeAndSettlementDateService tradeAndSettlementDateService;

        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            assertThat(
                List.of(
                    tradeAndSettlementDateService.getNextTradeDate(OCT_10, USDJPY),
                    tradeAndSettlementDateService.getNextTradeDate(OCT_10, USDJPY)
                )
            ).containsExactly(OCT_11, OCT_11);

            return RepeatStatus.FINISHED;
        }
    }

    @Component
    @RequiredArgsConstructor
    static class TestTaskletNext implements Tasklet {

        private final TradeAndSettlementDateService tradeAndSettlementDateService;

        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            assertThat(tradeAndSettlementDateService.getNextTradeDate(OCT_10, USDJPY)).isEqualTo(OCT_11);
            return RepeatStatus.FINISHED;
        }
    }

    @ComponentScan(
        basePackageClasses = {SpringBatchConfig.class, CacheConfig.class, CacheConfigTest.TestTasklet.class, CacheConfigTest.TestTaskletNext.class},
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = {SpringBatchConfig.class, CacheConfig.class, CacheConfigTest.TestTasklet.class, CacheConfigTest.TestTaskletNext.class})
    )
    @Configuration
    static class TestConfig {

        @Bean
        public Job job(
            @Autowired TestTasklet tasklet,
            @Autowired TestTaskletNext nextTasklet,
            @Autowired StepBuilderFactory steps,
            @Autowired JobBuilderFactory jobs
        ) {

            return jobs.get(TEST_JOB_NAME)
                .start(
                    steps.get(TEST_STEP)
                        .tasklet(tasklet)
                        .build()
                ).next(
                    steps.get(TEST_STEP_NEXT)
                        .tasklet(nextTasklet)
                        .build()
                )
                .build();
        }
    }

}