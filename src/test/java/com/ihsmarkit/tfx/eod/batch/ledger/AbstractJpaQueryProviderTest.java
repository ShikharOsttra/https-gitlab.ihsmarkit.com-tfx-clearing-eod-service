package com.ihsmarkit.tfx.eod.batch.ledger;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;
import org.springframework.beans.factory.annotation.Autowired;

import com.ihsmarkit.tfx.core.dl.repository.RepositoryTest;

public class AbstractJpaQueryProviderTest<T> extends RepositoryTest {


    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AbstractJpaQueryProvider jpaQueryProvider;

    @BeforeEach
    void setUp() {
        jpaQueryProvider.setEntityManager(entityManager);
    }

    protected List<T> getResultList() {
        return (List<T>) jpaQueryProvider.createQuery().getResultList();
    }

}
