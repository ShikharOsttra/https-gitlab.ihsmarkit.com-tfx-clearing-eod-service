package com.ihsmarkit.tfx.eod.model.ledger;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class CollateralListItem {

    private final LocalDate businessDate;
    @Builder.Default
    private final String tradeDate = EMPTY;
    @Builder.Default
    private final String evaluationDate = EMPTY;
    @Builder.Default
    private final String recordDate = EMPTY;
    private final String participantCode;
    private final String participantName;
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
    private final String evaluatedAmount;
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
