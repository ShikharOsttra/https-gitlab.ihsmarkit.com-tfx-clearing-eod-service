package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;

import lombok.RequiredArgsConstructor;


@Component
@JobScope
@RequiredArgsConstructor
public class OffsettedTradeMatchIdProvider {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final TradeRepository tradeRepository;

    private final Lazy<Table<String, String, String>> cancelledTrades = Lazy.of(this::loadOffsettingMatchIds);
    private final Lazy<Set<String>> offsettingMatchIds = cancelledTrades.map(Table::rowKeySet);

    public boolean hasOffsettingMatchId(final String matchingRef) {
        return offsettingMatchIds.get().contains(matchingRef);
    }

    public String getCancelledTradeClearingId(final String matchingRef, final String participantCode) {
        return Optional.ofNullable(cancelledTrades.get().get(matchingRef, participantCode))
            .orElseThrow(
                () -> new IllegalStateException("CancelledTrade can't be found by matchingRef: " + matchingRef + " and participantCode: " + participantCode)
            );
    }

    private Table<String, String, String> loadOffsettingMatchIds() {
        return tradeRepository.findAllOffsettingMatchIdsByTradeDate(businessDate)
            .collect(Tables.toTable(
                TradeRepository.BustedTradeProjection::getMatchingRef,
                TradeRepository.BustedTradeProjection::getParticipantCode,
                TradeRepository.BustedTradeProjection::getClearingRef,
                HashBasedTable::create
            ));
    }
}
