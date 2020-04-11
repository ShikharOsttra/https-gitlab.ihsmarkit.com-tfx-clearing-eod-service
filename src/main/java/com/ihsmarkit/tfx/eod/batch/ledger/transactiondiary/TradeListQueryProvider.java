package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import java.time.LocalDate;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;
import org.springframework.batch.item.database.orm.HibernateQueryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.LegalEntity_;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity_;
import com.ihsmarkit.tfx.core.domain.type.ClearingStatus;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@StepScope
public class TradeListQueryProvider extends AbstractJpaQueryProvider {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public Query createQuery() {
        final CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<TradeEntity> query = criteriaBuilder.createQuery(TradeEntity.class);

        final Root<TradeEntity> root = query.from(TradeEntity.class);
        root.fetch(TradeEntity_.currencyPair);
        root.fetch(TradeEntity_.originator).fetch(LegalEntity_.participant);
        root.fetch(TradeEntity_.counterparty).fetch(LegalEntity_.participant);

        return getEntityManager().createQuery(
            query.where(
                criteriaBuilder.and(
                    criteriaBuilder.equal(root.get(TradeEntity_.tradeDate), businessDate),
                    criteriaBuilder.equal(root.get(TradeEntity_.clearingStatus), ClearingStatus.NOVATED))));
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

}
