package com.ihsmarkit.tfx.eod.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.DailySettlementPriceRepository;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.JPYRateService;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@ContextConfiguration(classes = CacheConfigTest.TestConfig.class)
class CacheConfigTest extends AbstractSpringBatchTest {

    private static final String TEST_STEP = "TEST_STEP";
    private static final String TEST_STEP_NEXT = "TEST_STEP_NEXT";
    private static final String TEST_JOB_NAME = "TEST_JOB_NAME";
    private static final BigDecimal USD_RATE = BigDecimal.valueOf(99.45);
    private static final BigDecimal EUR_RATE = BigDecimal.valueOf(104.98);
    private static final CurrencyPairEntity USDJPY = CurrencyPairEntity.of(1L, "USD", "JPY");
    private static final CurrencyPairEntity EURJPY = CurrencyPairEntity.of(2L, "EUR", "JPY");
    private static final LocalDate OCT_10 = LocalDate.of(2019, 10, 10);
    private static final LocalDate OCT_11 = OCT_10.plusDays(1);

    private static final DailySettlementPriceEntity USD_DSP = DailySettlementPriceEntity.builder().currencyPair(USDJPY).dailySettlementPrice(USD_RATE).build();
    private static final DailySettlementPriceEntity EUR_DSP = DailySettlementPriceEntity.builder().currencyPair(EURJPY).dailySettlementPrice(EUR_RATE).build();

    @MockBean
    private CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    @MockBean
    private DailySettlementPriceRepository dailySettlementPriceRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Test
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    void shouldHaveSeparateCachesForJobs() {

        when(calendarTradingSwapPointRepository.findNextTradingDateFailFast(any(), any())).thenReturn(OCT_11);

        when(dailySettlementPriceRepository.findAllByBusinessDate(OCT_10)).thenReturn(List.of(USD_DSP, EUR_DSP));

        assertThat(
            Stream.generate(this::launchJobAndGetStatus).limit(2)
        ).containsExactly(
            BatchStatus.COMPLETED, BatchStatus.COMPLETED
        );

        verify(calendarTradingSwapPointRepository, times(2)).findNextTradingDateFailFast(OCT_10, USDJPY);
        verify(dailySettlementPriceRepository, times(2)).findAllByBusinessDate(OCT_10);

        verifyNoMoreInteractions(calendarTradingSwapPointRepository);
    };

    @SneakyThrows(Exception.class)
    private BatchStatus launchJobAndGetStatus() {
        return jobLauncherTestUtils.launchJob().getStatus();
    }

    @Component
    @RequiredArgsConstructor
    static class TestTasklet implements Tasklet {

        private final TradeAndSettlementDateService tradeAndSettlementDateService;
        private final DailySettlementPriceService dailySettlementPriceService;
        private final JPYRateService jpyRateService;

        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            assertThat(
                Stream
                    .generate(() -> tradeAndSettlementDateService.getNextTradeDate(OCT_10, USDJPY))
                    .limit(2)
            ).containsExactly(OCT_11, OCT_11);

            assertThat(jpyRateService.getJpyRate(OCT_10, EodJobConstants.USD)).isEqualByComparingTo(USD_RATE);
            assertThat(dailySettlementPriceService.getPrice(OCT_10, EURJPY)).isEqualByComparingTo(EUR_RATE);
            assertThat(dailySettlementPriceService.getPrice(OCT_10, USDJPY)).isEqualByComparingTo(USD_RATE);

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
    @TestConfiguration
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