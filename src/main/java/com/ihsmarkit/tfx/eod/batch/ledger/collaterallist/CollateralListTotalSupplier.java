package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;


import static com.ihsmarkit.tfx.common.streams.Streams.summingBigDecimal;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.PARTICIPANT_TOTAL_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL_RECORD_TYPE;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.eod.batch.ledger.TotalSupplier;
import com.ihsmarkit.tfx.eod.model.ledger.CollateralListItem;

import lombok.RequiredArgsConstructor;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

@StepScope
@RequiredArgsConstructor
@Component
public class CollateralListTotalSupplier implements TotalSupplier<CollateralListItem> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['total']}")
    private final Map<String, BigDecimal> total;

    @Override
    public List<CollateralListItem> get() {
        final BigDecimal totalOfTotals = StreamEx.of(total.values())
            .collect(summingBigDecimal());

        return EntryStream.of(total)
            .mapKeyValue((participantCode, amount) -> mapToItem(participantCode, amount, PARTICIPANT_TOTAL_RECORD_TYPE))
            .append(mapToItem(null, totalOfTotals, TOTAL_RECORD_TYPE))
            .toList();
    }

    private CollateralListItem mapToItem(@Nullable final String participantCode, final BigDecimal amount, final int recordType) {
        return CollateralListItem.builder()
            .businessDate(businessDate)
            .participantName(recordType == TOTAL_RECORD_TYPE ? TOTAL : EMPTY)
            .participantCode(recordType == PARTICIPANT_TOTAL_RECORD_TYPE ? participantCode : EMPTY)
            .collateralPurposeType(recordType == PARTICIPANT_TOTAL_RECORD_TYPE ? TOTAL : EMPTY)
            .evaluatedAmount(amount.toString())
            .orderId(Long.MAX_VALUE)
            .recordType(recordType)
            .build();
    }

}
