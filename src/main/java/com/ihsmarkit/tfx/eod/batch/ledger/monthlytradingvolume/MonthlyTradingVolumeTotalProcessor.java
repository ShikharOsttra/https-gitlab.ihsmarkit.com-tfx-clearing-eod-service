package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume;

import javax.annotation.Nullable;

import com.ihsmarkit.tfx.eod.batch.ledger.common.total.MapTotalHolder;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TfxAndParticipantTotalProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.domain.MonthlyTradingVolumeItem;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.domain.MonthlyTradingVolumeParticipantTotalKey;
import com.ihsmarkit.tfx.eod.model.BuySellAmounts;

public class MonthlyTradingVolumeTotalProcessor extends TfxAndParticipantTotalProcessor<MonthlyTradingVolumeItem,
    String, BuySellAmounts,
    MonthlyTradingVolumeParticipantTotalKey, BuySellAmounts> {

    public MonthlyTradingVolumeTotalProcessor(
        final MapTotalHolder<String, BuySellAmounts> tfxTotal,
        final MapTotalHolder<MonthlyTradingVolumeParticipantTotalKey, BuySellAmounts> participantTotal) {
        super(tfxTotal, participantTotal);
    }

    @Nullable
    @Override
    protected String toTfxKey(final MonthlyTradingVolumeItem item) {
        return item.getParticipantAndCurrencyPair().getCurrencyPair().getCode();
    }

    @Override
    protected BuySellAmounts toTfxValue(final MonthlyTradingVolumeItem item) {
        return item.getBuySellAmounts();
    }

    @Nullable
    @Override
    protected MonthlyTradingVolumeParticipantTotalKey toParticipantKey(final MonthlyTradingVolumeItem item) {
        return MonthlyTradingVolumeParticipantTotalKey.of(item.getParticipantAndCurrencyPair().getParticipant().getCode());
    }

    @Override
    protected BuySellAmounts toParticipantValue(final MonthlyTradingVolumeItem item) {
        return item.getBuySellAmounts();
    }
}
