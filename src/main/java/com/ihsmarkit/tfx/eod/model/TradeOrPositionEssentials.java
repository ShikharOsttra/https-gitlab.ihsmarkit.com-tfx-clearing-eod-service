package com.ihsmarkit.tfx.eod.model;

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
public class TradeOrPositionEssentials implements CcyParticipantAmount {

    @NonNull
    private final CurrencyPairEntity currencyPair;

    @NonNull
    private final ParticipantEntity participant;

    @NonNull
    private final BigDecimal amount;

    @NonNull
    private final BigDecimal spotRate;

}
