package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.domain;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class MonthlyTradingVolumeWriteItem {

    @NonNull
    private final LocalDate businessDate;
    @Builder.Default
    private final String tradeDate = EMPTY;
    @Builder.Default
    private final String recordDate = EMPTY;
    @NonNull
    private final String participantCode;
    @Builder.Default
    private final String participantName = EMPTY;
    @Builder.Default
    private final String participantType = EMPTY;
    @Builder.Default
    private final String currencyPairNumber = EMPTY;
    @Builder.Default
    private final String currencyPairCode = EMPTY;
    @NonNull
    private final String sellTradingVolumeInUnit;
    @NonNull
    private final String buyTradingVolumeInUnit;

    private final long orderId;
    private final int recordType;
}
