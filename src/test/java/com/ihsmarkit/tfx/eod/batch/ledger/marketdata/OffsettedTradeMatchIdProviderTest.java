package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;

import lombok.Value;

@ExtendWith(MockitoExtension.class)
class OffsettedTradeMatchIdProviderTest {

    private static final String MATCHING_REF = "matchingRef";
    private static final String PARTICIPANT_CODE = "participantCode";
    private static final String CLEARED_REF = "clearedRef";

    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private OffsettedTradeMatchIdProvider offsettedTradeMatchIdProvider;

    @BeforeEach
    void init() {
        when(tradeRepository.findAllOffsettingMatchIdsByTradeDate(any())).thenReturn(
            Stream.of(
                BustedTradeProjectionImpl.of(MATCHING_REF, PARTICIPANT_CODE, CLEARED_REF)
            )
        );
    }

    @Test
    void shouldFindCancelledTradeData() {
        assertThat(offsettedTradeMatchIdProvider.hasOffsettingMatchId(MATCHING_REF)).isTrue();
        assertThat(offsettedTradeMatchIdProvider.getCancelledTradeClearingId(MATCHING_REF, PARTICIPANT_CODE))
            .isEqualTo(CLEARED_REF);
        verify(tradeRepository, times(1)).findAllOffsettingMatchIdsByTradeDate(any());
    }

    @Value(staticConstructor = "of")
    private static class BustedTradeProjectionImpl implements TradeRepository.BustedTradeProjection {

        private final String matchingRef;
        private final String participantCode;
        private final String clearingRef;
    }
}