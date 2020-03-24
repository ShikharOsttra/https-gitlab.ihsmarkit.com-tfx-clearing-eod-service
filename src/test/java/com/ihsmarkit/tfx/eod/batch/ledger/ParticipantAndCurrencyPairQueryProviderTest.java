package com.ihsmarkit.tfx.eod.batch.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.ihsmarkit.tfx.core.dl.repository.RepositoryTest;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;

@ContextConfiguration(classes = ParticipantAndCurrencyPairQueryProvider.class)
class ParticipantAndCurrencyPairQueryProviderTest extends RepositoryTest {

    @Autowired
    private ParticipantAndCurrencyPairQueryProvider participantAndCurrencyPairQueryProvider;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        participantAndCurrencyPairQueryProvider.setEntityManager(entityManager);
    }

    @Test
    @Commit
    @DatabaseSetup("/ParticipantAndCurrencyPairQueryProviderTest/setup.xml")
    void shouldCreateQuery() {
        assertThat((List<ParticipantAndCurrencyPair>) participantAndCurrencyPairQueryProvider.createQuery().getResultList())
            .extracting(pair -> pair.getParticipant().getId(), pair -> pair.getCurrencyPair().getId())
            .containsExactly(
                tuple(1L, 1L),
                tuple(1L, 2L),
                tuple(2L, 1L),
                tuple(2L, 2L),
                tuple(3L, 1L),
                tuple(3L, 2L)
            );
    }

}