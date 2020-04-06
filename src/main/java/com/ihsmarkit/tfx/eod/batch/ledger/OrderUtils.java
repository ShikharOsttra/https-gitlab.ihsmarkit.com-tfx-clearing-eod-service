package com.ihsmarkit.tfx.eod.batch.ledger;

import static java.util.stream.Collectors.joining;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
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

    public static <T extends Comparable<T>> Comparator<T> matchingComparator(final Predicate<T> predicate, final boolean returnFirst) {
        return (o1, o2) -> {
            if (predicate.test(o1) && predicate.test(o2)) {
                return 0;
            } else if (predicate.test(o1)) {
                return returnFirst ? -1 : 1;
            } else if (predicate.test(o2)) {
                return returnFirst ? 1 : -1;
            } else {
                return o1.compareTo(o2);
            }
        };
    }

    private static <T> Map<T, Integer> buildIndexBasedOrder(final EntryStream<Integer, T> items) {
        return items
            .invert()
            .toImmutableMap();
    }
}
