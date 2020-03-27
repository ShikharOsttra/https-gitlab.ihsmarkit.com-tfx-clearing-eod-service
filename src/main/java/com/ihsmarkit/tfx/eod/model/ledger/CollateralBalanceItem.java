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
    @Builder.Default
    private final String requiredAmount = EMPTY;
    @Builder.Default
    private final String totalInitialMargin = EMPTY;
    @Builder.Default
    private final String totalVariationMargin = EMPTY;
    @Builder.Default
    private final String totalExcessDeficit = EMPTY;
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
