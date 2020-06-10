package com.ihsmarkit.tfx.eod.batch.ledger;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity_;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity_;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component
public class ParticipantAndCurrencyPairQueryProvider extends AbstractJpaQueryProvider {

    @Override
    public Query createQuery() {
        final CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<ParticipantAndCurrencyPair> query = criteriaBuilder.createQuery(ParticipantAndCurrencyPair.class);

        final Root<ParticipantEntity> participantRoot = query.from(ParticipantEntity.class);
        final Root<CurrencyPairEntity> currencyPairRoot = query.from(CurrencyPairEntity.class);

        query.where(SpecificationFactory.participantPathSpecification().toPredicate(participantRoot, query, criteriaBuilder))
            .orderBy(criteriaBuilder.asc(participantRoot.get(ParticipantEntity_.id)), criteriaBuilder.asc(currencyPairRoot.get(CurrencyPairEntity_.id)))
            .multiselect(participantRoot, currencyPairRoot);

        return getEntityManager().createQuery(query);
    }

    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public void afterPropertiesSet() throws Exception {
    }
}
