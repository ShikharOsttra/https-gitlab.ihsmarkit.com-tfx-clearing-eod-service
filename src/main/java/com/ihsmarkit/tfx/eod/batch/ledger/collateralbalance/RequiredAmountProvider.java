package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Lazy;
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
public class RequiredAmountProvider {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final ParticipantCollateralRequirementRepository requirementRepository;

    private final Lazy<Table<Long, CollateralPurpose, BigDecimal>> requiredAmounts = Lazy.of(this::loadRequiredAmount);

    private Table<Long, CollateralPurpose, BigDecimal> loadRequiredAmount() {

        return requirementRepository.findByBusinessDate(businessDate).stream()
            .collect(Tables.toTable(
                requiredAmount -> requiredAmount.getParticipant().getId(),
                ParticipantCollateralRequirementEntity::getPurpose,
                requirementEntity -> requirementEntity.getValue().setScale(0, RoundingMode.DOWN),
                HashBasedTable::create));
    }

    public Optional<BigDecimal> getRequiredAmount(final Long participantId, final CollateralPurpose purpose) {
        return Optional.ofNullable(requiredAmounts.get().get(participantId, purpose));
    }

}
