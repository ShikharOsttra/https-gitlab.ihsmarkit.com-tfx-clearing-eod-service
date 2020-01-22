package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.eod.model.BalanceTrade;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;

public class SingleCurrencyRebalanceCalculatorTest {

    private static final CurrencyPairEntity USDJPY = CurrencyPairEntity.builder().baseCurrency("USD").valueCurrency("JPY").build();

    private SingleCurrencyRebalanceCalculator rebalanceCalculator = new SingleCurrencyRebalanceCalculator();

    @Test
    void shouldRebalanceRDCase() {

        final List<TradeOrPositionEssentials> positions = List.of(
            TradeOrPositionEssentials.builder()
                .currencyPair(USDJPY)
                .participant(aParticipantEntityBuilder()
                    .code("LPA")
                    .name("LPA-name")
                    .build())
                .amount(BigDecimal.valueOf(212_000_000))
                .spotRate(BigDecimal.TEN)
                .build(),
            TradeOrPositionEssentials.builder()
                .currencyPair(USDJPY)
                .participant(aParticipantEntityBuilder()
                    .code("LPB")
                    .name("LPB-name")
                    .build())
                .amount(BigDecimal.valueOf(30_000_000))
                .spotRate(BigDecimal.TEN)
                .build(),
            TradeOrPositionEssentials.builder()
                .currencyPair(USDJPY)
                .participant(aParticipantEntityBuilder()
                    .code("LPC")
                    .name("LPC-name")
                    .build())
                .amount(BigDecimal.valueOf(-123_539_000))
                .spotRate(BigDecimal.TEN)
                .build(),
            TradeOrPositionEssentials.builder()
                .currencyPair(USDJPY)
                .participant(aParticipantEntityBuilder()
                    .code("LPD")
                    .name("LPD-name")
                    .build())
                .amount(BigDecimal.valueOf(-47_000_000))
                .spotRate(BigDecimal.TEN)
                .build());

        final List<BalanceTrade> rebalance = rebalanceCalculator.rebalance(positions, BigDecimal.valueOf(100_000), 5);

        assertThat(rebalance).hasSize(3)
            .extracting(
                trade -> trade.getOriginator().getCode(),
                trade -> trade.getCounterparty().getCode(),
                BalanceTrade::getAmount
            ).contains(
            Tuple.tuple("LPA", "LPC", BigDecimal.valueOf(-123_539_000)),
            Tuple.tuple("LPA", "LPD", BigDecimal.valueOf(-25_900_000)),
            Tuple.tuple("LPB", "LPD", BigDecimal.valueOf(-21_100_000))
        );

    }
}
