package com.ihsmarkit.tfx.eod.batch.ledger.common.total;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import javax.annotation.Nullable;

public interface TotalValue<V> extends Serializable {

    V add(V value);

    @Nullable
    default BigDecimal addBigDecimal(V v1, V v2, final Function<V, BigDecimal> getter) {
        return addNullables(v1, v2, getter, BigDecimal::add);
    }

    @Nullable
    default <MK, MV> Map<MK, MV> addMap(
        final V v1,
        final V v2,
        final Function<V, Map<MK, MV>> getter,
        final BinaryOperator<MV> remappingFunction
    ) {
        return addNullables(v1, v2, getter, (map1, map2) -> {
            final Map<MK, MV> result = new HashMap<>(map1);
            map2.forEach((k, v) -> result.merge(k, v, remappingFunction));
            return result;
        });
    }

    @Nullable
    default <R> R addNullables(V v1, V v2, final Function<V, R> getter, final BinaryOperator<R> remappingFunction) {
        @Nullable
        final R mappedValue1 = getter.apply(v1);
        @Nullable
        final R mappedValue2 = getter.apply(v2);

        if (mappedValue1 == null) {
            return mappedValue2;
        }

        if (mappedValue2 == null) {
            return mappedValue1;
        }

        return remappingFunction.apply(mappedValue1, mappedValue2);
    }

}
