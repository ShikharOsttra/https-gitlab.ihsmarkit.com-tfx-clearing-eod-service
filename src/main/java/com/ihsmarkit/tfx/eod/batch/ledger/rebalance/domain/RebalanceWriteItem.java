package com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class RebalanceWriteItem {

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
    private final String initialMtmAmount;
    @NonNull
    private final String dailyMtmAmount;
    @NonNull
    private final String swapPoint;
    @NonNull
    private final String totalVariationMargin;
    @NonNull
    private final String settlementDate;
    private final long orderId;
    private final int recordType;

}

