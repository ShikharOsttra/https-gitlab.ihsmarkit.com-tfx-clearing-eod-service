package com.ihsmarkit.tfx.eod.statemachine;

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
    default void execute(EodContext context) {
        final JobExecution execution = executeJob(context);
        if (execution.getStatus() != BatchStatus.COMPLETED) {
            throw new JobFailedException(String.format("Job %s failed", execution.getJobInstance().getJobName()));
        }
    }

    JobExecution executeJob(EodContext context)
        throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException;
}
