package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;

import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity_;

public class CollateralListQueryProvider extends AbstractJpaQueryProvider {

    @Override
    public Query createQuery() {
        final CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<CollateralBalanceEntity> query = criteriaBuilder.createQuery(CollateralBalanceEntity.class);

        final Root<CollateralBalanceEntity> root = query.from(CollateralBalanceEntity.class);
        root.fetch(CollateralBalanceEntity_.product);
        root.fetch(CollateralBalanceEntity_.participant);

        return getEntityManager().createQuery(query);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

}
