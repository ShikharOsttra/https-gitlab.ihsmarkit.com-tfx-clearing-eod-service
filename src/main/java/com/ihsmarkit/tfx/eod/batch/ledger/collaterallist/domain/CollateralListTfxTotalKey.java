package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain;

import java.io.Serializable;

import com.ihsmarkit.tfx.core.domain.type.CollateralProductType;
import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;

import lombok.Value;

@Value(staticConstructor = "of")
public class CollateralListTfxTotalKey implements Serializable {

    private final CollateralPurpose purpose;
    private final CollateralProductType productType;

}
