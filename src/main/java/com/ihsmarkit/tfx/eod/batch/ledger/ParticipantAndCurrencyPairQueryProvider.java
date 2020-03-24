package com.ihsmarkit.tfx.eod.batch.ledger;

import static com.ihsmarkit.tfx.core.domain.type.ParticipantStatus.ACTIVE;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantStatus.INACTIVE;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantStatus.SUSPENDED;

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
import com.ihsmarkit.tfx.core.domain.Participant;
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

        query.where(
            participantRoot.get(ParticipantEntity_.status).in(ACTIVE, INACTIVE, SUSPENDED),
            criteriaBuilder.notEqual(participantRoot.get(ParticipantEntity_.code), Participant.CLEARING_HOUSE_CODE)
        )
            .orderBy(criteriaBuilder.asc(participantRoot.get(ParticipantEntity_.id)), criteriaBuilder.asc(currencyPairRoot.get(CurrencyPairEntity_.id)))
            .multiselect(participantRoot, currencyPairRoot);

        return getEntityManager().createQuery(query);
    }

    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public void afterPropertiesSet() throws Exception {
    }
}
