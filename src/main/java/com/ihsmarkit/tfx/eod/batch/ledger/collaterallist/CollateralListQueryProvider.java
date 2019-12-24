package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import java.util.List;

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
        final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<CollateralBalanceEntity> query = cb.createQuery(CollateralBalanceEntity.class);

        final Root<CollateralBalanceEntity> root = query.from(CollateralBalanceEntity.class);
        root.fetch(CollateralBalanceEntity_.product);
        root.fetch(CollateralBalanceEntity_.participant);

        query.orderBy(List.of(
            cb.asc(root.get(CollateralBalanceEntity_.participant)),
            cb.asc(root.get(CollateralBalanceEntity_.purpose)),
            cb.asc(root.get(CollateralBalanceEntity_.id))
        ));

        return getEntityManager().createQuery(query);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

}
