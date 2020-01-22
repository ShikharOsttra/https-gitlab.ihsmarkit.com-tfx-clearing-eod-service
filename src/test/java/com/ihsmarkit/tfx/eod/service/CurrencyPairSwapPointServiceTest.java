package com.ihsmarkit.tfx.eod.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.test.JobScopeTestExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.EodSwapPointEntity;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.EodSwapPointRepository;
import com.ihsmarkit.tfx.eod.config.SpringBatchConfig;

@ExtendWith(SpringExtension.class)
@TestExecutionListeners(
    listeners = {DependencyInjectionTestExecutionListener.class, JobScopeTestExecutionListener.class},
    mergeMode = MERGE_WITH_DEFAULTS)
class CurrencyPairSwapPointServiceTest {

    private static final CurrencyPairEntity USDJPY = CurrencyPairEntity.of(1L, "USD", "JPY");
    private static final CurrencyPairEntity EURUSD = CurrencyPairEntity.of(2L, "EUR", "USD");

    private static final EodSwapPointEntity SWAP_PNT_USDJPY =
        EodSwapPointEntity.builder().currencyPair(USDJPY).swapPoint(BigDecimal.ONE).swapPointDays(1).build();
    private static final EodSwapPointEntity SWAP_PNT_EURUSD =
        EodSwapPointEntity.builder().currencyPair(EURUSD).swapPoint(null).swapPointDays(0).build();

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 10, 1);

    @MockBean
    private EodSwapPointRepository eodSwapPointRepository;

    @Autowired
    private CurrencyPairSwapPointService currencyPairSwapPointService;

    @Test
    void getSwapPoint() {
        when(eodSwapPointRepository.findAllByDateOrderedByProductNumber(any())).thenReturn(List.of(SWAP_PNT_USDJPY, SWAP_PNT_EURUSD));

        assertThat(currencyPairSwapPointService.getSwapPoint(BUSINESS_DATE, USDJPY)).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(currencyPairSwapPointService.getSwapPoint(BUSINESS_DATE, EURUSD)).isEqualByComparingTo(BigDecimal.ZERO);

        verify(eodSwapPointRepository).findAllByDateOrderedByProductNumber(BUSINESS_DATE);

    }


    @TestConfiguration
    @Import(SpringBatchConfig.class)
    @ComponentScan(basePackageClasses = CurrencyPairSwapPointService.class,
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CurrencyPairSwapPointService.class)
    )
    static class TestConfig {

    }
}