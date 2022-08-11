package com.ihsmarkit.tfx.eod.batch.ledger.rebalance;

import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.DAILY_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.INITIAL_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.SWAP_PNL;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.BUY;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.NET;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.REBALANCING;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SELL;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SOD;
import static java.math.BigDecimal.ZERO;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceItem;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.service.CalendarDatesProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@StepScope
@Slf4j
public class RebalanceInputProcessor implements ItemProcessor<ParticipantAndCurrencyPair, RebalanceItem> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final ParticipantPositionRepository participantPositionRepository;
    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;
    private final CalendarDatesProvider calendarDatesProvider;

    @Override
    public RebalanceItem process(final ParticipantAndCurrencyPair participantAndCurrencyPair) {
        final ParticipantEntity participant = participantAndCurrencyPair.getParticipant();
        final CurrencyPairEntity currencyPair = participantAndCurrencyPair.getCurrencyPair();
        final Map<ParticipantPositionType, ParticipantPositionEntity> positions =
            participantPositionRepository.findAllByParticipantAndCurrencyPairAndTradeDate(participant, currencyPair, businessDate)
                .collect(Collectors.toMap(ParticipantPositionEntity::getType, Function.identity()));

        final Map<EodProductCashSettlementType, EodProductCashSettlementEntity> cashSettlements =
            eodProductCashSettlementRepository.findAllByParticipantAndCurrencyPairAndDate(participant, currencyPair, businessDate)
                .collect(Collectors.toMap(EodProductCashSettlementEntity::getType, Function.identity()));

        final Optional<BigDecimal> eodPositionAmount = getPositionsSummed(positions, NET, REBALANCING);

        return RebalanceItem.builder()
            .participantAndCurrencyPair(participantAndCurrencyPair)
            .shortPositionPreviousDay(shortPosition(getPosition(positions, SOD)))
            .longPositionPreviousDay(longPosition(getPosition(positions, SOD)))
            .buyTradingAmount(getBuyOrSellPosition(positions, BUY))
            .sellTradingAmount(getBuyOrSellPosition(positions, SELL))
            .shortPosition(shortPosition(eodPositionAmount))
            .longPosition(longPosition(eodPositionAmount))
            .initialMtmAmount(getMarginOrZero(cashSettlements, INITIAL_MTM))
            .dailyMtmAmount(getMarginOrZero(cashSettlements, DAILY_MTM))
            .swapPoint(getMarginOrZero(cashSettlements, SWAP_PNL))
            .totalVariationMargin(getMarginOrZero(cashSettlements, TOTAL_VM))
            .settlementDate(calendarDatesProvider.getSettlementDate(participantAndCurrencyPair.getCurrencyPair()))
            .build();
    }

    @Nullable
    private static BigDecimal getBuyOrSellPosition(
        final Map<ParticipantPositionType, ParticipantPositionEntity> positions,
        final ParticipantPositionType positionType
    ) {
        final Optional<BigDecimal> position = getPosition(positions, positionType);
        final Optional<BigDecimal> rebalancingPosition = getPosition(positions, REBALANCING)
            .filter(value -> (value.signum() >= 0 ? BUY : SELL) == positionType);

        if (position.isEmpty() && rebalancingPosition.isEmpty()) {
            return null;
        }

        return position.orElse(ZERO)
            .add(rebalancingPosition.orElse(ZERO))
            .abs();
    }

    private static BigDecimal longPosition(final Optional<BigDecimal> amount) {
        return amount
            .map(ZERO::max)
            .orElse(ZERO);
    }

    private static BigDecimal shortPosition(final Optional<BigDecimal> amount) {
        return amount
            .map(ZERO::min)
            .map(BigDecimal::negate)
            .orElse(ZERO);
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
        final ParticipantPositionType... type
    ) {
        return Arrays.stream(type)
            .map(positions::get)
            .filter(Objects::nonNull)
            .map(ParticipantPositionEntity::getAmount)
            .map(AmountEntity::getValue)
            .reduce(BigDecimal::add);
    }
}
