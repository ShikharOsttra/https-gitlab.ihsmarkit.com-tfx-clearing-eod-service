package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aLegalEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.LegalEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.domain.type.Side;
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

    private static final TradeEntity A_BUYS_20_USD = TradeEntity.builder()
        .direction(Side.BUY)
        .currencyPair(USDJPY)
        .spotRate(BigDecimal.valueOf(99.5))
        .baseAmount(AmountEntity.of(BigDecimal.valueOf(20.0), "USD"))
        .originator(ORIGINATOR_A)
        .build();

    private static final TradeEntity A_SELLS_10_USD = TradeEntity.builder()
        .direction(Side.SELL)
        .currencyPair(USDJPY)
        .spotRate(BigDecimal.valueOf(99.6))
        .baseAmount(AmountEntity.of(BigDecimal.valueOf(10.0), "USD"))
        .originator(ORIGINATOR_A)
        .build();

    private static final TradeEntity A_BUYS_10_EUR = TradeEntity.builder()
        .direction(Side.BUY)
        .currencyPair(EURUSD)
        .spotRate(BigDecimal.valueOf(1.2))
        .baseAmount(AmountEntity.of(BigDecimal.TEN, "EUR"))
        .originator(ORIGINATOR_A)
        .build();

    private static final TradeEntity B_SELLS_20_EUR = TradeEntity.builder()
        .direction(Side.SELL)
        .currencyPair(EURUSD)
        .spotRate(BigDecimal.valueOf(1.2))
        .baseAmount(AmountEntity.of(BigDecimal.valueOf(20), "EUR"))
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


    private static final TradeEntity A_SELLS_30_EUR = TradeEntity.builder()
        .direction(Side.SELL)
        .currencyPair(EURUSD)
        .spotRate(BigDecimal.valueOf(1.2))
        .baseAmount(AmountEntity.of(BigDecimal.valueOf(30), "EUR"))
        .originator(ORIGINATOR_A)
        .build();

    private static final ParticipantPositionEntity A_POSITION_USD = ParticipantPositionEntity.builder()
        .currencyPair(USDJPY)
        .amount(AmountEntity.of(BigDecimal.valueOf(100000), "USD"))
        .price(BigDecimal.valueOf(99.3))
        .participant(PARTICIPANT_A)
        .build();

    private static final ParticipantPositionEntity A_POSITION_EUR = ParticipantPositionEntity.builder()
        .currencyPair(EURUSD)
        .amount(AmountEntity.of(BigDecimal.valueOf(100000), "EUR"))
        .price(BigDecimal.valueOf(1.09))
        .participant(PARTICIPANT_A)
        .build();

    private static final ParticipantPositionEntity A_POS_EUR_L_212M = ParticipantPositionEntity.builder()
        .currencyPair(EURUSD)
        .amount(AmountEntity.of(BigDecimal.valueOf(212000000), "EUR"))
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
            eodCalculator.netAllTtrades(
                Stream.of(A_BUYS_20_USD, A_SELLS_10_USD, B_SELLS_20_EUR, A_SELLS_30_EUR)
                    .map(tradeOrPositionMapper::convertTrade)
            );

        assertThat(mtm)
            .extracting(
                ParticipantCurrencyPairAmount::getParticipant,
                ParticipantCurrencyPairAmount::getCurrencyPair,
                ParticipantCurrencyPairAmount::getAmount
            )
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, EURUSD, BigDecimal.valueOf(-30)),
                tuple(PARTICIPANT_A, USDJPY, BigDecimal.valueOf(10.0)),
                tuple(PARTICIPANT_B, EURUSD, BigDecimal.valueOf(-20))
            );
    }

    @Test
    void shouldRoundPositiveFractionalPYDown() {

        final Stream<TradeEntity> trades = Stream.of(A_SELLS_1_USD_AT_991);
        assertThat(eodCalculator.calculateAndAggregateInitialMtm(trades, PRICE_MAP))
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
        assertThat(eodCalculator.calculateAndAggregateInitialMtm(trades, PRICE_MAP))
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
        final Stream<TradeEntity> trades = Stream.of(A_BUYS_10_EUR, A_BUYS_20_USD, A_SELLS_10_USD, B_SELLS_20_EUR);
        assertThat(eodCalculator.calculateAndAggregateInitialMtm(trades, PRICE_MAP))
            .extracting(ParticipantCurrencyPairAmount::getParticipant, ParticipantCurrencyPairAmount::getCurrencyPair, ParticipantCurrencyPairAmount::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, EURUSD, BigDecimal.valueOf(-99)),
                tuple(PARTICIPANT_A, USDJPY, BigDecimal.valueOf(-4)),
                tuple(PARTICIPANT_B, EURUSD, BigDecimal.valueOf(198))
            );
    }

    @Test
    void shouldCalculateAndAggregateMultiplePositions() {
        Stream<ParticipantCurrencyPairAmount> mtm =
            eodCalculator.calculateAndAggregateDailyMtm(List.of(A_POSITION_EUR, A_POSITION_USD), PRICE_MAP);

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
                Stream.of(A_POS_EUR_L_212M, B_POS_EUR_L_30M, C_POS_EUR_S_123M539, D_POS_EUR_S_47M, C_POS_USD_L_37M7, D_POS_USD_S_47M)
            );

        assertThat(balanceTrades.get(EURUSD))
            .extracting(BalanceTrade::getOriginator, BalanceTrade::getCounterparty, trade -> trade.getAmount().toPlainString())
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, PARTICIPANT_C, "-123539000"),
                tuple(PARTICIPANT_A, PARTICIPANT_D, "-25761000"),
                tuple(PARTICIPANT_B, PARTICIPANT_D, "-21100000"),
                tuple(PARTICIPANT_A, PARTICIPANT_D, "-100000")
            );
        assertThat(balanceTrades.get(USDJPY))
            .extracting(BalanceTrade::getOriginator, BalanceTrade::getCounterparty, trade -> trade.getAmount().toPlainString())
            .containsOnly(
                tuple(PARTICIPANT_D, PARTICIPANT_C, "37700000")
            );
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = { EODCalculator.class, TradeOrPositionEssentialsMapper.class },
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = { EODCalculator.class, TradeOrPositionEssentialsMapper.class })
    )
    static class TestConfig {

    }

}