package com.ihsmarkit.tfx.eod.mapper;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import com.ihsmarkit.tfx.core.domain.type.ParticipantType;
import com.ihsmarkit.tfx.core.margin.MarginAdapter;
import com.ihsmarkit.tfx.eod.model.ParticipantMargin;

import lombok.Value;

@Value(staticConstructor = "of")
class MarginAdapterImpl implements MarginAdapter {

    private final ParticipantMargin margin;

    @Override
    public ParticipantType getParticipantType() {
        return margin.getParticipant().getType();
    }

    @Nullable
    @Override
    public BigDecimal getRequiredMarginBaseAmount() {
        return margin.getInitialMargin().orElse(ZERO);
    }

    @Nullable
    @Override
    public BigDecimal getPnl() {
        return margin.getPnl().orElse(ZERO);
    }

    @Override
    public BigDecimal getTodaySettlement() {
        return margin.getTodaySettlement().orElse(ZERO);
    }

    @Nullable
    @Override
    public BigDecimal getFollowingDaySettlement() {
        return margin.getNextDaySettlement().orElse(ZERO);
    }

    @Override
    public BigDecimal getCashCollateralAmount() {
        return margin.getCashCollateralAmount().orElse(ZERO);
    }

    @Override
    public BigDecimal getLogCollateralAmount() {
        return margin.getLogCollateralAmount().orElse(ZERO);
    }
}
