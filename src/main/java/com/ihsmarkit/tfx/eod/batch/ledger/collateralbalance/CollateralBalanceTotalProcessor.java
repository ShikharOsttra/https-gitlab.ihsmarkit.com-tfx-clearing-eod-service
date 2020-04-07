package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance;

import java.util.stream.Collectors;

import org.springframework.data.util.Pair;

import com.google.common.collect.Table;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.CashSettlementTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.CollateralBalanceItem;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.CollateralBalanceTotal;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.SingleValueTotalHolder;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TotalProcessor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
public class CollateralBalanceTotalProcessor implements TotalProcessor<CollateralBalanceItem> {

    private final SingleValueTotalHolder<CollateralBalanceTotal> tfxTotal;

    @Override
    public CollateralBalanceItem process(final CollateralBalanceItem item) {
        final CollateralBalanceTotal total = CollateralBalanceTotal.builder()
            .collateralDeposits(item.getCollateralDeposits())
            .cashSettlements(item.getCashSettlements().cellSet().stream()
                .collect(Collectors.toMap(cell -> CashSettlementTotalKey.of(cell.getRowKey(), cell.getColumnKey()), Table.Cell::getValue)))
            .requiredAmount(item.getRequiredAmount())
            .initialMargin(item.getInitialMargin())
            .deficitInCashSettlement(item.getDeficitInCashSettlement())
            .nextClearingDepositRequiredAmount(item.getNextClearingDeposit().map(Pair::getSecond).orElse(null))
            .nextClearingDepositApplicableDate(item.getNextClearingDeposit().map(Pair::getFirst).orElse(null))
            .build();

        tfxTotal.contributeToTotals(total);
        return item;
    }

}
