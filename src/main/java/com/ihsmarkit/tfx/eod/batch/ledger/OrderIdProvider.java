package com.ihsmarkit.tfx.eod.batch.ledger;

import static com.ihsmarkit.tfx.eod.batch.ledger.OrderUtils.buildIndexBasedOrder;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.util.Lazy;

public abstract class OrderIdProvider {

    private final Lazy<Map<String, Integer>> orderIdMap = Lazy.of(this::loadOrderIdMap);

    public int get(final String key) {
        return Optional.ofNullable(orderIdMap.get().get(key))
            .orElseThrow(() -> new IllegalStateException(String.format("Cannot final order id for key [%s]", key)));
    }

    private Map<String, Integer> loadOrderIdMap() {
        final List<String> sortedCodes = loadDataStream()
            .collect(toList());

        return buildIndexBasedOrder(sortedCodes);
    }

    abstract Stream<String> loadDataStream();
}