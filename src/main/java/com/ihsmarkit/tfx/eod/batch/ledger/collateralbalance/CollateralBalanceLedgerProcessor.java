package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance;

import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.MARGIN;
import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.values;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.DAY;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.FOLLOWING;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.TOTAL;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.DAILY_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.INITIAL_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.SWAP_PNL;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
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
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralCalculator;
import com.ihsmarkit.tfx.eod.model.ledger.CollateralBalanceItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class CollateralBalanceLedgerProcessor implements ItemProcessor<ParticipantEntity, List<CollateralBalanceItem>> {

    private static final TotalBalance ZERO_BALANCE = TotalBalance.of(ZERO, ZERO, ZERO, ZERO);

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final CollateralBalanceRepository collateralBalanceRepository;

    private final RequiredAmountProvider requiredAmountProvider;

    private final EodCashSettlementRepository eodCashSettlementRepository;

    private final EodParticipantMarginRepository eodParticipantMarginRepository;

    private final CollateralCalculator collateralCalculator;

    @Override
    public List<CollateralBalanceItem> process(final ParticipantEntity participant) {

        final var balances = calculateTotalBalances(participant);
        final var margin = eodParticipantMarginRepository.findByDateAndParticipant(businessDate, participant);
        final var cashSettlement = margin.map(marginVal -> getCashSettlement(participant)).orElseGet(HashBasedTable::create);

        return Stream.of(values())
            .sorted(Comparator.comparing(CollateralPurpose::getValue))
            .map(purpose -> mapToCollateralBalance(purpose, participant, balances, margin, cashSettlement))
            .collect(Collectors.toList());
    }

    private CollateralBalanceItem mapToCollateralBalance(
        final CollateralPurpose purpose,
        final ParticipantEntity participant,
        final Map<CollateralPurpose, TotalBalance> balances,
        final Optional<EodParticipantMarginEntity> margin,
        final Table<EodProductCashSettlementType, EodCashSettlementDateType, BigDecimal> cashSettlement
    ) {
        final BigDecimal requiredAmount = getRequiredAmount(purpose, margin, participant).orElse(ZERO);
        final BigDecimal totalExcessDeficit = balances.getOrDefault(purpose, ZERO_BALANCE).getTotal().subtract(requiredAmount);

        return CollateralBalanceItem.builder()
            .businessDate(businessDate)
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .participantCode(participant.getCode())
            .participantName(participant.getName())
            .participantType(formatEnum(participant.getType()))
            .collateralPurposeType(purpose.getValue().toString())
            .collateralPurpose(formatEnum(purpose))
            .totalDeposit(balances.getOrDefault(purpose, ZERO_BALANCE).getTotal().toString())
            .cash(balances.getOrDefault(purpose, ZERO_BALANCE).getCash().toString())
            .lg(balances.getOrDefault(purpose, ZERO_BALANCE).getLg().toString())
            .securities(balances.getOrDefault(purpose, ZERO_BALANCE).getSecurities().toString())
            .requiredAmount(requiredAmount.toString())
            .totalInitialMargin(getFromMarginEntity(purpose, margin, EodParticipantMarginEntity::getInitialMargin))
            .totalVariationMargin(getForMargin(purpose, cashSettlement.get(TOTAL_VM, TOTAL)))
            .totalExcessDeficit(totalExcessDeficit.toString())
            .deficitInCashSettlement(getFromMarginEntity(purpose, margin, EodParticipantMarginEntity::getCashDeficit))
            .cashSettlement(getForMargin(purpose, cashSettlement.get(TOTAL_VM, DAY)))
            .cashSettlementFollowingDay(getForMargin(purpose, cashSettlement.get(TOTAL_VM, FOLLOWING)))
            .initialMtmTotal(getForMargin(purpose, cashSettlement.get(INITIAL_MTM, TOTAL)))
            .initialMtmDay(getForMargin(purpose, cashSettlement.get(INITIAL_MTM, DAY)))
            .initialMtmFollowingDay(getForMargin(purpose, cashSettlement.get(INITIAL_MTM, FOLLOWING)))
            .dailyMtmTotal(getForMargin(purpose, cashSettlement.get(DAILY_MTM, TOTAL)))
            .dailyMtmDay(getForMargin(purpose, cashSettlement.get(DAILY_MTM, DAY)))
            .dailyMtmFollowingDay(getForMargin(purpose, cashSettlement.get(DAILY_MTM, FOLLOWING)))
            .swapPointTotal(getForMargin(purpose, cashSettlement.get(SWAP_PNL, TOTAL)))
            .swapPointDay(getForMargin(purpose, cashSettlement.get(SWAP_PNL, DAY)))
            .swapPointFollowingDay(getForMargin(purpose, cashSettlement.get(SWAP_PNL, FOLLOWING)))
            //todo: ???
            .followingApplicableDayForClearingDeposit(EMPTY)
            .build();
    }

    private Optional<BigDecimal> getRequiredAmount(
        final CollateralPurpose purpose,
        final Optional<EodParticipantMarginEntity> margin,
        final ParticipantEntity participant
    ) {
        return purpose == MARGIN
               ? margin.map(marginVal -> marginVal.getRequiredAmount().getValue())
               : requiredAmountProvider.getRequiredAmount(participant.getId(), purpose);
    }

    private Map<CollateralPurpose, TotalBalance> calculateTotalBalances(final ParticipantEntity participant) {
        return collateralBalanceRepository.findByParticipant(participant).stream()
            .collect(
                groupingBy(
                    CollateralBalanceEntity::getPurpose,
                    reducing(
                        ZERO_BALANCE,
                        balance -> TotalBalance.of(balance.getProduct().getType(), collateralCalculator.calculateEvaluatedAmount(balance)),
                        TotalBalance::add
                    )
                )
            );
    }

    private Table<EodProductCashSettlementType, EodCashSettlementDateType, BigDecimal> getCashSettlement(final ParticipantEntity participant) {
        return eodCashSettlementRepository.findByDateAndParticipant(businessDate, participant).stream()
            .collect(
                Tables.toTable(
                    EodCashSettlementEntity::getType,
                    EodCashSettlementEntity::getDateType,
                    cashSettlement -> cashSettlement.getAmount().getValue(),
                    HashBasedTable::create)
            );
    }

    private static String getFromMarginEntity(
        final CollateralPurpose purpose,
        final Optional<EodParticipantMarginEntity> margin,
        final Function<EodParticipantMarginEntity, AmountEntity> amountMapper
    ) {
        return purpose == MARGIN
               ? margin.map(amountMapper).map(amount -> amount.getValue().toString()).orElse(EMPTY)
               : EMPTY;
    }

    private static String getForMargin(final CollateralPurpose purpose, final @Nullable BigDecimal value) {
        return purpose == MARGIN
               ? Optional.ofNullable(value).map(BigDecimal::toString).orElse(EMPTY)
               : EMPTY;
    }

}
