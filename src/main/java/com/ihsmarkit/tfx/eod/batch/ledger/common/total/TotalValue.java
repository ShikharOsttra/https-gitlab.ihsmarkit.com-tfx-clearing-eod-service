package com.ihsmarkit.tfx.eod.batch.ledger.common.total;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.function.Function;

public interface TotalValue<V> extends Serializable {

    V add(V value);

    default BigDecimal addBigDecimal(V v1, V v2, final Function<V, BigDecimal> getter) {
        return getter.apply(v1).add(getter.apply(v2));
    }

}
