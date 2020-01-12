package com.ihsmarkit.tfx.eod.service.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class PositionRebalanceCSVWriterTest {

    private static String HEADER_ONLY = "Trade Date,Trade Type,Participant Code Source,Participant Code Target,Currency Pair,Side,Trade Price,Base Currency Amount,Value Currency Amount,Value Date,Trade ID,Timestamp\r\n";

    private static String ONE_ROW = HEADER_ONLY +
        "20190101,2,ORIGINATOR_01,CPARTY_01,USD/CHF,SELL,1.10123,10000000,12312323,20190102,tradeId_01,20190101 12:13:14\r\n";

    private final PositionRebalanceCSVWriter csvWriter = new PositionRebalanceCSVWriter();

    @Test
    void shouldReturnHeaderOnlyOnNoRecords() {

        final String recordsAsCsv = csvWriter.getRecordsAsCsv(List.of());
        assertThat(recordsAsCsv).isEqualTo(HEADER_ONLY);
    }

    @Test
    void shouldReturnSingleRowCsv() {
        final LocalDate tradeDate = LocalDate.of(2019, 1, 1);
        final String recordsAsCsv = csvWriter.getRecordsAsCsv(List.of(PositionRebalanceRecord.builder()
            .tradeDate(tradeDate)
            .participantCodeSource("ORIGINATOR_01")
            .participantCodeTarget("CPARTY_01")
            .currencyPair("USD/CHF")
            .side("SELL")
            .tradePrice(new BigDecimal("1.10123"))
            .baseCurrencyAmount(new BigDecimal("10000000"))
            .valueCurrencyAmount(new BigDecimal("12312323"))
            .valueDate(LocalDate.of(2019, 1, 2))
            .tradeId("tradeId_01")
            .timestamp(LocalDateTime.of(tradeDate, LocalTime.of(12, 13, 14)))
            .build()));
        assertThat(recordsAsCsv).isEqualTo(ONE_ROW);
    }
}
