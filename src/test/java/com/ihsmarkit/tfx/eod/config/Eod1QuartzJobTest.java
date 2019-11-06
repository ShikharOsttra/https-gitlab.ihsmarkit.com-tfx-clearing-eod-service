package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.time.LocalDate;

import org.assertj.core.matcher.AssertionMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @BeforeEach
    void setUp() {
        eod1QuartzJob = new Eod1QuartzJob(JOB_NAME, jobLauncher, jobLocator, clockService);
    }

    @Test
    @SneakyThrows
    void shouldRunSpringBatchJob() {
        when(jobLocator.getJob(JOB_NAME)).thenReturn(job);
        final LocalDate scheduleDate = LocalDate.of(2019, 11, 10);
        when(clockService.getCurrentDate()).thenReturn(scheduleDate);

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