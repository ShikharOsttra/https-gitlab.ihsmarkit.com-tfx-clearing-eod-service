package com.ihsmarkit.tfx.eod.model.ledger;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Value
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class OpenPositionsListItem {
    private final LocalDate businessDate;
    private final String tradeDate;
    private final String recordDate;
    private final String participantCode;
    private final String participantName;
    private final String participantType;
    private final String currencyNo;
    private final String currencyCode;
    private final String shortPositionPreviousDay;
    private final String longPositionPreviousDay;
    private final String sellTradingAmount;
    private final String buyTradingAmount;
    private final String shortPosition;
    private final String longPosition;
    private final String initialMtmAmount;
    private final String dailyMtmAmount;
    private final String swapPoint;
    private final String totalVariationMargin;
    private final String settlementDate;
}

