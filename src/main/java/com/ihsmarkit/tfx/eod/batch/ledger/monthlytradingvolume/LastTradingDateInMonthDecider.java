package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume;

import java.time.LocalDate;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@JobScope
@Service
@AllArgsConstructor
@Slf4j
public class LastTradingDateInMonthDecider implements JobExecutionDecider {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{jobParameters['generateMonthlyLedger'] ?: false}")
    private final boolean generateMonthlyLedger;

    private final CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    @Override
    public FlowExecutionStatus decide(final JobExecution jobExecution, final StepExecution stepExecution) {
        final LocalDate nextTradingDate = calendarTradingSwapPointRepository.findNextTradingDate(businessDate)
            .orElseThrow(() -> new IllegalStateException("Missing Trading/Swap calendar for given business date: " + businessDate));

        final boolean isLastTradingDateInMonth = generateMonthlyLedger || businessDate.getMonth() != nextTradingDate.getMonth();
        log.info("business date: {}, next trading date: {}, generateMonthlyLedger: {}, is last trading date in month: {}",
            businessDate, nextTradingDate, generateMonthlyLedger, isLastTradingDateInMonth);

        return new FlowExecutionStatus(Boolean.toString(isLastTradingDateInMonth));
    }
}
