package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.common.test.assertion.Matchers.argThat;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aCurrencyPairEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SOD;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MTM_TRADES_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.test.context.ContextConfiguration;

import com.ihsmarkit.tfx.common.function.ThrowingConsumer;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.eod.config.AbstractSpringBatchTest;
import com.ihsmarkit.tfx.eod.config.EOD1JobConfig;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.EodCashSettlementMappingService;
import com.ihsmarkit.tfx.eod.service.JPYRateService;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

@ContextConfiguration(classes = EOD1JobConfig.class)
class MarkToMarketTradesTaskletTest extends AbstractSpringBatchTest {

    private static final CurrencyPairEntity CURRENCY_PAIR_USD = aCurrencyPairEntityBuilder().build();
    private static final CurrencyPairEntity CURRENCY_PAIR_JPY = aCurrencyPairEntityBuilder().baseCurrency(JPY).build();
    private static final ParticipantEntity PARTICIPANT = aParticipantEntityBuilder().build();

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 10, 6);
    private static final LocalDate VALUE_DATE = BUSINESS_DATE.plusDays(2);

    private static final BigDecimal JPY_RATE = BigDecimal.valueOf(99);
    private static final BigDecimal USD_RATE = BigDecimal.valueOf(1.177);

    private static final ParticipantCurrencyPairAmount INITIAL_POS_1 = ParticipantCurrencyPairAmount.of(PARTICIPANT, CURRENCY_PAIR_USD, BigDecimal.ONE);
    private static final ParticipantCurrencyPairAmount INITIAL_POS_2 = ParticipantCurrencyPairAmount.of(PARTICIPANT, CURRENCY_PAIR_JPY, BigDecimal.valueOf(2));
    private static final ParticipantCurrencyPairAmount DAILY_POS_1 = ParticipantCurrencyPairAmount.of(PARTICIPANT, CURRENCY_PAIR_USD, BigDecimal.TEN);

    private static final EodProductCashSettlementEntity POS_ENTITY_1 = EodProductCashSettlementEntity.builder().id(1L).build();
    private static final EodProductCashSettlementEntity POS_ENTITY_2 = EodProductCashSettlementEntity.builder().id(2L).build();
    private static final EodProductCashSettlementEntity POS_ENTITY_3 = EodProductCashSettlementEntity.builder().id(3L).build();

    @MockBean
    private TradeRepository tradeRepository;

    @MockBean
    private DailySettlementPriceService dailySettlementPriceService;

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

    @Mock
    private Stream<ParticipantPositionEntity> positions;

    @Test
    void shouldCalculateAndStoreDailyAndInitialMtm() {

        when(tradeAndSettlementDateService.getValueDate(BUSINESS_DATE, CURRENCY_PAIR_USD)).thenReturn(VALUE_DATE);
        when(tradeAndSettlementDateService.getValueDate(BUSINESS_DATE, CURRENCY_PAIR_JPY)).thenReturn(VALUE_DATE);

        when(tradeRepository.findAllNovatedForTradeDate(any())).thenReturn(trades);

        when(participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(any(), any()))
            .thenReturn(positions);

        when(dailySettlementPriceService.getPrice(BUSINESS_DATE, CURRENCY_PAIR_USD)).thenReturn(USD_RATE);
        when(dailySettlementPriceService.getPrice(BUSINESS_DATE, CURRENCY_PAIR_JPY)).thenReturn(JPY_RATE);
        when(jpyRateService.getJpyRate(BUSINESS_DATE, USD)).thenReturn(JPY_RATE);

        when(eodCalculator.calculateAndAggregateInitialMtm(any(), any(), any()))
            .thenReturn(Stream.of(INITIAL_POS_1, INITIAL_POS_2));

        when(eodCalculator.calculateAndAggregateDailyMtm(any(), any(), any()))
            .thenReturn(Stream.of(DAILY_POS_1));

        when(eodCashSettlementMappingService.mapInitialMtm(INITIAL_POS_1)).thenReturn(POS_ENTITY_1);
        when(eodCashSettlementMappingService.mapInitialMtm(INITIAL_POS_2)).thenReturn(POS_ENTITY_2);
        when(eodCashSettlementMappingService.mapDailyMtm(DAILY_POS_1)).thenReturn(POS_ENTITY_3);

        final JobExecution execution = jobLauncherTestUtils.launchStep(MTM_TRADES_STEP_NAME,
            new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString(BUSINESS_DATE_JOB_PARAM_NAME, BUSINESS_DATE.format(BUSINESS_DATE_FMT))
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        final ThrowingConsumer<Function<CurrencyPairEntity, BigDecimal>> dspAssertion =
            actual -> assertThat(actual)
                .returns(USD_RATE, a -> a.apply(CURRENCY_PAIR_USD))
                .returns(JPY_RATE, a -> a.apply(CURRENCY_PAIR_JPY));

        final ThrowingConsumer<Function<String, BigDecimal>> jpyRatesAssertion =
            actual -> assertThat(actual).returns(JPY_RATE, a -> a.apply(USD));

        verify(eodCalculator).calculateAndAggregateDailyMtm(
            eq(positions),
            argThat(dspAssertion),
            argThat(jpyRatesAssertion)
        );
        verify(eodCalculator).calculateAndAggregateInitialMtm(
            eq(trades),
            argThat(dspAssertion),
            argThat(jpyRatesAssertion)
        );

        verify(tradeRepository).findAllNovatedForTradeDate(BUSINESS_DATE);
        verify(participantPositionRepository).findAllByPositionTypeAndTradeDateFetchCurrencyPair(SOD, BUSINESS_DATE);

        verify(eodProductCashSettlementRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
            .containsExactlyInAnyOrder(POS_ENTITY_1, POS_ENTITY_2, POS_ENTITY_3);

        verify(jpyRateService, atLeastOnce()).getJpyRate(BUSINESS_DATE, USD);
        verify(dailySettlementPriceService, atLeastOnce()).getPrice(BUSINESS_DATE, CURRENCY_PAIR_JPY);
        verify(dailySettlementPriceService, atLeastOnce()).getPrice(BUSINESS_DATE, CURRENCY_PAIR_USD);

        verifyNoMoreInteractions(
            tradeRepository,
            participantPositionRepository,
            eodCalculator,
            eodProductCashSettlementRepository,
            jpyRateService,
            dailySettlementPriceService
        );

        verifyZeroInteractions(eodFailedStepAlertSender);
    }

    @Test
    void shouldSendAlert_whenExceptionOccur() {
        final Exception cause = new RuntimeException("mtm step failed message");
        doThrow(cause).when(eodCalculator).calculateAndAggregateInitialMtm(any(), any(), any());

        final JobExecution execution = jobLauncherTestUtils.launchStep(MTM_TRADES_STEP_NAME,
            new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString(BUSINESS_DATE_JOB_PARAM_NAME, BUSINESS_DATE.format(BUSINESS_DATE_FMT))
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        verify(eodFailedStepAlertSender).mtmFailed(cause);
    }
}