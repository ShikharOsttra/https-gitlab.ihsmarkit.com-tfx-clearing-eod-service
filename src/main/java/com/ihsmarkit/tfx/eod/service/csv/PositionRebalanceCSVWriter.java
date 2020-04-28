package com.ihsmarkit.tfx.eod.service.csv;

import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;

@Component
public class PositionRebalanceCSVWriter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");


    private static final Object[] HEADER_LINE = {
        "Trade Date",
        "Trade Type",
        "Participant Code Source",
        "Participant Code Target",
        "Currency Pair",
        "Side",
        "Trade Price",
        "Base Currency Amount",
        "Value Currency Amount",
        "Value Date",
        "Trade ID",
        "Timestamp"
    };

    @SneakyThrows
    public String getRecordsAsCsv(final List<PositionRebalanceRecord> records) {
        try (StringWriter writer = new StringWriter();
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            printRecords(csvPrinter, records);
            csvPrinter.flush();
            return writer.toString();
        }
    }

    @SneakyThrows
    private void printRecords(final CSVPrinter csvPrinter, final List<PositionRebalanceRecord> items) {

        csvPrinter.printRecord(HEADER_LINE);
        for (final PositionRebalanceRecord item : items) {
            csvPrinter.printRecord(
                DATE_FORMATTER.format(item.getTradeDate()),
                item.getTradeType(),
                item.getParticipantCodeSource(),
                item.getParticipantCodeTarget(),
                item.getCurrencyPair(),
                item.getSide(),
                item.getTradePrice(),
                item.getBaseCurrencyAmount(),
                item.getValueCurrencyAmount(),
                DATE_FORMATTER.format(item.getValueDate()),
                item.getTradeId(),
                DATE_TIME_FORMATTER.format(item.getTimestamp()));
        }
    }

}
