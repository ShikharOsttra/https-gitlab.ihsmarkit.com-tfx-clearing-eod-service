package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class CollateralListItem {

    @NonNull
    private final CollateralBalanceEntity balance;
    @Nullable
    private final BigDecimal evaluatedPrice;
    @NonNull
    private final BigDecimal evaluatedAmount;

}
