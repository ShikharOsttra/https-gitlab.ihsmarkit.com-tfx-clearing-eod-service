package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CURRENT_TSP_JOB_PARAM_NAME;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

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

import com.ihsmarkit.tfx.core.dl.entity.eod.EodStage;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodStatusCompositeId;
import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodDataRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodStatusRepository;
import com.ihsmarkit.tfx.core.domain.type.SystemParameters;
import com.ihsmarkit.tfx.core.domain.type.TransactionType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EODControlService {

    private final SystemParameterRepository systemParameterRepository;
    private final EodStatusRepository eodStatusRepository;
    private final TradeRepository tradeRepository;
    private final CalendarTradingSwapPointRepository calendarRepository;

    private final List<EodDataRepository> eodDataRepositories;

    private final JobLauncher jobLauncher;
    private final JobLocator jobLocator;

    public LocalDate getCurrentBusinessDate() {
        return systemParameterRepository.getParameterValueFailFast(SystemParameters.BUSINESS_DATE);
    }

    private void setCurrentBusinessDate(final LocalDate businessDate) {
        systemParameterRepository.setParameter(SystemParameters.BUSINESS_DATE, businessDate);
    }

    @Transactional
    public LocalDate undoPreviousDayEOD() {
        final LocalDate currentBusinessDate = getCurrentBusinessDate();
        undoEODByDate(currentBusinessDate);

        final LocalDate previousBusinessDate = getPreviousBusinessDate(currentBusinessDate);

        if (!previousBusinessDate.isEqual(currentBusinessDate)) {
            undoEODByDate(previousBusinessDate);
            setCurrentBusinessDate(previousBusinessDate);
        }
        return currentBusinessDate;
    }

    @Transactional
    public LocalDate undoCurrentDayEOD() {
        final LocalDate currentBusinessDate = getCurrentBusinessDate();
        undoEODByDate(currentBusinessDate);
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

    private void undoEODByDate(final LocalDate currentBusinessDate) {
        tradeRepository.deleteAllByTransactionTypeAndTradeDate(TransactionType.BALANCE, currentBusinessDate);
        eodDataRepositories.forEach(repository -> repository.deleteAllByDate(currentBusinessDate));
        removeEODStageRecordsForDate(currentBusinessDate);
    }

    private void removeEODStageRecordsForDate(final LocalDate businessDate) {
        Stream.of(
            EodStage.EOD1_COMPLETE,
            EodStage.EOD2_COMPLETE,
            EodStage.DSP_APPROVED,
            EodStage.SWAP_POINTS_APPROVED
        )
            .map(eodStage -> new EodStatusCompositeId(eodStage, businessDate))
            .forEach(this::deleteEodStatusIfExist);
    }

    private void deleteEodStatusIfExist(final EodStatusCompositeId eod2CompleteStatus) {
        if (eodStatusRepository.existsById(eod2CompleteStatus)) {
            eodStatusRepository.deleteById(eod2CompleteStatus);
        }
    }

    private LocalDate getPreviousBusinessDate(final LocalDate currentBusinessDate) {
        return calendarRepository.findPrevBankBusinessDate(currentBusinessDate).orElse(currentBusinessDate);
    }
}
