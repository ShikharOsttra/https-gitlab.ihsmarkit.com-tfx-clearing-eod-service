package com.ihsmarkit.tfx.eod.batch.ledger.common.total;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class MapTotalHolder<K extends Serializable, V extends TotalValue<V>> extends AbstractTotalHolder<ConcurrentMap<K, V>> {

    private ConcurrentMap<K, V> total;

    public MapTotalHolder(final String name, final boolean saveState) {
        super(name, saveState);
    }

    public void contributeToTotals(final K key, final V totalContribution) {
        total.compute(key, (k, currentTotal) -> currentTotal == null ? totalContribution : totalContribution.add(currentTotal));
    }

    @Override
    protected void initValue(@Nullable final ConcurrentMap<K, V> value) {
        total = Objects.requireNonNullElseGet(value, ConcurrentHashMap::new);
    }

    @Override
    public ConcurrentMap<K, V> get() {
        return total;
    }
}
