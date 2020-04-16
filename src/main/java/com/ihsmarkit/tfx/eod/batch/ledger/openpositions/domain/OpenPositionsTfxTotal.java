package com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TotalValue;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OpenPositionsTfxTotal implements TotalValue<OpenPositionsTfxTotal> {

    @NotNull
    private final BigDecimal shortPositionPreviousDay;
    @NotNull
    private final BigDecimal longPositionPreviousDay;
    @Nullable
    private final BigDecimal sellTradingAmount;
    @Nullable
    private final BigDecimal buyTradingAmount;
    @NotNull
    private final BigDecimal shortPosition;
    @NotNull
    private final BigDecimal longPosition;
    @NotNull
    private final BigDecimal initialMtmAmount;
    @NotNull
    private final BigDecimal dailyMtmAmount;
    @NotNull
    private final BigDecimal swapPoint;
    @NotNull
    private final BigDecimal totalVariationMargin;
    @Nullable
    private final LocalDate settlementDate;

    @Override
    public OpenPositionsTfxTotal add(final OpenPositionsTfxTotal total) {
        return OpenPositionsTfxTotal.builder()
            .shortPositionPreviousDay(addBigDecimal(this, total, OpenPositionsTfxTotal::getShortPositionPreviousDay))
            .longPositionPreviousDay(addBigDecimal(this, total, OpenPositionsTfxTotal::getLongPositionPreviousDay))
            .sellTradingAmount(addBigDecimal(this, total, OpenPositionsTfxTotal::getSellTradingAmount))
            .buyTradingAmount(addBigDecimal(this, total, OpenPositionsTfxTotal::getBuyTradingAmount))
            .shortPosition(addBigDecimal(this, total, OpenPositionsTfxTotal::getShortPosition))
            .longPosition(addBigDecimal(this, total, OpenPositionsTfxTotal::getLongPosition))
            .initialMtmAmount(addBigDecimal(this, total, OpenPositionsTfxTotal::getInitialMtmAmount))
            .dailyMtmAmount(addBigDecimal(this, total, OpenPositionsTfxTotal::getDailyMtmAmount))
            .swapPoint(addBigDecimal(this, total, OpenPositionsTfxTotal::getSwapPoint))
            .totalVariationMargin(addBigDecimal(this, total, OpenPositionsTfxTotal::getTotalVariationMargin))
            .settlementDate(total.getSettlementDate())
            .build();
    }

}
