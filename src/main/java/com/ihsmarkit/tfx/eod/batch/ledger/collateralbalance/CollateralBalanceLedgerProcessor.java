package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance;

import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.CLEARING_DEPOSIT;
import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.MARGIN;
import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.MARKET_ENTRY_DEPOSIT;
import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.SPECIAL_PURPOSE_COLLATERAL;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.DAY;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.FOLLOWING;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.TOTAL;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.DAILY_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.INITIAL_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.SWAP_PNL;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.ITEM_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimalRoundTo1Jpy;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimalRoundTo1JpyDefaultZero;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static com.ihsmarkit.tfx.eod.batch.ledger.OrderUtils.buildIndexBasedOrder;
import static com.ihsmarkit.tfx.eod.batch.ledger.OrderUtils.buildOrderId;
import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
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
import com.ihsmarkit.tfx.eod.batch.ledger.EvaluationDateProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantCodeOrderIdProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralCalculator;
import com.ihsmarkit.tfx.eod.model.ledger.CollateralBalanceItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class CollateralBalanceLedgerProcessor implements ItemProcessor<ParticipantEntity, List<CollateralBalanceItem>> {

    private static final String FOLLOWING_CLEARING_DEPOSIT_PURPOSE = "(The Following Applicable Amount)";

    private static final TotalBalance ZERO_BALANCE = TotalBalance.of(ZERO, ZERO, ZERO, ZERO);

    private static final Map<String, Integer> PURPOSE_ORDER_ID_MAP =
        buildIndexBasedOrder(
            MARGIN.name(),
            MARKET_ENTRY_DEPOSIT.name(),
            CLEARING_DEPOSIT.name(),
            FOLLOWING_CLEARING_DEPOSIT_PURPOSE,
            SPECIAL_PURPOSE_COLLATERAL.name()
        );

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final CollateralBalanceRepository collateralBalanceRepository;

    private final CollateralRequirementProvider collateralRequirementProvider;

    private final EodCashSettlementRepository eodCashSettlementRepository;

    private final EodParticipantMarginRepository eodParticipantMarginRepository;

    private final CollateralCalculator collateralCalculator;

    private final ParticipantCodeOrderIdProvider participantCodeOrderIdProvider;

    private final EvaluationDateProvider evaluationDateProvider;

    @Override
    public List<CollateralBalanceItem> process(final ParticipantEntity participant) {

        final var balances = calculateTotalBalances(participant);
        final var margin = eodParticipantMarginRepository.findByDateAndParticipant(businessDate, participant);
        final var cashSettlement = margin.map(marginVal -> getCashSettlement(participant)).orElseGet(HashBasedTable::create);

        return Stream.of(CollateralPurpose.values())
            .flatMap(purpose -> mapToCollateralBalance(purpose, participant, balances.getOrDefault(purpose, ZERO_BALANCE), margin, cashSettlement))
            .collect(toList());
    }

    private Stream<CollateralBalanceItem> mapToCollateralBalance(
        final CollateralPurpose purpose,
        final ParticipantEntity participant,
        final TotalBalance balance,
        final Optional<EodParticipantMarginEntity> margin,
        final Table<EodProductCashSettlementType, EodCashSettlementDateType, BigDecimal> cashSettlement
    ) {
        switch (purpose) {
            case MARGIN:
                return Stream.of(mapToMarginItem(participant, balance, margin, cashSettlement));
            case CLEARING_DEPOSIT:
                return mapToClearingDepositItems(participant, balance);
            default:
                return Stream.of(mapToPurposeItem(purpose, participant, balance));
        }
    }

    private Stream<CollateralBalanceItem> mapToClearingDepositItems(final ParticipantEntity participant, final TotalBalance balance) {
        final Optional<Pair<LocalDate, BigDecimal>> nextClearingDepositRequiredAmount = collateralRequirementProvider.getNextClearingDepositRequiredAmount(
            participant.getId());

        return Stream.of(
            mapToPurposeItem(CLEARING_DEPOSIT, participant, balance),
            mapToNextClearingDepositItem(nextClearingDepositRequiredAmount, participant, balance)
        );
    }

    private CollateralBalanceItem mapToNextClearingDepositItem(
        final Optional<Pair<LocalDate, BigDecimal>> nextClearingDepositRequiredAmount,
        final ParticipantEntity participant,
        final TotalBalance balance
    ) {
        final CollateralBalanceItem.CollateralBalanceItemBuilder itemBuilder = itemBuilder(participant)
            .collateralPurpose(FOLLOWING_CLEARING_DEPOSIT_PURPOSE)
            .orderId(getOrderId(participant, FOLLOWING_CLEARING_DEPOSIT_PURPOSE));

        nextClearingDepositRequiredAmount.ifPresent(requiredAmount ->
            itemBuilder.requiredAmount(formatBigDecimalRoundTo1Jpy(requiredAmount.getSecond()))
                .totalExcessDeficit(formatBigDecimalRoundTo1Jpy(balance.getTotal().subtract(requiredAmount.getSecond())))
                .followingApplicableDayForClearingDeposit(formatDate(requiredAmount.getFirst()))
        );

        return itemBuilder.build();
    }

    private CollateralBalanceItem mapToPurposeItem(
        final CollateralPurpose purpose,
        final ParticipantEntity participant,
        final TotalBalance balance
    ) {
        final BigDecimal requiredAmount = collateralRequirementProvider.getRequiredAmount(participant.getId(), purpose).orElse(ZERO);
        final BigDecimal totalExcessDeficit = balance.getTotal().subtract(requiredAmount);

        return purposeItemBuilder(purpose, participant, balance)
            .requiredAmount(formatBigDecimalRoundTo1Jpy(requiredAmount))
            .totalExcessDeficit(formatBigDecimalRoundTo1Jpy(totalExcessDeficit))
            .orderId(getOrderId(participant, purpose.name()))
            .build();
    }

    private CollateralBalanceItem mapToMarginItem(
        final ParticipantEntity participant,
        final TotalBalance balance,
        final Optional<EodParticipantMarginEntity> margin,
        final Table<EodProductCashSettlementType, EodCashSettlementDateType, BigDecimal> cashSettlement
    ) {
        final BigDecimal requiredAmount = margin.map(EodParticipantMarginEntity::getRequiredAmount).orElse(ZERO);
        final BigDecimal totalExcessDeficit = balance.getTotal().subtract(requiredAmount);

        return purposeItemBuilder(MARGIN, participant, balance)
            .requiredAmount(formatBigDecimalRoundTo1Jpy(requiredAmount))
            .totalInitialMargin(formatBigDecimalRoundTo1Jpy(margin.map(EodParticipantMarginEntity::getInitialMargin)))
            .totalVariationMargin(formatBigDecimalRoundTo1Jpy(cashSettlement.get(TOTAL_VM, TOTAL)))
            .totalExcessDeficit(formatBigDecimalRoundTo1Jpy(totalExcessDeficit))
            .deficitInCashSettlement(formatBigDecimalRoundTo1Jpy(getCashDeficit(margin)))
            .cashSettlement(formatBigDecimalRoundTo1Jpy(cashSettlement.get(TOTAL_VM, DAY)))
            .cashSettlementFollowingDay(formatBigDecimalRoundTo1Jpy(cashSettlement.get(TOTAL_VM, FOLLOWING)))
            .initialMtmTotal(formatBigDecimalRoundTo1JpyDefaultZero(cashSettlement.get(INITIAL_MTM, TOTAL)))
            .initialMtmDay(formatBigDecimalRoundTo1JpyDefaultZero(cashSettlement.get(INITIAL_MTM, DAY)))
            .initialMtmFollowingDay(formatBigDecimalRoundTo1JpyDefaultZero(cashSettlement.get(INITIAL_MTM, FOLLOWING)))
            .dailyMtmTotal(formatBigDecimalRoundTo1JpyDefaultZero(cashSettlement.get(DAILY_MTM, TOTAL)))
            .dailyMtmDay(formatBigDecimalRoundTo1JpyDefaultZero(cashSettlement.get(DAILY_MTM, DAY)))
            .dailyMtmFollowingDay(formatBigDecimalRoundTo1JpyDefaultZero(cashSettlement.get(DAILY_MTM, FOLLOWING)))
            .swapPointTotal(formatBigDecimalRoundTo1JpyDefaultZero(cashSettlement.get(SWAP_PNL, TOTAL)))
            .swapPointDay(formatBigDecimalRoundTo1JpyDefaultZero(cashSettlement.get(SWAP_PNL, DAY)))
            .swapPointFollowingDay(formatBigDecimalRoundTo1JpyDefaultZero(cashSettlement.get(SWAP_PNL, FOLLOWING)))
            .build();
    }

    private Optional<BigDecimal> getCashDeficit(final Optional<EodParticipantMarginEntity> margin) {
        return margin.map(eodParticipantMarginEntity -> {
            final BigDecimal cashDeficit = eodParticipantMarginEntity.getCashDeficit();
            return cashDeficit.signum() < 0 ? ZERO : cashDeficit;
        });
    }

    private CollateralBalanceItem.CollateralBalanceItemBuilder itemBuilder(final ParticipantEntity participant) {
        return CollateralBalanceItem.builder()
            .businessDate(businessDate)
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .evaluationDate(formatDate(evaluationDateProvider.get()))
            .participantCode(participant.getCode())
            .participantName(participant.getName())
            .participantType(formatEnum(participant.getType()))
            .recordType(ITEM_RECORD_TYPE);
    }

    private CollateralBalanceItem.CollateralBalanceItemBuilder purposeItemBuilder(
        final CollateralPurpose purpose,
        final ParticipantEntity participant,
        final TotalBalance balance
    ) {
        return itemBuilder(participant)
            .collateralPurposeType(purpose.getValue().toString())
            .collateralPurpose(formatEnum(purpose))
            .totalDeposit(formatBigDecimalRoundTo1Jpy(balance.getTotal()))
            .cash(formatBigDecimalRoundTo1Jpy(balance.getCash()))
            .lg(formatBigDecimalRoundTo1Jpy(balance.getLg()))
            .securities(formatBigDecimalRoundTo1Jpy(balance.getSecurities()))
            .orderId(getOrderId(participant, purpose.name()));
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
        return eodCashSettlementRepository.findAllByDateAndParticipant(businessDate, participant).stream()
            .collect(
                Tables.toTable(
                    EodCashSettlementEntity::getType,
                    EodCashSettlementEntity::getDateType,
                    cashSettlement -> cashSettlement.getAmount().getValue(),
                    HashBasedTable::create)
            );
    }

    private long getOrderId(final ParticipantEntity participantEntity, final String purpose) {
        return buildOrderId(
            participantCodeOrderIdProvider.get(participantEntity.getCode()),
            PURPOSE_ORDER_ID_MAP.get(purpose)
        );
    }

}
