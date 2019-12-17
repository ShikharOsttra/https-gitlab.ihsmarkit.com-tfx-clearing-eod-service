package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.DAY;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.eod.service.EODCalculator;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@JobScope
public class MarginCollateralExcessDeficiencyTasklet implements Tasklet {

    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;

    private final EodCashSettlementRepository eodCashSettlementRepository;

    private final EODCalculator eodCalculator;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {

        final Stream<EodProductCashSettlementEntity> margin =
            eodProductCashSettlementRepository.findBySettlementDateIsGreaterThanEqual(businessDate);

        var aggregated = eodCalculator.aggregateRequiredMargin(margin, businessDate);

        final Stream<EodCashSettlementEntity> cashSettlement = aggregated.entrySet().stream()
            .flatMap(
                byParticipant -> byParticipant.getValue().entrySet().stream()
                    .flatMap(byType -> byType.getValue().entrySet().stream()
                        .map(
                            byDateType -> createCashSettlement(byParticipant.getKey(), byType.getKey(), byDateType.getKey(), byDateType.getValue())
                        )
                    )
            );

        eodCashSettlementRepository.saveAll(cashSettlement::iterator);

        final Map<ParticipantEntity, BigDecimal> dayCashSettlement = aggregated.entrySet().stream()
            .flatMap(
                byParticipant -> Optional.ofNullable(byParticipant.getValue().get(TOTAL_VM))
                    .flatMap(byType -> Optional.ofNullable(byType.get(DAY)))
                    .map(amount -> Pair.of(byParticipant.getKey(), amount))
                    .stream()
            ).collect(
                toMap(Pair::getLeft, Pair::getRight)
            );

        return RepeatStatus.FINISHED;
    }

    private EodCashSettlementEntity createCashSettlement(ParticipantEntity participant, EodProductCashSettlementType type, EodCashSettlementDateType dateType, BigDecimal amount) {
        return EodCashSettlementEntity.builder()
            .date(businessDate)
            .dateType(dateType)
            .participant(participant)
            .type(type)
            .amount(AmountEntity.of(amount, JPY))
            .build();
    }

}
