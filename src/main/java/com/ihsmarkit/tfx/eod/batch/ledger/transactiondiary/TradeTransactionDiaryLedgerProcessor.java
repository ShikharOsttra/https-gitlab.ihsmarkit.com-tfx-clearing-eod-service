package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
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
    private final DspProvider dspProvider;

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
        final Optional<DailySettlementPriceEntity> dailySettlementPriceEntity = dspProvider.getDspByCurrencyCode(trade.getCurrencyPair().getCode());

        return TransactionDiary.builder()
            .businessDate(businessDate)
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .participantCode(originatorParticipant.getCode())
            .participantName(originatorParticipant.getName())
            .participantType(formatEnum(originatorParticipant.getType()))
            //todo ???
            .currencyNo(trade.getCurrencyPair().getCode())
            .currencyPair(trade.getCurrencyPair().getCode())
            .matchDate(matchingTsp == null ? null : formatDate(matchingTsp.toLocalDate()))
            .matchTime(matchingTsp == null ? null : formatTime(matchingTsp))
            .matchId(trade.getMatchingRef())
            .clearDate(clearingTsp == null ? null : formatDate(clearingTsp.toLocalDate()))
            .clearTime(clearingTsp == null ? null : formatTime(clearingTsp))
            .clearingId(trade.getClearingRef())
            .tradePrice(trade.getSpotRate().toString())
            .sellAmount(trade.getDirection() == Side.SELL ? trade.getBaseAmount().getValue().toString() : null)
            .buyAmount(trade.getDirection() == Side.BUY ? trade.getBaseAmount().getValue().toString() : null)
            .counterpartyCode(counterpartyParticipant.getCode())
            .counterpartyType(formatEnum(counterpartyParticipant.getType()))
            .dsp(dailySettlementPriceEntity.map(dsp -> dsp.getDailySettlementPrice().toString()).orElse(null))
            .dailyMtMAmount(eodCalculator.calculateInitialMtmValue(trade, dspResolver, jpyRatesResolver).getAmount().toString())
            .settlementDate(formatDate(trade.getValueDate()))
            //todo?????
            .tradeId(trade.getTradeReference())
            .tradeType(formatEnum(trade.getTransactionType()))
            //todo????????
            .reference(trade.getUtiTradeId())
            .userReference(null)
            .build();
    }
}
