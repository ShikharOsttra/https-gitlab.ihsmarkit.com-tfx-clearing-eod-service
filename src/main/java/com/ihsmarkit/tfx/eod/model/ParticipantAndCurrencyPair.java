package com.ihsmarkit.tfx.eod.model;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
public class ParticipantAndCurrencyPair {
    private final ParticipantEntity participant;
    private final CurrencyPairEntity currencyPair;
}
