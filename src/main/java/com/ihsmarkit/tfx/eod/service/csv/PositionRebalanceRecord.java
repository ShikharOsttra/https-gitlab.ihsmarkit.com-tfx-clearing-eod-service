package com.ihsmarkit.tfx.eod.service.csv;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PositionRebalanceRecord {

    private final LocalDate tradeDate;
    private final int tradeType;
    private final String participantCodeSource;
    private final String participantCodeTarget;
    private final String currencyPair;
    private final String side;
    private BigDecimal tradePrice;
    private BigDecimal baseCurrencyAmount;
    private BigDecimal valueCurrencyAmount;
    private LocalDate valueDate;
    private String tradeId;
    private LocalDateTime timestamp;
}
