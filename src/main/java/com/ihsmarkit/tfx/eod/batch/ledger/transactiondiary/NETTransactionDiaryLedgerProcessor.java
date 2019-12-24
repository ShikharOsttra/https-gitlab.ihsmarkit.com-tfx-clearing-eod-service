package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.eod.model.ledger.TransactionDiary;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class NETTransactionDiaryLedgerProcessor implements TransactionDiaryLedgerProcessor<ParticipantPositionEntity> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final DailySettlementPriceService dailySettlementPriceService;

    @Override
    public TransactionDiary process(final ParticipantPositionEntity participantPosition) {

        final ParticipantEntity participant = participantPosition.getParticipant();

        //todo: are rest of the fields empty??
        return TransactionDiary.builder()
            .businessDate(businessDate)
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .participantCode(participant.getCode())
            .participantName(participant.getName())
            .participantType(formatEnum(participant.getType()))
            //todo ???
            .currencyNo(participantPosition.getCurrencyPair().getCode())
            .currencyPair(participantPosition.getCurrencyPair().getCode())
            //todo is it correct??
            .tradePrice(dailySettlementPriceService.getPrice(businessDate.minusDays(1), participantPosition.getCurrencyPair()).toString())
            .dsp(dailySettlementPriceService.getPrice(businessDate, participantPosition.getCurrencyPair()).toString())
            .outstandingPositionAmount(participantPosition.getAmount().toString())
            .userReference(null)
            .build();
    }
}
