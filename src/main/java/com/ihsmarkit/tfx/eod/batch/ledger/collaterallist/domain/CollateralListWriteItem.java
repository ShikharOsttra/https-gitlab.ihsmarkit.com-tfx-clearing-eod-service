package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.time.LocalDate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class CollateralListWriteItem {

    @NonNull
    private final LocalDate businessDate;
    @Builder.Default
    private final String tradeDate = EMPTY;
    @Builder.Default
    private final String evaluationDate = EMPTY;
    @Builder.Default
    private final String recordDate = EMPTY;
    @Builder.Default
    private final String participantCode = EMPTY;
    @Builder.Default
    private final String participantName = EMPTY;
    @Builder.Default
    private final String participantType = EMPTY;
    @Builder.Default
    private final String collateralPurposeType = EMPTY;
    @Builder.Default
    private final String collateralPurpose = EMPTY;
    @Builder.Default
    private final String collateralName = EMPTY;
    @Builder.Default
    private final String collateralType = EMPTY;
    @Builder.Default
    private final String collateralTypeNo = EMPTY;
    @Builder.Default
    private final String securityCode = EMPTY;
    @Builder.Default
    private final String isinCode = EMPTY;
    @Builder.Default
    private final String amount = EMPTY;
    @Builder.Default
    private final String marketPrice = EMPTY;
    @Builder.Default
    private final String evaluatedPrice = EMPTY;
    @Builder.Default
    private final String evaluatedAmount = EMPTY;
    @Builder.Default
    private final String bojCode = EMPTY;
    @Builder.Default
    private final String jasdecCode = EMPTY;
    @Builder.Default
    private final String interestPaymentDay = EMPTY;
    @Builder.Default
    private final String interestPaymentDay2 = EMPTY;
    @Builder.Default
    private final String maturityDate = EMPTY;
    private final long orderId;
    private final int recordType;

}
