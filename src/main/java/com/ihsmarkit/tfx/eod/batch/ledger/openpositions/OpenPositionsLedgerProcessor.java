package com.ihsmarkit.tfx.eod.batch.ledger.openpositions;

import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.DAILY_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.INITIAL_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.BUY;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.NET;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.REBALANCING;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SELL;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SOD;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimal;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ledger.OpenPositionsListItem;
import com.ihsmarkit.tfx.eod.service.CurrencyPairSwapPointService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;
import com.ihsmarkit.tfx.eod.service.JPYRateService;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@StepScope
@Slf4j
public class OpenPositionsLedgerProcessor implements ItemProcessor<ParticipantAndCurrencyPair, OpenPositionsListItem> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final ParticipantPositionRepository participantPositionRepository;
    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;
    private final FXSpotProductService fxSpotProductService;
    private final TradeAndSettlementDateService tradeAndSettlementDateService;
    private final EODCalculator eodCalculator;
    private final CurrencyPairSwapPointService currencyPairSwapPointService;
    private final JPYRateService jpyRateService;

    @Override
    public OpenPositionsListItem process(final ParticipantAndCurrencyPair item) {

        final ParticipantEntity participant = item.getParticipant();
        final CurrencyPairEntity currencyPair = item.getCurrencyPair();

        final Map<ParticipantPositionType, ParticipantPositionEntity> positions =
            participantPositionRepository.findAllByParticipantAndCurrencyPairAndTradeDate(participant, currencyPair, businessDate)
                .collect(Collectors.toMap(ParticipantPositionEntity::getType, Function.identity()));

        final Map<EodProductCashSettlementType, EodProductCashSettlementEntity> cashSettlements =
            eodProductCashSettlementRepository.findAllByParticipantAndCurrencyPairAndDate(participant, currencyPair, businessDate)
                .collect(Collectors.toMap(EodProductCashSettlementEntity::getType, Function.identity()));

        final Optional<BigDecimal> eodPositionAmount = getPositionsSummed(positions, NET, REBALANCING);
        final BigDecimal swapPoints = getEodSwapPoints(participant, currencyPair);

        return OpenPositionsListItem.builder()
            .businessDate(businessDate)
            .participantCode(participant.getCode())
            .participantName(participant.getName())
            .participantType(formatEnum(participant.getType()))
            .currencyCode(currencyPair.getCode())
            .currencyNo(fxSpotProductService.getFxSpotProduct(currencyPair).getProductNumber())
            .tradeDate(formatDate(businessDate))
            .shortPositionPreviousDay(formatAmount(shortPosition(getPosition(positions, SOD))))
            .longPositionPreviousDay(formatAmount(longPosition(getPosition(positions, SOD))))
            .buyTradingAmount(formatBigDecimal(getPosition(positions, BUY).orElse(ZERO).abs()))
            .sellTradingAmount(formatBigDecimal(getPosition(positions, SELL).orElse(ZERO).abs()))
            .shortPosition(formatAmount(shortPosition(eodPositionAmount)))
            .longPosition(formatAmount(longPosition(eodPositionAmount)))
            .initialMtmAmount(formatAmount(getMargin(cashSettlements, INITIAL_MTM)))
            .dailyMtmAmount(formatAmount(getMargin(cashSettlements, DAILY_MTM)))
            .swapPoint(formatBigDecimal(swapPoints))
            .totalVariationMargin(formatAmount(getMargin(cashSettlements, TOTAL_VM)))
            .settlementDate(formatDate(tradeAndSettlementDateService.getValueDate(businessDate, currencyPair)))
            .recordDate(formatDateTime(recordDate))
            .build();
    }

    private BigDecimal getEodSwapPoints(final ParticipantEntity participant, final CurrencyPairEntity currencyPair) {
        final Optional<ParticipantPositionEntity> eodPosition = participantPositionRepository.findNextDayPosition(participant, currencyPair, SOD,
            businessDate);
        return eodPosition.map(positionEntity -> eodCalculator.calculateSwapPoint(positionEntity, this::getSwapPoint, this::getJpyRate).getAmount())
            .orElseGet(() -> {
                log.warn("[openPositionsLedger] next day SOD not found for participant: {} and currencyPair: {} and businessDate: {}",
                        participant, currencyPair, businessDate);
                return ZERO;
            });
    }


    private static String formatAmount(final Optional<BigDecimal> value) {
        return value.orElse(ZERO).toString();
    }

    private static Optional<BigDecimal> longPosition(final Optional<BigDecimal> amount) {
        return amount.map(ZERO::max);
    }

    private static Optional<BigDecimal> shortPosition(final Optional<BigDecimal> amount) {
        return amount.map(ZERO::min).map(BigDecimal::negate);
    }

    private static Optional<BigDecimal> getMargin(
        final Map<EodProductCashSettlementType, EodProductCashSettlementEntity> margins,
        final EodProductCashSettlementType initialMtm
    ) {
        return Optional.ofNullable(margins.get(initialMtm)).map(EodProductCashSettlementEntity::getAmount).map(AmountEntity::getValue);
    }

    private static Optional<BigDecimal> getPosition(
        final Map<ParticipantPositionType, ParticipantPositionEntity> positions,
        final ParticipantPositionType type
    ) {
        return Optional.ofNullable(positions.get(type))
            .map(ParticipantPositionEntity::getAmount)
            .map(AmountEntity::getValue);
    }

    private static Optional<BigDecimal> getPositionsSummed(
        final Map<ParticipantPositionType, ParticipantPositionEntity> positions,
        final ParticipantPositionType...type
    ) {
        return Arrays.stream(type)
            .map(positions::get)
            .filter(Objects::nonNull)
            .map(ParticipantPositionEntity::getAmount)
            .map(AmountEntity::getValue)
            .reduce(BigDecimal::add);
    }

    private BigDecimal getSwapPoint(final CurrencyPairEntity ccy) {
        return currencyPairSwapPointService.getSwapPoint(businessDate, ccy);
    }

    private BigDecimal getJpyRate(final String ccy) {
        return jpyRateService.getJpyRate(businessDate, ccy);
    }

}
