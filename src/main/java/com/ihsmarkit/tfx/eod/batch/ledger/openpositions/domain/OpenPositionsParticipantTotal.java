package com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain;

import java.math.BigDecimal;

import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TotalValue;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OpenPositionsParticipantTotal implements TotalValue<OpenPositionsParticipantTotal> {

    public static final OpenPositionsParticipantTotal ZERO = OpenPositionsParticipantTotal.builder().build();

    @Builder.Default
    private final BigDecimal initialMtmAmount = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal dailyMtmAmount = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal swapPoint = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal totalVariationMargin = BigDecimal.ZERO;

    @Override
    public OpenPositionsParticipantTotal add(final OpenPositionsParticipantTotal total) {
        return OpenPositionsParticipantTotal.builder()
            .initialMtmAmount(addBigDecimal(this, total, OpenPositionsParticipantTotal::getInitialMtmAmount))
            .dailyMtmAmount(addBigDecimal(this, total, OpenPositionsParticipantTotal::getDailyMtmAmount))
            .swapPoint(addBigDecimal(this, total, OpenPositionsParticipantTotal::getSwapPoint))
            .totalVariationMargin(addBigDecimal(this, total, OpenPositionsParticipantTotal::getTotalVariationMargin))
            .build();
    }

}
