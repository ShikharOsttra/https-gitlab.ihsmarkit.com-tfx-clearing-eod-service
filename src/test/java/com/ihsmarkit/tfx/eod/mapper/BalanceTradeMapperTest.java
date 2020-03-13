package com.ihsmarkit.tfx.eod.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

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
import com.ihsmarkit.tfx.core.domain.type.ClearingStatus;
import com.ihsmarkit.tfx.core.domain.type.MatchingStatus;
import com.ihsmarkit.tfx.core.domain.type.Side;
import com.ihsmarkit.tfx.core.domain.type.TradeActivity;
import com.ihsmarkit.tfx.eod.model.BalanceTrade;

@ExtendWith(SpringExtension.class)
class BalanceTradeMapperTest {

    private static final LocalDateTime CURRENT_TSP = LocalDateTime.of(2019, 1, 1, 1, 1);
    private static final CurrencyPairEntity CURRENCY_PAIR = EntityTestDataFactory.aCurrencyPairEntityBuilder().build();
    private static final LegalEntity ORIGINATOR_A = EntityTestDataFactory.aLegalEntityBuilder().name("A-LE").build();
    private static final LegalEntity ORIGINATOR_B = EntityTestDataFactory.aLegalEntityBuilder().name("B-LE").build();
    private static final LocalDate NOVEMBER_13 = LocalDate.of(2019, 11, 13);
    private static final LocalDate NOVEMBER_15 = LocalDate.of(2019, 11, 15);

    private static final ParticipantEntity PARTICIPANT_A = EntityTestDataFactory.aParticipantEntityBuilder()
        .name("A")
        .legalEntities(Collections.singletonList(ORIGINATOR_A))
        .build();

    private static final ParticipantEntity PARTICIPANT_B = EntityTestDataFactory.aParticipantEntityBuilder()
        .name("B")
        .legalEntities(Collections.singletonList(ORIGINATOR_B))
        .build();


    @Autowired
    private BalanceTradeMapper mapper;

    @Test
    void shouldMapToTrade() {
        TradeEntity tradeEntity = mapper.toTrade(
            new BalanceTrade(PARTICIPANT_A, PARTICIPANT_B, BigDecimal.valueOf(200000)),
            NOVEMBER_13,
            NOVEMBER_15,
            CURRENT_TSP,
            CURRENCY_PAIR,
            BigDecimal.valueOf(2)
        );
        assertThat(tradeEntity.getBaseAmount()).isEqualTo(AmountEntity.of(BigDecimal.valueOf(200000), "USD"));
        assertThat(tradeEntity.getValueAmount().getCurrency()).isEqualTo("EUR");
        assertThat(tradeEntity.getValueAmount().getValue()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(tradeEntity.getSpotRate()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(tradeEntity.getCounterparty()).isEqualTo(ORIGINATOR_B);
        assertThat(tradeEntity.getOriginator()).isEqualTo(ORIGINATOR_A);
        assertThat(tradeEntity.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
        assertThat(tradeEntity.getProductCode()).isEqualTo("USD/EUR");
        assertThat(tradeEntity.getDirection()).isEqualTo(Side.BUY);
        assertThat(tradeEntity.getTradeDate()).isEqualTo(NOVEMBER_13);
        assertThat(tradeEntity.getValueDate()).isEqualTo(NOVEMBER_15);
        assertThat(tradeEntity.getClearingStatus()).isEqualTo(ClearingStatus.NOVATED);
        assertThat(tradeEntity.getActivity()).isEqualTo(TradeActivity.NEW);
        assertThat(tradeEntity.getMatchingStatus()).isEqualTo(MatchingStatus.CONFIRMED);
        assertThat(tradeEntity.getSubmissionTsp()).isEqualTo(CURRENT_TSP);
        assertThat(tradeEntity.getExecutionTime()).isEqualTo(CURRENT_TSP);
        assertThat(tradeEntity.getVersionTsp()).isEqualTo(CURRENT_TSP);
        assertThat(tradeEntity.getClearingTsp()).isEqualTo(CURRENT_TSP);
        assertThat(tradeEntity.getMatchingTsp()).isEqualTo(CURRENT_TSP);

    }

    @Test
    void shouldMapToTradeForSale() {
        TradeEntity tradeEntity = mapper.toTrade(
            new BalanceTrade(PARTICIPANT_A, PARTICIPANT_B, BigDecimal.TEN.negate()),
            NOVEMBER_13,
            NOVEMBER_15,
            CURRENT_TSP,
            CURRENCY_PAIR,
            BigDecimal.valueOf(2)
        );

        assertThat(tradeEntity.getDirection()).isEqualTo(Side.SELL);
        assertThat(tradeEntity.getBaseAmount().getValue()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(tradeEntity.getValueAmount().getValue()).isEqualByComparingTo(BigDecimal.valueOf(5));

    }

    @Test
    void shouldRoundProperly() {
        TradeEntity tradeEntity = mapper.toTrade(
            new BalanceTrade(PARTICIPANT_A, PARTICIPANT_B, BigDecimal.valueOf(200000)),
            NOVEMBER_13,
            NOVEMBER_15,
            CURRENT_TSP,
            CURRENCY_PAIR,
            BigDecimal.valueOf(1.69)
        );

        assertThat(tradeEntity.getValueAmount().getValue()).isEqualByComparingTo(BigDecimal.valueOf(118343.19));
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = BalanceTradeMapper.class)
    static class TestConfig {

    }
}