package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance;

import java.util.List;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity_;
import com.ihsmarkit.tfx.core.domain.Participant;

public class ParticipantQueryProvider extends AbstractJpaQueryProvider {

    @Override
    public Query createQuery() {
        final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<ParticipantEntity> query = cb.createQuery(ParticipantEntity.class);

        final Root<ParticipantEntity> root = query.from(ParticipantEntity.class);

        query.where(cb.notEqual(root.get(ParticipantEntity_.code), Participant.CLEARING_HOUSE_CODE));

        query.orderBy(List.of(
            cb.asc(root.get(ParticipantEntity_.id))
        ));

        return getEntityManager().createQuery(query);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

}
