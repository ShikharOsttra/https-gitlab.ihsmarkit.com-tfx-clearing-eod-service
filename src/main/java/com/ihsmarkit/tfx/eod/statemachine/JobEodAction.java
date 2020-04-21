package com.ihsmarkit.tfx.eod.statemachine;

import java.util.function.Function;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobEodAction implements EodAction {

    private JobLauncher jobLauncher;
    private Job job;
    private Function<EodContext, JobParameters> parametersMapper;

    public JobEodAction(final JobLauncher jobLauncher, final Job job, final Function<EodContext, JobParameters> parametersMapper) {
        this.jobLauncher = jobLauncher;
        this.job = job;
        this.parametersMapper = parametersMapper;
    }

    @SneakyThrows
    @Override
    public void execute(EodContext context) {
        try {
            final JobExecution execution = jobLauncher.run(job, parametersMapper.apply(context));

            if (execution.getStatus() != BatchStatus.COMPLETED) {
                throw new JobFailedException(String.format("Job %s failed", execution.getJobInstance().getJobName()));
            }

        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("Ignored attempt to rerun successfully executed job {}.", job.getName());
        }
    }
}
