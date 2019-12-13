package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.DAILY_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.INITIAL_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.SWAP_PNL;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;

import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.eod.mapper.ParticipantCurrencyPairAmountMapper;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@JobScope
public class EodCashSettlementMappingService {

    private final ParticipantCurrencyPairAmountMapper mtmMapper;

    private final TradeAndSettlementDateService tradeAndSettlementDateService;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    public EodProductCashSettlementEntity map(final ParticipantCurrencyPairAmount mtm, final EodProductCashSettlementType type) {
        return mtmMapper.toEodProductCashSettlement(
            mtm,
            businessDate,
            tradeAndSettlementDateService.getValueDate(businessDate, mtm.getCurrencyPair()),
            type
        );
    }

    public EodProductCashSettlementEntity mapInitialMtm(final ParticipantCurrencyPairAmount mtm) {
        return map(mtm, INITIAL_MTM);
    }

    public EodProductCashSettlementEntity mapDailyMtm(final ParticipantCurrencyPairAmount mtm) {
        return map(mtm, DAILY_MTM);
    }

    public EodProductCashSettlementEntity mapSwapPnL(final ParticipantCurrencyPairAmount mtm) {
        return map(mtm, SWAP_PNL);
    }

    public EodProductCashSettlementEntity mapTotalVM(final ParticipantCurrencyPairAmount mtm) {
        return map(mtm, TOTAL_VM);
    }
}
