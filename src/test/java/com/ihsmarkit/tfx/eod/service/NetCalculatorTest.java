package com.ihsmarkit.tfx.eod.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
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
import com.ihsmarkit.tfx.core.domain.type.Side;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.ParticipantPositionForPair;

@ExtendWith(SpringExtension.class)
public class NetCalculatorTest {

    private static final CurrencyPairEntity USDJPY = CurrencyPairEntity.of(1L, "USD", "JPY");
    private static final CurrencyPairEntity EURUSD = CurrencyPairEntity.of(2L, "EUR", "USD");

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

    private static final TradeEntity B_SELLS_20_EUR = TradeEntity.builder()
            .direction(Side.SELL)
            .currencyPair(EURUSD)
            .spotRate(BigDecimal.valueOf(1.2))
            .baseAmount(AmountEntity.of(BigDecimal.valueOf(20), "EUR"))
            .originator(ORIGINATOR_B)
            .build();

    private static final TradeEntity A_SELLS_30_EUR = TradeEntity.builder()
            .direction(Side.SELL)
            .currencyPair(EURUSD)
            .spotRate(BigDecimal.valueOf(1.2))
            .baseAmount(AmountEntity.of(BigDecimal.valueOf(30), "EUR"))
            .originator(ORIGINATOR_A)
            .build();

    @Autowired
    private NetCalculator netCalculator;

    @Test
    void shouldCalculateNetAmounts() {
        Stream<ParticipantPositionForPair> mtm =
                netCalculator.netAllTtrades(Stream.of(A_BUYS_20_USD, A_SELLS_10_USD, B_SELLS_20_EUR, A_SELLS_30_EUR));

        assertThat(mtm).extracting(ParticipantPositionForPair::getParticipant, ParticipantPositionForPair::getCurrencyPair, ParticipantPositionForPair::getAmount)
                .containsExactlyInAnyOrder(
                        tuple(PARTICIPANT_A, EURUSD, BigDecimal.valueOf(-30)),
                        tuple(PARTICIPANT_A, USDJPY, BigDecimal.valueOf(10.0)),
                        tuple(PARTICIPANT_B, EURUSD, BigDecimal.valueOf(-20))
                );
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = { NetCalculator.class, TradeOrPositionEssentialsMapper.class },
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = { NetCalculator.class, TradeOrPositionEssentialsMapper.class })
    )
    static class TestConfig {

    }

}