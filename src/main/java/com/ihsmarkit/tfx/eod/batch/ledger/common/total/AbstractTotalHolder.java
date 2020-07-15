package com.ihsmarkit.tfx.eod.batch.ledger.common.total;

import javax.annotation.Nullable;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public abstract class AbstractTotalHolder<V> extends ItemStreamSupport implements TotalHolder<V> {

    private static final String KEY = "total";

    private final boolean saveState;

    public AbstractTotalHolder(final String name, final boolean saveState) {
        setName(name);
        this.saveState = saveState;
    }

    @Override
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public void open(final ExecutionContext executionContext) {
        if (saveState) {
            initValue((V) executionContext.get(getExecutionContextKey(KEY)));
        } else {
            initValue(null);
        }
    }

    protected abstract void initValue(@Nullable V value);

    @Override
    public void update(final ExecutionContext executionContext) {
        if (saveState) {
            executionContext.put(getExecutionContextKey(KEY), get());
        }
    }

}
