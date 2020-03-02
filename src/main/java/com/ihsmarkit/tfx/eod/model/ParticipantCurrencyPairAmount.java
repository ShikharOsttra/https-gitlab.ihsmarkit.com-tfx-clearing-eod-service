package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@ToString(callSuper = true)
public final class ParticipantCurrencyPairAmount extends CcyParticipantAmount {

    public static ParticipantCurrencyPairAmount of(final ParticipantEntity participant, final CurrencyPairEntity currencyPair, final BigDecimal amount) {
        return builder()
            .participant(participant)
            .currencyPair(currencyPair)
            .amount(amount)
            .build();
    }
}
