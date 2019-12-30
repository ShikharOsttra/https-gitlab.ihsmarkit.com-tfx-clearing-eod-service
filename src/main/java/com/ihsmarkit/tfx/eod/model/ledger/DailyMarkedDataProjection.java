package com.ihsmarkit.tfx.eod.model.ledger;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor(staticName = "of")
public final class DailyMarkedDataProjection {

    private final long tradeId;
    private final long currencyPairId;
    private final String currencyPairCode;
    private final String productNumber;
    private final BigDecimal valueAmount;
    private final LocalDateTime versionTsp;

}