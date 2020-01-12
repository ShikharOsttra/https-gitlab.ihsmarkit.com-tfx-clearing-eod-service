package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CURRENT_TSP_JOB_PARAM_NAME;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.core.domain.type.SystemParameters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EODControlService {

    private final SystemParameterRepository systemParameterRepository;

    private final CalendarTradingSwapPointRepository calendarRepository;

    private final JobLauncher jobLauncher;
    private final JobLocator jobLocator;

    private final EODCleanupService cleanupService;

    private final FutureValueService futureValueService;

    public LocalDate getCurrentBusinessDate() {
        return systemParameterRepository.getParameterValueFailFast(SystemParameters.BUSINESS_DATE);
    }

    @Transactional
    public LocalDate undoPreviousDayEOD() {
        final LocalDate currentBusinessDate = getCurrentBusinessDate();
        cleanupService.undoEODByDate(currentBusinessDate);

        final LocalDate previousBusinessDate = getPreviousBusinessDate(currentBusinessDate);

        if (!previousBusinessDate.isEqual(currentBusinessDate)) {
            cleanupService.undoEODByDate(previousBusinessDate);
            futureValueService.unrollFutureValues(currentBusinessDate);
            systemParameterRepository.setParameter(SystemParameters.BUSINESS_DATE, previousBusinessDate);
            return previousBusinessDate;
        }
        return currentBusinessDate;
    }

    @Transactional
    public LocalDate undoCurrentDayEOD() {
        final LocalDate currentBusinessDate = getCurrentBusinessDate();
        cleanupService.undoEODByDate(currentBusinessDate);
        return currentBusinessDate;
    }

    public String runEODJob(final String jobName) {
        final LocalDate currentBusinessDay = getCurrentBusinessDate();
        return triggerEOD(jobName, currentBusinessDay).name();
    }

    private BatchStatus triggerEOD(final String jobName, final LocalDate businessDate) {
        try {
            final Job job = jobLocator.getJob(jobName);
            final JobParameters params = new JobParametersBuilder()
                .addString(BUSINESS_DATE_JOB_PARAM_NAME, businessDate.format(BUSINESS_DATE_FMT))
                .addString(CURRENT_TSP_JOB_PARAM_NAME, LocalDateTime.now().toString())
                .toJobParameters();
            return jobLauncher.run(job, params).getStatus();
        } catch (final NoSuchJobException | JobExecutionAlreadyRunningException | JobRestartException
            | JobInstanceAlreadyCompleteException | JobParametersInvalidException ex) {
            log.error("exception while triggering jobName: {} on businessDate: {} with message: {}", jobName, businessDate, ex.getMessage(), ex);
            return BatchStatus.FAILED;
        }
    }

    private LocalDate getPreviousBusinessDate(final LocalDate currentBusinessDate) {
        return calendarRepository.findPreviousTradingDate(currentBusinessDate.minusDays(1)).orElse(currentBusinessDate);
    }
}
