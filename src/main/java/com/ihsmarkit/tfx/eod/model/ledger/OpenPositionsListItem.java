package com.ihsmarkit.tfx.eod.model.ledger;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Value
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class OpenPositionsListItem<T> {

    private final LocalDate businessDate;
    @Builder.Default
    private final String tradeDate = EMPTY;
    @Builder.Default
    private final String recordDate = EMPTY;
    private final String participantCode;
    private final String participantName;
    @Builder.Default
    private final String participantType = EMPTY;
    @Builder.Default
    private final String currencyNo = EMPTY;
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
    private final T initialMtmAmount;
    private final T dailyMtmAmount;
    private final T swapPoint;
    private final T totalVariationMargin;
    private final String settlementDate;
    private final long orderId;
    private final int recordType;

}

