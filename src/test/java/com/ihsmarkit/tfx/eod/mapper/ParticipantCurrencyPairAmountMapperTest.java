package com.ihsmarkit.tfx.eod.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ihsmarkit.tfx.core.dl.EntityTestDataFactory;
import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantType;
import com.ihsmarkit.tfx.eod.config.EodJobConstants;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;

@ExtendWith(SpringExtension.class)
class ParticipantCurrencyPairAmountMapperTest {

    private static final ParticipantEntity PARTICIPANT_A = EntityTestDataFactory.aParticipantEntityBuilder().build();
    private static final CurrencyPairEntity CURRENCY_PAIR = EntityTestDataFactory.aCurrencyPairEntityBuilder().build();

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 11, 5);
    private static final LocalDate SETTLEMENT_DATE = LocalDate.of(2019, 11, 8);

    @Autowired
    private ParticipantCurrencyPairAmountMapper mapper;

    @Test
    void shouldConvertToParticipantPositionEntity() {
        ParticipantPositionEntity position = mapper.toParticipantPosition(
            ParticipantCurrencyPairAmount.of(PARTICIPANT_A, CURRENCY_PAIR, BigDecimal.ONE),
            ParticipantPositionType.NET,
            BUSINESS_DATE,
            SETTLEMENT_DATE,
            BigDecimal.TEN
        );

        assertThat(position.getAmount()).isEqualTo(AmountEntity.of(BigDecimal.ONE, EodJobConstants.USD));
        assertThat(position.getCurrencyPair()).isSameAs(CURRENCY_PAIR);
        assertThat(position.getParticipant()).isSameAs(PARTICIPANT_A);
        assertThat(position.getTradeDate()).isEqualTo(BUSINESS_DATE);
        assertThat(position.getValueDate()).isEqualTo(SETTLEMENT_DATE);
        assertThat(position.getType()).isSameAs(ParticipantPositionType.NET);
        assertThat(position.getParticipantType()).isSameAs(ParticipantType.LIQUIDITY_PROVIDER);
        assertThat(position.getPrice()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void shouldConvertToEodProductCashSettlementEntity() {

        EodProductCashSettlementEntity eod = mapper.toEodProductCashSettlement(
            ParticipantCurrencyPairAmount.of(PARTICIPANT_A, CURRENCY_PAIR, BigDecimal.ONE),
            BUSINESS_DATE,
            SETTLEMENT_DATE,
            EodProductCashSettlementType.INITIAL_MTM
        );

        assertThat(eod.getAmount()).isEqualTo(AmountEntity.of(BigDecimal.ONE, EodJobConstants.JPY));
        assertThat(eod.getCurrencyPair()).isSameAs(CURRENCY_PAIR);
        assertThat(eod.getParticipant()).isSameAs(PARTICIPANT_A);
        assertThat(eod.getDate()).isEqualTo(BUSINESS_DATE);
        assertThat(eod.getSettlementDate()).isEqualTo(SETTLEMENT_DATE);
        assertThat(eod.getType()).isSameAs(EodProductCashSettlementType.INITIAL_MTM);

    }

    @Test
    void shouldConvertEodProductCashSettlementEntity() {
        EodProductCashSettlementEntity eod =
            EodProductCashSettlementEntity.builder()
                .participant(PARTICIPANT_A)
                .currencyPair(CURRENCY_PAIR)
                .amount(AmountEntity.of(BigDecimal.ONE, "JPY"))
            .build();
        assertThat(mapper.toParticipantCurrencyPairAmount(eod))
            .extracting("participant", "currencyPair", "amount")
            .containsExactly(PARTICIPANT_A, CURRENCY_PAIR, BigDecimal.ONE);
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = ParticipantCurrencyPairAmountMapper.class)
    static class TestConfig {

    }
}