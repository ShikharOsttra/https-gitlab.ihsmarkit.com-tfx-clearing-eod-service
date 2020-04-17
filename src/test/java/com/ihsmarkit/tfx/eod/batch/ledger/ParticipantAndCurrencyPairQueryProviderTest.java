package com.ihsmarkit.tfx.eod.batch.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;

@ContextConfiguration(classes = ParticipantAndCurrencyPairQueryProvider.class)
class ParticipantAndCurrencyPairQueryProviderTest extends AbstractJpaQueryProviderTest<ParticipantAndCurrencyPair> {

    @Test
    @Commit
    @DatabaseSetup({
        "/common/all_participant_types.xml",
        "/common/currency.xml"
    })
    void shouldCreateQuery() {
        assertThat(getResultList())
            .extracting(pair -> pair.getParticipant().getId(), pair -> pair.getCurrencyPair().getId())
            .containsExactly(
                tuple(1L, 2L),
                tuple(1L, 13L),
                tuple(1L, 14L),
                tuple(2L, 2L),
                tuple(2L, 13L),
                tuple(2L, 14L),
                tuple(3L, 2L),
                tuple(3L, 13L),
                tuple(3L, 14L)
            );
    }

}