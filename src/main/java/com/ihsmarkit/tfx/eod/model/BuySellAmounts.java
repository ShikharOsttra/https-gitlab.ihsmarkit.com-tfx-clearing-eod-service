package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
@Getter
public class BuySellAmounts {

    private final BigDecimal buy;
    private final BigDecimal sell;

    public BuySellAmounts add(final BuySellAmounts buySellAmounts) {
        return BuySellAmounts.of(
            buy.add(buySellAmounts.getBuy()),
            sell.add(buySellAmounts.getSell())
        );
    }
}
