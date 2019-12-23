package com.ihsmarkit.tfx.eod.batch.ledger.marketdata.model;

import java.util.Date;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class DailyMarketDataAggregate {
    private final Date businessDate;
    private final String tradeDate;
    private final String recordDate;
    private final String currencyNumber;
    private final String currencyPairCode;
    private final String openPrice;
    private final String openPriceTime;
    private final String highPrice;
    private final String highPriceTime;
    private final String lowPrice;
    private final String lowPriceTime;
    private final String closePrice;
    private final String closePriceTime;
    private final String swapPoint;
    private final String previousDsp;
    private final String currentDsp;
    private final String dspChange;
    private final String tradingVolumeAmount;
    private final String tradingVolumeAmountInUnit;
    private final String openPositionAmount;
    private final String openPositionAmountInUit;
}