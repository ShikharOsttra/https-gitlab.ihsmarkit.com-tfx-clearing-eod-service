package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.common.test.assertion.Matchers.argThat;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aCurrencyPairEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.SWAP_PNL_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.eod.config.AbstractSpringBatchTest;
import com.ihsmarkit.tfx.eod.config.EOD2JobConfig;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.service.CurrencyPairSwapPointService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.EodCashSettlementMappingService;
import com.ihsmarkit.tfx.eod.service.JPYRateService;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

@Import(EOD2JobConfig.class)
class SwapPnLTaskletTest extends AbstractSpringBatchTest {

    private static final CurrencyPairEntity CURRENCY_PAIR_USD = aCurrencyPairEntityBuilder().build();
    private static final CurrencyPairEntity CURRENCY_PAIR_JPY = aCurrencyPairEntityBuilder().baseCurrency(JPY).build();
    private static final ParticipantEntity PARTICIPANT = aParticipantEntityBuilder().build();

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 10, 6);
    private static final LocalDate VALUE_DATE = BUSINESS_DATE.plusDays(2);

    private static final BigDecimal EURJPY_SWP_PNT = BigDecimal.valueOf(99);
    private static final BigDecimal EURUSD_SWP_PNT = BigDecimal.valueOf(1.177);
    private static final BigDecimal USDJPY_RATE = BigDecimal.valueOf(99.7);


    private static final ParticipantCurrencyPairAmount INITIAL_POS_1 = ParticipantCurrencyPairAmount.of(PARTICIPANT, CURRENCY_PAIR_USD, BigDecimal.ONE);
    private static final ParticipantCurrencyPairAmount INITIAL_POS_2 = ParticipantCurrencyPairAmount.of(PARTICIPANT, CURRENCY_PAIR_JPY, BigDecimal.valueOf(2));

    private static final EodProductCashSettlementEntity POS_ENTITY_1 = EodProductCashSettlementEntity.builder().id(1L).build();
    private static final EodProductCashSettlementEntity POS_ENTITY_2 = EodProductCashSettlementEntity.builder().id(2L).build();

    @MockBean
    private TradeRepository tradeRepository;

    @MockBean
    private CurrencyPairSwapPointService currencyPairSwapPointService;

    @MockBean
    private JPYRateService jpyRateService;

    @MockBean
    private EodProductCashSettlementRepository eodProductCashSettlementRepository;

    @MockBean
    private ParticipantPositionRepository participantPositionRepository;

    @MockBean
    private EODCalculator eodCalculator;

    @MockBean
    private TradeAndSettlementDateService tradeAndSettlementDateService;

    @MockBean
    private EodCashSettlementMappingService eodCashSettlementMappingService;

    @Captor
    private ArgumentCaptor<Iterable<EodProductCashSettlementEntity>> captor;

    @Mock
    private Stream<TradeEntity> trades;

    @Test
    void shouldCalculateAndStoreDailyAndInitialMtm() {

        when(tradeAndSettlementDateService.getValueDate(BUSINESS_DATE, CURRENCY_PAIR_USD)).thenReturn(VALUE_DATE);
        when(tradeAndSettlementDateService.getValueDate(BUSINESS_DATE, CURRENCY_PAIR_JPY)).thenReturn(VALUE_DATE);

        when(tradeRepository.findAllNovatedForTradeDate(any())).thenReturn(trades);

        when(currencyPairSwapPointService.getSwapPoint(BUSINESS_DATE, CURRENCY_PAIR_USD)).thenReturn(EURUSD_SWP_PNT);
        when(currencyPairSwapPointService.getSwapPoint(BUSINESS_DATE, CURRENCY_PAIR_JPY)).thenReturn(EURJPY_SWP_PNT);
        when(jpyRateService.getJpyRate(BUSINESS_DATE, USD)).thenReturn(USDJPY_RATE);

        when(eodCalculator.calculateAndAggregateSwapPnL(any(), any(), any()))
            .thenReturn(Stream.of(INITIAL_POS_1, INITIAL_POS_2));

        when(eodCashSettlementMappingService.mapSwapPnL(INITIAL_POS_1)).thenReturn(POS_ENTITY_1);
        when(eodCashSettlementMappingService.mapSwapPnL(INITIAL_POS_2)).thenReturn(POS_ENTITY_2);

        final JobExecution execution = jobLauncherTestUtils.launchStep(SWAP_PNL_STEP_NAME,
            new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString(BUSINESS_DATE_JOB_PARAM_NAME, BUSINESS_DATE.format(BUSINESS_DATE_FMT))
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        final Consumer<Function<CurrencyPairEntity, BigDecimal>> dspAssertion =
            actual -> assertThat(actual)
                    .returns(EURUSD_SWP_PNT, a -> a.apply(CURRENCY_PAIR_USD))
                    .returns(EURJPY_SWP_PNT, a -> a.apply(CURRENCY_PAIR_JPY));

        final Consumer<Function<String, BigDecimal>> jpyRatesAssertion =
            actual -> assertThat(actual).returns(USDJPY_RATE, a -> a.apply(USD));

        verify(eodCalculator).calculateAndAggregateSwapPnL(
            eq(trades),
            argThat(dspAssertion),
            argThat(jpyRatesAssertion)
        );

        verify(tradeRepository).findAllNovatedForTradeDate(BUSINESS_DATE);

        verify(eodProductCashSettlementRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
            .containsExactlyInAnyOrder(POS_ENTITY_1, POS_ENTITY_2);

        verify(jpyRateService, atLeastOnce()).getJpyRate(BUSINESS_DATE, USD);
        verify(currencyPairSwapPointService, atLeastOnce()).getSwapPoint(BUSINESS_DATE, CURRENCY_PAIR_JPY);
        verify(currencyPairSwapPointService, atLeastOnce()).getSwapPoint(BUSINESS_DATE, CURRENCY_PAIR_USD);

        verifyNoMoreInteractions(
            tradeRepository,
            participantPositionRepository,
            eodCalculator,
            eodProductCashSettlementRepository,
            jpyRateService,
            currencyPairSwapPointService
        );
    }
}