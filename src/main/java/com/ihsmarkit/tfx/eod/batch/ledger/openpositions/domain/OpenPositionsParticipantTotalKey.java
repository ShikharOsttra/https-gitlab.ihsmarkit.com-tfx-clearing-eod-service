package com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain;

import java.time.LocalDate;

import com.ihsmarkit.tfx.eod.batch.ledger.common.total.ParticipantTotalKey;

import lombok.Value;
import lombok.experimental.Wither;

@Value(staticConstructor = "of")
@Wither
public class OpenPositionsParticipantTotalKey implements ParticipantTotalKey<OpenPositionsParticipantTotalKey> {

    private final String participantCode;
    private final LocalDate settlementDate;
}
