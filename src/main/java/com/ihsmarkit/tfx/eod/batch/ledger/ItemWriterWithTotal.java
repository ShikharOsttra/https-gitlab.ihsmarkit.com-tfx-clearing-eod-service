package com.ihsmarkit.tfx.eod.batch.ledger;

import java.util.List;

import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class ItemWriterWithTotal<T> extends ItemStreamSupport implements ItemWriter<T> {

    private final TotalSupplier<T> totalSupplier;

    private final ItemWriter<T> delegate;

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
