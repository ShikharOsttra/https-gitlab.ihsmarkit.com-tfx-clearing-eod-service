package com.ihsmarkit.tfx.eod.batch.ledger.openpositions.total;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Value;

@Value(staticConstructor = "of")
public class OpenPositionsListItemTotalKey implements Serializable {

    private final String participantCode;
    private final LocalDate settlementDate;
}
