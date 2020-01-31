package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CURRENT_TSP_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD1_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD2_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.ROLL_BUSINESS_DATE_JOB_NAME;

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
import com.ihsmarkit.tfx.eod.statemachine.StateMachineActionsConfig;

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

    private final StateMachineActionsConfig stateMachineActionsConfig;

    public LocalDate getCurrentBusinessDate() {
        return systemParameterRepository.getParameterValueFailFast(SystemParameters.BUSINESS_DATE);
    }

    @Transactional
    public LocalDate undoPreviousDayEOD(final boolean keepMarketData) {
        LocalDate currentBusinessDate = getCurrentBusinessDate();
        log.info("[control-service] undo previous day EOD called for business date: {}", currentBusinessDate);
        cleanupService.undoEODByDate(currentBusinessDate, keepMarketData);

        final LocalDate previousBusinessDate = getPreviousBusinessDate(currentBusinessDate);

        if (!previousBusinessDate.isEqual(currentBusinessDate)) {
            cleanupService.undoEODByDate(previousBusinessDate, keepMarketData);
            log.info("[cleanup] unrolling futures values for business date: {}", currentBusinessDate);
            futureValueService.unrollFutureValues(currentBusinessDate);
            log.info("[cleanup] setting current business date to : {}", previousBusinessDate);
            systemParameterRepository.setParameter(SystemParameters.BUSINESS_DATE, previousBusinessDate);
            currentBusinessDate = previousBusinessDate;
        }
        log.info("[control-service] undo previous day EOD completed for business date: {}", currentBusinessDate);
        return currentBusinessDate;
    }

    @Transactional
    public LocalDate undoCurrentDayEOD(final boolean keepMarketData) {
        final LocalDate currentBusinessDate = getCurrentBusinessDate();
        log.info("[control-service] undo current day EOD called for business date: {}", currentBusinessDate);
        cleanupService.undoEODByDate(currentBusinessDate, keepMarketData);
        log.info("[control-service] undo current day EOD completed for business date: {}", currentBusinessDate);
        return currentBusinessDate;
    }

    public BatchStatus runJob(String jobName) {
        final LocalDate currentBusinessDay = getCurrentBusinessDate();
        log.info("[control-service] triggering eod job: {} for business date: {}", jobName, currentBusinessDay);
        return triggerEOD(jobName, currentBusinessDay);
    }

    public BatchStatus runEOD1Job() {
        final BatchStatus status = runJob(EOD1_BATCH_JOB_NAME);
        if (BatchStatus.COMPLETED == status) {
            stateMachineActionsConfig.eod1CompleteAction();
        }
        return status;
    }

    public BatchStatus runEOD2Job() {
        final BatchStatus status = runJob(EOD2_BATCH_JOB_NAME);
        if (BatchStatus.COMPLETED == status) {
            stateMachineActionsConfig.eod2CompleteAction();
        }
        return status;
    }

    public BatchStatus rollBusinessDateJob() {
        return runJob(ROLL_BUSINESS_DATE_JOB_NAME);
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
