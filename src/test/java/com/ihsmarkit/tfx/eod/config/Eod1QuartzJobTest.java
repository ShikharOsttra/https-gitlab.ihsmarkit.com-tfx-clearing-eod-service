package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.assertj.core.matcher.AssertionMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class Eod1QuartzJobTest {

    private static final String JOB_NAME = "eod1Job";

    @InjectMocks
    private Eod1QuartzJob eod1QuartzJob;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private JobLocator jobLocator;

    @Mock
    private ClockService clockService;

    @Mock
    private Job job;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Test
    @SneakyThrows
    void shouldRunSpringBatchJob() {
        eod1QuartzJob.setJobName(JOB_NAME);
        when(jobLocator.getJob(JOB_NAME)).thenReturn(job);
        final LocalDateTime scheduleTime = LocalDateTime.ofInstant(LocalDateTime.of(2019, 11, 10, 9, 30, 0).atZone(ZoneId.of(JST)).toInstant(),
            ZoneId.of(JST));
        when(clockService.getCurrentDateTime()).thenReturn(scheduleTime);

        eod1QuartzJob.executeInternal(jobExecutionContext);

        verify(jobLauncher).run(eq(job), argThat(new AssertionMatcher<>() {
            @SneakyThrows
            @Override
            public void assertion(final JobParameters jobParameters) {
                assertThat(jobParameters.getString(BUSINESS_DATE_JOB_PARAM_NAME)).isEqualTo("20191109");
            }
        }));
    }
}