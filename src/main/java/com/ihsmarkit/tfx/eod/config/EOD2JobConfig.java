package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD2_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.SWAP_PNL_STEP_NAME;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ihsmarkit.tfx.eod.batch.SwapPnLTasklet;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Configuration
public class EOD2JobConfig {

    private final JobBuilderFactory jobs;

    private final StepBuilderFactory steps;

    private final SwapPnLTasklet swapPnLTasklet;

    @Bean(name = EOD2_BATCH_JOB_NAME)
    public Job eod2Job() {
        return jobs.get(EOD2_BATCH_JOB_NAME)
            .start(swapPnL())
            .build();
    }

    private Step swapPnL() {
        return steps.get(SWAP_PNL_STEP_NAME)
            .tasklet(swapPnLTasklet)
            .build();
    }

}
