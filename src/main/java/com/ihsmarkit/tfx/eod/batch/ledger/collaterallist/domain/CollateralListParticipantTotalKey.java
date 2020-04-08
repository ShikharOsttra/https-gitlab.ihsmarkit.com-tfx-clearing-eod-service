package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain;

import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.ParticipantTotalKey;

import lombok.Value;
import lombok.experimental.Wither;

@Value(staticConstructor = "of")
@Wither
public class CollateralListParticipantTotalKey implements ParticipantTotalKey<CollateralListParticipantTotalKey> {

    private final String participantCode;
    private final CollateralPurpose purpose;
}
