package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@ToString(callSuper = true)
public class TradeOrPositionEssentials extends CcyParticipantAmount {

    @NonNull
    private final BigDecimal spotRate;

}
