package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import lombok.Value;

@Value(staticConstructor = "of")
public class CollateralListItemTotalKey {

    private final String participantCode;
    private final String collateralPurposeType;
}
