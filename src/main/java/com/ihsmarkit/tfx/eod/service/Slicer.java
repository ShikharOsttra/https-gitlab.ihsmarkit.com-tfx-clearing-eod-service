package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Slicer<T> {

    private final Queue<T> list;
    private final Function<T, BigDecimal> accessor;

    @Nullable
    private BigDecimal toConsume;

    @Nullable
    private T current;

    public Slicer(final Queue<T> list, final Function<T, BigDecimal> accessor) {
        this.list = list;
        this.accessor = accessor;
    }

    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private void peek() {
        while (toConsume == null || toConsume.compareTo(BigDecimal.ZERO) == 0) {
            current = list.remove();
            toConsume = accessor.apply(current);
        }
    }

    public <R> Stream<R> produce(final BigDecimal amount, final BiFunction<T, BigDecimal, R> producer) {

        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(new ProducingIterator<R>(amount, producer), 0),
            false
        );
    }

    private final class ProducingIterator<R> implements Iterator<R> {

        private final BiFunction<T, BigDecimal, R> producer;
        private BigDecimal toProduce;

        private ProducingIterator(final BigDecimal toProduce, final BiFunction<T, BigDecimal, R> producer) {
            this.toProduce = toProduce;
            this.producer = producer;
        }


        @Override
        public boolean hasNext() {
            return toProduce.compareTo(BigDecimal.ZERO) > 0;
        }

        @Override
        public R next() {

            if (toProduce.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NoSuchElementException();
            }

            peek();
            R result;
            if (toProduce.compareTo(toConsume) > 0) {
                toProduce = toProduce.subtract(toConsume);
                result = producer.apply(current, toConsume);
                toConsume = BigDecimal.ZERO;

            } else {
                toConsume = toConsume.subtract(toProduce);
                result = producer.apply(current, toProduce);
                toProduce = BigDecimal.ZERO;
            }
            return result;
        }
    }
}