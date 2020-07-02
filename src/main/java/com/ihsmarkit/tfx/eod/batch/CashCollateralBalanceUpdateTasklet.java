package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.CASH;
import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.MARGIN;
import static java.math.BigDecimal.ZERO;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.partitioningBy;
import static lombok.AccessLevel.PRIVATE;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.common.math.BigDecimals;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodCashBalanceAdjustmentEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.core.dl.repository.collateral.CollateralBalanceRepository;
import com.ihsmarkit.tfx.core.dl.repository.collateral.CollateralProductRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodCashBalanceAdjustmentRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodCashSettlementRepository;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.mapper.CashSettlementMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@JobScope
@Slf4j
public class CashCollateralBalanceUpdateTasklet implements Tasklet {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    private final EodCashSettlementRepository eodCashSettlementRepository;

    private final CollateralBalanceRepository collateralBalanceRepository;

    private final EodCashBalanceAdjustmentRepository eodCashBalanceAdjustmentRepository;

    private final ClockService clockService;

    private final CashSettlementMapper cashSettlementMapper;

    @Getter(PRIVATE)
    private final CollateralProductRepository collateralProductRepository;

    private final Lazy<CollateralProductEntity> cashProduct =
        Lazy.of(() -> getCollateralProductRepository().findAllCashProducts().get(0));

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        if (!calendarTradingSwapPointRepository.existsByTradeDateAndBankBusinessDateIsTrue(businessDate)) {
            log.info("[var-cash-settlement] business day: {} is not a bank business day => settlement is not possible", businessDate);
            return RepeatStatus.FINISHED;
        }

        final LocalDate previousTradingDate = calendarTradingSwapPointRepository.findPreviousTradingDateFailFast(businessDate);
        log.info("[var-cash-settlement] starting for trade date: {} on business date: {}", previousTradingDate, businessDate);

        final List<EodCashSettlementEntity> margins = eodCashSettlementRepository.findAllActionableCashSettlements(previousTradingDate);
        log.info("[var-cash-settlement] found: {} actionable cash settlements for trade date: {}", margins.size(), previousTradingDate);

        final var separated = getMarginCashBalancePairs(margins);

        collateralBalanceRepository.deleteAll(
            separated.get(Boolean.TRUE).stream()
                .map(Pair::getRight)
                .filter(Objects::nonNull)
                ::iterator
        );
        collateralBalanceRepository.saveAll(
            separated.get(Boolean.FALSE).stream()
                .map(this::adjustAmount)
                .filter(not(BigDecimals.isEqualToZero(CollateralBalanceEntity::getAmount)))
                ::iterator
        );

        eodCashBalanceAdjustmentRepository.saveAll(margins.stream().map(this::mapToAdjustment)::iterator);

        log.info("[var-cash-settlement] completed for trade date: {} on business date: {}", previousTradingDate, businessDate);

        return RepeatStatus.FINISHED;
    }

    private Map<Boolean, List<Pair<EodCashSettlementEntity, CollateralBalanceEntity>>> getMarginCashBalancePairs(final List<EodCashSettlementEntity> margins) {
        final Map<ParticipantEntity, CollateralBalanceEntity> balanceByParticipant =
            collateralBalanceRepository.findAllByParticipantInAndPurposeAndProductType(
                margins.stream().map(EodCashSettlementEntity::getParticipant).collect(Collectors.toSet()),
                MARGIN,
                CASH
            ).collect(Collectors.toMap(CollateralBalanceEntity::getParticipant, Function.identity()));

        return margins.stream()
            .map(margin -> Pair.of(margin, balanceByParticipant.get(margin.getParticipant())))
            .collect(partitioningBy(CashCollateralBalanceUpdateTasklet::sameAmounts));
    }

    private EodCashBalanceAdjustmentEntity mapToAdjustment(final EodCashSettlementEntity margin) {
        log.info("[var-cash-settlement] participant: {}, amount: {}, date: {}",
            margin.getParticipant().getCode(), margin.getAmount().getValue().toPlainString(), margin.getDate());
        return cashSettlementMapper.toEodCashBalanceAdjustmentEntity(margin, clockService.getCurrentDateTimeUTC());
    }

    private static boolean sameAmounts(final Pair<EodCashSettlementEntity, CollateralBalanceEntity> pair) {
        return 0 == Optional.ofNullable(pair.getRight())
            .map(CollateralBalanceEntity::getAmount)
            .orElse(ZERO)
            .compareTo(pair.getLeft().getAmount().getValue().negate());
    }

    private void adjustBalance(final EodCashSettlementEntity settlement, final CollateralBalanceEntity balance) {
        balance.setAmount(balance.getAmount().add(settlement.getAmount().getValue()));
    }

    private CollateralBalanceEntity newBalance(final EodCashSettlementEntity settlement) {
        return CollateralBalanceEntity.builder()
            .product(cashProduct.get())
            .amount(settlement.getAmount().getValue())
            .participant(settlement.getParticipant())
            .purpose(MARGIN)
            .build();
    }

    private CollateralBalanceEntity adjustAmount(final Pair<EodCashSettlementEntity, CollateralBalanceEntity> pair) {
        final CollateralBalanceEntity balance = pair.getRight();
        final EodCashSettlementEntity settlement = pair.getLeft();
        if (balance == null) {
            return newBalance(settlement);
        } else {
            adjustBalance(settlement, balance);
            return balance;
        }
    }
}
