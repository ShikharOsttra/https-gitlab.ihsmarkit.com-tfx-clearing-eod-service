package com.ihsmarkit.tfx.eod.batch.ledger.common.total;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;

@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class MapTotalHolder<K extends Serializable, V extends TotalValue<V>> extends ItemStreamSupport implements TotalHolder<Map<K, V>> {

    private static final String KEY = "total";

    @Getter
    private ConcurrentMap<K, V> total;

    public MapTotalHolder(final String name) {
        setName(name);
    }

    public void contributeToTotals(final K key, final V totalContribution) {
        total.compute(key, (k, currentTotal) -> currentTotal == null ? totalContribution : totalContribution.add(currentTotal));
    }

    @Override
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public void open(final ExecutionContext executionContext) {
        if (executionContext.containsKey(getExecutionContextKey(KEY))) {
            total = (ConcurrentMap<K, V>) executionContext.get(getExecutionContextKey(KEY));
        } else {
            total = new ConcurrentHashMap<>();
        }
    }

    @Override
    public void update(final ExecutionContext executionContext) {
        executionContext.put(getExecutionContextKey(KEY), total);
    }

    @Override
    public Map<K, V> get() {
        return total;
    }
}
