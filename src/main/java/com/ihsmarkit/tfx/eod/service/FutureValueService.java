package com.ihsmarkit.tfx.eod.service;

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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
@SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
public class FutureValueService {

    private final EntityManager entityManager;

    public void rollFutureValues(final LocalDate currentBusinessDate, final LocalDate nextBusinessDate) {
        rollFutureValue(currentBusinessDate, nextBusinessDate, MarginRatioEntity.class, entity -> entity.getCurrencyPair().getId());
        rollFutureValue(currentBusinessDate, nextBusinessDate, MarginRatioMultiplierEntity.class, entity -> entity.getCurrencyPair().getId(),
            entity -> entity.getParticipant().getId());
        rollFutureValue(currentBusinessDate, nextBusinessDate, ParticipantCollateralRequirementEntity.class, ParticipantCollateralRequirementEntity::getPurpose,
            entity -> entity.getParticipant().getId());
        rollFutureValue(currentBusinessDate, nextBusinessDate, FxSpotTickSizeEntity.class, entity -> entity.getFxSpotProduct().getId());
        rollFutureValue(currentBusinessDate, nextBusinessDate, LogHaircutRateEntity.class, entity -> entity.getIssuer().getId());
        rollFutureValue(currentBusinessDate, nextBusinessDate, BondHaircutRateEntity.class, BondHaircutRateEntity::getBondSubType,
            BondHaircutRateEntity::getRemainingDuration);
        rollFutureValue(currentBusinessDate, nextBusinessDate, EquityHaircutRateEntity.class, EquityHaircutRateEntity::getType);
    }

    public void unrollFutureValues(final LocalDate businessDate) {
        unrollFutureValue(businessDate, MarginRatioEntity.class);
        unrollFutureValue(businessDate, MarginRatioMultiplierEntity.class);
        unrollFutureValue(businessDate, ParticipantCollateralRequirementEntity.class);
        unrollFutureValue(businessDate, FxSpotTickSizeEntity.class);
        unrollFutureValue(businessDate, LogHaircutRateEntity.class);
        unrollFutureValue(businessDate, BondHaircutRateEntity.class);
        unrollFutureValue(businessDate, EquityHaircutRateEntity.class);
    }

    @SafeVarargs
    private <T extends FutureValueEntity> void rollFutureValue(
        final LocalDate currentBusinessDate,
        final LocalDate nextBusinessDate,
        final Class<T> clazz,
        final Function<T, ?>... keyExtractors
    ) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<T> query = cb.createQuery(clazz);
        final Root<T> root = query.from(clazz);
        query.where(cb.between(root.get(FutureValueEntity_.businessDate), currentBusinessDate, nextBusinessDate));

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

    private <T extends FutureValueEntity> void unrollFutureValue(final LocalDate businessDate, final Class<T> clazz) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<T> query = cb.createQuery(clazz);
        final Root<T> root = query.from(clazz);
        query.where(cb.greaterThanOrEqualTo(root.get(FutureValueEntity_.businessDate), businessDate));

        entityManager.createQuery(query)
            .getResultList()
            .forEach(entityManager::remove);
    }

    @SafeVarargs
    private <T extends FutureValueEntity> List<?> toFutureValueKey(final T entity, final Function<T, ?>... keyExtractors) {
        return Stream.of(keyExtractors)
            .map(keyExtractor -> keyExtractor.apply(entity))
            .collect(Collectors.toList());
    }
}
