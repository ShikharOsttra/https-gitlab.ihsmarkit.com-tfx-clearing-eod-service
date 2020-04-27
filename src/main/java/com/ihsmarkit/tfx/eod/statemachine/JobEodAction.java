package com.ihsmarkit.tfx.eod.statemachine;

import java.util.function.Function;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JobEodAction implements EodAction {

    private final JobLauncher jobLauncher;
    private final Job job;
    private final Function<EodContext, JobParameters> parametersMapper;

    @SneakyThrows
    @Override
    public void execute(final EodContext context) {
        try {
            final JobExecution execution = jobLauncher.run(job, parametersMapper.apply(context));

            if (execution.getStatus() != BatchStatus.COMPLETED) {
                throw new JobFailedException(String.format("Job %s failed", execution.getJobInstance().getJobName()));
            }

        } catch (final JobInstanceAlreadyCompleteException ex) {
            log.info("Ignored attempt to rerun successfully executed job {}.", job.getName());
        }
    }
}
