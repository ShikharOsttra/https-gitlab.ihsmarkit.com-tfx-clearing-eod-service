package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimal;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static org.apache.logging.log4j.util.Strings.EMPTY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.model.ledger.TransactionDiary;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@StepScope
@Slf4j
public class NETTransactionDiaryLedgerProcessor implements TransactionDiaryLedgerProcessor<ParticipantPositionEntity> {

    private static final String DEFAULT_TIME = "07:00:00";

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final DailySettlementPriceService dailySettlementPriceService;
    private final FXSpotProductService fxSpotProductService;

    private final ParticipantPositionRepository participantPositionRepository;

    @Override
    public TransactionDiary process(final ParticipantPositionEntity participantPosition) {

        final ParticipantEntity participant = participantPosition.getParticipant();
        final CurrencyPairEntity currencyPair = participantPosition.getCurrencyPair();
        final int priceScale = fxSpotProductService.getScaleForCurrencyPair(currencyPair);

        return TransactionDiary.builder()
            .businessDate(businessDate)
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .participantCode(participant.getCode())
            .participantName(participant.getName())
            .participantType(formatEnum(participant.getType()))
            .currencyNo(fxSpotProductService.getFxSpotProduct(currencyPair).getProductNumber())
            .currencyPair(currencyPair.getCode())
            .matchDate(formatDate(businessDate))
            .matchTime(DEFAULT_TIME)
            .matchId(EMPTY)
            .clearDate(formatDate(businessDate))
            .clearTime(DEFAULT_TIME)
            .clearingId(EMPTY)
            .tradePrice(formatBigDecimal(dailySettlementPriceService.getPrice(businessDate, currencyPair), priceScale))
            .sellAmount(EMPTY)
            .buyAmount(EMPTY)
            .counterpartyCode(EMPTY)
            .counterpartyType(EMPTY)
            .dsp(formatBigDecimal(dailySettlementPriceService.getPrice(businessDate, currencyPair), priceScale))
            .dailyMtMAmount(EMPTY)
            .swapPoint(EMPTY)
            .outstandingPositionAmount(formatBigDecimal(getSODNextDayAmount(participant, currencyPair)))
            .settlementDate(EMPTY)
            .tradeId(EMPTY)
            .tradeType(EMPTY)
            .reference(EMPTY)
            .userReference(EMPTY)
            .build();
    }

    private BigDecimal getSODNextDayAmount(final ParticipantEntity participant, final CurrencyPairEntity currencyPair) {
        final Optional<ParticipantPositionEntity> nextDayPosition = participantPositionRepository.findNextDayPosition(participant, currencyPair,
            ParticipantPositionType.SOD, businessDate);
        return nextDayPosition.map(entity -> entity.getAmount().getValue()).orElseGet(() -> {
            log.warn("[transactionDiaryLedger] next day SOD not found for participant: {} and currencyPair: {} and businessDate: {}",
                participant.getCode(), currencyPair.getCode(), businessDate);
            return BigDecimal.ZERO;
        });
    }
}