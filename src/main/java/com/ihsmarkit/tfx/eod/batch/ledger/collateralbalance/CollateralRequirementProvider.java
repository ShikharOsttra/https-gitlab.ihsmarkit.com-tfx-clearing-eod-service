package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

import lombok.AllArgsConstructor;

@StepScope
@Component
@AllArgsConstructor
public class CollateralRequirementProvider {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final ParticipantCollateralRequirementRepository requirementRepository;

    private final Lazy<Table<Long, CollateralPurpose, BigDecimal>> requiredAmounts = Lazy.of(this::loadRequiredAmount);

    private final Lazy<Map<Long, Pair<LocalDate, BigDecimal>>> nextClearingDepositRequiredAmount = Lazy.of(this::loadNextClearingDepositRequiredAmount);

    private Table<Long, CollateralPurpose, BigDecimal> loadRequiredAmount() {

        return requirementRepository.findByBusinessDate(businessDate).stream()
            .collect(Tables.toTable(
                requiredAmount -> requiredAmount.getParticipant().getId(),
                ParticipantCollateralRequirementEntity::getPurpose,
                requirementEntity -> requirementEntity.getValue().setScale(0, RoundingMode.DOWN),
                HashBasedTable::create));
    }

    private Map<Long, Pair<LocalDate, BigDecimal>> loadNextClearingDepositRequiredAmount() {
        return requirementRepository.findFutureOnlyByBusinessDate(businessDate).stream()
            .filter(requirement -> requirement.getPurpose() == CollateralPurpose.CLEARING_DEPOSIT)
            .collect(Collectors.toMap(
                requiredAmount -> requiredAmount.getParticipant().getId(),
                requiredAmount -> Pair.of(requiredAmount.getApplicableDate(), requiredAmount.getValue().setScale(0, RoundingMode.DOWN))
            ));
    }

    public Optional<BigDecimal> getRequiredAmount(final Long participantId, final CollateralPurpose purpose) {
        return Optional.ofNullable(requiredAmounts.get().get(participantId, purpose));
    }

    public Optional<Pair<LocalDate, BigDecimal>> getNextClearingDepositRequiredAmount(final Long participantId) {
        return Optional.ofNullable(nextClearingDepositRequiredAmount.get().get(participantId));
    }

}
