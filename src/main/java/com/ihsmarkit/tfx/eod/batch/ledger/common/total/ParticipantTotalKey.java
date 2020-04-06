package com.ihsmarkit.tfx.eod.batch.ledger.common.total;

import java.io.Serializable;

public interface ParticipantTotalKey<K> extends Serializable {

    K withParticipantCode(String participantCode);

}
