package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import java.time.LocalDate;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;

import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity_;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ParticipantPositionQueryProvider extends AbstractJpaQueryProvider {

    private final LocalDate businessDate;
    private final ParticipantPositionType type;

    @Override
    public Query createQuery() {
        final CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<ParticipantPositionEntity> query = criteriaBuilder.createQuery(ParticipantPositionEntity.class);

        final Root<ParticipantPositionEntity> root = query.from(ParticipantPositionEntity.class);
        root.fetch(ParticipantPositionEntity_.currencyPair);
        root.fetch(ParticipantPositionEntity_.participant);

        return getEntityManager().createQuery(
            query.where(
                criteriaBuilder.and(
                    criteriaBuilder.equal(root.get(ParticipantPositionEntity_.tradeDate), businessDate),
                    criteriaBuilder.equal(root.get(ParticipantPositionEntity_.type), type))));
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
