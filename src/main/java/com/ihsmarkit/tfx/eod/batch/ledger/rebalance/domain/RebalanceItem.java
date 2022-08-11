package com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain;

import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RebalanceItem {

    @NotNull
    private ParticipantAndCurrencyPair participantAndCurrencyPair;
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

}
