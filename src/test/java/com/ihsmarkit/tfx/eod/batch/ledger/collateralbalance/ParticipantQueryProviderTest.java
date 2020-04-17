package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.AbstractJpaQueryProviderTest;

@ContextConfiguration(classes = ParticipantQueryProvider.class)
class ParticipantQueryProviderTest extends AbstractJpaQueryProviderTest<ParticipantEntity> {

    @Test
    @Commit
    @DatabaseSetup("/common/all_participant_types.xml")
    void shouldCreateQuery() {
        assertThat(getResultList())
            .extracting(ParticipantEntity::getId)
            .containsExactly(1L, 2L, 3L);
    }

}