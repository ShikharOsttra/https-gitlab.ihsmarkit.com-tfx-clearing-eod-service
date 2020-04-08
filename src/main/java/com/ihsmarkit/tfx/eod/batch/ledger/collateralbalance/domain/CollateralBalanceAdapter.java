package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.springframework.data.util.Pair;

import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantType;

public interface CollateralBalanceAdapter {

    String getParticipantCode();

    String getParticipantName();

    @Nullable
    ParticipantType getParticipantType();

    boolean isShowLgBalance(CollateralPurpose collateralPurpose);

    Map<CollateralPurpose, CollateralDeposit> getCollateralDeposits();

    @Nullable
    BigDecimal getCashSettlement(EodProductCashSettlementType settlementType, EodCashSettlementDateType dateType);

    Map<CollateralPurpose, BigDecimal> getRequiredAmount();

    Optional<Pair<LocalDate, BigDecimal>> getNextClearingDeposit();

    @Nullable
    BigDecimal getInitialMargin();

    @Nullable
    BigDecimal getDeficitInCashSettlement();
}
