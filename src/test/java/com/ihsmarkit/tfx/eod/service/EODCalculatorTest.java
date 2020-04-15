package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aLegalEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableMap;
import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.LegalEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.Side;
import com.ihsmarkit.tfx.eod.config.EodJobConstants;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.BalanceTrade;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;

import lombok.NonNull;

@ExtendWith(SpringExtension.class)
class EODCalculatorTest {

    private static final CurrencyPairEntity USDJPY = CurrencyPairEntity.of(1L, "USD", "JPY");
    private static final CurrencyPairEntity EURUSD = CurrencyPairEntity.of(2L, "EUR", "USD");

    private static final Map<CurrencyPairEntity, BigDecimal> PRICE_MAP = Map.of(
        USDJPY, BigDecimal.valueOf(99.0),
        EURUSD, BigDecimal.valueOf(1.1)
    );

    private static final Map<CurrencyPairEntity, BigDecimal> SWP_PNT_MAP = Map.of(EURUSD, BigDecimal.valueOf(-0.31));

    private static final Map<String, BigDecimal> JPY_PRICE_MAP = Map.of(
        EodJobConstants.USD, BigDecimal.valueOf(99.0)
    );

    private static final ParticipantEntity PARTICIPANT_A = aParticipantEntityBuilder().name("A").build();
    private static final LegalEntity ORIGINATOR_A = aLegalEntityBuilder()
        .participant(PARTICIPANT_A)
        .build();

    private static final ParticipantEntity PARTICIPANT_B = aParticipantEntityBuilder().name("B").build();
    private static final LegalEntity ORIGINATOR_B = aLegalEntityBuilder()
        .participant(PARTICIPANT_B)
        .build();

    private static final ParticipantEntity PARTICIPANT_C = aParticipantEntityBuilder().name("C").build();
    private static final ParticipantEntity PARTICIPANT_D = aParticipantEntityBuilder().name("D").build();

    private static final TradeEntity A_BUYS_20K_USD = TradeEntity.builder()
        .direction(Side.BUY)
        .currencyPair(USDJPY)
        .spotRate(BigDecimal.valueOf(99.5))
        .baseAmount(AmountEntity.of(BigDecimal.valueOf(20000), "USD"))
        .originator(ORIGINATOR_A)
        .build();

    private static final TradeEntity A_SELLS_10K_USD = TradeEntity.builder()
        .direction(Side.SELL)
        .currencyPair(USDJPY)
        .spotRate(BigDecimal.valueOf(99.6))
        .baseAmount(AmountEntity.of(BigDecimal.valueOf(10000), "USD"))
        .originator(ORIGINATOR_A)
        .build();

    private static final TradeEntity A_BUYS_10K_EUR = TradeEntity.builder()
        .direction(Side.BUY)
        .currencyPair(EURUSD)
        .spotRate(BigDecimal.valueOf(1.2))
        .baseAmount(AmountEntity.of(BigDecimal.valueOf(10000), "EUR"))
        .originator(ORIGINATOR_A)
        .build();

    private static final TradeEntity B_SELLS_20K_EUR = TradeEntity.builder()
        .direction(Side.SELL)
        .currencyPair(EURUSD)
        .spotRate(BigDecimal.valueOf(1.2))
        .baseAmount(AmountEntity.of(BigDecimal.valueOf(20000), "EUR"))
        .originator(ORIGINATOR_B)
        .build();

    private static final TradeEntity A_BUYS_1_USD_AT_991 = TradeEntity.builder()
        .direction(Side.BUY)
        .currencyPair(USDJPY)
        .spotRate(BigDecimal.valueOf(99.1))
        .baseAmount(AmountEntity.of(BigDecimal.ONE, "USD"))
        .originator(ORIGINATOR_A)
        .build();

    private static final TradeEntity A_SELLS_1_USD_AT_991 = TradeEntity.builder()
        .direction(Side.SELL)
        .currencyPair(USDJPY)
        .spotRate(BigDecimal.valueOf(99.1))
        .baseAmount(AmountEntity.of(BigDecimal.ONE, "USD"))
        .originator(ORIGINATOR_A)
        .build();


    private static final TradeEntity A_SELLS_30K_EUR = TradeEntity.builder()
        .direction(Side.SELL)
        .currencyPair(EURUSD)
        .spotRate(BigDecimal.valueOf(1.2))
        .baseAmount(AmountEntity.of(BigDecimal.valueOf(30000), "EUR"))
        .originator(ORIGINATOR_A)
        .build();

    private static final ParticipantPositionEntity A_POS_USD_L_100K = ParticipantPositionEntity.builder()
        .currencyPair(USDJPY)
        .amount(AmountEntity.of(BigDecimal.valueOf(100_000), "USD"))
        .price(BigDecimal.valueOf(99.3))
        .participant(PARTICIPANT_A)
        .build();

    private static final ParticipantPositionEntity A_POS_EUR_L_100K = ParticipantPositionEntity.builder()
        .currencyPair(EURUSD)
        .amount(AmountEntity.of(BigDecimal.valueOf(100_000), "EUR"))
        .price(BigDecimal.valueOf(1.09))
        .participant(PARTICIPANT_A)
        .build();

    private static final ParticipantPositionEntity A_POS_EUR_L_212M = ParticipantPositionEntity.builder()
        .currencyPair(EURUSD)
        .amount(AmountEntity.of(BigDecimal.valueOf(212_000_000), "EUR"))
        .price(BigDecimal.valueOf(1.09))
        .participant(PARTICIPANT_A)
        .build();

    private static final ParticipantPositionEntity B_POS_EUR_L_30M = ParticipantPositionEntity.builder()
        .currencyPair(EURUSD)
        .amount(AmountEntity.of(BigDecimal.valueOf(30000000), "EUR"))
        .price(BigDecimal.valueOf(1.09))
        .participant(PARTICIPANT_B)
        .build();

    private static final ParticipantPositionEntity C_POS_EUR_S_123M539 = ParticipantPositionEntity.builder()
        .currencyPair(EURUSD)
        .amount(AmountEntity.of(BigDecimal.valueOf(-123539000), "EUR"))
        .price(BigDecimal.valueOf(1.09))
        .participant(PARTICIPANT_C)
        .build();

    private static final ParticipantPositionEntity D_POS_EUR_S_47M = ParticipantPositionEntity.builder()
        .currencyPair(EURUSD)
        .amount(AmountEntity.of(BigDecimal.valueOf(-47000000), "EUR"))
        .price(BigDecimal.valueOf(1.09))
        .participant(PARTICIPANT_D)
        .build();

    private static final ParticipantPositionEntity D_POS_USD_S_47M = ParticipantPositionEntity.builder()
        .currencyPair(USDJPY)
        .amount(AmountEntity.of(BigDecimal.valueOf(-47000000), "USD"))
        .price(BigDecimal.valueOf(99))
        .participant(PARTICIPANT_D)
        .build();

    private static final ParticipantPositionEntity C_POS_USD_L_37M7 = ParticipantPositionEntity.builder()
        .currencyPair(USDJPY)
        .amount(AmountEntity.of(BigDecimal.valueOf(37700000), "USD"))
        .price(BigDecimal.valueOf(99))
        .participant(PARTICIPANT_C)
        .build();

    @Autowired
    private EODCalculator eodCalculator;

    @Autowired
    private TradeOrPositionEssentialsMapper tradeOrPositionMapper;

    @Test
    void shouldCalculateNetAmounts() {
        Stream<ParticipantCurrencyPairAmount> mtm =
            eodCalculator.netAll(
                Stream.of(A_BUYS_20K_USD, A_SELLS_10K_USD, B_SELLS_20K_EUR, A_SELLS_30K_EUR)
                    .map(tradeOrPositionMapper::convertTrade)
            );

        assertThat(mtm)
            .extracting(
                ParticipantCurrencyPairAmount::getParticipant,
                ParticipantCurrencyPairAmount::getCurrencyPair,
                ParticipantCurrencyPairAmount::getAmount
            )
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, EURUSD, BigDecimal.valueOf(-30000)),
                tuple(PARTICIPANT_A, USDJPY, BigDecimal.valueOf(10000)),
                tuple(PARTICIPANT_B, EURUSD, BigDecimal.valueOf(-20000))
            );
    }

    @Test
    void shouldCalculateSwapPoint() {
        assertThat(
            eodCalculator.calculateAndAggregateSwapPnL(
                Stream.of(A_BUYS_20K_USD, A_SELLS_10K_USD, B_SELLS_20K_EUR, A_SELLS_30K_EUR),
                SWP_PNT_MAP::get,
                JPY_PRICE_MAP::get)
        ).extracting(
            ParticipantCurrencyPairAmount::getParticipant,
            ParticipantCurrencyPairAmount::getCurrencyPair,
            ParticipantCurrencyPairAmount::getAmount
        ).containsExactlyInAnyOrder(
            tuple(PARTICIPANT_A, EURUSD, BigDecimal.valueOf(920)),
            tuple(PARTICIPANT_A, USDJPY, BigDecimal.ZERO),
            tuple(PARTICIPANT_B, EURUSD, BigDecimal.valueOf(613))
        );
    }

    @Test
    void shouldRoundPositiveFractionalPYDown() {

        final Stream<TradeEntity> trades = Stream.of(A_SELLS_1_USD_AT_991);
        assertThat(eodCalculator.calculateAndAggregateInitialMtm(trades, PRICE_MAP::get, JPY_PRICE_MAP::get))
            .extracting(
                ParticipantCurrencyPairAmount::getParticipant,
                ParticipantCurrencyPairAmount::getCurrencyPair,
                ParticipantCurrencyPairAmount::getAmount
            ).containsExactly(
            tuple(PARTICIPANT_A, USDJPY, BigDecimal.ZERO)
        );

    }

    @Test
    void shouldRoundNegativeFractionalPYUp() {

        final Stream<TradeEntity> trades = Stream.of(A_BUYS_1_USD_AT_991);
        assertThat(eodCalculator.calculateAndAggregateInitialMtm(trades, PRICE_MAP::get, JPY_PRICE_MAP::get))
            .extracting(
                ParticipantCurrencyPairAmount::getParticipant,
                ParticipantCurrencyPairAmount::getCurrencyPair,
                ParticipantCurrencyPairAmount::getAmount
            ).containsExactly(
            tuple(PARTICIPANT_A, USDJPY, BigDecimal.valueOf(-1))
        );

    }

    @Test
    void shouldCalculateAndAggregateMultipleTrades() {
        final Stream<TradeEntity> trades = Stream.of(A_BUYS_10K_EUR, A_BUYS_20K_USD, A_SELLS_10K_USD, B_SELLS_20K_EUR);
        assertThat(eodCalculator.calculateAndAggregateInitialMtm(trades, PRICE_MAP::get, JPY_PRICE_MAP::get))
            .extracting(ParticipantCurrencyPairAmount::getParticipant, ParticipantCurrencyPairAmount::getCurrencyPair, ParticipantCurrencyPairAmount::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, EURUSD, BigDecimal.valueOf(-99000)),
                tuple(PARTICIPANT_A, USDJPY, BigDecimal.valueOf(-4000)),
                tuple(PARTICIPANT_B, EURUSD, BigDecimal.valueOf(198000))
            );
    }

    @Test
    void shouldCalculateAndAggregateMultiplePositions() {
        Stream<ParticipantCurrencyPairAmount> mtm =
            eodCalculator.calculateAndAggregateDailyMtm(Stream.of(A_POS_EUR_L_100K, A_POS_USD_L_100K), PRICE_MAP::get, JPY_PRICE_MAP::get);

        assertThat(mtm)
            .extracting(
                ParticipantCurrencyPairAmount::getParticipant,
                ParticipantCurrencyPairAmount::getCurrencyPair,
                ParticipantCurrencyPairAmount::getAmount
            )
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, EURUSD, BigDecimal.valueOf(99000)),
                tuple(PARTICIPANT_A, USDJPY, BigDecimal.valueOf(-30000))
            );
    }

    @Test
    void shouldRebalancePositions() {
        Map<@NonNull CurrencyPairEntity, List<BalanceTrade>> balanceTrades =
            eodCalculator.rebalanceLPPositions(
                Stream.of(A_POS_EUR_L_212M, B_POS_EUR_L_30M, C_POS_EUR_S_123M539, D_POS_EUR_S_47M, C_POS_USD_L_37M7, D_POS_USD_S_47M),
                Map.of(EURUSD, 100000L, USDJPY, 100000L)
            );

        assertThat(balanceTrades.get(EURUSD))
            .extracting(BalanceTrade::getOriginator, BalanceTrade::getCounterparty, trade -> trade.getAmount().intValue())
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, PARTICIPANT_C, -123539000),
                tuple(PARTICIPANT_A, PARTICIPANT_D, -25900000),
                tuple(PARTICIPANT_B, PARTICIPANT_D, -21100000)
            );
        assertThat(balanceTrades.get(USDJPY))
            .extracting(BalanceTrade::getOriginator, BalanceTrade::getCounterparty, trade -> trade.getAmount().intValue())
            .containsOnly(
                tuple(PARTICIPANT_D, PARTICIPANT_C, 37700000)
            );
    }

    @Test
    void shouldAggregatePositions() {
        assertThat(
            eodCalculator.aggregatePositions(Stream.of(A_POS_EUR_L_100K, A_POS_EUR_L_212M, A_POS_USD_L_100K, B_POS_EUR_L_30M))
        )
            .extracting(
                ParticipantCurrencyPairAmount::getParticipant,
                ParticipantCurrencyPairAmount::getCurrencyPair,
                participantCcyAmount -> participantCcyAmount.getAmount().intValue()
            ).containsExactlyInAnyOrder(
            tuple(PARTICIPANT_A, EURUSD, 212100000),
            tuple(PARTICIPANT_A, USDJPY, 100000),
            tuple(PARTICIPANT_B, EURUSD, 30000000)
        );

    }

    @Test
    void shouldCalculateRequiredInitialMargin() {
        final Stream<ParticipantPositionEntity> positions = Stream.of(
            position("USD", "JPY", 761000L),
            position("EUR", "JPY", 442000L),
            position("GBP", "JPY", 669000L),
            position("AUD", "JPY", 1940000L),
            position("CAD", "JPY", 421000L),
            position("NZD", "JPY", -300000L),
            position("TRY", "JPY", 915000L),
            position("NOK", "JPY", -44000L),
            position("HKD", "JPY", 1563000L),
            position("SEK", "JPY", 766000L),
            position("MXN", "JPY", 817000L),
            position("EUR", "USD", 1532000L),
            position("EUR", "CHF", 315000L),
            position("EUR", "GBP", 1478000L),
            position("NZD", "USD", 728000L),
            position("EUR", "AUD", 237000L),
            position("GBP", "AUD", -462000L),
            position("AUD", "NZD", 673000L),
            position("NZD", "CHF", 456000L)
        );
        final Map<CurrencyPairEntity, BigDecimal> marginRatios = ImmutableMap.<CurrencyPairEntity, BigDecimal>builder()
            .put(CurrencyPairEntity.of(1L, "USD", "JPY"), BigDecimal.valueOf(7))
            .put(CurrencyPairEntity.of(1L, "EUR", "JPY"), BigDecimal.valueOf(6))
            .put(CurrencyPairEntity.of(1L, "GBP", "JPY"), BigDecimal.valueOf(4))
            .put(CurrencyPairEntity.of(1L, "AUD", "JPY"), BigDecimal.valueOf(5))
            .put(CurrencyPairEntity.of(1L, "CAD", "JPY"), BigDecimal.valueOf(6))
            .put(CurrencyPairEntity.of(1L, "NZD", "JPY"), BigDecimal.valueOf(5))
            .put(CurrencyPairEntity.of(1L, "TRY", "JPY"), BigDecimal.valueOf(7))
            .put(CurrencyPairEntity.of(1L, "NOK", "JPY"), BigDecimal.valueOf(6))
            .put(CurrencyPairEntity.of(1L, "HKD", "JPY"), BigDecimal.valueOf(7))
            .put(CurrencyPairEntity.of(1L, "SEK", "JPY"), BigDecimal.valueOf(6))
            .put(CurrencyPairEntity.of(1L, "MXN", "JPY"), BigDecimal.valueOf(5))
            .put(CurrencyPairEntity.of(1L, "EUR", "USD"), BigDecimal.valueOf(5))
            .put(CurrencyPairEntity.of(1L, "EUR", "CHF"), BigDecimal.valueOf(6))
            .put(CurrencyPairEntity.of(1L, "EUR", "GBP"), BigDecimal.valueOf(6))
            .put(CurrencyPairEntity.of(1L, "NZD", "USD"), BigDecimal.valueOf(4))
            .put(CurrencyPairEntity.of(1L, "EUR", "AUD"), BigDecimal.valueOf(7))
            .put(CurrencyPairEntity.of(1L, "GBP", "AUD"), BigDecimal.valueOf(6))
            .put(CurrencyPairEntity.of(1L, "AUD", "NZD"), BigDecimal.valueOf(5))
            .put(CurrencyPairEntity.of(1L, "NZD", "CHF"), BigDecimal.valueOf(7))
            .build();
        final BiFunction<CurrencyPairEntity, ParticipantEntity, BigDecimal> marginRatioResolver =
            (pairEntity, participantEntity) -> marginRatios.get(pairEntity);

        final Map<String, BigDecimal> jpyRates = ImmutableMap.<String, BigDecimal>builder()
            .put("EUR", BigDecimal.valueOf(124.05))
            .put("USD", BigDecimal.valueOf(110.74))
            .put("GBP", BigDecimal.valueOf(146.5))
            .put("AUD", BigDecimal.valueOf(73.55))
            .put("CAD", BigDecimal.valueOf(84.19))
            .put("NZD", BigDecimal.valueOf(76.51))
            .put("TRY", BigDecimal.valueOf(17.339))
            .put("NOK", BigDecimal.valueOf(12.219))
            .put("HKD", BigDecimal.valueOf(15.637))
            .put("SEK", BigDecimal.valueOf(11.63))
            .put("MXN", BigDecimal.valueOf(7.09))
            .put("CHF", BigDecimal.valueOf(108.22))
            .build();

        final Map<ParticipantEntity, BigDecimal> initialMargin =
            eodCalculator.calculateRequiredInitialMargin(positions, marginRatioResolver, (baseCcy, valueCCy) -> jpyRates.get(baseCcy));

        assertThat(initialMargin)
            .hasSize(1)
            .containsValue(BigDecimal.valueOf(63307344L));
    }

    @Test
    void shouldAggregateRequiredMargin() {

        final LocalDate today = LocalDate.of(2020, 10, 10);
        final LocalDate tomorrow = LocalDate.of(2020, 10, 11);
        final LocalDate dayAfterTomorrow = LocalDate.of(2020, 10, 12);

        final EodProductCashSettlementEntity dailyMtmTB = EodProductCashSettlementEntity.builder()
            .amount(AmountEntity.builder().value(BigDecimal.valueOf(100)).currency("JPY").build())
            .currencyPair(EURUSD)
            .participant(PARTICIPANT_B)
            .settlementDate(tomorrow)
            .type(EodProductCashSettlementType.DAILY_MTM)
            .build();
        final EodProductCashSettlementEntity initialMtmT = EodProductCashSettlementEntity.builder()
            .amount(AmountEntity.builder().value(BigDecimal.valueOf(100)).currency("JPY").build())
            .currencyPair(EURUSD)
            .participant(PARTICIPANT_A)
            .settlementDate(today)
            .type(EodProductCashSettlementType.INITIAL_MTM)
            .build();
        final EodProductCashSettlementEntity dailyMtmT11 = EodProductCashSettlementEntity.builder()
            .amount(AmountEntity.builder().value(BigDecimal.valueOf(200)).currency("JPY").build())
            .currencyPair(EURUSD)
            .participant(PARTICIPANT_A)
            .settlementDate(tomorrow)
            .type(EodProductCashSettlementType.DAILY_MTM)
            .build();
        final EodProductCashSettlementEntity dailyMtmT12 = EodProductCashSettlementEntity.builder()
            .amount(AmountEntity.builder().value(BigDecimal.valueOf(120)).currency("JPY").build())
            .currencyPair(EURUSD)
            .participant(PARTICIPANT_A)
            .settlementDate(tomorrow)
            .type(EodProductCashSettlementType.DAILY_MTM)
            .build();
        final EodProductCashSettlementEntity dailyMtmT2 = EodProductCashSettlementEntity.builder()
            .amount(AmountEntity.builder().value(BigDecimal.valueOf(200)).currency("JPY").build())
            .currencyPair(EURUSD)
            .participant(PARTICIPANT_A)
            .settlementDate(dayAfterTomorrow)
            .type(EodProductCashSettlementType.DAILY_MTM)
            .build();
        final EodProductCashSettlementEntity dailyMtmT3 = EodProductCashSettlementEntity.builder()
            .amount(AmountEntity.builder().value(BigDecimal.valueOf(50)).currency("JPY").build())
            .currencyPair(EURUSD)
            .participant(PARTICIPANT_A)
            .settlementDate(dayAfterTomorrow.plusDays(1))
            .type(EodProductCashSettlementType.DAILY_MTM)
            .build();

        final Map<ParticipantEntity, Map<EodProductCashSettlementType, EnumMap<EodCashSettlementDateType, BigDecimal>>> participantEntityMapMap =
            eodCalculator.aggregateRequiredMargin(List.of(dailyMtmTB, initialMtmT, dailyMtmT11, dailyMtmT12, dailyMtmT2, dailyMtmT3),
                Optional.of(tomorrow), Optional.of(dayAfterTomorrow));

        assertThat(participantEntityMapMap)
            .extracting(
                map -> map.get(PARTICIPANT_A).get(EodProductCashSettlementType.DAILY_MTM).get(EodCashSettlementDateType.DAY),
                map -> map.get(PARTICIPANT_A).get(EodProductCashSettlementType.DAILY_MTM).get(EodCashSettlementDateType.FOLLOWING),
                map -> map.get(PARTICIPANT_A).get(EodProductCashSettlementType.DAILY_MTM).get(EodCashSettlementDateType.TOTAL))
            .containsExactly(BigDecimal.valueOf(320), BigDecimal.valueOf(200), BigDecimal.valueOf(570));
    }

    private static ParticipantPositionEntity position(final String baseCurrency, final String valueCurrency, final long amount) {
        return ParticipantPositionEntity.builder()
            .currencyPair(
                CurrencyPairEntity.builder()
                    .id(1L)
                    .baseCurrency(baseCurrency)
                    .valueCurrency(valueCurrency)
                    .build()
            )
            .amount(AmountEntity.of(BigDecimal.valueOf(amount), baseCurrency))
            .participant(PARTICIPANT_A)
            .price(BigDecimal.ZERO)
            .build();
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = { EODCalculator.class, TradeOrPositionEssentialsMapper.class, SingleCurrencyRebalanceCalculator.class },
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = { EODCalculator.class, TradeOrPositionEssentialsMapper.class, SingleCurrencyRebalanceCalculator.class })
    )
    static class TestConfig {

    }

}