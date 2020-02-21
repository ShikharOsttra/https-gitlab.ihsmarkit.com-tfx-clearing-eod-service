package com.ihsmarkit.tfx.eod.model.ledger;

import static org.apache.logging.log4j.util.Strings.EMPTY;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class DailyMarketDataEnriched {

    private final LocalDate businessDate;
    @Builder.Default
    private final String tradeDate = EMPTY;
    @Builder.Default
    private final String recordDate = EMPTY;
    @Builder.Default
    private final String currencyNumber = EMPTY;
    @Builder.Default
    private final String currencyPairCode = EMPTY;
    @Builder.Default
    private final String openPrice = EMPTY;
    @Builder.Default
    private final String openPriceTime = EMPTY;
    @Builder.Default
    private final String highPrice = EMPTY;
    @Builder.Default
    private final String highPriceTime = EMPTY;
    @Builder.Default
    private final String lowPrice = EMPTY;
    @Builder.Default
    private final String lowPriceTime = EMPTY;
    @Builder.Default
    private final String closePrice = EMPTY;
    @Builder.Default
    private final String closePriceTime = EMPTY;
    @Builder.Default
    private final String swapPoint = EMPTY;
    @Builder.Default
    private final String previousDsp = EMPTY;
    @Builder.Default
    private final String currentDsp = EMPTY;
    @Builder.Default
    private final String dspChange = EMPTY;
    @Builder.Default
    private final String tradingVolumeAmount = EMPTY;
    private final String tradingVolumeAmountInUnit;
    @Builder.Default
    private final String openPositionAmount = EMPTY;
    private final String openPositionAmountInUnit;
    private final long orderId;
    private final int recordType;
}