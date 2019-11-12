package com.ihsmarkit.tfx.eod.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.junit.jupiter.api.Test;

import lombok.AllArgsConstructor;
import lombok.Data;

class SlicerTest {

    @Test
    void shouldSlice() {
        Queue<Pair> queue = new LinkedList<Pair>(List.of(
            new Pair("a", BigDecimal.valueOf(13)),
            new Pair("b", BigDecimal.valueOf(7)),
            new Pair("c", BigDecimal.valueOf(8))
        ));

        Slicer<Pair> slicer = new Slicer<Pair>(queue, Pair::getValue);

        assertThat(slicer.produce(BigDecimal.valueOf(14), (a, b) -> a.getName() + " " + b.toString()))
            .containsExactly("a 13", "b 1");
        assertThat(slicer.produce(BigDecimal.valueOf(8), (a, b) -> a.getName() + " " + b.toString()))
            .containsExactly("b 6", "c 2");
        assertThat(slicer.produce(BigDecimal.valueOf(6), (a, b) -> a.getName() + " " + b.toString()))
            .containsExactly("c 6");

        assertThat(catchThrowable(() -> slicer.produce(BigDecimal.ONE, (a, b) -> a.getName() + " " + b.toString()).findAny()))
            .isInstanceOf(NoSuchElementException.class);

    }

    @Test
    void shouldReturnEmptyWhenAskedToProduceZero() {
        Queue<Pair> queue = new LinkedList<Pair>(List.of(
            new Pair("c", BigDecimal.valueOf(8))
        ));

        Slicer<Pair> slicer = new Slicer<Pair>(queue, Pair::getValue);
        assertThat(slicer.produce(BigDecimal.ZERO, (a, b) -> a)).isEmpty();
    }

    @AllArgsConstructor
    @Data
    private static final class Pair {
        private final String name;
        private final BigDecimal value;
    }

}