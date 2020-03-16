package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CASH_BALANCE_UPDATE_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CURRENT_TSP_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD1_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD2_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.GENERATE_MONTHLY_LEDGER_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.ROLL_BUSINESS_DATE_JOB_NAME;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

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

import com.ihsmarkit.tfx.core.dl.entity.eod.EodStatusCompositeId;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodStatusEntity;
import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodStatusRepository;
import com.ihsmarkit.tfx.core.domain.eod.EodStage;
import com.ihsmarkit.tfx.core.domain.type.SystemParameters;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.exception.LockException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class EODControlService {

    private final SystemParameterRepository systemParameterRepository;

    private final CalendarTradingSwapPointRepository calendarRepository;

    private final JobLauncher jobLauncher;

    private final JobLocator jobLocator;

    private final EODCleanupService cleanupService;

    private final FutureValueService futureValueService;

    private final EodStatusRepository eodStatusRepository;

    private final ClockService clockService;

    private final ReentrantLock lock = new ReentrantLock();

    public LocalDate getCurrentBusinessDate() {
        return systemParameterRepository.getParameterValueFailFast(SystemParameters.BUSINESS_DATE);
    }

    @Transactional
    public LocalDate undoPreviousDayEOD(final boolean keepMarketData) {
        return executeWithLock(() -> {
            LocalDate currentBusinessDate = getCurrentBusinessDate();
            log.info("[control-service] undo previous day EOD called for business date: {}", currentBusinessDate);
            cleanupService.undoEODByDate(currentBusinessDate, keepMarketData);

            final LocalDate previousBusinessDate = getPreviousBusinessDate(currentBusinessDate);

            if (!previousBusinessDate.isEqual(currentBusinessDate)) {
                cleanupService.undoEODByDate(previousBusinessDate, keepMarketData);
                log.info("[cleanup] unrolling futures values for business date: {}", currentBusinessDate);
                futureValueService.unrollFutureValues(currentBusinessDate, previousBusinessDate);
                log.info("[cleanup] setting current business date to : {}", previousBusinessDate);
                systemParameterRepository.setParameter(SystemParameters.BUSINESS_DATE, previousBusinessDate);
                currentBusinessDate = previousBusinessDate;
            }
            log.info("[control-service] undo previous day EOD completed for business date: {}", currentBusinessDate);
            return currentBusinessDate;
        });
    }

    @Transactional
    public LocalDate undoCurrentDayEOD(final boolean keepMarketData) {
        return executeWithLock(() -> {
            final LocalDate currentBusinessDate = getCurrentBusinessDate();
            log.info("[control-service] undo current day EOD called for business date: {}", currentBusinessDate);
            cleanupService.undoEODByDate(currentBusinessDate, keepMarketData);
            log.info("[control-service] undo current day EOD completed for business date: {}", currentBusinessDate);
            return currentBusinessDate;
        });
    }

    public BatchStatus runEOD1Job() {
        return executeWithLock(() -> {
            final BatchStatus status = runJob(EOD1_BATCH_JOB_NAME);
            if (BatchStatus.COMPLETED == status) {
                this.saveEodStatus(EodStage.EOD1_COMPLETE, getCurrentBusinessDate());
            }
            return status;
        });
    }

    public BatchStatus runEOD2Job(final Boolean generateMonthlyLedger) {
        return executeWithLock(() -> {
            final BatchStatus status = runJob(EOD2_BATCH_JOB_NAME, Map.of(GENERATE_MONTHLY_LEDGER_JOB_PARAM_NAME, generateMonthlyLedger.toString()));
            if (BatchStatus.COMPLETED == status) {
                this.saveEodStatus(EodStage.EOD2_COMPLETE, getCurrentBusinessDate());
            }
            return status;
        });
    }

    public BatchStatus rollBusinessDateJob() {
        return executeWithLock(() -> runJob(ROLL_BUSINESS_DATE_JOB_NAME));
    }

    public LocalDate runAll(final Boolean generateMonthlyLedger) {
        return executeWithLock(() -> {
            if (runEOD1Job() == BatchStatus.COMPLETED
                && runEOD2Job(generateMonthlyLedger) == BatchStatus.COMPLETED
                && rollBusinessDateJob() == BatchStatus.COMPLETED) {
                return getCurrentBusinessDate();
            }
            return LocalDate.MIN;
        });
    }

    public BatchStatus runCashBalanceUpdateJob() {
        return executeWithLock(() -> runJob(CASH_BALANCE_UPDATE_BATCH_JOB_NAME));
    }

    private BatchStatus runJob(final String jobName) {
        return runJob(jobName, Map.of());
    }

    private BatchStatus runJob(final String jobName, final Map<String, String> jobParameters) {
        final LocalDate currentBusinessDay = getCurrentBusinessDate();
        log.info("[control-service] triggering eod job: {} for business date: {}", jobName, currentBusinessDay);
        return triggerEOD(jobName, currentBusinessDay, jobParameters);
    }

    private BatchStatus triggerEOD(final String jobName, final LocalDate businessDate, final Map<String, String> jobParameters) {
        try {
            final Job job = jobLocator.getJob(jobName);
            final JobParametersBuilder jobParametersBuilder = new JobParametersBuilder()
                .addString(BUSINESS_DATE_JOB_PARAM_NAME, businessDate.format(BUSINESS_DATE_FMT))
                .addString(CURRENT_TSP_JOB_PARAM_NAME, LocalDateTime.now().toString());
            jobParameters.forEach(jobParametersBuilder::addString);
            final JobParameters params = jobParametersBuilder.toJobParameters();
            return jobLauncher.run(job, params).getStatus();
        } catch (final NoSuchJobException | JobExecutionAlreadyRunningException | JobRestartException
            | JobInstanceAlreadyCompleteException | JobParametersInvalidException ex) {
            log.error("exception while triggering jobName: {} on businessDate: {} with message: {}", jobName, businessDate, ex.getMessage(), ex);
            return BatchStatus.FAILED;
        }
    }

    private LocalDate getPreviousBusinessDate(final LocalDate currentBusinessDate) {
        return calendarRepository.findPreviousTradingDate(currentBusinessDate).orElse(currentBusinessDate);
    }

    private void saveEodStatus(final EodStage stage, final LocalDate businessDate) {
        eodStatusRepository.save(
            EodStatusEntity.builder()
                .id(new EodStatusCompositeId(stage, businessDate))
                .timestamp(clockService.getCurrentDateTimeUTC())
                .build()
        );
    }

    @SneakyThrows
    @SuppressFBWarnings({ "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS", "MDM_THREAD_FAIRNESS" })
    private <T> T executeWithLock(final Callable<T> callable) {
        if (lock.tryLock()) {
            try {
                return callable.call();
            } finally {
                lock.unlock();
            }
        } else {
            throw new LockException();
        }
    }

}
