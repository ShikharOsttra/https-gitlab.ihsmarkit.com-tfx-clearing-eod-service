package com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain;

import com.ihsmarkit.tfx.eod.batch.ledger.common.total.ParticipantTotalKey;
import java.time.LocalDate;
import lombok.Value;
import lombok.experimental.Wither;

@Value(staticConstructor = "of")
@Wither
public class RebalanceParticipantTotalKey implements ParticipantTotalKey<RebalanceParticipantTotalKey> {

    private final String participantCode;
    private final LocalDate settlementDate;
}
