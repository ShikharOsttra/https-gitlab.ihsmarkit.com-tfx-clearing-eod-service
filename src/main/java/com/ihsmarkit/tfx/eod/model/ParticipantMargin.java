package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;
import java.util.Optional;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.domain.type.MarginAlertLevel;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParticipantMargin {
    private final ParticipantEntity participant;
    private final Optional<MarginAlertLevel> marginAlertLevel;
    private final Optional<BigDecimal> cashCollateralAmount;
    private final Optional<BigDecimal> logCollateralAmount;
    private final Optional<BigDecimal> pnl;
    private final Optional<BigDecimal> todaySettlement;
    private final Optional<BigDecimal> nextDaySettlement;
    private final Optional<BigDecimal> initialMargin;
    private final Optional<BigDecimal> requiredAmount;
    private final Optional<BigDecimal> totalDeficit;
    private final Optional<BigDecimal> cashDeficit;

}
