package com.ihsmarkit.tfx.eod.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.core.domain.type.SystemParameters;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class CalendarDatesProvider {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final SystemParameterRepository systemParameterRepository;
    private final CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    public Optional<LocalDate> getNextTradingDate(final LocalDate currentDate) {
        return calendarTradingSwapPointRepository.findNextTradingDate(currentDate);
    }

    public LocalDate getNextBusinessDate() {
        return getNextTradingDate(systemParameterRepository.getParameterValueFailFast(SystemParameters.BUSINESS_DATE))
            .orElseThrow(() -> new IllegalStateException("Missing Trading/Swap calendar for given business date: " + businessDate));
    }

    public Optional<LocalDate> getNextBankBusinessDate(final LocalDate currentDate) {
        return calendarTradingSwapPointRepository.findNextBankBusinessDate(currentDate);
    }
}
