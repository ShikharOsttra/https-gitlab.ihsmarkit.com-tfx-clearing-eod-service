package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain.CollateralListItem;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain.CollateralListParticipantTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain.CollateralListTfxTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.BigDecimalTotalValue;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.MapTotalHolder;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TfxAndParticipantTotalProcessor;


public class CollateralListTotalProcessor extends TfxAndParticipantTotalProcessor<CollateralListItem,
    CollateralListTfxTotalKey, BigDecimalTotalValue,
    CollateralListParticipantTotalKey, BigDecimalTotalValue> {

    public CollateralListTotalProcessor(
        final MapTotalHolder<CollateralListTfxTotalKey, BigDecimalTotalValue> tfxTotal,
        final MapTotalHolder<CollateralListParticipantTotalKey, BigDecimalTotalValue> participantTotal
    ) {
        super(tfxTotal, participantTotal);
    }

    @Override
    protected CollateralListTfxTotalKey toTfxKey(final CollateralListItem item) {
        return CollateralListTfxTotalKey.of(item.getBalance().getPurpose(), item.getBalance().getProduct().getType());
    }

    @Override
    protected BigDecimalTotalValue toTfxValue(final CollateralListItem item) {
        return BigDecimalTotalValue.of(item.getEvaluatedAmount());
    }

    @Override
    protected CollateralListParticipantTotalKey toParticipantKey(final CollateralListItem item) {
        return CollateralListParticipantTotalKey.of(item.getBalance().getParticipant().getCode(), item.getBalance().getPurpose());
    }

    @Override
    protected BigDecimalTotalValue toParticipantValue(final CollateralListItem item) {
        return BigDecimalTotalValue.of(item.getEvaluatedAmount());
    }

}
