package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MARGIN_COLLATERAL_EXCESS_OR_DEFICIENCY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.MarginAlertConfigurationEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodParticipantMarginEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.repository.MarginAlertConfigurationRepository;
import com.ihsmarkit.tfx.core.dl.repository.ParticipantRepository;
import com.ihsmarkit.tfx.core.dl.repository.collateral.CollateralBalanceRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodParticipantMarginRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType;
import com.ihsmarkit.tfx.core.domain.type.MarginAlertLevel;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralCalculator;
import com.ihsmarkit.tfx.eod.mapper.CashSettlementMapper;
import com.ihsmarkit.tfx.eod.mapper.ParticipantMarginMapper;
import com.ihsmarkit.tfx.eod.model.BalanceContribution;
import com.ihsmarkit.tfx.eod.model.CashSettlement;
import com.ihsmarkit.tfx.eod.service.CalendarDatesProvider;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.JPYRateService;
import com.ihsmarkit.tfx.eod.service.MarginRatioService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;

@Service
@AllArgsConstructor
@JobScope
@Slf4j
@SuppressWarnings("PMD.TooManyFields")
public class MarginCollateralExcessDeficiencyTasklet implements Tasklet {

    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;

    private final ParticipantPositionRepository participantPositionRepository;

    private final EodCashSettlementRepository eodCashSettlementRepository;

    private final EodParticipantMarginRepository eodParticipantMarginRepository;

    private final CollateralBalanceRepository collateralBalanceRepository;

    private final MarginAlertConfigurationRepository marginAlertConfigurationRepository;

    private final ClockService clockService;

    private final JPYRateService jpyRateService;

    private final MarginRatioService marginRatioService;

    private final EODCalculator eodCalculator;

    private final CollateralCalculator collateralCalculator;

    private final ParticipantMarginMapper participantMarginMapper;

    private final CashSettlementMapper cashSettlementMapper;

    private final CalendarDatesProvider calendarDatesProvider;

    private final ParticipantRepository participantRepository;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {

        final List<EodProductCashSettlementEntity> margin =
            eodProductCashSettlementRepository.findAllBySettlementDateIsGreaterThan(businessDate).collect(Collectors.toList());

        final Optional<LocalDate> theDay = calendarDatesProvider.getNextTradingDate(businessDate);
        final Optional<LocalDate> followingDay = theDay.flatMap(calendarDatesProvider::getNextTradingDate);
        final var aggregated = eodCalculator.aggregateRequiredMargin(margin, theDay, followingDay);

        final Stream<EodCashSettlementEntity> cashSettlement = EntryStream.of(aggregated)
            .flatMapKeyValue((participant, participantSettlements) ->
                EntryStream.of(participantSettlements)
                    .flatMapKeyValue((settlementType, settlements) ->
                        EntryStream.of(settlements)
                            .mapKeyValue((dateType, amount) ->
                                CashSettlement.builder()
                                    .participant(participant)
                                    .type(settlementType)
                                    .dateType(dateType)
                                    .amount(amount)
                                    .build()
                            )
                            .map(settlement -> cashSettlementMapper.toEntity(settlement, businessDate, clockService.getCurrentDateTimeUTC()))
                    )
            );

        eodCashSettlementRepository.saveAll(cashSettlement::iterator);

        final Map<ParticipantEntity, EnumMap<EodCashSettlementDateType, BigDecimal>> variationMargins =
            EntryStream.of(aggregated)
                .flatMapValues(byParticipant -> Stream.ofNullable(byParticipant.get(TOTAL_VM)))
                .toMap();

        final var requiredInitialMargin = eodCalculator
            .calculateRequiredInitialMargin(
                participantPositionRepository.findAllNetAndRebalancingPositionsByTradeDate(businessDate),
                marginRatioService::getRequiredMarginRatio,
                (baseCcy, valueCcy) -> jpyRateService.getJpyRate(businessDate, baseCcy, valueCcy)
            );

        final var deposits = calculateDeposits(uniqueParticipantIds(requiredInitialMargin, variationMargins));
        final var marginAlertCalculatorsPerParticipant = marginAlertConfigurationRepository.findAll().stream()
            .collect(Collectors.groupingBy(
                entity -> entity.getParticipant().getId(),
                Collectors.collectingAndThen(
                    Collectors.toMap(
                        MarginAlertConfigurationEntity::getLevel,
                        MarginAlertConfigurationEntity::getTriggerLevel
                    ),
                    MarginAlertLevel::associatedLevelCalculator
                )
            ));

        final var participantMap = participantRepository.findAllNotDeletedWithoutClearingHouse().stream()
            .collect(Collectors.toMap(ParticipantEntity::getCode, Function.identity()));

        final var participantMargin =
            eodCalculator
                .calculateParticipantMargin(
                    requiredInitialMargin,
                    variationMargins,
                    deposits,
                    marginAlertCalculatorsPerParticipant
                )
                .map(marginEntry -> participantMarginMapper.toEntity(marginEntry, businessDate, clockService.getCurrentDateTimeUTC()))
                .collect(Collectors.toMap(item -> item.getParticipant().getCode(), Function.identity()));

        final var allParticipantMargin = EntryStream.of(participantMap)
            .mapKeyValue((participantCode, participant) -> participantMargin.getOrDefault(participantCode, emptyMargin(participant)))
            .toImmutableList();

        eodParticipantMarginRepository.saveAll(allParticipantMargin);

        return RepeatStatus.FINISHED;
    }

    private EodParticipantMarginEntity emptyMargin(final ParticipantEntity participant) {
        return participantMarginMapper.toEmptyEodMargin(participant, businessDate, clockService.getCurrentDateTimeUTC());
    }

    private Map<ParticipantEntity, BalanceContribution> calculateDeposits(final Set<Long> participants) {
        if (participants.isEmpty()) {
            log.warn("[{}] no participants with collateral requirements found", MARGIN_COLLATERAL_EXCESS_OR_DEFICIENCY);
            return Map.of();
        } else {
            return eodCalculator.calculateDeposits(
                collateralBalanceRepository.findByParticipantIdAndPurpose(participants, Set.of(CollateralPurpose.MARGIN)).stream(),
                collateralCalculator::calculateEvaluatedAmount
            );
        }
    }

    @SafeVarargs
    private Set<Long> uniqueParticipantIds(final Map<ParticipantEntity, ?>...requiredInitialMargin) {
        return Arrays.stream(requiredInitialMargin)
            .map(Map::keySet)
            .flatMap(Set::stream)
            .map(ParticipantEntity::getId)
            .collect(Collectors.toSet());
    }
}
