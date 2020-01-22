package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimal;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static org.apache.logging.log4j.util.Strings.EMPTY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@StepScope
@Slf4j
public class NETTransactionDiaryLedgerProcessor implements TransactionDiaryLedgerProcessor<ParticipantPositionEntity> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final DailySettlementPriceService dailySettlementPriceService;
    private final FXSpotProductService fxSpotProductService;

    private final ParticipantPositionRepository participantPositionRepository;

    private final TradeAndSettlementDateService tradeAndSettlementDateService;

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
            .tradePrice(formatBigDecimal(dailySettlementPriceService.getPrice(businessDate, currencyPair)))
            .sellAmount(EMPTY)
            .buyAmount(EMPTY)
            .counterpartyCode(EMPTY)
            .counterpartyType(EMPTY)
            .dsp(formatBigDecimal(dailySettlementPriceService.getPrice(businessDate, currencyPair)))
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
        final LocalDate nextDate = tradeAndSettlementDateService.getNextTradeDate(businessDate, currencyPair);
        final Map<ParticipantPositionType, ParticipantPositionEntity> positions =
            participantPositionRepository.findAllByParticipantAndCurrencyPairAndTradeDate(participant, currencyPair, nextDate)
                .collect(Collectors.toMap(ParticipantPositionEntity::getType, Function.identity()));
        return positions.get(ParticipantPositionType.SOD).getAmount().getValue();
    }
}