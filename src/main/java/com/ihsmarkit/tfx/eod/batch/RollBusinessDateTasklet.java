package com.ihsmarkit.tfx.eod.batch;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.FutureValueEntity;
import com.ihsmarkit.tfx.core.dl.entity.FutureValueEntity_;
import com.ihsmarkit.tfx.core.dl.entity.FxSpotTickSizeEntity;
import com.ihsmarkit.tfx.core.dl.entity.MarginRatioEntity;
import com.ihsmarkit.tfx.core.dl.entity.MarginRatioMultiplierEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.ParticipantCollateralRequirementEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.haircut.BondHaircutRateEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.haircut.EquityHaircutRateEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.haircut.LogHaircutRateEntity;
import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.core.domain.type.SystemParameters;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@JobScope
@Slf4j
@SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
public class RollBusinessDateTasklet implements Tasklet {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final EntityManager entityManager;

    private final SystemParameterRepository systemParameterRepository;

    private final CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        final LocalDate nextBusinessDate = calendarTradingSwapPointRepository.findNextTradingDate(businessDate)
            .orElseThrow(() -> new IllegalStateException("Missing Trading/Swap calendar for given business date: " + businessDate));

        rollFutureValues(nextBusinessDate);

        systemParameterRepository.setParameter(SystemParameters.BUSINESS_DATE, nextBusinessDate);

        return RepeatStatus.FINISHED;
    }

    private void rollFutureValues(final LocalDate nextBusinessDate) {
        rollFutureValue(nextBusinessDate, MarginRatioEntity.class, entity -> entity.getCurrencyPair().getId());
        rollFutureValue(nextBusinessDate, MarginRatioMultiplierEntity.class, entity -> entity.getCurrencyPair().getId(),
            entity -> entity.getParticipant().getId());
        rollFutureValue(nextBusinessDate, ParticipantCollateralRequirementEntity.class, ParticipantCollateralRequirementEntity::getPurpose,
            entity -> entity.getParticipant().getId());
        rollFutureValue(nextBusinessDate, FxSpotTickSizeEntity.class, entity -> entity.getFxSpotProduct().getId());
        rollFutureValue(nextBusinessDate, LogHaircutRateEntity.class, entity -> entity.getIssuer().getId());
        rollFutureValue(nextBusinessDate, BondHaircutRateEntity.class, BondHaircutRateEntity::getBondSubType, BondHaircutRateEntity::getRemainingDuration);
        rollFutureValue(nextBusinessDate, EquityHaircutRateEntity.class, EquityHaircutRateEntity::getType);
    }

    @SafeVarargs
    private <T extends FutureValueEntity> void rollFutureValue(
        final LocalDate nextBusinessDate,
        final Class<T> clazz,
        final Function<T, ?>... keyExtractors
    ) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<T> query = cb.createQuery(clazz);
        final Root<T> root = query.from(clazz);
        query.where(cb.between(root.get(FutureValueEntity_.businessDate), businessDate, nextBusinessDate));

        entityManager.createQuery(query)
            .getResultList()
            .stream()
            .collect(Collectors.toMap(
                entity -> toFutureValueKey(entity, keyExtractors),
                Function.identity(),
                BinaryOperator.maxBy(Comparator.comparing(FutureValueEntity::getBusinessDate))
            )).values().stream()
            .filter(entity -> !entity.getBusinessDate().isEqual(nextBusinessDate))
            .peek(entity -> {
                entityManager.detach(entity);
                entity.setId(null);
                entity.setBusinessDate(nextBusinessDate);
            })
            .forEach(entityManager::persist);
    }

    @SafeVarargs
    private <T extends FutureValueEntity> List<?> toFutureValueKey(final T entity, final Function<T, ?>... keyExtractors) {
        return Stream.of(keyExtractors)
            .map(keyExtractor -> keyExtractor.apply(entity))
            .collect(Collectors.toList());
    }

}
