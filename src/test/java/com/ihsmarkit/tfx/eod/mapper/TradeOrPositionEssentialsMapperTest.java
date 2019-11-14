package com.ihsmarkit.tfx.eod.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ihsmarkit.tfx.core.dl.EntityTestDataFactory;
import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.LegalEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.domain.type.Side;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;

@ExtendWith(SpringExtension.class)
class TradeOrPositionEssentialsMapperTest {

    private static final CurrencyPairEntity CURRENCY_PAIR = EntityTestDataFactory.aCurrencyPairEntityBuilder().build();
    private static final ParticipantEntity PARTICIPANT_A = EntityTestDataFactory.aParticipantEntityBuilder().name("A").build();
    private static final LegalEntity ORIGINATOR_A = EntityTestDataFactory.aLegalEntityBuilder()
            .participant(PARTICIPANT_A)
            .build();

    private static final TradeEntity.TradeEntityBuilder TRADE = TradeEntity.builder()
            .currencyPair(CURRENCY_PAIR)
            .spotRate(BigDecimal.valueOf(99.5))
            .baseAmount(AmountEntity.of(BigDecimal.valueOf(20.0), "USD"))
            .originator(ORIGINATOR_A);

    @Autowired
    private TradeOrPositionEssentialsMapper mapper;

    @Test
    void shouldMapBuyTrade() {
        TradeOrPositionEssentials mapped = mapper.convertTrade(TRADE.direction(Side.BUY).build());

        assertThat(mapped.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
        assertThat(mapped.getSpotRate()).isEqualByComparingTo(BigDecimal.valueOf(99.5));
        assertThat(mapped.getCurrencyPair()).isSameAs(CURRENCY_PAIR);
        assertThat(mapped.getParticipant()).isSameAs(PARTICIPANT_A);

    }

    @Test
    void shouldMapSellTrade() {
        TradeOrPositionEssentials mapped = mapper.convertTrade(TRADE.direction(Side.SELL).build());

        assertThat(mapped.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(-20.0));
        assertThat(mapped.getSpotRate()).isEqualByComparingTo(BigDecimal.valueOf(99.5));
        assertThat(mapped.getCurrencyPair()).isSameAs(CURRENCY_PAIR);
        assertThat(mapped.getParticipant()).isSameAs(PARTICIPANT_A);

    }

    @Test
    void shouldMapPosition() {
        ParticipantPositionEntity position = ParticipantPositionEntity.builder()
                .amount(AmountEntity.of(BigDecimal.valueOf(20.0), "USD"))
                .currencyPair(CURRENCY_PAIR)
                .participant(PARTICIPANT_A)
                .price(BigDecimal.valueOf(99.5))
                .build();

        TradeOrPositionEssentials mapped = mapper.convertPosition(position);

        assertThat(mapped.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
        assertThat(mapped.getSpotRate()).isEqualByComparingTo(BigDecimal.valueOf(99.5));
        assertThat(mapped.getCurrencyPair()).isSameAs(CURRENCY_PAIR);
        assertThat(mapped.getParticipant()).isSameAs(PARTICIPANT_A);

    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = TradeOrPositionEssentialsMapper.class)
    static class TestConfig {

    }

}
