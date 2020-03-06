package com.ihsmarkit.tfx.eod.batch.ledger;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;
import one.util.streamex.EntryStream;

@UtilityClass
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class OrderUtils {

    /*
     * Builds an orderId from the numeric values(Number-s or numeric String-s)
     *
     * @param parts - numeric values or strings representing the numbers
     *
     */
    public static Long buildOrderId(final Object... parts) {
        final String concatenatedKey = Stream.of(parts)
            .map(Object::toString)
            .collect(joining());
        return Long.valueOf(concatenatedKey);

    }

    public static <T> Map<T, Integer> buildIndexBasedOrder(final List<T> items) {
        return buildIndexBasedOrder(EntryStream.of(items));
    }

    @SafeVarargs
    public static <T> Map<T, Integer> buildIndexBasedOrder(final T... items) {
        return buildIndexBasedOrder(EntryStream.of(items));
    }

    private static <T> Map<T, Integer> buildIndexBasedOrder(final EntryStream<Integer, T> items) {
        return items
            .invert()
            .toImmutableMap();
    }
}
