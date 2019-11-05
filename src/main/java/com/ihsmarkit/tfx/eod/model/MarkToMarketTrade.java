package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor(staticName = "of")
@Getter
@ToString
public class MarkToMarketTrade {

    @NonNull
    private final ParticipantEntity participant;

    @NonNull
    private final CurrencyPairEntity currencyPair;

    @NonNull
    private final BigDecimal amount;
}
