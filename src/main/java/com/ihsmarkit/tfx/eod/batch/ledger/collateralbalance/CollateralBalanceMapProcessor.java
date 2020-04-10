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
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.eod.batch.ledger.EvaluationDateProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantCodeOrderIdProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.CollateralBalanceAdapter;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.CollateralBalanceItem;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.CollateralBalanceTotal;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.CollateralBalanceWriteItem;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.CollateralDeposit;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.ParticipantCollateralBalanceAdapter;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.TfxTotalCollateralBalanceAdapter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class CollateralBalanceMapProcessor implements ItemProcessor<CollateralBalanceItem, List<CollateralBalanceWriteItem>> {

    private static final String FOLLOWING_CLEARING_DEPOSIT_PURPOSE = "(The Following Applicable Amount)";

    private static final Set<CollateralPurpose> SECURITIES_BALANCE_PURPOSES = Set.of(MARKET_ENTRY_DEPOSIT, CLEARING_DEPOSIT);

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

    private final ParticipantCodeOrderIdProvider participantCodeOrderIdProvider;

    private final EvaluationDateProvider evaluationDateProvider;

    @Override
    public List<CollateralBalanceWriteItem> process(final CollateralBalanceItem item) {
        return mapToCollateralBalanceWriteItems(ParticipantCollateralBalanceAdapter.of(item));
    }

    public List<CollateralBalanceWriteItem> mapToTfxTotal(@Nullable final CollateralBalanceTotal total) {
        if (total == null) {
            return List.of();
        }

        return mapToCollateralBalanceWriteItems(TfxTotalCollateralBalanceAdapter.of(total));
    }

    private List<CollateralBalanceWriteItem> mapToCollateralBalanceWriteItems(final CollateralBalanceAdapter collateralBalanceAdapter) {
        return List.of(
            mapToMarginItem(collateralBalanceAdapter),
            purposeItemBuilder(MARKET_ENTRY_DEPOSIT, collateralBalanceAdapter).build(),
            purposeItemBuilder(CLEARING_DEPOSIT, collateralBalanceAdapter).build(),
            mapToNextClearingDepositItem(collateralBalanceAdapter),
            purposeItemBuilder(SPECIAL_PURPOSE_COLLATERAL, collateralBalanceAdapter).build()
        );
    }

    private CollateralBalanceWriteItem mapToNextClearingDepositItem(final CollateralBalanceAdapter collateralBalanceAdapter) {
        final CollateralBalanceWriteItem.CollateralBalanceWriteItemBuilder itemBuilder = itemBuilder(collateralBalanceAdapter)
            .collateralPurpose(FOLLOWING_CLEARING_DEPOSIT_PURPOSE)
            .orderId(getOrderId(collateralBalanceAdapter.getParticipantCode(), FOLLOWING_CLEARING_DEPOSIT_PURPOSE));

        collateralBalanceAdapter.getNextClearingDeposit().ifPresent(requiredAmount ->
            itemBuilder.requiredAmount(formatBigDecimalRoundTo1Jpy(requiredAmount.getSecond()))
                .totalExcessDeficit(formatBigDecimalRoundTo1Jpy(collateralBalanceAdapter.getCollateralDeposits()
                    .getOrDefault(CLEARING_DEPOSIT, CollateralDeposit.ZERO).getTotal().subtract(requiredAmount.getSecond())))
                .followingApplicableDayForClearingDeposit(formatDate(requiredAmount.getFirst()))
        );

        return itemBuilder.build();
    }

    private CollateralBalanceWriteItem mapToMarginItem(final CollateralBalanceAdapter collateralBalanceAdapter) {
        return purposeItemBuilder(MARGIN, collateralBalanceAdapter)
            .totalInitialMargin(formatBigDecimalRoundTo1Jpy(collateralBalanceAdapter.getInitialMargin()))
            .totalVariationMargin(formatBigDecimalRoundTo1Jpy(collateralBalanceAdapter.getCashSettlement(TOTAL_VM, TOTAL)))
            .deficitInCashSettlement(formatBigDecimalRoundTo1Jpy(collateralBalanceAdapter.getDeficitInCashSettlement()))
            .cashSettlement(formatBigDecimalRoundTo1Jpy(collateralBalanceAdapter.getCashSettlement(TOTAL_VM, DAY)))
            .cashSettlementFollowingDay(formatBigDecimalRoundTo1Jpy(collateralBalanceAdapter.getCashSettlement(TOTAL_VM, FOLLOWING)))
            .initialMtmTotal(formatBigDecimalRoundTo1JpyDefaultZero(collateralBalanceAdapter.getCashSettlement(INITIAL_MTM, TOTAL)))
            .initialMtmDay(formatBigDecimalRoundTo1JpyDefaultZero(collateralBalanceAdapter.getCashSettlement(INITIAL_MTM, DAY)))
            .initialMtmFollowingDay(formatBigDecimalRoundTo1JpyDefaultZero(collateralBalanceAdapter.getCashSettlement(INITIAL_MTM, FOLLOWING)))
            .dailyMtmTotal(formatBigDecimalRoundTo1JpyDefaultZero(collateralBalanceAdapter.getCashSettlement(DAILY_MTM, TOTAL)))
            .dailyMtmDay(formatBigDecimalRoundTo1JpyDefaultZero(collateralBalanceAdapter.getCashSettlement(DAILY_MTM, DAY)))
            .dailyMtmFollowingDay(formatBigDecimalRoundTo1JpyDefaultZero(collateralBalanceAdapter.getCashSettlement(DAILY_MTM, FOLLOWING)))
            .swapPointTotal(formatBigDecimalRoundTo1JpyDefaultZero(collateralBalanceAdapter.getCashSettlement(SWAP_PNL, TOTAL)))
            .swapPointDay(formatBigDecimalRoundTo1JpyDefaultZero(collateralBalanceAdapter.getCashSettlement(SWAP_PNL, DAY)))
            .swapPointFollowingDay(formatBigDecimalRoundTo1JpyDefaultZero(collateralBalanceAdapter.getCashSettlement(SWAP_PNL, FOLLOWING)))
            .build();
    }

    private CollateralBalanceWriteItem.CollateralBalanceWriteItemBuilder purposeItemBuilder(
        final CollateralPurpose purpose,
        final CollateralBalanceAdapter collateralBalanceAdapter
    ) {
        final CollateralDeposit deposit = collateralBalanceAdapter.getCollateralDeposits().getOrDefault(purpose, CollateralDeposit.ZERO);
        final BigDecimal totalDeposit = deposit.getTotal();
        final BigDecimal requiredAmount = collateralBalanceAdapter.getRequiredAmount().getOrDefault(purpose, ZERO);
        final BigDecimal totalExcessDeficit = totalDeposit.subtract(requiredAmount);

        return itemBuilder(collateralBalanceAdapter)
            .collateralPurposeType(purpose.getValue().toString())
            .collateralPurpose(formatEnum(purpose))
            .cash(formatBigDecimalRoundTo1Jpy(deposit.getCash()))
            .lg(collateralBalanceAdapter.isShowLgBalance(purpose) ? formatBigDecimalRoundTo1Jpy(deposit.getLg()) : EMPTY)
            .securities(SECURITIES_BALANCE_PURPOSES.contains(purpose) ? formatBigDecimalRoundTo1Jpy(deposit.getSecurities()) : EMPTY)
            .totalDeposit(formatBigDecimalRoundTo1Jpy(deposit.getTotal()))
            .requiredAmount(formatBigDecimalRoundTo1Jpy(requiredAmount))
            .totalExcessDeficit(formatBigDecimalRoundTo1Jpy(totalExcessDeficit))
            .orderId(getOrderId(collateralBalanceAdapter.getParticipantCode(), purpose.name()));
    }

    private CollateralBalanceWriteItem.CollateralBalanceWriteItemBuilder itemBuilder(final CollateralBalanceAdapter collateralBalanceAdapter) {
        return CollateralBalanceWriteItem.builder()
            .businessDate(businessDate)
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .evaluationDate(formatDate(evaluationDateProvider.get()))
            .participantCode(collateralBalanceAdapter.getParticipantCode())
            .participantName(collateralBalanceAdapter.getParticipantName())
            .participantType(formatEnum(collateralBalanceAdapter.getParticipantType()))
            .recordType(ITEM_RECORD_TYPE);
    }

    private long getOrderId(final String participantCode, final String purpose) {
        return buildOrderId(
            participantCodeOrderIdProvider.get(participantCode),
            PURPOSE_ORDER_ID_MAP.get(purpose)
        );
    }

}
