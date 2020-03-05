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

import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.AbstractTotalProcessor;
import com.ihsmarkit.tfx.eod.model.ledger.CollateralListItem;

import lombok.RequiredArgsConstructor;
import one.util.streamex.EntryStream;

@StepScope
@Component
@RequiredArgsConstructor
public class CollateralListTotalProcessor extends
    AbstractTotalProcessor<String, BigDecimal, CollateralBalanceEntity, CollateralListItem<BigDecimal>, CollateralListItem<String>> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    protected String toTotalKey(final CollateralListItem<BigDecimal> collateralListItem) {
        return collateralListItem.getParticipantCode();
    }

    @Override
    protected BigDecimal toTotalValue(final CollateralListItem<BigDecimal> collateralListItem) {
        return collateralListItem.getEvaluatedAmount();
    }

    @Override
    protected BigDecimal merge(final BigDecimal prev, final BigDecimal stepContribution) {
        return prev.add(stepContribution);
    }

    @Override
    protected List<CollateralListItem<String>> extractTotals(final Map<String, BigDecimal> totals) {
        final BigDecimal totalOfTotals = totals.values().stream()
            .collect(summingBigDecimal());

        return EntryStream.of(totals)
            .mapKeyValue((participantCode, amount) -> mapToItem(participantCode, amount, PARTICIPANT_TOTAL_RECORD_TYPE))
            .append(mapToItem(null, totalOfTotals, TOTAL_RECORD_TYPE))
            .toList();
    }

    private CollateralListItem<String> mapToItem(@Nullable final String participantCode, final BigDecimal amount, final int recordType) {
        return CollateralListItem.<String>builder()
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
