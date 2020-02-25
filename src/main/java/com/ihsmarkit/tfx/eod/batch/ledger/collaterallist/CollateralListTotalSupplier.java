package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;


import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.PARTICIPANT_TOTAL_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL_RECORD_TYPE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        return total.entrySet().stream()
            .map(totalEntry -> CollateralListItem.builder()
                .businessDate(businessDate)
                .participantName(LedgerConstants.TOTAL)
                .participantCode(totalEntry.getKey())
                .evaluatedAmount(totalEntry.getValue().toString())
                .orderId(Long.MAX_VALUE)
                .recordType(totalEntry.getKey().isEmpty() ? TOTAL_RECORD_TYPE : PARTICIPANT_TOTAL_RECORD_TYPE)
                .build())
            .collect(Collectors.toList());
    }

}
