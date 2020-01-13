package com.ihsmarkit.tfx.eod.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.eod.service.csv.PositionRebalanceCSVWriter;
import com.ihsmarkit.tfx.eod.service.csv.PositionRebalanceRecord;
import com.ihsmarkit.tfx.mailing.client.AwsSesMailClient;
import com.ihsmarkit.tfx.mailing.model.EmailAttachment;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class PositionRebalancePublishingService {

    private final AwsSesMailClient mailClient;

    private final PositionRebalanceCSVWriter csvWriter;

    public void publishTrades(final LocalDate businessDate, final List<TradeEntity> trades) {
        final String subject = String.format("%s rebalance results", businessDate.toString());
        try {
            mailClient.sendEmailWithAttachments(
                subject,
                StringUtils.EMPTY,
                List.of("SpecialIDL-CLHCoreTeam@epam.com"),
                List.of(EmailAttachment.of("positions-rebalance.csv", "text/csv", getPositionRebalanceCsv(trades))));
        } catch (final Exception ex) {
            log.error("error while publish position rebalance csv for businessDate: {} with error: {}", businessDate, ex.getMessage(), ex);
        }
    }

    private List<PositionRebalanceRecord> getPositionRebalanceTradesAsRecords(final List<TradeEntity> trades) {
        return trades.stream().map(tradeEntity -> PositionRebalanceRecord.builder()
            .tradeDate(tradeEntity.getTradeDate())
            .tradeType(2)
            .participantCodeSource(tradeEntity.getOriginator().getCode())
            .participantCodeTarget(tradeEntity.getCounterparty().getCode())
            .currencyPair(tradeEntity.getCurrencyPair().getCode())
            .side(tradeEntity.getDirection().name())
            .tradePrice(tradeEntity.getSpotRate())
            .baseCurrencyAmount(tradeEntity.getBaseAmount().getValue())
            .valueCurrencyAmount(tradeEntity.getValueAmount().getValue())
            .valueDate(tradeEntity.getValueDate())
            .tradeId(tradeEntity.getTradeReference())
            .timestamp(tradeEntity.getSubmissionTsp())
            .build())
            .collect(Collectors.toList());
    }

    private byte[] getPositionRebalanceCsv(final List<TradeEntity> trades) {
        return csvWriter.getRecordsAsCsv(getPositionRebalanceTradesAsRecords(trades)).getBytes(StandardCharsets.UTF_8);
    }
}
