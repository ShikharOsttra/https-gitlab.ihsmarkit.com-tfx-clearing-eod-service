package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain;

import java.io.Serializable;

import com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;

import lombok.Value;

@Value(staticConstructor = "of")
public class CashSettlementTotalKey implements Serializable {

    private final EodProductCashSettlementType settlementType;
    private final EodCashSettlementDateType dateType;
}
