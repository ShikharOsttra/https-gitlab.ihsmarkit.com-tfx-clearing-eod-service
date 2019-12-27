package com.ihsmarkit.tfx.eod.model.ledger;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class CollateralBalanceItem {

    private final LocalDate businessDate;
    private final String tradeDate;
    private final String recordDate;
    private final String participantCode;
    private final String participantName;
    private final String participantType;
    private final String collateralPurposeType;
    private final String collateralPurpose;
    private final String totalDeposit;
    private final String cash;
    private final String lg;
    private final String securities;
    private final String requiredAmount;
    private final String totalInitialMargin;
    private final String totalVariationMargin;
    private final String totalExcessDeficit;
    private final String deficitInCashSettlement;
    private final String cashSettlement;
    private final String cashSettlementFollowingDay;
    private final String initialMtmTotal;
    private final String initialMtmDay;
    private final String initialMtmFollowingDay;
    private final String dailyMtmTotal;
    private final String dailyMtmDay;
    private final String dailyMtmFollowingDay;
    private final String swapPointTotal;
    private final String swapPointDay;
    private final String swapPointFollowingDay;
    private final String followingApplicableDayForClearingDeposit;

}
