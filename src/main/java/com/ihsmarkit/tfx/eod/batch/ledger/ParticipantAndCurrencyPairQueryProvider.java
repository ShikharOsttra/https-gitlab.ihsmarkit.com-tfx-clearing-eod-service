package com.ihsmarkit.tfx.eod.batch.ledger;

import java.util.function.BiFunction;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;

import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity_;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class ParticipantAndCurrencyPairQueryProvider extends AbstractJpaQueryProvider {

    private final BiFunction<Root<ParticipantPositionEntity>, CriteriaBuilder, Expression<Boolean>> restrictionSupplier;

    @Override
    public Query createQuery() {
        final CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<ParticipantAndCurrencyPair> query = criteriaBuilder.createQuery(ParticipantAndCurrencyPair.class);

        final Root<ParticipantPositionEntity> root = query.from(ParticipantPositionEntity.class);

        query.select(criteriaBuilder.construct(
            ParticipantAndCurrencyPair.class,
            root.get(ParticipantPositionEntity_.participant),
            root.get(ParticipantPositionEntity_.currencyPair))
        ).distinct(true);

        query.where(restrictionSupplier.apply(root, criteriaBuilder));

        return getEntityManager().createQuery(query);
    }

    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public void afterPropertiesSet() throws Exception {
    }
}
