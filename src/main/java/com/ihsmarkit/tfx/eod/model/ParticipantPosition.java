package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@ToString(callSuper = true)
public final class ParticipantPosition extends CcyParticipantAmount {

    @NonNull
    private final ParticipantPositionType type;

    public static ParticipantPosition of(final ParticipantEntity participant, final CurrencyPairEntity currencyPair, final BigDecimal amount,
        final ParticipantPositionType type) {
        return builder()
            .participant(participant)
            .currencyPair(currencyPair)
            .amount(amount)
            .type(type)
            .build();
    }
}
