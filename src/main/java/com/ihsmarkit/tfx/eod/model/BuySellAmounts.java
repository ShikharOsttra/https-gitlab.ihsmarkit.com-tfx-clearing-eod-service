package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
@Getter
public class BuySellAmounts {

    private static final BuySellAmounts EMPTY_BUY_SELL_AMOUNTS = BuySellAmounts.of(BigDecimal.ZERO, BigDecimal.ZERO);

    private final BigDecimal buy;
    private final BigDecimal sell;

    public BuySellAmounts add(final BuySellAmounts buySellAmounts) {
        return BuySellAmounts.of(
            buy.add(buySellAmounts.getBuy()),
            sell.add(buySellAmounts.getSell())
        );
    }

    public static BuySellAmounts empty() {
        return EMPTY_BUY_SELL_AMOUNTS;
    }
}
