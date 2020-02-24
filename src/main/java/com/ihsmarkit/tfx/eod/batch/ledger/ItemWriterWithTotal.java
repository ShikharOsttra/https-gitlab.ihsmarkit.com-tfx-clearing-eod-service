package com.ihsmarkit.tfx.eod.batch.ledger;

import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.ItemWriter;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class ItemWriterWithTotal<T> implements ItemStreamWriter<T> {

    private final TotalSupplier<T> totalSupplier;

    private final ItemWriter<T> delegate;

    @Override
    public void open(final ExecutionContext executionContext) {
    }

    @Override
    public void update(final ExecutionContext executionContext) {
    }

    @Override
    public void close() {
        try {
            write(totalSupplier.get());
        } catch (final Exception ex) {
            throw new ItemStreamException(ex);
        }
    }

    @Override
    public void write(final List<? extends T> items) throws Exception {
        delegate.write(items);
    }

}
