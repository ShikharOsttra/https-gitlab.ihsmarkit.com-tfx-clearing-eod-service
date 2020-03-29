package com.ihsmarkit.tfx.eod.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalendarDatesProvider {

    private final CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    public Optional<LocalDate> getNextTradingDate(final LocalDate currentDate) {
        return calendarTradingSwapPointRepository.findNextTradingDate(currentDate);
    }
}
