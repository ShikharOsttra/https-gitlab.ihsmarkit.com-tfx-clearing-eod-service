package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JST;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.quartz.JobExecutionContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.time.ClockService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@Service
public class Eod1QuartzJob extends QuartzJobBean {

    @Getter
    @Setter
    private String jobName;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobLocator jobLocator;

    @Autowired
    private ClockService clockService;

    @Override
    @SneakyThrows
    protected void executeInternal(final JobExecutionContext context) {
        final Job job = jobLocator.getJob(jobName);
        final JobParameters params = new JobParametersBuilder()
            .addString(BUSINESS_DATE_JOB_PARAM_NAME, getBusinessDate().format(BUSINESS_DATE_FMT))
            .addString("currentTimestamp", LocalDateTime.now().toString())
            .toJobParameters();

        jobLauncher.run(job, params);
    }

    private LocalDate getBusinessDate() {
        return clockService.getCurrentDateTime().atZone(ZoneId.of(JST)).toLocalDate().minus(1, ChronoUnit.DAYS);
    }
}