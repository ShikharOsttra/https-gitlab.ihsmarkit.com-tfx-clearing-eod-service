package com.ihsmarkit.tfx.eod.batch.ledger.openpositions;

import static com.ihsmarkit.tfx.core.domain.Participant.CLEARING_HOUSE_CODE;

import java.time.LocalDate;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.MapTotalHolder;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TotalProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain.OpenPositionsItem;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain.OpenPositionsParticipantTotal;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain.OpenPositionsParticipantTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain.OpenPositionsTfxTotal;

import lombok.RequiredArgsConstructor;
import one.util.streamex.EntryStream;

@RequiredArgsConstructor
public class OpenPositionsTotalProcessor implements TotalProcessor<OpenPositionsItem> {

    private final MapTotalHolder<String, OpenPositionsTfxTotal> tfxTotal;

    private final MapTotalHolder<OpenPositionsParticipantTotalKey, OpenPositionsParticipantTotal> participantTotal;

    @Override
    public OpenPositionsItem process(final OpenPositionsItem item) {
        tfxTotal.contributeToTotals(item.getParticipantAndCurrencyPair().getCurrencyPair().getCode(), toTfxTotalValue(item));
        if (item.getSettlementDate() != null) {
            final OpenPositionsParticipantTotalKey participantTotalKey = toParticipantTotalKey(item);
            final OpenPositionsParticipantTotal participantTotalValue = toParticipantTotalValue(item);
            participantTotal.contributeToTotals(participantTotalKey, participantTotalValue);
            participantTotal.contributeToTotals(participantTotalKey.withParticipantCode(CLEARING_HOUSE_CODE), participantTotalValue);
        }
        return item;
    }

    protected OpenPositionsParticipantTotalKey toParticipantTotalKey(final OpenPositionsItem openPositionsItem) {
        return OpenPositionsParticipantTotalKey.of(
            openPositionsItem.getParticipantAndCurrencyPair().getParticipant().getCode(),
            openPositionsItem.getSettlementDate()
        );
    }

    protected OpenPositionsTfxTotal toTfxTotalValue(final OpenPositionsItem openPositionsItem) {
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

    protected OpenPositionsParticipantTotal toParticipantTotalValue(final OpenPositionsItem openPositionsItem) {
        return OpenPositionsParticipantTotal.builder()
            .initialMtmAmount(openPositionsItem.getInitialMtmAmount())
            .dailyMtmAmount(openPositionsItem.getDailyMtmAmount())
            .swapPoint(openPositionsItem.getSwapPoint())
            .totalVariationMargin(openPositionsItem.getTotalVariationMargin())
            .build();
    }

    public Map<String, OpenPositionsTfxTotal> getTfxTotal() {
        return tfxTotal.getTotal();
    }

    public Table<String, LocalDate, OpenPositionsParticipantTotal> getParticipantTotal() {
        return EntryStream.of(participantTotal.getTotal())
            .collect(
                Tables.toTable(
                    entry -> entry.getKey().getParticipantCode(),
                    entry -> entry.getKey().getSettlementDate(),
                    Map.Entry::getValue,
                    HashBasedTable::create
                )
            );
    }
}
