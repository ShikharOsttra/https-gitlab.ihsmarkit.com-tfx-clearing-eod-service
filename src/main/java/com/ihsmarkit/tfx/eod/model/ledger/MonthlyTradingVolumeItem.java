package com.ihsmarkit.tfx.eod.model.ledger;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MonthlyTradingVolumeItem<T> {

    private final LocalDate businessDate;
    private final String tradeDate;
    private final String recordDate;
    private final String participantCode;
    private final String participantName;
    private final String participantType;
    private final String currencyPairNumber;
    private final String currencyPairCode;
    private final T sellTradingVolumeInUnit;
    private final T buyTradingVolumeInUnit;

}
