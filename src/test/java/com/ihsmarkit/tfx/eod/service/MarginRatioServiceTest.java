package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aCurrencyPairEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.MarginRatioEntity;
import com.ihsmarkit.tfx.core.dl.entity.MarginRatioMultiplierEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.repository.MarginRatioMultiplierRepository;
import com.ihsmarkit.tfx.core.dl.repository.MarginRatioRepository;

@ExtendWith(MockitoExtension.class)
class MarginRatioServiceTest {

    @Mock
    private MarginRatioRepository marginRatioRepository;
    @Mock
    private MarginRatioMultiplierRepository marginRatioMultiplierRepository;
    @InjectMocks
    private MarginRatioService marginRatioService;

    @ParameterizedTest
    @CsvSource({
        "1.23, 1.23, 1.52",
        "1.56, 2.78, 4.34"
    })
    void shouldReturnRequiredMarginRatio(final BigDecimal marginRatio, final BigDecimal marginMultiplier, final BigDecimal requiredMarginRatio) {
        final CurrencyPairEntity currencyPair = aCurrencyPairEntityBuilder().build();
        final ParticipantEntity participant = aParticipantEntityBuilder().build();

        when(marginRatioMultiplierRepository.findAllByBusinessDateAndParticipantCode(any(), anyString()))
            .thenReturn(List.of(MarginRatioMultiplierEntity.builder()
                .currencyPair(currencyPair)
                .participant(participant)
                .value(marginMultiplier)
                .build()));
        when(marginRatioRepository.findByBusinessDate(any())).thenReturn(List.of(MarginRatioEntity.builder()
            .currencyPair(currencyPair)
            .value(marginRatio)
            .build()));

        assertThat(marginRatioService.getRequiredMarginRatio(currencyPair, participant))
            .isEqualTo(requiredMarginRatio);
    }

    @Test
    void shouldThrowException_whenMultiplierIsNotFound() {
        assertThatThrownBy(() ->
            marginRatioService.getRequiredMarginRatio(aCurrencyPairEntityBuilder().build(), aParticipantEntityBuilder().build()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldThrowException_whenMultiplierRepositoryThrowsException() {
        when(marginRatioMultiplierRepository.findAllByBusinessDateAndParticipantCode(any(), anyString()))
            .thenThrow(new IllegalArgumentException());

        assertThatThrownBy(() ->
            marginRatioService.getRequiredMarginRatio(aCurrencyPairEntityBuilder().build(), aParticipantEntityBuilder().build()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}