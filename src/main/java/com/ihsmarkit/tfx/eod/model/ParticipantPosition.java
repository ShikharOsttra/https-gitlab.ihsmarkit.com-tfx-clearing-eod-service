package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor(staticName = "of")
@Builder
@Getter
@ToString
public class ParticipantPosition implements CcyParticipantAmount<BigDecimal> {

    private final ParticipantEntity participant;

    private final CurrencyPairEntity currencyPair;

    private final BigDecimal amount;

    private final ParticipantPositionType type;
}
