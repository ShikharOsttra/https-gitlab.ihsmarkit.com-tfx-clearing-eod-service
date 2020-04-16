package com.ihsmarkit.tfx.eod.service;

import static io.vavr.API.$;
import static io.vavr.API.Case;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.repository.ParticipantRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantStatus;
import com.ihsmarkit.tfx.core.domain.type.ParticipantType;
import com.ihsmarkit.tfx.core.domain.type.TransactionType;
import com.ihsmarkit.tfx.eod.exception.RebalancingCsvGenerationException;
import com.ihsmarkit.tfx.eod.exception.RebalancingMailSendingException;
import com.ihsmarkit.tfx.eod.service.csv.PositionRebalanceCSVWriter;
import com.ihsmarkit.tfx.eod.service.csv.PositionRebalanceRecord;
import com.ihsmarkit.tfx.mailing.client.AwsSesMailClient;
import com.ihsmarkit.tfx.mailing.model.EmailAttachment;

import io.vavr.API;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;

@Service
@AllArgsConstructor
@Slf4j
public class PositionRebalancePublishingService {

    private static final String POSITIONS_REBALANCE_CSV_FILENAME = "positions-rebalance.csv";
    private static final String TEXT_CSV_TYPE = "text/csv";

    private final AwsSesMailClient mailClient;

    private final PositionRebalanceCSVWriter csvWriter;

    private final ParticipantRepository participantRepository;

    @SuppressWarnings("unchecked")
    public void publishTrades(final LocalDate businessDate, final List<TradeEntity> trades) {
        final Map<String, byte[]> participantCsvFiles = Try.ofSupplier(() -> prepareCsvFiles(trades))
            .mapFailure(mapAnyException(RebalancingCsvGenerationException::new))
            .get();

        Try.run(() -> sendCsvToLiquidityProviders(businessDate, participantCsvFiles))
            .mapFailure(mapAnyException(RebalancingMailSendingException::new))
            .get();
    }

    private void sendCsvToLiquidityProviders(final LocalDate businessDate, final Map<String, byte[]> participantCsvFiles) {
        final String businessDateString = businessDate.toString();

        getAllActiveLPs()
            // todo: should we send empty CSV to participants without rebalancing trade?
            .filter(participant -> participantCsvFiles.containsKey(participant.getCode()))
            .forEach(participant -> mailClient.sendEmailWithAttachments(
                String.format("%s rebalance results for %s", businessDateString, participant.getCode()),
                StringUtils.EMPTY,
                parseRecipients(participant.getNotificationEmail()),
                csvEmailAttachment(participantCsvFiles.get(participant.getCode()))
            ));
    }

    private List<EmailAttachment> csvEmailAttachment(final byte[] csvFile) {
        return List.of(EmailAttachment.of(POSITIONS_REBALANCE_CSV_FILENAME, TEXT_CSV_TYPE, csvFile));
    }

    private Map<String, byte[]> prepareCsvFiles(final List<TradeEntity> trades) {
        return trades.stream()
            .collect(Collectors.collectingAndThen(
                Collectors.groupingBy(trade -> trade.getOriginator().getParticipant().getCode()),
                participantTradesMap -> EntryStream.of(participantTradesMap)
                    .mapValues(this::getPositionRebalanceCsv)
                    .toImmutableMap()
            ));
    }

    private static List<String> parseRecipients(final String notificationEmails) {
        return Arrays.stream(notificationEmails.split(","))
            .map(String::strip)
            .collect(Collectors.toUnmodifiableList());
    }

    private static List<PositionRebalanceRecord> getPositionRebalanceTradesAsRecords(final List<TradeEntity> trades) {
        return trades.stream().map(tradeEntity -> PositionRebalanceRecord.builder()
            .tradeDate(tradeEntity.getTradeDate())
            .tradeType(TransactionType.BALANCE.getValue())
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

    private static API.Match.Case<Throwable, ? extends Throwable> mapAnyException(final Function<Throwable, ? extends Throwable> remapper) {
        return Case($(exception -> true), remapper);
    }
}
