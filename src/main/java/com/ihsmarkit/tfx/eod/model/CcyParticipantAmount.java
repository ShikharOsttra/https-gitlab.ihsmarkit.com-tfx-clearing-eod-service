package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuppressWarnings({ "PMD.AbstractClassWithoutAbstractMethod", "PMD.AbstractClassWithoutAnyMethod" })
@SuperBuilder
@Getter
@ToString
public abstract class CcyParticipantAmount {

    @NonNull
    private final ParticipantEntity participant;

    @NonNull
    private final CurrencyPairEntity currencyPair;

    @NonNull
    private final  BigDecimal amount;
}
