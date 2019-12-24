package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;
import java.util.Optional;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParticipantMargin {
    private final ParticipantEntity participant;
    private final Optional<BigDecimal> initialMargin;
    private final Optional<BigDecimal> requiredAmount;
    private final Optional<BigDecimal> totalDeficit;
    private final Optional<BigDecimal> cashDeficit;

}
