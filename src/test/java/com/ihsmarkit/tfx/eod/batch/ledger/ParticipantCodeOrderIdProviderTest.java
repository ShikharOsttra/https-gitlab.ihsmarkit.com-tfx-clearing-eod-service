package com.ihsmarkit.tfx.eod.batch.ledger;

import static com.ihsmarkit.tfx.core.domain.Participant.CLEARING_HOUSE_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.repository.ParticipantRepository;

@ExtendWith(MockitoExtension.class)
class ParticipantCodeOrderIdProviderTest {

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private ParticipantCodeOrderIdProvider participantCodeOrderIdProvider;

    @Test
    void shouldReturnOrderId() {
        when(participantRepository.findAllCodes()).thenReturn(List.of("P22", CLEARING_HOUSE_CODE, "P11"));

        assertThat(participantCodeOrderIdProvider.get("P11")).isEqualTo(0);
        assertThat(participantCodeOrderIdProvider.get("P22")).isEqualTo(1);
        assertThat(participantCodeOrderIdProvider.get(CLEARING_HOUSE_CODE)).isEqualTo(2);
    }
}