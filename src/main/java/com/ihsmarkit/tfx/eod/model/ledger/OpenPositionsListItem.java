package com.ihsmarkit.tfx.eod.model.ledger;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class OpenPositionsListItem<T> {

    @NonNull
    private final LocalDate businessDate;
    @Builder.Default
    private final String tradeDate = EMPTY;
    @Builder.Default
    private final String recordDate = EMPTY;
    @NonNull
    private final String participantCode;
    @NonNull
    private final String participantName;
    @Builder.Default
    private final String participantType = EMPTY;
    @Builder.Default
    private final String currencyNo = EMPTY;
    @NonNull
    private final String currencyCode;
    @Builder.Default
    private final String shortPositionPreviousDay = EMPTY;
    @Builder.Default
    private final String longPositionPreviousDay = EMPTY;
    @Builder.Default
    private final String sellTradingAmount = EMPTY;
    @Builder.Default
    private final String buyTradingAmount = EMPTY;
    @Builder.Default
    private final String shortPosition = EMPTY;
    @Builder.Default
    private final String longPosition = EMPTY;
    @NonNull
    private final T initialMtmAmount;
    @NonNull
    private final T dailyMtmAmount;
    @NonNull
    private final T swapPoint;
    @NonNull
    private final T totalVariationMargin;
    @NonNull
    private final String settlementDate;
    private final long orderId;
    private final int recordType;

}

