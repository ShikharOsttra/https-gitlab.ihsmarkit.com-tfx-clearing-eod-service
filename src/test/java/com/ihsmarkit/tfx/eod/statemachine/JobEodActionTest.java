package com.ihsmarkit.tfx.eod.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;

@ExtendWith(SpringExtension.class)
class JobEodActionTest {
    private static final LocalDate OCT_1 = LocalDate.of(2019, 10, 10);
    private static final LocalDateTime OCT_1_NOON = LocalDateTime.of(OCT_1, LocalTime.NOON);

    @MockBean
    private JobLauncher jobLauncher;

    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    @SneakyThrows
    void shouldThrowExceptionOnFailure() {
        final JobExecution failureExecution = mock(JobExecution.class);
        final JobInstance failureInstance = mock(JobInstance.class);
        final Job job = mock(Job.class);
        final JobParameters jobParameters =  new JobParameters();

        when(failureExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(failureExecution.getJobInstance()).thenReturn(failureInstance);
        when(failureInstance.getJobName()).thenReturn("TEST_JOB");

        when(jobLauncher.run(any(), any())).thenReturn(failureExecution);

        final EodAction failing = new JobEodAction(jobLauncher, job, context ->  jobParameters);

        final Throwable catched = catchThrowable(() -> failing.execute(new EodContext(OCT_1, OCT_1_NOON)));

        assertThat(catched)
            .isInstanceOf(JobFailedException.class)
            .hasMessage("Job TEST_JOB failed");

        verify(failureExecution).getStatus();
        verify(jobLauncher).run(job, jobParameters);
        verifyNoMoreInteractions(jobLauncher);

    }

    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    @SneakyThrows
    void shouldRunIfNoException() {
        final JobExecution successExecution = mock(JobExecution.class);
        final Job job = mock(Job.class);
        final JobParameters jobParameters =  new JobParameters();

        when(successExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

        when(jobLauncher.run(any(), any())).thenReturn(successExecution);

        final EodAction success = new JobEodAction(jobLauncher, job, context ->  jobParameters);

        success.execute(new EodContext(OCT_1, OCT_1_NOON));

        verify(successExecution).getStatus();
        verify(jobLauncher).run(job, jobParameters);
        verifyNoMoreInteractions(jobLauncher);

    }

    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    @SneakyThrows
    void shouldIgonereAlreadyExecuted() {

        final Job job = mock(Job.class);
        final JobParameters jobParameters =  new JobParameters();

        when(jobLauncher.run(any(), any())).thenThrow(new JobInstanceAlreadyCompleteException("TEST"));

        final EodAction success = new JobEodAction(jobLauncher, job, context ->  jobParameters);

        success.execute(new EodContext(OCT_1, OCT_1_NOON));

        verify(jobLauncher).run(job, jobParameters);
        verifyNoMoreInteractions(jobLauncher);

    }
}