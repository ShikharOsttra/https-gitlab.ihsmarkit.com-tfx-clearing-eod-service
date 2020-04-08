package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.domain;

import com.ihsmarkit.tfx.eod.batch.ledger.common.total.ParticipantTotalKey;

import lombok.Value;
import lombok.experimental.Wither;

@Wither
@Value(staticConstructor = "of")
public class MonthlyTradingVolumeParticipantTotalKey implements ParticipantTotalKey<MonthlyTradingVolumeParticipantTotalKey> {

    private final String participantCode;

}
