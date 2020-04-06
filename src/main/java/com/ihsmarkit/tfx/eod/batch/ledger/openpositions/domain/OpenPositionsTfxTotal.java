package com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.annotation.Nullable;

import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TotalValue;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OpenPositionsTfxTotal implements TotalValue<OpenPositionsTfxTotal> {

    public static final OpenPositionsItem ZERO = OpenPositionsItem.builder().build();

    @Builder.Default
    private final BigDecimal shortPositionPreviousDay = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal longPositionPreviousDay = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal sellTradingAmount = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal buyTradingAmount = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal shortPosition = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal longPosition = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal initialMtmAmount = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal dailyMtmAmount = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal swapPoint = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal totalVariationMargin = BigDecimal.ZERO;
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
