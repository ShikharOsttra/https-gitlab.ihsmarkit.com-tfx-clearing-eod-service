package com.ihsmarkit.tfx.eod.model.ledger;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Value
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class CollateralListItem {

    private final LocalDate businessDate;
    private final String tradeDate;
    private final String evaluationDate;
    private final String recordDate;
    private final String participantCode;
    private final String participantName;
    private final String participantType;
    private final String collateralPurposeType;
    private final String collateralPurpose;
    private final String collateralName;
    private final String collateralType;
    private final String securityCode;
    private final String isinCode;
    private final String amount;
    private final String marketPrice;
    private final String evaluatedPrice;
    private final String evaluatedAmount;
    private final String bojCode;
    private final String jasdecCode;
    private final String interestPaymentDay;
    private final String interestPaymentDay2;
    private final String maturityDate;

}
