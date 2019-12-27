package com.ihsmarkit.tfx.eod.model;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;

public interface CcyParticipantAmount<T> {

    CurrencyPairEntity getCurrencyPair();

    ParticipantEntity getParticipant();

    T getAmount();
}
