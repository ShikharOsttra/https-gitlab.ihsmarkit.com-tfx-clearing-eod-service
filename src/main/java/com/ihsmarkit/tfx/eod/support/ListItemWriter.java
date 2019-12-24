package com.ihsmarkit.tfx.eod.support;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import lombok.AllArgsConstructor;


@AllArgsConstructor
public class ListItemWriter<T> implements ItemWriter<List<T>>, ItemStream, InitializingBean {

    private final ItemWriter<T> delegate;

    @Override
    public void write(final List<? extends List<T>> lists) throws Exception {
        final List<T> items = lists.stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());

        delegate.write(items);
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(delegate, "You must set a delegate!");
    }

    @Override
    public void open(final ExecutionContext executionContext) {
        if (delegate instanceof ItemStream) {
            ((ItemStream) delegate).open(executionContext);
        }
    }

    @Override
    public void update(final ExecutionContext executionContext) {
        if (delegate instanceof ItemStream) {
            ((ItemStream) delegate).update(executionContext);
        }
    }

    @Override
    public void close() {
        if (delegate instanceof ItemStream) {
            ((ItemStream) delegate).close();
        }
    }

}