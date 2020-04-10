package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.springframework.data.util.Pair;

import com.google.common.collect.Table;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class CollateralBalanceItem {

    @NonNull
    private final ParticipantEntity participant;
    @NonNull
    private final Map<CollateralPurpose, CollateralDeposit> collateralDeposits;
    @NonNull
    private final Table<EodProductCashSettlementType, EodCashSettlementDateType, BigDecimal> cashSettlements;
    @NonNull
    private final Map<CollateralPurpose, BigDecimal> requiredAmount;
    @Nullable
    private final BigDecimal initialMargin;
    @Nullable
    private final BigDecimal deficitInCashSettlement;
    @NonNull
    private final Optional<Pair<LocalDate, BigDecimal>> nextClearingDeposit;

}
