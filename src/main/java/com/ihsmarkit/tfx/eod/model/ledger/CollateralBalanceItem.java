package com.ihsmarkit.tfx.eod.model.ledger;

import static org.apache.commons.lang3.StringUtils.EMPTY;

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
    @Builder.Default
    private final String collateralPurposeType = EMPTY;
    private final String collateralPurpose;
    @Builder.Default
    private final String totalDeposit = EMPTY;
    @Builder.Default
    private final String cash = EMPTY;
    @Builder.Default
    private final String lg = EMPTY;
    @Builder.Default
    private final String securities = EMPTY;
    private final String requiredAmount;
    @Builder.Default
    private final String totalInitialMargin = EMPTY;
    @Builder.Default
    private final String totalVariationMargin = EMPTY;
    private final String totalExcessDeficit;
    @Builder.Default
    private final String deficitInCashSettlement = EMPTY;
    @Builder.Default
    private final String cashSettlement = EMPTY;
    @Builder.Default
    private final String cashSettlementFollowingDay = EMPTY;
    @Builder.Default
    private final String initialMtmTotal = EMPTY;
    @Builder.Default
    private final String initialMtmDay = EMPTY;
    @Builder.Default
    private final String initialMtmFollowingDay = EMPTY;
    @Builder.Default
    private final String dailyMtmTotal = EMPTY;
    @Builder.Default
    private final String dailyMtmDay = EMPTY;
    @Builder.Default
    private final String dailyMtmFollowingDay = EMPTY;
    @Builder.Default
    private final String swapPointTotal = EMPTY;
    @Builder.Default
    private final String swapPointDay = EMPTY;
    @Builder.Default
    private final String swapPointFollowingDay = EMPTY;
    @Builder.Default
    private final String followingApplicableDayForClearingDeposit = EMPTY;
    private final long orderId;
    private final int recordType;

}
