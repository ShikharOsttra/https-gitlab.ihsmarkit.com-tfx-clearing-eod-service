package com.ihsmarkit.tfx.eod.batch.ledger.openpositions;

import java.time.LocalDate;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity_;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@StepScope
public class OpenPositionsQueryProvider extends AbstractJpaQueryProvider {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

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
        query.where(criteriaBuilder.equal(root.get(ParticipantPositionEntity_.tradeDate), businessDate));

        return getEntityManager().createQuery(query);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }
}
