package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.CASH;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.DAY;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.TOTAL;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodParticipantMarginEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.repository.collateral.CollateralBalanceRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodParticipantMarginRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.JPYRateService;
import com.ihsmarkit.tfx.eod.service.MarginRatioService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@JobScope
public class MarginCollateralExcessDeficiencyTasklet implements Tasklet {

    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;

    private final ParticipantPositionRepository participantPositionRepository;

    private final EodCashSettlementRepository eodCashSettlementRepository;

    private EodParticipantMarginRepository eodParticipantMarginRepository;

    private final CollateralBalanceRepository collateralBalanceRepository;

    private final ClockService clockService;

    private final JPYRateService jpyRateService;

    private final MarginRatioService marginRatioService;

    private final EODCalculator eodCalculator;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {

        final Stream<EodProductCashSettlementEntity> margin =
            eodProductCashSettlementRepository.findBySettlementDateIsGreaterThanEqual(businessDate);

        final var aggregated = eodCalculator.aggregateRequiredMargin(margin, businessDate);

        final Stream<EodCashSettlementEntity> cashSettlement = aggregated.entrySet().stream()
            .flatMap(
                byParticipant -> byParticipant.getValue().entrySet().stream()
                    .flatMap(byType -> byType.getValue().entrySet().stream()
                        .map(
                            byDateType -> createCashSettlement(byParticipant.getKey(), byType.getKey(), byDateType.getKey(), byDateType.getValue())
                        )
                    )
            );

        eodCashSettlementRepository.saveAll(cashSettlement::iterator);

        final var dayCashSettlement = aggregated.entrySet().stream()
            .flatMap(
                byParticipant -> Optional.ofNullable(byParticipant.getValue().get(TOTAL_VM))
                    .flatMap(byType -> optionalPair(Optional.ofNullable(byType.get(DAY)), Optional.ofNullable(byType.get(TOTAL))))
                    .map(amount -> ImmutablePair.of(byParticipant.getKey(), amount))
                    .stream()
            ).collect(
                toMap(Pair::getLeft, Pair::getRight)
            );

        final var requiredInitialMargin = eodCalculator
            .calculateRequiredInitialMargin(
                participantPositionRepository.findAllNetAndRebalancingPositionsByTradeDate(businessDate),
                marginRatioService::getRequiredMarginRatio,
                ccy -> jpyRateService.getJpyRate(businessDate, ccy)
            );

        final Set<Long> participants =
            Stream.of(requiredInitialMargin, dayCashSettlement)
                .map(Map::keySet)
                .flatMap(Set::stream)
                .map(ParticipantEntity::getId)
                .collect(Collectors.toSet());

        final var deposits =
            collateralBalanceRepository.findByParticipantIdAndPurpose(participants, Set.of(CollateralPurpose.MARGIN))
            .stream()
                .collect(
                    Collectors.groupingBy(
                        CollateralBalanceEntity::getParticipant,
                        Collectors.reducing(
                            Pair.of(BigDecimal.ZERO, BigDecimal.ZERO),
                            balance -> Pair.of(balance.getAmount(), balance.getProduct().getType() == CASH ? balance.getAmount() : BigDecimal.ZERO),
                            (a, b) -> Pair.of(a.getLeft().add(b.getLeft()), a.getRight().add(b.getRight()))
                        )
                    )
                );

        final var participantMargin = Stream.of(requiredInitialMargin, dayCashSettlement, deposits)
            .map(Map::keySet)
            .flatMap(Set::stream)
            .distinct()
            .map(
                participant -> creaetEodParticipantMargin(
                    participant,
                    Optional.ofNullable(requiredInitialMargin.get(participant)),
                    Optional.ofNullable(dayCashSettlement.get(participant)),
                    Optional.ofNullable(deposits.get(participant))
                )
            );

        eodParticipantMarginRepository.saveAll(participantMargin::iterator);

        return RepeatStatus.FINISHED;
    }

    private Optional<Pair<Optional<BigDecimal>, Optional<BigDecimal>>> optionalPair(final Optional<BigDecimal> left, final Optional<BigDecimal> right) {
        return left.isEmpty() && right.isEmpty() ? Optional.empty() : Optional.of(Pair.of(left, right));
    }

    private EodParticipantMarginEntity creaetEodParticipantMargin(
        final ParticipantEntity participant,
        final Optional<BigDecimal> requiredInitialMargin,
        final Optional<Pair<Optional<BigDecimal>, Optional<BigDecimal>>> dayCashSettlement,
        final Optional<Pair<BigDecimal, BigDecimal>> balance) {

        return EodParticipantMarginEntity.builder()
            .date(businessDate)
            .participant(participant)
            .timestamp(clockService.getCurrentDateTimeUTC())
            .initialMargin(jpyAmountOf(requiredInitialMargin))
            .requiredAmount(jpyAmountOfDifference(requiredInitialMargin, dayCashSettlement.flatMap(Pair::getLeft)))
            .totalDeficit(jpyAmountOfDifference(balance.map(Pair::getLeft), dayCashSettlement.flatMap(Pair::getRight)))
            .cashDeficit(jpyAmountOfDifference(balance.map(Pair::getRight), dayCashSettlement.flatMap(Pair::getLeft)))
            .build();
    }


    private static AmountEntity jpyAmountOf(final Optional<BigDecimal> amnt) {
        return AmountEntity.of(amnt.orElse(BigDecimal.ZERO), JPY);
    }

    private static AmountEntity jpyAmountOfDifference(final Optional<BigDecimal> left, final Optional<BigDecimal> right) {
        return jpyAmountOf(
            left
                .flatMap(l -> right.map(l::subtract))
                .or(() -> left)
                .or(() -> right.map(BigDecimal::negate))
        );
    }

    private EodCashSettlementEntity createCashSettlement(
        final ParticipantEntity participant,
        final EodProductCashSettlementType type,
        final EodCashSettlementDateType dateType,
        final BigDecimal amount) {

        return EodCashSettlementEntity.builder()
            .date(businessDate)
            .timestamp(clockService.getCurrentDateTimeUTC())
            .dateType(dateType)
            .participant(participant)
            .type(type)
            .amount(AmountEntity.of(amount, JPY))
            .build();
    }

}
