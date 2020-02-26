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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.eod.batch.ledger.TotalSupplier;
import com.ihsmarkit.tfx.eod.model.ledger.OpenPositionsListItem;

import lombok.RequiredArgsConstructor;

@StepScope
@Component
@RequiredArgsConstructor
public class OpenPositionTotalSupplier implements TotalSupplier<OpenPositionsListItem> {

    private static final String SETTLEMENT_DATE_TOTAL = "Settlement Date Total";

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['total']}")
    private final Map<OpenPositionsListItemTotalKey, OpenPositionsListItemTotal> total;

    @Override
    public List<OpenPositionsListItem> get() {
        final var totalsPerParticipant = total.entrySet().stream().collect(
            Collectors.groupingBy(
                entry -> entry.getKey().getParticipantCode(),
                Collectors.toMap(entry -> entry.getKey().getSettlementDate(), Map.Entry::getValue)
            ));

        final var totalOfTotals = totalsPerParticipant.values().stream()
            .flatMap(map -> map.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, OpenPositionsListItemTotal::add));

        return Stream.concat(

            totalsPerParticipant.entrySet().stream().flatMap(entry -> mapToTotalItems(entry.getKey(), entry.getValue(), PARTICIPANT_TOTAL_RECORD_TYPE)),

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

        final List<LocalDate> settlementDates = totals.keySet().stream().sorted().collect(Collectors.toList());

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
