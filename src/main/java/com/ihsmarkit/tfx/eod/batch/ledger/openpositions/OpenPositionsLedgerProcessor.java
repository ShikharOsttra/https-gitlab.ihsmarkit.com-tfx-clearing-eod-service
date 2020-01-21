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
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class OpenPositionsLedgerProcessor implements ItemProcessor<ParticipantAndCurrencyPair, OpenPositionsListItem> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final ParticipantPositionRepository participantPositionRepository;

    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;

    private final FXSpotProductService fxSpotProductService;

    private final TradeAndSettlementDateService tradeAndSettlementDateService;

    @Override
    public OpenPositionsListItem process(final ParticipantAndCurrencyPair item) {

        final ParticipantEntity participant = item.getParticipant();
        final CurrencyPairEntity currencyPair = item.getCurrencyPair();

        final Map<ParticipantPositionType, ParticipantPositionEntity> positions =
            participantPositionRepository.findAllByParticipantAndCurrencyPairAndTradeDate(participant, currencyPair, businessDate)
                .collect(Collectors.toMap(ParticipantPositionEntity::getType, Function.identity()));

        final Map<EodProductCashSettlementType, EodProductCashSettlementEntity> margins =
            eodProductCashSettlementRepository.findAllByParticipantAndCurrencyPairAndDate(participant, currencyPair, businessDate)
                .collect(Collectors.toMap(EodProductCashSettlementEntity::getType, Function.identity()));

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
            .shortPosition(formatAmount(shortPosition(getPositionsSummed(positions, NET, REBALANCING))))
            .longPosition(formatAmount(longPosition(getPositionsSummed(positions, NET, REBALANCING))))
            .initialMtmAmount(formatAmount(getMargin(margins, INITIAL_MTM)))
            .dailyMtmAmount(formatAmount(getMargin(margins, DAILY_MTM)))
            .swapPoint(formatAmount(getMargin(margins, SWAP_PNL)))
            .totalVariationMargin(formatAmount(getMargin(margins, TOTAL_VM)))
            .settlementDate(formatDate(tradeAndSettlementDateService.getValueDate(businessDate, currencyPair)))
            .recordDate(formatDateTime(recordDate))
            .build();
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

}
