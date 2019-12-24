package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatTime;
import static org.apache.logging.log4j.util.Strings.EMPTY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Function;

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

    @Override
    public TransactionDiary process(final TradeEntity trade) {
        final Function<CurrencyPairEntity, BigDecimal> dspResolver = ccy -> dailySettlementPriceService.getPrice(businessDate, ccy);
        final Function<String, BigDecimal> jpyRatesResolver = ccy -> jpyRateService.getJpyRate(businessDate, ccy);

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
            //todo currencyNo used for sorting
            .currencyNo(trade.getCurrencyPair().getId().toString())
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
            .dailyMtMAmount(eodCalculator.calculateInitialMtmValue(trade, dspResolver, jpyRatesResolver).getAmount().toString())
            .swapPoint(EMPTY)
            .outstandingPositionAmount(EMPTY)
            .settlementDate(formatDate(trade.getValueDate()))
            //todo Trade ID registered by the member. This is not system generated. Blank if not provided.
            .tradeId(trade.getTradeReference())
            //todo: enum name or index??? See DataTypes for types of transactions. For type 5 show opposing transaction ID in the reference field.
            .tradeType(trade.getTransactionType().getValue().toString())
            //todo When the Trade Type is 5 Cancellation, the Trade ID of the trade being cancelled is displayed.
            .reference(getSafeString(trade.getUtiTradeId()))
            .userReference(EMPTY)
            .build();
    }

    private String getSafeString(@Nullable final String record) {
        return Strings.isNotBlank(record) ? record : EMPTY;
    }
}
