package com.ihsmarkit.tfx.eod.batch.ledger.openpositions.total;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OpenPositionsListItemTotal implements Serializable {

    public static final OpenPositionsListItemTotal ZERO = OpenPositionsListItemTotal.builder()
        .initialMtmAmount(BigDecimal.ZERO)
        .dailyMtmAmount(BigDecimal.ZERO)
        .swapPoint(BigDecimal.ZERO)
        .totalVariationMargin(BigDecimal.ZERO)
        .build();

    private final BigDecimal initialMtmAmount;
    private final BigDecimal dailyMtmAmount;
    private final BigDecimal swapPoint;
    private final BigDecimal totalVariationMargin;

    public OpenPositionsListItemTotal add(final OpenPositionsListItemTotal total) {
        return OpenPositionsListItemTotal.builder()
            .initialMtmAmount(this.initialMtmAmount.add(total.initialMtmAmount))
            .dailyMtmAmount(this.dailyMtmAmount.add(total.dailyMtmAmount))
            .swapPoint(this.swapPoint.add(total.swapPoint))
            .totalVariationMargin(this.totalVariationMargin.add(total.totalVariationMargin))
            .build();
    }

}
