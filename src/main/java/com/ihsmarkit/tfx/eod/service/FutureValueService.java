package com.ihsmarkit.tfx.eod.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.EODThresholdFutureValueEntity;
import com.ihsmarkit.tfx.core.dl.entity.ForcedAllocationThresholdFutureValueEntity;
import com.ihsmarkit.tfx.core.dl.entity.FutureValueEntity;
import com.ihsmarkit.tfx.core.dl.entity.FutureValueEntity_;
import com.ihsmarkit.tfx.core.dl.entity.FxSpotTickSizeEntity;
import com.ihsmarkit.tfx.core.dl.entity.MarginRatioEntity;
import com.ihsmarkit.tfx.core.dl.entity.MarginRatioMultiplierEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.ParticipantCollateralRequirementEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.haircut.BondHaircutRateEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.haircut.EquityHaircutRateEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.haircut.LogHaircutRateEntity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
@SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
public class FutureValueService {

    private static final List<Class<? extends FutureValueEntity<?>>> FUTURE_VALUE_CLASSES = List.of(
        MarginRatioEntity.class,
        MarginRatioMultiplierEntity.class,
        ParticipantCollateralRequirementEntity.class,
        FxSpotTickSizeEntity.class,
        EODThresholdFutureValueEntity.class,
        ForcedAllocationThresholdFutureValueEntity.class,
        LogHaircutRateEntity.class,
        BondHaircutRateEntity.class,
        EquityHaircutRateEntity.class
    );

    private final EntityManager entityManager;

    public void rollFutureValues(final LocalDate businessDate, final LocalDate nextBusinessDate) {
        FUTURE_VALUE_CLASSES.forEach(
            clazz -> copyFutureValues(businessDate, nextBusinessDate, BinaryOperator::maxBy, nextBusinessDate, clazz)
        );
    }

    public void unrollFutureValues(final LocalDate businessDate, final LocalDate prevBusinessDate) {
        FUTURE_VALUE_CLASSES.forEach(
            clazz -> {
                copyFutureValues(prevBusinessDate, businessDate, BinaryOperator::minBy, prevBusinessDate, clazz);
                deleteFutureValue(businessDate, clazz);
            }
        );
    }

    private <T extends FutureValueEntity<?>> void copyFutureValues(
        final LocalDate from,
        final LocalDate to,
        final Function<Comparator<T>, BinaryOperator<T>> mergeFunction,
        final LocalDate newDate,
        final Class<T> clazz
    ) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<T> query = cb.createQuery(clazz);
        final Root<T> root = query.from(clazz);
        query.where(cb.between(root.get(FutureValueEntity_.businessDate), from, to));

        entityManager.createQuery(query)
            .getResultList()
            .stream()
            .collect(Collectors.toMap(
                FutureValueEntity::getKey,
                Function.identity(),
                mergeFunction.apply(Comparator.comparing(FutureValueEntity::getBusinessDate))
            )).values().stream()
            .filter(entity -> !entity.getBusinessDate().isEqual(newDate))
            .peek(entity -> {
                entityManager.detach(entity);
                entity.setId(null);
                entity.setBusinessDate(newDate);
            })
            .forEach(entityManager::persist);
    }

    private <T extends FutureValueEntity<?>> void deleteFutureValue(final LocalDate businessDate, final Class<T> clazz) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaDelete<T> query = cb.createCriteriaDelete(clazz);
        final Root<T> root = query.from(clazz);
        query.where(cb.greaterThanOrEqualTo(root.get(FutureValueEntity_.businessDate), businessDate));
        entityManager.createQuery(query).executeUpdate();
    }

}
