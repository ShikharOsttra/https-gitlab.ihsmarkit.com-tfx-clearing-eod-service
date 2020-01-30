package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.CASH;
import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.MARGIN;
import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.partitioningBy;
import static lombok.AccessLevel.PRIVATE;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

@Service
@AllArgsConstructor
@JobScope
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
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {

        final LocalDate previousTradingDate = calendarTradingSwapPointRepository.findPreviousTradingDateFailFast(businessDate);

        final List<EodCashSettlementEntity> margins = eodCashSettlementRepository.findAllActionableCashSettlements(previousTradingDate);

        final Map<ParticipantEntity, CollateralBalanceEntity> balanceByParticipant =
            collateralBalanceRepository.findAllByParticipantInAndPurposeAndProductType(
                margins.stream().map(EodCashSettlementEntity::getParticipant).collect(Collectors.toSet()),
                MARGIN,
                CASH
            ).collect(Collectors.toMap(CollateralBalanceEntity::getParticipant, Function.identity()));

        final Map<Boolean, List<Pair<EodCashSettlementEntity, CollateralBalanceEntity>>> separated = margins
            .stream()
            .map(margin -> Pair.of(margin, balanceByParticipant.get(margin.getParticipant())))
            .collect(partitioningBy(CashCollateralBalanceUpdateTasklet::sameAmounts));

        collateralBalanceRepository.deleteAll(separated.get(Boolean.TRUE).stream().map(Pair::getRight)::iterator);
        collateralBalanceRepository.saveAll(separated.get(Boolean.FALSE).stream().map(this::adjustAmount)::iterator);

        eodCashBalanceAdjustmentRepository.saveAll(margins.stream().map(this::mapToAdjustment)::iterator);

        return RepeatStatus.FINISHED;
    }

    private EodCashBalanceAdjustmentEntity mapToAdjustment(final EodCashSettlementEntity margin) {
        return cashSettlementMapper.toEodCashBalanceAdjustmentEntity(margin, clockService.getCurrentDateTimeUTC());
    }

    private static boolean sameAmounts(final Pair<EodCashSettlementEntity, CollateralBalanceEntity> pair) {
        return 0 == Optional.ofNullable(pair.getRight())
            .map(CollateralBalanceEntity::getAmount).orElse(ZERO)
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
