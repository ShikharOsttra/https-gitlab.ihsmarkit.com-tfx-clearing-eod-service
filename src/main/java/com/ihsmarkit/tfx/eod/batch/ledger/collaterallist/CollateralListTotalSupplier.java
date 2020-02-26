package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;


import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.PARTICIPANT_TOTAL_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL_RECORD_TYPE;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants;
import com.ihsmarkit.tfx.eod.batch.ledger.TotalSupplier;
import com.ihsmarkit.tfx.eod.model.ledger.CollateralListItem;

import lombok.RequiredArgsConstructor;

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
        final BigDecimal totalOfTotals = total.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return
            Stream.concat(

                total.entrySet().stream()
                    .map(totalEntry -> mapToItem(totalEntry.getKey(), totalEntry.getValue(), PARTICIPANT_TOTAL_RECORD_TYPE)),

                Stream.of(mapToItem(EMPTY, totalOfTotals, TOTAL_RECORD_TYPE))
            ).collect(Collectors.toList());
    }

    private CollateralListItem mapToItem(final String participantCode, final BigDecimal amount, final int recordType) {
        return CollateralListItem.builder()
            .businessDate(businessDate)
            .participantName(LedgerConstants.TOTAL)
            .participantCode(participantCode)
            .evaluatedAmount(amount.toString())
            .orderId(Long.MAX_VALUE)
            .recordType(recordType)
            .build();
    }

}
