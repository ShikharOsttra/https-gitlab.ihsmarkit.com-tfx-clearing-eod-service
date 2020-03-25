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
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@Service
@SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
public class FutureValueService {

    private static final List<FutureValueClassWrapper<?>> FUTURE_VALUE_CLASS_WRAPPERS = List.of(
        FutureValueClassWrapper.of(MarginRatioEntity.class, entity -> entity.getCurrencyPair().getId()),
        FutureValueClassWrapper.of(MarginRatioMultiplierEntity.class, entity -> entity.getCurrencyPair().getId(), entity -> entity.getParticipant().getId()),
        FutureValueClassWrapper.of(ParticipantCollateralRequirementEntity.class, ParticipantCollateralRequirementEntity::getPurpose,
            entity -> entity.getParticipant().getId()),
        FutureValueClassWrapper.of(FxSpotTickSizeEntity.class, entity -> entity.getFxSpotProduct().getId()),
        FutureValueClassWrapper.of(EODThresholdFutureValueEntity.class, entity -> entity.getFxSpotProduct().getId()),
        FutureValueClassWrapper.of(ForcedAllocationThresholdFutureValueEntity.class, entity -> entity.getFxSpotProduct().getId()),
        FutureValueClassWrapper.of(LogHaircutRateEntity.class, entity -> entity.getIssuer().getId()),
        FutureValueClassWrapper.of(BondHaircutRateEntity.class, BondHaircutRateEntity::getBondSubType, BondHaircutRateEntity::getRemainingDuration),
        FutureValueClassWrapper.of(EquityHaircutRateEntity.class, EquityHaircutRateEntity::getType)
    );

    private final EntityManager entityManager;

    public void rollFutureValues(final LocalDate businessDate, final LocalDate nextBusinessDate) {
        FUTURE_VALUE_CLASS_WRAPPERS.forEach(
            futureValueClassWrapper -> copyFutureValues(businessDate, nextBusinessDate, BinaryOperator::maxBy, nextBusinessDate, futureValueClassWrapper)
        );
    }

    public void unrollFutureValues(final LocalDate businessDate, final LocalDate prevBusinessDate) {
        FUTURE_VALUE_CLASS_WRAPPERS.forEach(
            futureValueClassWrapper -> {
                copyFutureValues(prevBusinessDate, businessDate, BinaryOperator::minBy, prevBusinessDate, futureValueClassWrapper);
                deleteFutureValue(businessDate, futureValueClassWrapper.getClazz());
            }
        );
    }

    private <T extends FutureValueEntity> void copyFutureValues(
        final LocalDate from,
        final LocalDate to,
        final Function<Comparator<T>, BinaryOperator<T>> mergeFunction,
        final LocalDate newDate,
        final FutureValueClassWrapper<T> futureValueClassWrapper
    ) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<T> query = cb.createQuery(futureValueClassWrapper.getClazz());
        final Root<T> root = query.from(futureValueClassWrapper.getClazz());
        query.where(cb.between(root.get(FutureValueEntity_.businessDate), from, to));

        entityManager.createQuery(query)
            .getResultList()
            .stream()
            .collect(Collectors.toMap(
                futureValueClassWrapper::extractKey,
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

    private <T extends FutureValueEntity> void deleteFutureValue(final LocalDate businessDate, final Class<T> clazz) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaDelete<T> query = cb.createCriteriaDelete(clazz);
        final Root<T> root = query.from(clazz);
        query.where(cb.greaterThanOrEqualTo(root.get(FutureValueEntity_.businessDate), businessDate));
        entityManager.createQuery(query).executeUpdate();
    }

    @RequiredArgsConstructor
    @Getter
    private static class FutureValueClassWrapper<T extends FutureValueEntity> {

        private final Class<T> clazz;
        private final Function<T, ?>[] keyExtractors;

        List<?> extractKey(final T futureValue) {
            return Stream.of(keyExtractors)
                .map(keyExtractor -> keyExtractor.apply(futureValue))
                .collect(Collectors.toList());
        }

        @SafeVarargs
        static <T extends FutureValueEntity> FutureValueClassWrapper<T> of(final Class<T> clazz, final Function<T, ?>... keyExtractors) {
            return new FutureValueClassWrapper<>(clazz, keyExtractors);
        }
    }
}
