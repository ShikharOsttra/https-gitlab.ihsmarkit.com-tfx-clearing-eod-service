package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance;


import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.BOND;
import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.CASH;
import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.EQUITY;
import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.LOG;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.util.Set;

import com.ihsmarkit.tfx.core.domain.type.CollateralProductType;

import lombok.Getter;
import lombok.Value;

@Value(staticConstructor = "of")
@Getter
class TotalBalance {

    private final BigDecimal total;
    private final BigDecimal cash;
    private final BigDecimal lg;
    private final BigDecimal securities;

    static TotalBalance of(final CollateralProductType type, final BigDecimal amount) {
        return TotalBalance.of(
            amount,
            matchTypeOrZero(amount, type, CASH),
            matchTypeOrZero(amount, type, LOG),
            matchTypeOrZero(amount, type, BOND, EQUITY)
        );
    }

    TotalBalance add(final TotalBalance balance) {
        return TotalBalance.of(
            total.add(balance.getTotal()),
            cash.add(balance.getCash()),
            lg.add(balance.getLg()),
            securities.add(balance.getSecurities())
        );
    }

    private static BigDecimal matchTypeOrZero(final BigDecimal amount, final CollateralProductType matchedType, final CollateralProductType... types) {
        return Set.of(types).contains(matchedType) ? amount : ZERO;
    }
}
