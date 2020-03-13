package com.ihsmarkit.tfx.eod.model.ledger;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(builderClassName = "Builder")
@AllArgsConstructor(staticName = "of")
public class DailyMarkedDataAggregated {
    private final BigDecimal shortPositionsAmount;
    private final BigDecimal openPrice;
    private final LocalDateTime openPriceTime;
    private final BigDecimal highPrice;
    private final LocalDateTime highPriceTime;
    private final BigDecimal lowPrice;
    private final LocalDateTime lowPriceTime;
    private final BigDecimal closePrice;
    private final LocalDateTime closePriceTime;
}
