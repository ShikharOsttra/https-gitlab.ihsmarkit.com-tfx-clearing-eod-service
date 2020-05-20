package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance;

import static com.google.common.collect.Tables.transformValues;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.ihsmarkit.tfx.core.dl.entity.collateral.ParticipantCollateralRequirementEntity;
import com.ihsmarkit.tfx.core.dl.repository.collateral.ParticipantCollateralRequirementRepository;
import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.eod.service.CalendarDatesProvider;

import lombok.AllArgsConstructor;

@StepScope
@Component
@AllArgsConstructor
public class CollateralRequirementProvider {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final ParticipantCollateralRequirementRepository requirementRepository;

    private final CalendarDatesProvider calendarDatesProvider;

    private final Lazy<Table<Long, CollateralPurpose, BigDecimal>> requiredAmounts = Lazy.of(this::loadRequiredAmount);

    private final Lazy<Map<Long, Pair<LocalDate, BigDecimal>>> nextClearingDepositRequiredAmount = Lazy.of(this::loadNextClearingDepositRequiredAmount);

    private Table<Long, CollateralPurpose, BigDecimal> loadRequiredAmount() {

        return requirementRepository.findNotOutdatedByBusinessDate(businessDate).stream()
            .filter(requiredAmount -> !requiredAmount.getBusinessDate().isAfter(calendarDatesProvider.getNextBusinessDate()))
            .collect(
                collectingAndThen(
                    Tables.toTable(
                        requiredAmount -> requiredAmount.getParticipant().getId(),
                        ParticipantCollateralRequirementEntity::getPurpose,
                        Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(ParticipantCollateralRequirementEntity::getBusinessDate)),
                        HashBasedTable::create
                    ),
                    table -> transformValues(table, requiredAmount -> requiredAmount.getValue().setScale(0, RoundingMode.DOWN))
                )
            );
    }

    private Map<Long, Pair<LocalDate, BigDecimal>> loadNextClearingDepositRequiredAmount() {
        return requirementRepository.findFutureOnlyByBusinessDate(calendarDatesProvider.getNextBusinessDate()).stream()
            .filter(requirement -> requirement.getPurpose() == CollateralPurpose.CLEARING_DEPOSIT)
            .collect(toMap(
                requiredAmount -> requiredAmount.getParticipant().getId(),
                requiredAmount -> Pair.of(requiredAmount.getApplicableDate(), requiredAmount.getValue().setScale(0, RoundingMode.DOWN))
            ));
    }

    public Map<CollateralPurpose, BigDecimal> getRequiredAmount(final Long participantId) {
        return requiredAmounts.get().row(participantId);
    }

    public Optional<Pair<LocalDate, BigDecimal>> getNextClearingDeposit(final Long participantId) {
        return Optional.ofNullable(nextClearingDepositRequiredAmount.get().get(participantId));
    }

}
