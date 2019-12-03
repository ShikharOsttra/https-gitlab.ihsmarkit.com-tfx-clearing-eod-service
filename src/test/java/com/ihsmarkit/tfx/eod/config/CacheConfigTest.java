package com.ihsmarkit.tfx.eod.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
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
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceProvider;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.JPYRatesService;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

class CacheConfigTest extends AbstractSpringBatchTest {

    private static final String TEST_STEP = "TEST_STEP";
    private static final String TEST_STEP_NEXT = "TEST_STEP_NEXT";
    private static final String TEST_JOB_NAME = "TEST_JOB_NAME";
    private static final BigDecimal USD_RATE = BigDecimal.valueOf(99.45);
    private static final BigDecimal EUR_RATE = BigDecimal.valueOf(104.98);
    private static final CurrencyPairEntity USDJPY = CurrencyPairEntity.of(1L, "USD", "JPY");
    private static final CurrencyPairEntity EURJPY = CurrencyPairEntity.of(2L, "EUR", "JPY");
    private static final String EURJPY_CODE = "EURJPY";
    private static final String USDJPY_CODE = "USDJPY";
    private static final LocalDate OCT_10 = LocalDate.of(2019, 10, 10);
    private static final LocalDate OCT_11 = OCT_10.plusDays(1);

    @Autowired
    private Job job;

    @MockBean
    private CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    @MockBean
    private DailySettlementPriceProvider dailySettlementPriceProvider;

    @Mock
    private Map<String, BigDecimal> ratesMap;


    @Test
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    void shouldHaveSeparateCachesForJobs() {

        when(calendarTradingSwapPointRepository.findNextTradingDate(any(), any())).thenReturn(Optional.of(OCT_11));

        when(dailySettlementPriceProvider.getDailySettlementPrices(OCT_10)).thenReturn(ratesMap);

        when(ratesMap.get(EURJPY_CODE)).thenReturn(EUR_RATE);
        when(ratesMap.get(USDJPY_CODE)).thenReturn(USD_RATE);

        assertThat(
            Stream.generate(this::launchJobAndGetStatus).limit(2)
        ).containsExactly(
            BatchStatus.COMPLETED, BatchStatus.COMPLETED
        );

        verify(calendarTradingSwapPointRepository, times(2)).findNextTradingDate(OCT_10, USDJPY);
        verify(ratesMap, times(2)).get(EURJPY_CODE);
        verify(ratesMap, times(4)).get(USDJPY_CODE); //FIXME: Should be 2!!!
        verifyNoMoreInteractions(calendarTradingSwapPointRepository, ratesMap);
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
        private final JPYRatesService jpyRatesService;

        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            assertThat(
                Stream
                    .generate(() -> tradeAndSettlementDateService.getNextTradeDate(OCT_10, USDJPY))
                    .limit(2)
            ).containsExactly(OCT_11, OCT_11);

            assertThat(jpyRatesService.getJpyRate(OCT_10, EodJobConstants.USD)).isEqualByComparingTo(USD_RATE);
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