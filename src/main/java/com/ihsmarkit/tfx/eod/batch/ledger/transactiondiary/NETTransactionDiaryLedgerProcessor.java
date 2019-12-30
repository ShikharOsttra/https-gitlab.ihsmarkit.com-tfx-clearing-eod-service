package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static org.apache.logging.log4j.util.Strings.EMPTY;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.eod.model.ledger.TransactionDiary;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;

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
    private final FXSpotProductService fxSpotProductService;

    @Override
    public TransactionDiary process(final ParticipantPositionEntity participantPosition) {

        final ParticipantEntity participant = participantPosition.getParticipant();
        final CurrencyPairEntity currencyPair = participantPosition.getCurrencyPair();

        return TransactionDiary.builder()
            .businessDate(businessDate)
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .participantCode(participant.getCode())
            .participantName(participant.getName())
            .participantType(formatEnum(participant.getType()))
            .currencyNo(fxSpotProductService.getFxSpotProduct(currencyPair).getProductNumber())
            .currencyPair(currencyPair.getCode())
            .matchDate(EMPTY)
            .matchTime(EMPTY)
            .matchId(EMPTY)
            .clearDate(EMPTY)
            .clearTime(EMPTY)
            .clearingId(EMPTY)
            .tradePrice(dailySettlementPriceService.getPrice(businessDate, currencyPair).toString())
            .sellAmount(EMPTY)
            .buyAmount(EMPTY)
            .counterpartyCode(EMPTY)
            .counterpartyType(EMPTY)
            .dsp(dailySettlementPriceService.getPrice(businessDate, currencyPair).toString())
            .dailyMtMAmount(EMPTY)
            .swapPoint(EMPTY)
            .outstandingPositionAmount(participantPosition.getAmount().getValue().toString())
            .settlementDate(EMPTY)
            .tradeId(EMPTY)
            .tradeType(EMPTY)
            .reference(EMPTY)
            .userReference(EMPTY)
            .build();
    }
}