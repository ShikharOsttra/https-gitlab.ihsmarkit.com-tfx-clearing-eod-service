package com.ihsmarkit.tfx.eod.model.ledger;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class MonthlyTradingVolumeItem<T> {

    @NonNull
    private final LocalDate businessDate;
    @Builder.Default
    private final String tradeDate = EMPTY;
    @Builder.Default
    private final String recordDate = EMPTY;
    @Builder.Default
    private final String participantCode = EMPTY;
    @Builder.Default
    private final String participantName = EMPTY;
    @Builder.Default
    private final String participantType = EMPTY;
    @Builder.Default
    private final String currencyPairNumber = EMPTY;
    @Builder.Default
    private final String currencyPairCode = EMPTY;
    @NonNull
    private final T sellTradingVolumeInUnit;
    @NonNull
    private final T buyTradingVolumeInUnit;

    private final long orderId;
    private final int recordType;
}
