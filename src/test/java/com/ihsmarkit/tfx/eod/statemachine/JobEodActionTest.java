package com.ihsmarkit.tfx.eod.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class JobEodActionTest {
    private static final LocalDate OCT_1 = LocalDate.of(2019, 10, 10);
    private static final LocalDateTime OCT_1_NOON = LocalDateTime.of(OCT_1, LocalTime.NOON);

    @Test
    void shouldThrowExceptionOnFailure() {
        final JobExecution failureExecution = mock(JobExecution.class);
        final JobInstance failureInstance = mock(JobInstance.class);

        when(failureExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(failureExecution.getJobInstance()).thenReturn(failureInstance);
        when(failureInstance.getJobName()).thenReturn("TEST_JOB");

        final JobEodAction failing = date -> failureExecution;
        final Throwable catched = catchThrowable(() -> failing.execute(new EodContext(OCT_1, OCT_1_NOON)));

        assertThat(catched)
            .isInstanceOf(JobFailedException.class)
            .hasMessage("Job TEST_JOB failed");

    }

    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    void shouldRunIfNoException() {
        final JobExecution successExecution = mock(JobExecution.class);
        when(successExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        final JobEodAction success = date -> successExecution;

        success.execute(new EodContext(OCT_1, OCT_1_NOON));

        verify(successExecution).getStatus();

    }
}