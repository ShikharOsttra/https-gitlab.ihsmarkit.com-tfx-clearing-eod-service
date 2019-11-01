package com.ihsmarkit.tfx.eod.mtm;

import java.math.BigDecimal;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
@Getter
@Builder
public class TradeOrPositionEssentials {

    @NonNull
    private final CurrencyPairEntity currencyPair;

    @NonNull
    private final ParticipantEntity participant;

    @NonNull
    private final BigDecimal baseAmount;

    @NonNull
    private final BigDecimal spotRate;

}
