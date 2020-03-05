package com.ihsmarkit.tfx.eod.batch.ledger.openpositions;

import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.DAILY_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.INITIAL_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.SWAP_PNL;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.BUY;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.NET;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.REBALANCING;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SELL;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SOD;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.ITEM_RECORD_TYPE;
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
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantCodeOrderIdProvider;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ledger.OpenPositionsListItem;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@StepScope
@Slf4j
public class OpenPositionsLedgerProcessor implements ItemProcessor<ParticipantAndCurrencyPair, OpenPositionsListItem<BigDecimal>> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final ParticipantPositionRepository participantPositionRepository;
    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;
    private final FXSpotProductService fxSpotProductService;
    private final TradeAndSettlementDateService tradeAndSettlementDateService;
    private final ParticipantCodeOrderIdProvider participantCodeOrderIdProvider;

    @Override
    public OpenPositionsListItem<BigDecimal> process(final ParticipantAndCurrencyPair participantAndCurrencyPair) {

        final ParticipantEntity participant = participantAndCurrencyPair.getParticipant();
        final CurrencyPairEntity currencyPair = participantAndCurrencyPair.getCurrencyPair();

        final Map<ParticipantPositionType, ParticipantPositionEntity> positions =
            participantPositionRepository.findAllByParticipantAndCurrencyPairAndTradeDate(participant, currencyPair, businessDate)
                .collect(Collectors.toMap(ParticipantPositionEntity::getType, Function.identity()));

        final Map<EodProductCashSettlementType, EodProductCashSettlementEntity> cashSettlements =
            eodProductCashSettlementRepository.findAllByParticipantAndCurrencyPairAndDate(participant, currencyPair, businessDate)
                .collect(Collectors.toMap(EodProductCashSettlementEntity::getType, Function.identity()));

        final Optional<BigDecimal> eodPositionAmount = getPositionsSummed(positions, NET, REBALANCING);

        final LocalDate settlementDate = tradeAndSettlementDateService.getVmSettlementDate(businessDate, currencyPair);
        final String productNumber = fxSpotProductService.getFxSpotProduct(currencyPair).getProductNumber();

        return OpenPositionsListItem.<BigDecimal>builder()
            .businessDate(businessDate)
            .participantCode(participant.getCode())
            .participantName(participant.getName())
            .participantType(formatEnum(participant.getType()))
            .currencyCode(currencyPair.getCode())
            .currencyNo(productNumber)
            .tradeDate(formatDate(businessDate))
            .shortPositionPreviousDay(formatBigDecimal(shortPosition(getPosition(positions, SOD))))
            .longPositionPreviousDay(formatBigDecimal(longPosition(getPosition(positions, SOD))))
            .buyTradingAmount(formatBigDecimal(getPosition(positions, BUY).orElse(ZERO).abs()))
            .sellTradingAmount(formatBigDecimal(getPosition(positions, SELL).orElse(ZERO).abs()))
            .shortPosition(formatBigDecimal(shortPosition(eodPositionAmount)))
            .longPosition(formatBigDecimal(longPosition(eodPositionAmount)))
            .initialMtmAmount(getMarginOrZero(cashSettlements, INITIAL_MTM))
            .dailyMtmAmount(getMarginOrZero(cashSettlements, DAILY_MTM))
            .swapPoint(getMarginOrZero(cashSettlements, SWAP_PNL))
            .totalVariationMargin(getMarginOrZero(cashSettlements, TOTAL_VM))
            .settlementDate(formatDate(settlementDate))
            .recordDate(formatDateTime(recordDate))
            .recordType(ITEM_RECORD_TYPE)
            .orderId(Long.parseLong(participantCodeOrderIdProvider.get(participant.getCode()) + productNumber))
            .build();
    }

    private static Optional<BigDecimal> longPosition(final Optional<BigDecimal> amount) {
        return amount
            .map(ZERO::max)
            .or(() -> Optional.of(ZERO));
    }

    private static Optional<BigDecimal> shortPosition(final Optional<BigDecimal> amount) {
        return amount
            .map(ZERO::min)
            .map(BigDecimal::negate)
            .or(() -> Optional.of(ZERO));
    }

    private static BigDecimal getMarginOrZero(
        final Map<EodProductCashSettlementType, EodProductCashSettlementEntity> margins,
        final EodProductCashSettlementType cashSettlementType
    ) {
        return getMargin(margins, cashSettlementType).orElse(ZERO);
    }

    private static Optional<BigDecimal> getMargin(
        final Map<EodProductCashSettlementType, EodProductCashSettlementEntity> margins,
        final EodProductCashSettlementType cashSettlementType
    ) {
        return Optional.ofNullable(margins.get(cashSettlementType))
            .map(EodProductCashSettlementEntity::getAmount)
            .map(AmountEntity::getValue);
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
}
