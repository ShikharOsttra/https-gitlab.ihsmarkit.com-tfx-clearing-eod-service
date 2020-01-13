package com.ihsmarkit.tfx.eod.statemachine;

import java.time.LocalDate;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import lombok.SneakyThrows;

@FunctionalInterface
public interface JobEodAction extends EodAction {

    @SneakyThrows
    @Override
    default void execute(LocalDate date) {
        final JobExecution execution = executeJob(date);
        if (execution.getStatus() != BatchStatus.COMPLETED) {
            throw new RuntimeException(String.format("Job %s failed", execution.getJobInstance().getJobName()));
        }
    }

    JobExecution executeJob(LocalDate date)
        throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException;
}
