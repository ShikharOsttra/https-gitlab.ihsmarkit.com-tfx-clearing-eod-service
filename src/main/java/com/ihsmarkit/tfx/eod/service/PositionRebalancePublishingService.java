package com.ihsmarkit.tfx.eod.service;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.repository.ParticipantRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantStatus;
import com.ihsmarkit.tfx.core.domain.type.ParticipantType;
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

    private static final Locale JA_LOCALE = new Locale("ja");
    private static final DateTimeFormatter DATE_JP_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        .withLocale(JA_LOCALE)
        .withChronology(Chronology.ofLocale(JA_LOCALE));
    private static final DateTimeFormatter DATE_EN_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final String REBALANCE_EMAIL_TEMPLATE = "/velocity/templates/rebalance_email.vm";
    private static final String PARTICIPANT_NAME = "participantName";
    private static final String TRADE_DATE_JP = "tradeDateJP";
    private static final String TRADE_DATE_EN = "tradeDateEN";

    private final AwsSesMailClient mailClient;

    private final PositionRebalanceCSVWriter csvWriter;

    private final ParticipantRepository participantRepository;

    public void publishTrades(final LocalDate businessDate, final List<TradeEntity> trades) {
        try {
            final Map<String, List<TradeEntity>> participantTradesMap = trades.stream().collect(
                Collectors.groupingBy(trade -> trade.getOriginator().getParticipant().getCode()));
            getAllActiveLPs()
                .sequential()
                .forEach(participant -> {
                    mailClient.sendEmailWithAttachments(
                        String.format("%s rebalance results for %s", businessDate.toString(), participant.getCode()),
                        generateEmailText(businessDate, participant.getName()),
                        parseRecipients(participant.getNotificationEmail()),
                        List.of(EmailAttachment.of("positions-rebalance.csv", "text/csv",
                            getPositionRebalanceCsv(participantTradesMap.getOrDefault(participant.getCode(), List.of())))));
                });
        } catch (final Exception ex) {
            log.error("error while publish position rebalance csv for businessDate: {} with error: {}", businessDate, ex.getMessage());
        }
    }

    private static List<String> parseRecipients(final String notificationEmails) {
        return Arrays.stream(notificationEmails.split(","))
            .map(String::strip)
            .collect(Collectors.toUnmodifiableList());
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

    private Stream<ParticipantEntity> getAllActiveLPs() {
        return participantRepository.findAllNotDeletedParticipantListItems().stream()
            .filter(participantEntity -> ParticipantType.LIQUIDITY_PROVIDER == participantEntity.getType())
            .filter(participantEntity -> ParticipantStatus.ACTIVE == participantEntity.getStatus());
    }

    private String generateEmailText(final LocalDate tradeDate, final String participantName) {
        final Context context = new VelocityContext();
        context.put(PARTICIPANT_NAME, participantName);
        context.put(TRADE_DATE_EN, DATE_EN_FORMATTER.format(tradeDate));
        context.put(TRADE_DATE_JP, DATE_JP_FORMATTER.format(tradeDate));

        final StringWriter writer = new StringWriter();
        Velocity.mergeTemplate(REBALANCE_EMAIL_TEMPLATE, StandardCharsets.UTF_8.name(), context, writer);
        return writer.toString();
    }
}
