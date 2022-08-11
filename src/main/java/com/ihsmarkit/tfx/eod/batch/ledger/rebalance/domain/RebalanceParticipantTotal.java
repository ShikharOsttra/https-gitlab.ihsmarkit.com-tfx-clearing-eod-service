package com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain;

import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TotalValue;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RebalanceParticipantTotal implements TotalValue<RebalanceParticipantTotal> {

    public static final RebalanceParticipantTotal ZERO = RebalanceParticipantTotal
        .builder().build();

    @Builder.Default
    private final BigDecimal initialMtmAmount = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal dailyMtmAmount = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal swapPoint = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal totalVariationMargin = BigDecimal.ZERO;

    @Override
    public RebalanceParticipantTotal add(final RebalanceParticipantTotal total) {
        return RebalanceParticipantTotal
            .builder()
            .initialMtmAmount(addBigDecimal(this, total, RebalanceParticipantTotal::getInitialMtmAmount))
            .dailyMtmAmount(addBigDecimal(this, total, RebalanceParticipantTotal::getDailyMtmAmount))
            .swapPoint(addBigDecimal(this, total, RebalanceParticipantTotal::getSwapPoint))
            .totalVariationMargin(addBigDecimal(this, total, RebalanceParticipantTotal::getTotalVariationMargin))
            .build();
    }

}
