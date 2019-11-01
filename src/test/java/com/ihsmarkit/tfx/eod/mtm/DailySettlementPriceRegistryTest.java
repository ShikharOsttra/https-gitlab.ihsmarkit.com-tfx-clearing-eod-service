package com.ihsmarkit.tfx.eod.mtm;

import static org.assertj.core.api.Assertions.assertThat;

import com.ihsmarkit.tfx.core.dl.EntityTestDataFactory;
import com.ihsmarkit.tfx.core.dl.entity.*;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.domain.type.Side;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class DailySettlementPriceRegistryTest {

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

    @Test
    public void shouldCalculatePositiveMTMAmountForSell() {
        var out = new DailySettlementPriceRegistry(PRICE_MAP);

        TradeEntity trade = TradeEntity.builder()
                .direction(Side.SELL)
                .currencyPair(USDJPY)
                .spotRate(BigDecimal.valueOf(99.5))
                .baseAmount(AmountEntity.of(BigDecimal.TEN, "USD"))
                .originator(ORIGINATOR_A)
                .build();

        TradeMTM mtm = out.calculateMTM(A_SELLS_10_USD);

        assertThat(mtm.getCurrencyPair()).isSameAs(USDJPY);
        assertThat(mtm.getParticipant()).isSameAs(PARTICIPANT_A);
        assertThat(mtm.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(6.0));

    }

    @Test
    public void shouldCalculateNegativeMTMAmountForBuy() {
        var out = new DailySettlementPriceRegistry(PRICE_MAP);

        TradeMTM mtm = out.calculateMTM(A_BUYS_20_USD);

        assertThat(mtm.getCurrencyPair()).isSameAs(USDJPY);
        assertThat(mtm.getParticipant()).isSameAs(PARTICIPANT_A);
        assertThat(mtm.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(-10.0));

    }

    @Test
    public void shouldRoundPositiveFractionalPYDown() {
        var out = new DailySettlementPriceRegistry(PRICE_MAP);

        TradeMTM mtm = out.calculateMTM(A_SELLS_1_USD_AT_991);

        assertThat(mtm.getCurrencyPair()).isSameAs(USDJPY);
        assertThat(mtm.getParticipant()).isSameAs(PARTICIPANT_A);
        assertThat(mtm.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);

    }

    @Test
    public void shouldRoundNegativeFractionalPYUp() {
        var out = new DailySettlementPriceRegistry(PRICE_MAP);

        TradeMTM mtm = out.calculateMTM(A_BUYS_1_USD_AT_99001);

        assertThat(mtm.getCurrencyPair()).isSameAs(USDJPY);
        assertThat(mtm.getParticipant()).isSameAs(PARTICIPANT_A);
        assertThat(mtm.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(-1));

    }

    @Test
    public void shouldConvertoTOJPYForNotJPYTrades() {
        var out = new DailySettlementPriceRegistry(PRICE_MAP);

        TradeMTM mtm = out.calculateMTM(A_BUYS_10_EUR);

        assertThat(mtm.getCurrencyPair()).isSameAs(EURUSD);
        assertThat(mtm.getParticipant()).isSameAs(PARTICIPANT_A);
        assertThat(mtm.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(-99.0));

    }


    @Test
    public void shouldCalculateAndAggregateMultipleTrades() {
        var out = new DailySettlementPriceRegistry(PRICE_MAP);

        Stream<TradeMTM> mtm =
                out.calculateAndAggregateInitialMTM(Stream.of(A_BUYS_10_EUR, A_BUYS_20_USD, A_SELLS_10_USD, B_SELLS_20_EUR));

        assertThat(mtm)
                .anySatisfy(
                        a-> Assertions.assertAll(
                                () -> assertThat(a.getParticipant()).isSameAs(PARTICIPANT_A),
                                () -> assertThat(a.getCurrencyPair()).isSameAs(EURUSD),
                                () -> assertThat(a.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(-99))
                        )
                )
                .anySatisfy(
                        a-> Assertions.assertAll(
                                () -> assertThat(a.getParticipant()).isSameAs(PARTICIPANT_A),
                                () -> assertThat(a.getCurrencyPair()).isSameAs(USDJPY),
                                () -> assertThat(a.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(-4))
                        )
                )
                .anySatisfy(
                        a-> Assertions.assertAll(
                                () -> assertThat(a.getParticipant()).isSameAs(PARTICIPANT_B),
                                () -> assertThat(a.getCurrencyPair()).isSameAs(EURUSD),
                                () -> assertThat(a.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(198))
                        )
                )
                .hasSize(3);

    }

    @Test
    public void shouldCalculateAndAggregateMultiplePositions() {
        var out = new DailySettlementPriceRegistry(PRICE_MAP);

        Stream<TradeMTM> mtm =
                out.calculateAndAggregateDailyMTM(List.of(A_POSITION_EUR, A_POSITION_USD));

        assertThat(mtm)
                .anySatisfy(
                        a-> Assertions.assertAll(
                                () -> assertThat(a.getParticipant()).isSameAs(PARTICIPANT_A),
                                () -> assertThat(a.getCurrencyPair()).isSameAs(EURUSD),
                                () -> assertThat(a.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(99000))
                        )
                    )
                .anySatisfy(
                        a-> Assertions.assertAll(
                            () -> assertThat(a.getParticipant()).isSameAs(PARTICIPANT_A),
                            () -> assertThat(a.getCurrencyPair()).isSameAs(USDJPY),
                            () -> assertThat(a.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(-30000))
                        )
                    )
                .hasSize(2);

    }
}