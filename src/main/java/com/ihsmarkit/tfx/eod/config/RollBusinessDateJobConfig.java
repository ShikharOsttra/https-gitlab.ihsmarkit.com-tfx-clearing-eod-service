package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD_COMPLETE_NOTIFY_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.ROLL_BUSINESS_DATE_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.ROLL_BUSINESS_DATE_STEP_NAME;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ihsmarkit.tfx.eod.batch.EodCompleteNotifierTasklet;
import com.ihsmarkit.tfx.eod.batch.RollBusinessDateTasklet;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Configuration
public class RollBusinessDateJobConfig {

    private final JobBuilderFactory jobs;

    private final StepBuilderFactory steps;

    private final RollBusinessDateTasklet rollBusinessDateTasklet;

    private final EodCompleteNotifierTasklet eodCompleteNotifierTasklet;

    @Bean(name = ROLL_BUSINESS_DATE_JOB_NAME)
    public Job rollBusinessDateJob() {
        return jobs.get(ROLL_BUSINESS_DATE_JOB_NAME)
            .start(rollBusinessDate())
            .next(sendEodCompleteNotification())
            .build();
    }

    private Step rollBusinessDate() {
        return steps.get(ROLL_BUSINESS_DATE_STEP_NAME)
            .tasklet(rollBusinessDateTasklet)
            .build();
    }

    private Step sendEodCompleteNotification() {
        return steps.get(EOD_COMPLETE_NOTIFY_STEP_NAME)
            .tasklet(eodCompleteNotifierTasklet)
            .build();
    }

}
