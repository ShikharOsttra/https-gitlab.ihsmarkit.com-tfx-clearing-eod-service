package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JOB_NAME_PARAM_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.time.LocalDate;
import java.util.Map;

import org.assertj.core.matcher.AssertionMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.launch.JobLauncher;

import com.ihsmarkit.tfx.core.time.ClockService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;

@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
@ExtendWith(MockitoExtension.class)
class EodQuartzJobTest {

    private static final String JOB_NAME = "eod1Job";

    @InjectMocks
    private EodQuartzJob eodQuartzJob;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private JobLocator jobLocator;

    @Mock
    private ClockService clockService;

    @Mock
    private Job job;

    @Mock
    private JobDetail jobDetail;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Test
    @SneakyThrows
    void shouldRunSpringBatchJob() {
        final LocalDate scheduleDate = LocalDate.of(2019, 11, 10);
        when(jobLocator.getJob(JOB_NAME)).thenReturn(job);
        when(clockService.getCurrentDate()).thenReturn(scheduleDate);
        when(jobDetail.getJobDataMap()).thenReturn(new JobDataMap(Map.of(JOB_NAME_PARAM_NAME, JOB_NAME)));
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);

        eodQuartzJob.executeInternal(jobExecutionContext);

        verify(jobLauncher).run(eq(job), argThat(new AssertionMatcher<>() {
            @SneakyThrows
            @Override
            public void assertion(final JobParameters jobParameters) {
                assertThat(jobParameters.getString(BUSINESS_DATE_JOB_PARAM_NAME)).isEqualTo("20191109");
            }
        }));
    }
}