package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.PARTICIPANT_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantCodeOrderIdProvider;

@ExtendWith(MockitoExtension.class)
class TransactionDiaryOrderIdProviderTest {

    @Mock
    private ParticipantCodeOrderIdProvider participantCodeOrderIdProvider;

    @InjectMocks
    private TransactionDiaryOrderIdProvider transactionDiaryOrderIdProvider;

    @ParameterizedTest
    @CsvSource({
        "0, 11020000000000",
        "9, 11029999999999"
    })
    void shouldReturnOrderId(final char suffix, final long orderId) {
        when(participantCodeOrderIdProvider.get(PARTICIPANT_CODE)).thenReturn(1);

        assertThat(transactionDiaryOrderIdProvider.getOrderId(PARTICIPANT_CODE, "102", suffix)).isEqualTo(orderId);
    }

    @ParameterizedTest
    @MethodSource("orderIdForClearingTspDataProvider")
    void shouldReturnOrderIdForClearingTsp(@Nullable final LocalDateTime clearingTsp, final long orderId) {
        when(participantCodeOrderIdProvider.get(PARTICIPANT_CODE)).thenReturn(2);

        assertThat(transactionDiaryOrderIdProvider.getOrderId(PARTICIPANT_CODE, "102", clearingTsp)).isEqualTo(orderId);
    }

    private static Stream<Arguments> orderIdForClearingTspDataProvider() {
        return Stream.of(
            Arguments.of(null, 21020000000001L),
            Arguments.of(LocalDateTime.of(1980, 3, 10, 16, 14, 11), 21020321552851L),
            Arguments.of(LocalDateTime.of(2020, 3, 10, 16, 14, 11), 21021583856851L),
            Arguments.of(LocalDateTime.of(2320, 3, 10, 16, 14, 11), 21021050877651L)
        );
    }

}