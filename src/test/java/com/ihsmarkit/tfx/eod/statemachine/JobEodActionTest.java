package com.ihsmarkit.tfx.eod.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;

class JobEodActionTest {
    private static final LocalDate OCT_1 = LocalDate.of(2019, 10, 10);

    @Test
    void shouldThrowExceptionOnFailure() {
        final JobExecution failureExecution = mock(JobExecution.class);
        final JobInstance failureInstance = mock(JobInstance.class);

        when(failureExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(failureExecution.getJobInstance()).thenReturn(failureInstance);
        when(failureInstance.getJobName()).thenReturn("TEST_JOB");

        final JobEodAction failing = date -> failureExecution;
        final Throwable catched = catchThrowable(() -> failing.execute(OCT_1));

        assertThat(catched)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Job TEST_JOB failed");

    }

    @Test
    void shouldRunIfNoException() {
        final JobExecution successExecution = mock(JobExecution.class);
        when(successExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        final JobEodAction success = date -> successExecution;

        success.execute(OCT_1);

        verify(successExecution).getStatus();

    }
}