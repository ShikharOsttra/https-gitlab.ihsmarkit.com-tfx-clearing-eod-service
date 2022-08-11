package com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain;

import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TotalValue;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RebalanceTfxTotal implements TotalValue<RebalanceTfxTotal> {

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
    public RebalanceTfxTotal add(final RebalanceTfxTotal total) {
        return RebalanceTfxTotal.builder()
            .shortPositionPreviousDay(addBigDecimal(this, total, RebalanceTfxTotal::getShortPositionPreviousDay))
            .longPositionPreviousDay(addBigDecimal(this, total, RebalanceTfxTotal::getLongPositionPreviousDay))
            .sellTradingAmount(addBigDecimal(this, total, RebalanceTfxTotal::getSellTradingAmount))
            .buyTradingAmount(addBigDecimal(this, total, RebalanceTfxTotal::getBuyTradingAmount))
            .shortPosition(addBigDecimal(this, total, RebalanceTfxTotal::getShortPosition))
            .longPosition(addBigDecimal(this, total, RebalanceTfxTotal::getLongPosition))
            .initialMtmAmount(addBigDecimal(this, total, RebalanceTfxTotal::getInitialMtmAmount))
            .dailyMtmAmount(addBigDecimal(this, total, RebalanceTfxTotal::getDailyMtmAmount))
            .swapPoint(addBigDecimal(this, total, RebalanceTfxTotal::getSwapPoint))
            .totalVariationMargin(addBigDecimal(this, total, RebalanceTfxTotal::getTotalVariationMargin))
            .settlementDate(total.getSettlementDate())
            .build();
    }

}
