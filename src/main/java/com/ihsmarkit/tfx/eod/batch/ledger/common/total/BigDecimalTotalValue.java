package com.ihsmarkit.tfx.eod.batch.ledger.common.total;

import java.math.BigDecimal;

import lombok.Value;

@Value(staticConstructor = "of")
public class BigDecimalTotalValue implements TotalValue<BigDecimalTotalValue> {

    public final static BigDecimalTotalValue ZERO = BigDecimalTotalValue.of(BigDecimal.ZERO);

    private final BigDecimal value;

    @Override
    public BigDecimalTotalValue add(final BigDecimalTotalValue total) {
        return of(this.value.add(total.value));
    }
}
