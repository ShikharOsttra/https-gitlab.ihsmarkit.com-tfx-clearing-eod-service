package com.ihsmarkit.tfx.eod.batch.ledger.rebalance;

import com.ihsmarkit.tfx.eod.batch.ledger.common.total.MapTotalHolder;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TfxAndParticipantTotalProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceItem;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceParticipantTotal;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceParticipantTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceTfxTotal;
import javax.annotation.Nullable;

public class RebalanceTotalProcessor extends TfxAndParticipantTotalProcessor<RebalanceItem,
    String, RebalanceTfxTotal,
    RebalanceParticipantTotalKey, RebalanceParticipantTotal> {

    public RebalanceTotalProcessor(
        final MapTotalHolder<String, RebalanceTfxTotal> tfxTotal,
        final MapTotalHolder<RebalanceParticipantTotalKey, RebalanceParticipantTotal> participantTotal
    ) {
        super(tfxTotal, participantTotal);
    }

    @Nullable
    @Override
    protected String toTfxKey(final RebalanceItem item) {
        return item.getParticipantAndCurrencyPair().getCurrencyPair().getCode();
    }

    @Nullable
    @Override
    protected RebalanceParticipantTotalKey toParticipantKey(final RebalanceItem openPositionsItem) {
        if (openPositionsItem.getSettlementDate() == null) {
            return null;
        }

        return RebalanceParticipantTotalKey.of(
            openPositionsItem.getParticipantAndCurrencyPair().getParticipant().getCode(),
            openPositionsItem.getSettlementDate()
        );
    }

    @Override
    protected RebalanceTfxTotal toTfxValue(final RebalanceItem openPositionsItem) {
        return RebalanceTfxTotal.builder()
            .shortPositionPreviousDay(openPositionsItem.getShortPositionPreviousDay())
            .longPositionPreviousDay(openPositionsItem.getLongPositionPreviousDay())
            .sellTradingAmount(openPositionsItem.getSellTradingAmount())
            .buyTradingAmount(openPositionsItem.getBuyTradingAmount())
            .shortPosition(openPositionsItem.getShortPosition())
            .longPosition(openPositionsItem.getLongPosition())
            .initialMtmAmount(openPositionsItem.getInitialMtmAmount())
            .dailyMtmAmount(openPositionsItem.getDailyMtmAmount())
            .swapPoint(openPositionsItem.getSwapPoint())
            .totalVariationMargin(openPositionsItem.getTotalVariationMargin())
            .settlementDate(openPositionsItem.getSettlementDate())
            .build();
    }

    @Override
    protected RebalanceParticipantTotal toParticipantValue(final RebalanceItem openPositionsItem) {
        return RebalanceParticipantTotal.builder()
            .initialMtmAmount(openPositionsItem.getInitialMtmAmount())
            .dailyMtmAmount(openPositionsItem.getDailyMtmAmount())
            .swapPoint(openPositionsItem.getSwapPoint())
            .totalVariationMargin(openPositionsItem.getTotalVariationMargin())
            .build();
    }

}
