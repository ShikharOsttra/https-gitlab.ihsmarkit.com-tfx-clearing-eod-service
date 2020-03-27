package com.ihsmarkit.tfx.eod.model.ledger;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class CollateralBalanceItem {

    @NonNull
    private final LocalDate businessDate;
    @NonNull
    private final String tradeDate;
    @NonNull
    private final String recordDate;
    @NonNull
    private final String evaluationDate;
    @NonNull
    private final String participantCode;
    @NonNull
    private final String participantName;
    @NonNull
    private final String participantType;
    @Builder.Default
    private final String collateralPurposeType = EMPTY;
    @NonNull
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
    @NonNull
    private final String totalExcessDeficit;
    @Builder.Default
    private final String deficitInCashSettlement = EMPTY;
    @Builder.Default
    private final String cashSettlement = EMPTY;
    @Builder.Default
    private final String cashSettlementFollowingDay = EMPTY;
    @Builder.Default
    private final String initialMtmTotal = "0";
    @Builder.Default
    private final String initialMtmDay = "0";
    @Builder.Default
    private final String initialMtmFollowingDay = "0";
    @Builder.Default
    private final String dailyMtmTotal = "0";
    @Builder.Default
    private final String dailyMtmDay = "0";
    @Builder.Default
    private final String dailyMtmFollowingDay = "0";
    @Builder.Default
    private final String swapPointTotal = "0";
    @Builder.Default
    private final String swapPointDay = "0";
    @Builder.Default
    private final String swapPointFollowingDay = "0";
    @Builder.Default
    private final String followingApplicableDayForClearingDeposit = EMPTY;
    private final long orderId;
    private final int recordType;

}
