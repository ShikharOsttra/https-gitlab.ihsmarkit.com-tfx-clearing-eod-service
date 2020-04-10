package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain;

import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.MARGIN;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantType.FX_BROKER;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantType;

import lombok.Value;
import lombok.experimental.Delegate;

@Value(staticConstructor = "of")
public class ParticipantCollateralBalanceAdapter implements CollateralBalanceAdapter {

    @Delegate
    private final CollateralBalanceItem collateralBalanceItem;

    @Override
    public String getParticipantCode() {
        return collateralBalanceItem.getParticipant().getCode();
    }

    @Override
    public String getParticipantName() {
        return collateralBalanceItem.getParticipant().getName();
    }

    @Override
    public ParticipantType getParticipantType() {
        return collateralBalanceItem.getParticipant().getType();
    }

    @Override
    public boolean isShowLgBalance(final CollateralPurpose collateralPurpose) {
        return collateralPurpose == MARGIN && collateralBalanceItem.getParticipant().getType() == FX_BROKER;
    }

    @Nullable
    @Override
    public BigDecimal getCashSettlement(final EodProductCashSettlementType settlementType, final EodCashSettlementDateType dateType) {
        return collateralBalanceItem.getCashSettlements().get(settlementType, dateType);
    }

}
