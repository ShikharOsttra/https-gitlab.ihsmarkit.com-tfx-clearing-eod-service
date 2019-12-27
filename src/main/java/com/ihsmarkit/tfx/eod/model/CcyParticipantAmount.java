package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;

public interface CcyParticipantAmount {

    CurrencyPairEntity getCurrencyPair();

    ParticipantEntity getParticipant();

    BigDecimal getAmount();
}
