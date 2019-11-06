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
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.eod.config.EodJobConstants;
import com.ihsmarkit.tfx.eod.model.MarkToMarketTrade;

@ExtendWith(SpringExtension.class)
class MarkToMarketTradeMapperTest {

    private static final ParticipantEntity PARTICIPANT_A = EntityTestDataFactory.aParticipantEntityBuilder().build();
    private static final CurrencyPairEntity CURRENCY_PAIR = EntityTestDataFactory.aCurrencyPairEntityBuilder().build();

    @Autowired
    private MarkToMarketTradeMapper mapper;

    @Test
    void shouldConvertToEodProductCashSettlementEntity() {

        final LocalDate businessDate = LocalDate.of(2019, 11, 5);
        final LocalDate settlementDate = LocalDate.of(2019, 11, 8);

        EodProductCashSettlementEntity eod = mapper.toEodProductCashSettlement(
            MarkToMarketTrade.of(PARTICIPANT_A, CURRENCY_PAIR, BigDecimal.ONE),
            businessDate,
            settlementDate,
            EodProductCashSettlementType.INITIAL_MTM
        );

        assertThat(eod.getAmount().getCurrency()).isEqualTo(EodJobConstants.JPY);
        assertThat(eod.getAmount().getValue()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(eod.getCurrencyPair()).isSameAs(CURRENCY_PAIR);
        assertThat(eod.getParticipant()).isSameAs(PARTICIPANT_A);
        assertThat(eod.getDate()).isEqualTo(businessDate);
        assertThat(eod.getSettlementDate()).isEqualTo(settlementDate);
        assertThat(eod.getType()).isSameAs(EodProductCashSettlementType.INITIAL_MTM);

    }
    @TestConfiguration
    @ComponentScan(basePackageClasses = MarkToMarketTradeMapper.class)
    static class TestConfig {

    }
}