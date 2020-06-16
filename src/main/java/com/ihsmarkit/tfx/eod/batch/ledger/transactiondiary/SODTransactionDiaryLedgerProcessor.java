package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SOD;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimal;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimalStripZero;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatTime;
import static java.math.BigDecimal.ZERO;
import static org.apache.logging.log4j.util.Strings.EMPTY;

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
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ledger.TransactionDiary;
import com.ihsmarkit.tfx.eod.service.CalendarDatesProvider;
import com.ihsmarkit.tfx.eod.service.CurrencyPairSwapPointService;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;
import com.ihsmarkit.tfx.eod.service.JPYRateService;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class SODTransactionDiaryLedgerProcessor implements TransactionDiaryLedgerProcessor<ParticipantAndCurrencyPair> {

    private static final char ORDER_ID_SUFFIX = '0';

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{jobExecutionContext['transactionDiaryRecordDate']}")
    private final LocalDateTime recordDate;

    private final EODCalculator eodCalculator;
    private final CurrencyPairSwapPointService currencyPairSwapPointService;
    private final JPYRateService jpyRateService;
    private final DailySettlementPriceService dailySettlementPriceService;
    private final FXSpotProductService fxSpotProductService;
    private final TransactionDiaryOrderIdProvider transactionDiaryOrderIdProvider;
    private final ParticipantPositionRepository participantPositionRepository;
    private final TradeAndSettlementDateService tradeAndSettlementDateService;
    private final PositionDateProvider positionDateProvider;
    private final CalendarDatesProvider calendarDatesProvider;

    @Override
    public TransactionDiary process(final ParticipantAndCurrencyPair participantAndCurrencyPair) {
        final ParticipantEntity participant = participantAndCurrencyPair.getParticipant();
        final CurrencyPairEntity currencyPair = participantAndCurrencyPair.getCurrencyPair();

        final Optional<ParticipantPositionEntity> participantPosition = participantPositionRepository
            .findByPositionTypeAndTradeDateAndCurrencyPairAndParticipantFetchAll(SOD, businessDate, currencyPair, participant);

        final String dailyMtMAmount = getValueOrZero(participantPosition, this::getDailyMtMAmount);
        final String swapPoint = getValueOrZero(participantPosition, this::getSwapPoint);
        final int priceScale = fxSpotProductService.getScaleForCurrencyPair(currencyPair);
        final String dsp = formatBigDecimal(dailySettlementPriceService.getPrice(businessDate, currencyPair), priceScale);
        final BigDecimal positionAmount = participantPosition.map(position -> position.getAmount().getValue()).orElse(ZERO);
        final String productNumber = fxSpotProductService.getFxSpotProduct(currencyPair).getProductNumber();
        final LocalDateTime positionDateTime = positionDateProvider.getSodDate();

        return TransactionDiary.builder()
            .businessDate(businessDate)
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .participantCode(participant.getCode())
            .participantName(participant.getName())
            .participantType(formatEnum(participant.getType()))
            .currencyNo(productNumber)
            .currencyPair(currencyPair.getCode())
            .matchDate(formatDate(positionDateTime.toLocalDate()))
            .matchTime(formatTime(positionDateTime))
            .matchId(EMPTY)
            .clearDate(formatDate(positionDateTime.toLocalDate()))
            .clearTime(formatTime(positionDateTime))
            .clearingId(EMPTY)
            .tradePrice(getTradePrice(currencyPair, participantPosition, priceScale))
            .sellAmount(positionAmount.signum() < 0 ? formatBigDecimalStripZero(positionAmount.abs()) : EMPTY)
            .buyAmount(positionAmount.signum() > 0 ? formatBigDecimalStripZero(positionAmount) : EMPTY)
            .counterpartyCode(EMPTY)
            .counterpartyType(EMPTY)
            .dsp(dsp)
            .dailyMtMAmount(dailyMtMAmount)
            .swapPoint(swapPoint)
            .outstandingPositionAmount(formatBigDecimal(ZERO))
            .settlementDate(formatDate(calendarDatesProvider.getSettlementDate(currencyPair)))
            .tradeId(EMPTY)
            .reference(EMPTY)
            .userReference(EMPTY)
            .orderId(transactionDiaryOrderIdProvider.getOrderId(participant.getCode(), productNumber, ORDER_ID_SUFFIX))
            .build();
    }

    private String getTradePrice(final CurrencyPairEntity currencyPair, final Optional<ParticipantPositionEntity> participantPosition, final int priceScale) {
        return formatBigDecimal(participantPosition
                .map(ParticipantPositionEntity::getPrice)
                .orElseGet(() -> getPreviousDateTradePrice(currencyPair)),
            priceScale);
    }

    @Nullable
    private BigDecimal getPreviousDateTradePrice(final CurrencyPairEntity currencyPair) {
        return tradeAndSettlementDateService.getPreviousTradeDate(businessDate, currencyPair)
            .map(date -> dailySettlementPriceService.getPrice(date, currencyPair))
            .orElse(null);
    }

    private BigDecimal getDailyMtMAmount(final ParticipantPositionEntity participantPosition) {
        return eodCalculator.calculateDailyMtmValue(
            participantPosition,
            ccy -> dailySettlementPriceService.getPrice(businessDate, ccy),
            this::getJpyRate
        ).getAmount();
    }

    private BigDecimal getSwapPoint(final ParticipantPositionEntity participantPosition) {
        return eodCalculator.calculateSwapPoint(
            participantPosition,
            ccy -> currencyPairSwapPointService.getSwapPoint(businessDate, ccy),
            this::getJpyRate
        ).getAmount();
    }

    private BigDecimal getJpyRate(final String ccy) {
        return jpyRateService.getJpyRate(businessDate, ccy);
    }

    private static String getValueOrZero(final Optional<ParticipantPositionEntity> position, final Function<ParticipantPositionEntity, BigDecimal> mapper) {
        return position.map(mapper).orElse(ZERO).toString();
    }
}