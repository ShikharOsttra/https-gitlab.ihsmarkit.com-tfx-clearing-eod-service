package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.core.domain.type.SystemParameters.BUSINESS_DATE;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CASH_BALANCE_UPDATE_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CURRENT_TSP_JOB_PARAM_NAME;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class CashCollateralBalanceUpdateQuartzJob extends QuartzJobBean {

    private final JobLauncher jobLauncher;

    private final JobLocator jobLocator;
    private final CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;
    private final SystemParameterRepository systemParameterRepository;

    @Override
    @SneakyThrows
    protected void executeInternal(final JobExecutionContext context) throws JobExecutionException {

        final LocalDate businessDate = systemParameterRepository.getParameterValueFailFast(BUSINESS_DATE);
        final LocalDate previousTradingDate = calendarTradingSwapPointRepository.findPreviousTradingDate(businessDate).get();

        final Job job = jobLocator.getJob(CASH_BALANCE_UPDATE_BATCH_JOB_NAME);
        final JobParameters params = new JobParametersBuilder()
            .addString(BUSINESS_DATE_JOB_PARAM_NAME, previousTradingDate.format(BUSINESS_DATE_FMT))
            .addString(CURRENT_TSP_JOB_PARAM_NAME, LocalDateTime.now().toString())
            .toJobParameters();

        jobLauncher.run(job, params);
    }

}
