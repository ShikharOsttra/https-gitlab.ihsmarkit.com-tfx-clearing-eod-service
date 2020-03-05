package com.ihsmarkit.tfx.eod.batch.ledger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;

import lombok.RequiredArgsConstructor;

public abstract class AbstractTotalProcessor<K, V, T, I, R> {

    private final ConcurrentMap<K, V> totals = new ConcurrentHashMap<>();

    public ItemProcessor<T, R> wrapItemProcessor(final ItemProcessor<T, I> delegate, final Function<I, R> finisher) {
        return new ItemProcessorWithTotal<>(delegate, this::contributeToTotals, finisher);
    }

    public ItemWriter<R> wrapItemWriter(final ItemWriter<R> delegate) {
        return new ItemWriterWithTotal<>(delegate, () -> extractTotals(totals));
    }

    protected abstract K toTotalKey(I intermediateItem);

    protected abstract V toTotalValue(I intermediateItem);

    protected abstract V merge(V prev, V stepContribution);

    protected abstract List<R> extractTotals(Map<K, V> totals);

    private void contributeToTotals(final I intermediateResult) {
        final var totalKey = toTotalKey(intermediateResult);
        final var totalValue = toTotalValue(intermediateResult);
        totals.compute(totalKey, (key, currentTotal) -> currentTotal == null ? totalValue : merge(currentTotal, totalValue));
    }

    @RequiredArgsConstructor
    private static class ItemProcessorWithTotal<T, I, R> implements ItemProcessor<T, R> {

        private final ItemProcessor<T, I> delegate;
        private final Consumer<I> totalContributor;
        private final Function<I, R> finisher;

        @Override
        public R process(final T item) throws Exception {
            final I intermediateResult = delegate.process(item);
            totalContributor.accept(intermediateResult);
            return finisher.apply(intermediateResult);
        }
    }

    @RequiredArgsConstructor
    private static class ItemWriterWithTotal<R> extends ItemStreamSupport implements ItemWriter<R> {

        private final ItemWriter<R> delegate;
        private final Supplier<List<R>> totalsSupplier;

        @Override
        public void write(final List<? extends R> items) throws Exception {
            delegate.write(items);
        }

        @Override
        public void close() {
            try {
                write(totalsSupplier.get());
            } catch (final Exception ex) {
                throw new ItemStreamException(ex);
            }
        }
    }
}
