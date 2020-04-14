package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain;


import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.BOND;
import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.CASH;
import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.EQUITY;
import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.LOG;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

import com.ihsmarkit.tfx.core.domain.type.CollateralProductType;

import lombok.Getter;
import lombok.Value;

@Value(staticConstructor = "of")
@Getter
public class CollateralDeposit implements Serializable {

    public static final CollateralDeposit ZERO = CollateralDeposit.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    private final BigDecimal total;
    private final BigDecimal cash;
    private final BigDecimal lg;
    private final BigDecimal securities;

    public static CollateralDeposit of(final CollateralProductType type, final BigDecimal amount) {
        return CollateralDeposit.of(
            amount,
            matchTypeOrZero(amount, type, CASH),
            matchTypeOrZero(amount, type, LOG),
            matchTypeOrZero(amount, type, BOND, EQUITY)
        );
    }

    public CollateralDeposit add(final CollateralDeposit balance) {
        return CollateralDeposit.of(
            total.add(balance.getTotal()),
            cash.add(balance.getCash()),
            lg.add(balance.getLg()),
            securities.add(balance.getSecurities())
        );
    }

    private static BigDecimal matchTypeOrZero(final BigDecimal amount, final CollateralProductType matchedType, final CollateralProductType... types) {
        return Set.of(types).contains(matchedType) ? amount : BigDecimal.ZERO;
    }
}
