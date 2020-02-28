package com.ihsmarkit.tfx.eod.batch.ledger.openpositions.total;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.PARTICIPANT_TOTAL_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimal;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Tables;
import com.ihsmarkit.tfx.eod.batch.ledger.TotalSupplier;
import com.ihsmarkit.tfx.eod.model.ledger.OpenPositionsListItem;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

@StepScope
@Component
@RequiredArgsConstructor
public class OpenPositionTotalSupplier implements TotalSupplier<OpenPositionsListItem> {

    private static final String SETTLEMENT_DATE_TOTAL = "Settlement Date Total";

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['total']}")
    private final Map<OpenPositionsListItemTotalKey, OpenPositionsListItemTotal> total;

    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    @Override
    public List<OpenPositionsListItem> get() {
        final var totalsPerParticipant = EntryStream.of(total)
            .collect(
                Tables.toTable(
                    entry -> entry.getKey().getParticipantCode(),
                    entry -> entry.getKey().getSettlementDate(),
                    Map.Entry::getValue,
                    HashBasedTable::create
                )
            );

        final var totalOfTotals = StreamEx.of(totalsPerParticipant.rowMap().values())
            .flatMapToEntry(Function.identity())
            .toMap(OpenPositionsListItemTotal::add);

        return Stream.concat(

            EntryStream.of(totalsPerParticipant.rowMap())
                .flatMapKeyValue((participantCode, totals) -> mapToTotalItems(participantCode, totals, PARTICIPANT_TOTAL_RECORD_TYPE)),

            mapToTotalItems(EMPTY, totalOfTotals, TOTAL_RECORD_TYPE)

        ).collect(Collectors.toList());
    }

    private Stream<OpenPositionsListItem> mapToTotalItems(
        final String participantCode,
        final Map<LocalDate, OpenPositionsListItemTotal> totals,
        final int recordType
    ) {

        final OpenPositionsListItemTotal totalOfSettlementDates = totals.values().stream()
            .reduce(OpenPositionsListItemTotal.ZERO, OpenPositionsListItemTotal::add);

        final List<LocalDate> settlementDates = totals.keySet().stream()
            .sorted()
            .collect(Collectors.toList());

        return Stream.concat(
            Stream.of(mapToItem(participantCode, totalOfSettlementDates, totals.size(), TOTAL, null, recordType)),

            IntStream.range(0, settlementDates.size())
                .mapToObj(index -> mapToItem(
                    participantCode,
                    totals.get(settlementDates.get(index)),
                    totals.size() - 1 - index,
                    SETTLEMENT_DATE_TOTAL,
                    settlementDates.get(index),
                    recordType
                ))
        );
    }

    private OpenPositionsListItem mapToItem(
        final String participantCode,
        final OpenPositionsListItemTotal total,
        final int orderOffset,
        final String totalName,
        @Nullable final LocalDate settlementDate,
        final int recordType
    ) {
        return OpenPositionsListItem.builder()
            .businessDate(businessDate)
            .participantCode(participantCode)
            .participantName(recordType == TOTAL_RECORD_TYPE ? totalName : EMPTY)
            .currencyCode(recordType == PARTICIPANT_TOTAL_RECORD_TYPE ? totalName : EMPTY)
            .initialMtmAmount(formatBigDecimal(total.getInitialMtmAmount()))
            .dailyMtmAmount(formatBigDecimal(total.getDailyMtmAmount()))
            .swapPoint(formatBigDecimal(total.getSwapPoint()))
            .totalVariationMargin(formatBigDecimal(total.getTotalVariationMargin()))
            .settlementDate(formatDate(settlementDate))
            .recordType(recordType)
            .orderId(Long.MAX_VALUE - orderOffset)
            .build();
    }
}
