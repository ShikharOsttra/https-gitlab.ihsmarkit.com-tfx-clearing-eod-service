package com.ihsmarkit.tfx.eod.batch;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.assertj.core.groups.Tuple;
import org.assertj.core.matcher.AssertionMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.hamcrest.MockitoHamcrest;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.eod.config.DateConfig;
import com.ihsmarkit.tfx.eod.config.EOD1JobConfig;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.JPYRatesService;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

class MarkToMarketTradesTaskletTest extends AbstractSpringBatchTest {

    private static final CurrencyPairEntity CURRENCY_PAIR_USD = aCurrencyPairEntityBuilder().build();
    private static final CurrencyPairEntity CURRENCY_PAIR_JPY = aCurrencyPairEntityBuilder().baseCurrency(JPY).build();
    private static final ParticipantEntity PARTICIPANT = aParticipantEntityBuilder().build();

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 10, 6);
    private static final LocalDate VALUE_DATE = BUSINESS_DATE.plusDays(2);

    private static final BigDecimal JPY_RATE = BigDecimal.valueOf(99);
    private static final BigDecimal USD_RATE = BigDecimal.valueOf(1.177);

    @MockBean
    private TradeRepository tradeRepository;

    @MockBean
    private DailySettlementPriceService dailySettlementPriceService;

    @MockBean
    private JPYRatesService jpyRatesService;

    @MockBean
    private EodProductCashSettlementRepository eodProductCashSettlementRepository;

    @MockBean
    private ParticipantPositionRepository participantPositionRepository;

    @MockBean
    private EODCalculator eodCalculator;

    @MockBean
    private TradeAndSettlementDateService tradeAndSettlementDateService;

    @Captor
    private ArgumentCaptor<Iterable<EodProductCashSettlementEntity>> captor;

    @Mock
    private Stream<TradeEntity> trades;

    @Mock
    private Collection<ParticipantPositionEntity> positions;

    @Test
//    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    void shouldCalculateAndStoreDailyAndInitialMtm() {

        when(tradeAndSettlementDateService.getValueDate(BUSINESS_DATE, CURRENCY_PAIR_USD)).thenReturn(VALUE_DATE);
        when(tradeAndSettlementDateService.getValueDate(BUSINESS_DATE, CURRENCY_PAIR_JPY)).thenReturn(VALUE_DATE);

        when(tradeRepository.findAllNovatedForTradeDate(any())).thenReturn(trades);

        when(participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(any(), any()))
            .thenReturn(positions);

        when(dailySettlementPriceService.getPrice(BUSINESS_DATE, CURRENCY_PAIR_USD)).thenReturn(USD_RATE);
        when(dailySettlementPriceService.getPrice(BUSINESS_DATE, CURRENCY_PAIR_JPY)).thenReturn(JPY_RATE);
        when(jpyRatesService.getJpyRate(BUSINESS_DATE, USD)).thenReturn(JPY_RATE);

        when(eodCalculator.calculateAndAggregateInitialMtm(any(), any(), any()))
            .thenReturn(
                Stream.of(
                    ParticipantCurrencyPairAmount.of(PARTICIPANT, CURRENCY_PAIR_USD, BigDecimal.ONE),
                    ParticipantCurrencyPairAmount.of(PARTICIPANT, CURRENCY_PAIR_JPY, BigDecimal.valueOf(2))
                )
            );

        when(eodCalculator.calculateAndAggregateDailyMtm(any(), any(), any()))
            .thenReturn(Stream.of(ParticipantCurrencyPairAmount.of(PARTICIPANT, CURRENCY_PAIR_USD, BigDecimal.TEN)));

        final JobExecution execution = jobLauncherTestUtils.launchStep(MTM_TRADES_STEP_NAME,
            new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString("businessDate", BUSINESS_DATE.format(BUSINESS_DATE_FMT))
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        final Matcher<Function<CurrencyPairEntity, BigDecimal>> dspMatcher =
            assertionMatcher(
                actual -> assertThat(actual)
                    .returns(USD_RATE, a -> a.apply(CURRENCY_PAIR_USD))
                    .returns(JPY_RATE, a -> a.apply(CURRENCY_PAIR_JPY))
            );

        final Matcher<Function<String, BigDecimal>> jpyRates =
            assertionMatcher(actual -> assertThat(actual).returns(JPY_RATE, a -> a.apply(USD)));

        verify(eodCalculator).calculateAndAggregateDailyMtm(
            eq(positions),
            MockitoHamcrest.argThat(dspMatcher),
            MockitoHamcrest.argThat(jpyRates)
        );
        verify(eodCalculator).calculateAndAggregateInitialMtm(
            eq(trades),
            MockitoHamcrest.argThat(dspMatcher),
            MockitoHamcrest.argThat(jpyRates)
        );

        verify(tradeRepository).findAllNovatedForTradeDate(BUSINESS_DATE);
        verify(participantPositionRepository).findAllByPositionTypeAndTradeDateFetchCurrencyPair(SOD, BUSINESS_DATE);

        verify(eodProductCashSettlementRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
            .extracting(
                EodProductCashSettlementEntity::getParticipant,
                EodProductCashSettlementEntity::getCurrencyPair,
                EodProductCashSettlementEntity::getType,
                EodProductCashSettlementEntity::getAmount,
                EodProductCashSettlementEntity::getDate,
                EodProductCashSettlementEntity::getSettlementDate
            )
            .containsExactlyInAnyOrder(
                Tuple.tuple(
                    PARTICIPANT,
                    CURRENCY_PAIR_USD,
                    EodProductCashSettlementType.DAILY_MTM, AmountEntity.of(BigDecimal.TEN, JPY),
                    BUSINESS_DATE,
                    VALUE_DATE
                ),
                Tuple.tuple(
                    PARTICIPANT,
                    CURRENCY_PAIR_USD,
                    EodProductCashSettlementType.INITIAL_MTM, AmountEntity.of(BigDecimal.ONE, JPY),
                    BUSINESS_DATE,
                    VALUE_DATE
                ),
                Tuple.tuple(
                    PARTICIPANT,
                    CURRENCY_PAIR_JPY,
                    EodProductCashSettlementType.INITIAL_MTM, AmountEntity.of(BigDecimal.valueOf(2), JPY),
                    BUSINESS_DATE,
                    VALUE_DATE
                )
            );

        verify(jpyRatesService, atLeastOnce()).getJpyRate(BUSINESS_DATE, USD);
        verify(dailySettlementPriceService, atLeastOnce()).getPrice(BUSINESS_DATE, CURRENCY_PAIR_JPY);
        verify(dailySettlementPriceService, atLeastOnce()).getPrice(BUSINESS_DATE, CURRENCY_PAIR_USD);

        verifyNoMoreInteractions(
            tradeRepository,
            participantPositionRepository,
            eodCalculator,
            eodProductCashSettlementRepository,
            jpyRatesService,
            dailySettlementPriceService
        );
    }

    static <T> AssertionMatcher<T> assertionMatcher(Consumer<T> consumer) {
        return new AssertionMatcher<T>() {
            @Override
            public void assertion(T actual) {
                consumer.accept(actual);
            }
        };
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = {
        MarkToMarketTradesTasklet.class, TradeOrPositionEssentialsMapper.class, EodProductCashSettlementRepository.class,
        EODCalculator.class, EOD1JobConfig.class, DateConfig.class
    },
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                MarkToMarketTradesTasklet.class,
                EODCalculator.class,
                TradeOrPositionEssentialsMapper.class,
                EodProductCashSettlementRepository.class,
                EOD1JobConfig.class,
                DateConfig.class
        })
    )
    static class TestConfig {

    }
}