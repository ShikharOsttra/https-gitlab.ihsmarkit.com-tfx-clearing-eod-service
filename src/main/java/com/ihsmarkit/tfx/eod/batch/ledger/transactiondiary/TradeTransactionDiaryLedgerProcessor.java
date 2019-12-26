package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatTime;
import static org.apache.logging.log4j.util.Strings.EMPTY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.annotation.Nullable;

import org.apache.logging.log4j.util.Strings;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.domain.type.Side;
import com.ihsmarkit.tfx.eod.model.ledger.TransactionDiary;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.JPYRateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class TradeTransactionDiaryLedgerProcessor implements TransactionDiaryLedgerProcessor<TradeEntity> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final EODCalculator eodCalculator;
    private final JPYRateService jpyRateService;
    private final DailySettlementPriceService dailySettlementPriceService;
    private final FxSpotProductQueryProvider fxSpotProductQueryProvider;

    @Override
    public TransactionDiary process(final TradeEntity trade) {

        final ParticipantEntity originatorParticipant = trade.getOriginator().getParticipant();
        @Nullable
        final LocalDateTime matchingTsp = trade.getMatchingTsp();
        @Nullable
        final LocalDateTime clearingTsp = trade.getClearingTsp();
        final ParticipantEntity counterpartyParticipant = trade.getCounterparty().getParticipant();
        final String baseAmount = trade.getBaseAmount().getValue().toString();

        return TransactionDiary.builder()
            .businessDate(businessDate)
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .participantCode(originatorParticipant.getCode())
            .participantName(originatorParticipant.getName())
            .participantType(formatEnum(originatorParticipant.getType()))
            .currencyNo(fxSpotProductQueryProvider.getCurrencyNo(trade.getCurrencyPair()))
            .currencyPair(trade.getCurrencyPair().getCode())
            .matchDate(matchingTsp == null ? EMPTY : formatDate(matchingTsp.toLocalDate()))
            .matchTime(matchingTsp == null ? EMPTY : formatTime(matchingTsp))
            .matchId(getSafeString(trade.getMatchingRef()))
            .clearDate(clearingTsp == null ? EMPTY : formatDate(clearingTsp.toLocalDate()))
            .clearTime(clearingTsp == null ? EMPTY : formatTime(clearingTsp))
            .clearingId(getSafeString(trade.getClearingRef()))
            .tradePrice(trade.getSpotRate().toString())
            .sellAmount(trade.getDirection() == Side.SELL ? baseAmount : EMPTY)
            .buyAmount(trade.getDirection() == Side.BUY ? baseAmount : EMPTY)
            .counterpartyCode(counterpartyParticipant.getCode())
            .counterpartyType(formatEnum(counterpartyParticipant.getType()))
            .dsp(dailySettlementPriceService.getPrice(businessDate, trade.getCurrencyPair()).toString())
            .dailyMtMAmount(eodCalculator.calculateInitialMtmValue(trade, this::getDailySettlementPrice, this::getJpyRate).getAmount().toString())
            .swapPoint(EMPTY)
            .outstandingPositionAmount(EMPTY)
            .settlementDate(formatDate(trade.getValueDate()))
            .tradeId(trade.getTradeReference())
            .tradeType(trade.getTransactionType().getValue().toString())
            //todo When the Trade Type is 5 Cancellation, the Trade ID of the trade being cancelled is displayed.
            .reference(EMPTY)
            .userReference(EMPTY)
            .build();
    }

    private String getSafeString(@Nullable final String record) {
        return Strings.isNotBlank(record) ? record : EMPTY;
    }

    //todo: refactor it - extract it and reuse
    private BigDecimal getDailySettlementPrice(final CurrencyPairEntity ccy) {
        return dailySettlementPriceService.getPrice(businessDate, ccy);
    }

    private BigDecimal getJpyRate(final String ccy) {
        return jpyRateService.getJpyRate(businessDate, ccy);
    }
}
