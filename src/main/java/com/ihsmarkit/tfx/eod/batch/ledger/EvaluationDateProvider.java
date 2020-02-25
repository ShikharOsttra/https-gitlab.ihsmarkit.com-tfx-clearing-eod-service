package com.ihsmarkit.tfx.eod.batch.ledger;

import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;

import lombok.RequiredArgsConstructor;

@StepScope
@Component
@RequiredArgsConstructor
public class EvaluationDateProvider {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    private final Lazy<LocalDate> evaluationDate = Lazy.of(this::loadEvaluationDate);

    public LocalDate get() {
        return evaluationDate.get();
    }

    private LocalDate loadEvaluationDate() {
        return calendarTradingSwapPointRepository.findPrevBankBusinessDateOrToday(businessDate)
            .orElseThrow(() -> new IllegalStateException("Cannot find prev bank business date"));
    }

}
