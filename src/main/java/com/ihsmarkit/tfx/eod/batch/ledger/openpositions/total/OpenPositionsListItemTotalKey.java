package com.ihsmarkit.tfx.eod.batch.ledger.openpositions.total;

import lombok.Value;

@Value(staticConstructor = "of")
public class OpenPositionsListItemTotalKey {

    private final String participantCode;
    private final String settlementDate;
}
