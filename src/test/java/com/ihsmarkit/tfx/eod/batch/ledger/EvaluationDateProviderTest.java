package com.ihsmarkit.tfx.eod.batch.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;

@ExtendWith(MockitoExtension.class)
class EvaluationDateProviderTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2020, 1, 5);
    private static final LocalDate PREV_BANK_BUSINESS_DATE = LocalDate.of(2020, 1, 3);

    @Mock
    private CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    private EvaluationDateProvider evaluationDateProvider;

    @BeforeEach
    void setUp() {
        evaluationDateProvider = new EvaluationDateProvider(BUSINESS_DATE, calendarTradingSwapPointRepository);
    }

    @Test
    void shouldProvideEvaluationDate() {
        when(calendarTradingSwapPointRepository.findPrevBankBusinessDateOrToday(BUSINESS_DATE)).thenReturn(Optional.of(PREV_BANK_BUSINESS_DATE));

        assertThat(evaluationDateProvider.get()).isEqualTo(PREV_BANK_BUSINESS_DATE);
    }

}