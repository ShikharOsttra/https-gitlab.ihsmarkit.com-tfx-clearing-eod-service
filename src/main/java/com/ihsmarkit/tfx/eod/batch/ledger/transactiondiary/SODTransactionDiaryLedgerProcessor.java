package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimal;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static org.apache.logging.log4j.util.Strings.EMPTY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.eod.model.ledger.TransactionDiary;
import com.ihsmarkit.tfx.eod.service.CurrencyPairSwapPointService;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;
import com.ihsmarkit.tfx.eod.service.JPYRateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class SODTransactionDiaryLedgerProcessor implements TransactionDiaryLedgerProcessor<ParticipantPositionEntity> {

    private static final String DEFAULT_TIME = "07:00:00";

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final EODCalculator eodCalculator;
    private final CurrencyPairSwapPointService currencyPairSwapPointService;
    private final JPYRateService jpyRateService;
    private final DailySettlementPriceService dailySettlementPriceService;
    private final FXSpotProductService fxSpotProductService;

    @Override
    public TransactionDiary process(final ParticipantPositionEntity participantPosition) {
        final ParticipantEntity participant = participantPosition.getParticipant();
        final String dailyMtMAmount =
            eodCalculator.calculateDailyMtmValue(participantPosition, this::getDailySettlementPrice, this::getJpyRate).getAmount().toString();
        final String swapPoint = eodCalculator.calculateSwapPoint(participantPosition, this::getSwapPoint, this::getJpyRate).getAmount().toString();
        final String settlementDate = formatDate(participantPosition.getValueDate());
        final int priceScale = fxSpotProductService.getScaleForCurrencyPair(participantPosition.getCurrencyPair());
        final String dsp = formatBigDecimal(dailySettlementPriceService.getPrice(businessDate, participantPosition.getCurrencyPair()), priceScale);
        final String tradePrice = formatBigDecimal(participantPosition.getPrice(), priceScale);
        final String tradeDate = formatDate(participantPosition.getTradeDate());
        final BigDecimal positionAmount = participantPosition.getAmount().getValue();

        return TransactionDiary.builder()
            .businessDate(businessDate)
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .participantCode(participant.getCode())
            .participantName(participant.getName())
            .participantType(formatEnum(participant.getType()))
            .currencyNo(fxSpotProductService.getFxSpotProduct(participantPosition.getCurrencyPair()).getProductNumber())
            .currencyPair(participantPosition.getCurrencyPair().getCode())
            .matchDate(tradeDate)
            .matchTime(DEFAULT_TIME)
            .matchId(EMPTY)
            .clearDate(tradeDate)
            .clearTime(DEFAULT_TIME)
            .clearingId(EMPTY)
            .tradePrice(tradePrice)
            .sellAmount(positionAmount.signum() < 0 ? formatBigDecimal(positionAmount.abs()) : EMPTY)
            .buyAmount(positionAmount.signum() > 0 ? formatBigDecimal(positionAmount) : EMPTY)
            .counterpartyCode(EMPTY)
            .counterpartyType(EMPTY)
            .dsp(dsp)
            .dailyMtMAmount(dailyMtMAmount)
            .swapPoint(swapPoint)
            .outstandingPositionAmount(formatBigDecimal(BigDecimal.ZERO))
            .settlementDate(settlementDate)
            .tradeId(EMPTY)
            .reference(EMPTY)
            .userReference(EMPTY)
            .build();
    }

    private BigDecimal getDailySettlementPrice(final CurrencyPairEntity ccy) {
        return dailySettlementPriceService.getPrice(businessDate, ccy);
    }

    private BigDecimal getSwapPoint(final CurrencyPairEntity ccy) {
        return currencyPairSwapPointService.getSwapPoint(businessDate, ccy);
    }

    private BigDecimal getJpyRate(final String ccy) {
        return jpyRateService.getJpyRate(businessDate, ccy);
    }
}