package com.ihsmarkit.tfx.eod.batch.ledger.common.total;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class SingleValueTotalHolder<V extends TotalValue<V>> extends AbstractTotalHolder<V> {

    private AtomicReference<V> total;

    public SingleValueTotalHolder(final String name, final boolean saveState) {
        super(name, saveState);
    }

    public void contributeToTotals(final V totalContribution) {
        total.getAndUpdate(currentTotal -> currentTotal == null ? totalContribution : currentTotal.add(totalContribution));
    }

    @Override
    protected void initValue(@Nullable final V value) {
        total = new AtomicReference<>(value);
    }

    @Override
    public V get() {
        return total.get();
    }
}
