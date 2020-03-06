package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.SUBTOTAL_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
    AbstractTotalProcessor<CollateralListItemTotalKey, BigDecimal, CollateralBalanceEntity, CollateralListItem<BigDecimal>, CollateralListItem<String>> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final CollateralListItemOrderProvider collateralListItemOrderProvider;

    @Override
    protected CollateralListItemTotalKey toTotalKey(final CollateralListItem<BigDecimal> collateralListItem) {
        return CollateralListItemTotalKey.of(
            collateralListItem.getParticipantCode(),
            collateralListItem.getCollateralPurposeType(),
            collateralListItem.getCollateralTypeNo()
        );
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
    protected List<CollateralListItem<String>> extractTotals(final Map<CollateralListItemTotalKey, BigDecimal> totals) {
        return EntryStream.of(totals)
            .mapKeyValue((collateralListItemTotalKey, amount) ->
                CollateralListItem.<String>builder()
                    .businessDate(businessDate)
                    .participantCode(collateralListItemTotalKey.getParticipantCode())
                    .collateralPurpose(TOTAL)
                    .evaluatedAmount(amount.toString())
                    .orderId(collateralListItemOrderProvider.getOrderId(collateralListItemTotalKey, SUBTOTAL_RECORD_TYPE))
                    .recordType(SUBTOTAL_RECORD_TYPE)
                    .build()
            )
            .toList();
    }
}
