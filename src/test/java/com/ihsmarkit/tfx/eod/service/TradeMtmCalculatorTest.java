package com.ihsmarkit.tfx.eod.service;

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

import com.ihsmarkit.tfx.core.dl.EntityTestDataFactory;
import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.LegalEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.domain.type.Side;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.MarkToMarketTrade;

@ExtendWith(SpringExtension.class)
class TradeMtmCalculatorTest {

    private static final CurrencyPairEntity USDJPY = CurrencyPairEntity.of(1L, "USD", "JPY");
    private static final CurrencyPairEntity EURUSD = CurrencyPairEntity.of(2L, "EUR", "USD");

    private static final Map<CurrencyPairEntity, BigDecimal> PRICE_MAP = Map.of(
        USDJPY, BigDecimal.valueOf(99.0),
        EURUSD, BigDecimal.valueOf(1.1)
    );

    private static final ParticipantEntity PARTICIPANT_A = EntityTestDataFactory.aParticipantEntityBuilder().name("A").build();
    private static final LegalEntity ORIGINATOR_A = EntityTestDataFactory.aLegalEntityBuilder()
        .participant(PARTICIPANT_A)
        .build();

    private static final ParticipantEntity PARTICIPANT_B = EntityTestDataFactory.aParticipantEntityBuilder().name("B").build();
    private static final LegalEntity ORIGINATOR_B = EntityTestDataFactory.aLegalEntityBuilder()
        .participant(PARTICIPANT_B)
        .build();

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

    private static final TradeEntity A_BUYS_1_USD_AT_99001 = TradeEntity.builder()
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

    @Autowired
    private TradeMtmCalculator tradeMtmCalculator;

    @Test
    void shouldRoundPositiveFractionalPYDown() {

        final Stream<TradeEntity> trades = Stream.of(A_SELLS_1_USD_AT_991);
        Stream<MarkToMarketTrade> mtm = tradeMtmCalculator.calculateAndAggregateInitialMtm(trades, PRICE_MAP);

        assertThat(mtm)
                .anySatisfy(
                        a -> Assertions.assertAll(
                                () -> assertThat(a.getParticipant()).isSameAs(PARTICIPANT_A),
                                () -> assertThat(a.getCurrencyPair()).isSameAs(USDJPY),
                                () -> assertThat(a.getAmount()).isEqualByComparingTo(BigDecimal.ZERO)
                        )
                )
                .hasSize(1);

    }

    @Test
    void shouldRoundNegativeFractionalPYUp() {

        final Stream<TradeEntity> trades = Stream.of(A_BUYS_1_USD_AT_99001);
        Stream<MarkToMarketTrade> mtm = tradeMtmCalculator.calculateAndAggregateInitialMtm(trades, PRICE_MAP);

        assertThat(mtm)
                .anySatisfy(
                        a -> Assertions.assertAll(
                                () -> assertThat(a.getParticipant()).isSameAs(PARTICIPANT_A),
                                () -> assertThat(a.getCurrencyPair()).isSameAs(USDJPY),
                                () -> assertThat(a.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(-1))
                        )
                )
                .hasSize(1);

    }

    @Test
    void shouldCalculateAndAggregateMultipleTrades() {
        final Stream<TradeEntity> trades = Stream.of(A_BUYS_10_EUR, A_BUYS_20_USD, A_SELLS_10_USD, B_SELLS_20_EUR);
        assertThat(tradeMtmCalculator.calculateAndAggregateInitialMtm(trades, PRICE_MAP))
            .extracting(MarkToMarketTrade::getParticipant, MarkToMarketTrade::getCurrencyPair, MarkToMarketTrade::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, EURUSD, BigDecimal.valueOf(-99)),
                tuple(PARTICIPANT_A, USDJPY, BigDecimal.valueOf(-4)),
                tuple(PARTICIPANT_B, EURUSD, BigDecimal.valueOf(198))
            );
    }

    @Test
    void shouldCalculateAndAggregateMultiplePositions() {
        Stream<MarkToMarketTrade> mtm =
            tradeMtmCalculator.calculateAndAggregateDailyMtm(List.of(A_POSITION_EUR, A_POSITION_USD), PRICE_MAP);

        assertThat(mtm).extracting(MarkToMarketTrade::getParticipant, MarkToMarketTrade::getCurrencyPair, MarkToMarketTrade::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, EURUSD, BigDecimal.valueOf(99000)),
                tuple(PARTICIPANT_A, USDJPY, BigDecimal.valueOf(-30000))
            );
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = { TradeMtmCalculator.class, TradeOrPositionEssentialsMapper.class },
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = { TradeMtmCalculator.class, TradeOrPositionEssentialsMapper.class })
    )
    static class TestConfig {

    }

}