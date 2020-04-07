package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.springframework.util.comparator.Comparators;

import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TotalValue;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
@RequiredArgsConstructor
public class CollateralBalanceTotal implements TotalValue<CollateralBalanceTotal> {

    @NonNull
    private final Map<CollateralPurpose, CollateralDeposit> collateralDeposits;
    @NonNull
    private final Map<CashSettlementTotalKey, BigDecimal> cashSettlements;
    @NonNull
    private final Map<CollateralPurpose, BigDecimal> requiredAmount;
    @Nullable
    private final BigDecimal initialMargin;
    @Nullable
    private final BigDecimal deficitInCashSettlement;
    @Nullable
    private final BigDecimal nextClearingDepositRequiredAmount;
    @Nullable
    private final LocalDate nextClearingDepositApplicableDate;


    @Override
    public CollateralBalanceTotal add(final CollateralBalanceTotal total) {

        return CollateralBalanceTotal.builder()
            .collateralDeposits(addMap(this, total, CollateralBalanceTotal::getCollateralDeposits, CollateralDeposit::add))
            .cashSettlements(addMap(this, total, CollateralBalanceTotal::getCashSettlements, BigDecimal::add))
            .requiredAmount(addMap(this, total, CollateralBalanceTotal::getRequiredAmount, BigDecimal::add))
            .initialMargin(addBigDecimal(this, total, CollateralBalanceTotal::getInitialMargin))
            .deficitInCashSettlement(addBigDecimal(this, total, CollateralBalanceTotal::getDeficitInCashSettlement))
            .nextClearingDepositRequiredAmount(addBigDecimal(this, total, CollateralBalanceTotal::getNextClearingDepositRequiredAmount))
            .nextClearingDepositApplicableDate(minApplicableDate(this, total))
            .build();
    }

    @Nullable
    private LocalDate minApplicableDate(final CollateralBalanceTotal total1, final CollateralBalanceTotal total2) {
        return addNullables(
            total1,
            total2,
            CollateralBalanceTotal::getNextClearingDepositApplicableDate,
            (date1, date2) -> Stream.of(date1, date2).min(Comparators.comparable()).orElse(null)
        );
    }

}
