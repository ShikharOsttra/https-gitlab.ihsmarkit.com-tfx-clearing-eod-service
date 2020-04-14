package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance;

import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.MARGIN;
import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodParticipantMarginEntity;
import com.ihsmarkit.tfx.core.dl.repository.collateral.CollateralBalanceRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodParticipantMarginRepository;
import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.CollateralBalanceItem;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.CollateralDeposit;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralCalculator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class CollateralBalanceInputProcessor implements ItemProcessor<ParticipantEntity, CollateralBalanceItem> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final CollateralBalanceRepository collateralBalanceRepository;

    private final CollateralRequirementProvider collateralRequirementProvider;

    private final EodCashSettlementRepository eodCashSettlementRepository;

    private final EodParticipantMarginRepository eodParticipantMarginRepository;

    private final CollateralCalculator collateralCalculator;

    @Override
    public CollateralBalanceItem process(final ParticipantEntity participant) {

        final var margin = eodParticipantMarginRepository.findByDateAndParticipant(businessDate, participant);
        final var cashSettlements = margin.map(marginVal -> getCashSettlement(participant)).orElseGet(HashBasedTable::create);

        return CollateralBalanceItem.builder()
            .participant(participant)
            .collateralDeposits(aggregateCollateralBalances(participant))
            .requiredAmount(getRequiredAmounts(participant, margin))
            .cashSettlements(cashSettlements)
            .initialMargin(margin.map(EodParticipantMarginEntity::getInitialMargin).orElse(null))
            .deficitInCashSettlement(getCashDeficit(margin))
            .nextClearingDeposit(collateralRequirementProvider.getNextClearingDeposit(participant.getId()))
            .build();
    }

    @Nullable
    private BigDecimal getCashDeficit(final Optional<EodParticipantMarginEntity> margin) {
        return margin.map(eodParticipantMarginEntity -> {
            final BigDecimal cashDeficit = eodParticipantMarginEntity.getCashDeficit();
            return cashDeficit.signum() >= 0 ? ZERO : cashDeficit.abs();
        }).orElse(null);
    }

    private Map<CollateralPurpose, CollateralDeposit> aggregateCollateralBalances(final ParticipantEntity participant) {
        return collateralBalanceRepository.findByParticipant(participant).stream()
            .collect(
                groupingBy(
                    CollateralBalanceEntity::getPurpose,
                    reducing(
                        CollateralDeposit.ZERO,
                        balance -> CollateralDeposit.of(balance.getProduct().getType(), collateralCalculator.calculateEvaluatedAmount(balance)),
                        CollateralDeposit::add
                    )
                )
            );
    }

    private Table<EodProductCashSettlementType, EodCashSettlementDateType, BigDecimal> getCashSettlement(final ParticipantEntity participant) {
        return eodCashSettlementRepository.findAllByDateAndParticipant(businessDate, participant).stream()
            .collect(
                Tables.toTable(
                    EodCashSettlementEntity::getType,
                    EodCashSettlementEntity::getDateType,
                    cashSettlement -> cashSettlement.getAmount().getValue(),
                    HashBasedTable::create)
            );
    }

    private Map<CollateralPurpose, BigDecimal> getRequiredAmounts(final ParticipantEntity participant, final Optional<EodParticipantMarginEntity> margin) {
        final Map<CollateralPurpose, BigDecimal> storedRequiredAmount = collateralRequirementProvider.getRequiredAmount(participant.getId());

        final Map<CollateralPurpose, BigDecimal> requiredAmounts = new HashMap<>(storedRequiredAmount);
        requiredAmounts.put(MARGIN, margin.map(EodParticipantMarginEntity::getRequiredAmount).orElse(ZERO));

        return requiredAmounts;
    }

}
