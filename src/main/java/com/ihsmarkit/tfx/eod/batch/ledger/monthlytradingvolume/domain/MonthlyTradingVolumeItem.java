package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.domain;

import com.ihsmarkit.tfx.eod.model.BuySellAmounts;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class MonthlyTradingVolumeItem {

    @NonNull
    private final ParticipantAndCurrencyPair participantAndCurrencyPair;
    @NonNull
    private final BuySellAmounts buySellAmounts;

}
