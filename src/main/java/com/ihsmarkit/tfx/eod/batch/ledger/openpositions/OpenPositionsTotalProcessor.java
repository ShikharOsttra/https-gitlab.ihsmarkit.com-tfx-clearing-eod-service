package com.ihsmarkit.tfx.eod.batch.ledger.openpositions;

import javax.annotation.Nullable;

import com.ihsmarkit.tfx.eod.batch.ledger.common.total.MapTotalHolder;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TfxAndParticipantTotalProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain.OpenPositionsItem;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain.OpenPositionsParticipantTotal;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain.OpenPositionsParticipantTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain.OpenPositionsTfxTotal;

public class OpenPositionsTotalProcessor extends TfxAndParticipantTotalProcessor<OpenPositionsItem,
    String, OpenPositionsTfxTotal,
    OpenPositionsParticipantTotalKey, OpenPositionsParticipantTotal> {

    public OpenPositionsTotalProcessor(
        final MapTotalHolder<String, OpenPositionsTfxTotal> tfxTotal,
        final MapTotalHolder<OpenPositionsParticipantTotalKey, OpenPositionsParticipantTotal> participantTotal
    ) {
        super(tfxTotal, participantTotal);
    }

    @Nullable
    @Override
    protected String toTfxKey(final OpenPositionsItem item) {
        return item.getParticipantAndCurrencyPair().getCurrencyPair().getCode();
    }

    @Nullable
    @Override
    protected OpenPositionsParticipantTotalKey toParticipantKey(final OpenPositionsItem openPositionsItem) {
        if (openPositionsItem.getSettlementDate() == null) {
            return null;
        }

        return OpenPositionsParticipantTotalKey.of(
            openPositionsItem.getParticipantAndCurrencyPair().getParticipant().getCode(),
            openPositionsItem.getSettlementDate()
        );
    }

    @Override
    protected OpenPositionsTfxTotal toTfxValue(final OpenPositionsItem openPositionsItem) {
        return OpenPositionsTfxTotal.builder()
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
    protected OpenPositionsParticipantTotal toParticipantValue(final OpenPositionsItem openPositionsItem) {
        return OpenPositionsParticipantTotal.builder()
            .initialMtmAmount(openPositionsItem.getInitialMtmAmount())
            .dailyMtmAmount(openPositionsItem.getDailyMtmAmount())
            .swapPoint(openPositionsItem.getSwapPoint())
            .totalVariationMargin(openPositionsItem.getTotalVariationMargin())
            .build();
    }

}
