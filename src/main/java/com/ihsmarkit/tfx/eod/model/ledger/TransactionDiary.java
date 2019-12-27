package com.ihsmarkit.tfx.eod.model.ledger;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Value
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class TransactionDiary {

    private final LocalDate businessDate;
    private final String tradeDate;
    private final String recordDate;
    private final String participantCode;
    private final String participantName;
    private final String participantType;
    private final String currencyNo;
    private final String currencyPair;
    private final String matchDate;
    private final String matchTime;
    private final String matchId;
    private final String clearDate;
    private final String clearTime;
    private final String clearingId;
    private final String tradePrice;
    private final String sellAmount;
    private final String buyAmount;
    private final String counterpartyCode;
    private final String counterpartyType;
    private final String dsp;
    private final String dailyMtMAmount;
    private final String swapPoint;
    private final String outstandingPositionAmount;
    private final String settlementDate;
    private final String tradeId;
    private final String tradeType;
    private final String reference;
    private final String userReference;
}
