package com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Value;
import lombok.experimental.Wither;

@Value(staticConstructor = "of")
@Wither
public class OpenPositionsParticipantTotalKey implements Serializable {

    private final String participantCode;
    private final LocalDate settlementDate;
}
