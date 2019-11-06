package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD1_BATCH_JOB_NAME;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.ihsmarkit.tfx.eod.batch.MarkToMarketTradesTasklet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@Configuration
@ComponentScan(basePackages = { "com.ihsmarkit.tfx.eod.batch", "com.ihsmarkit.tfx.eod.service", "com.ihsmarkit.tfx.eod.mapper" })
@EnableBatchProcessing
public class SpringBatchConfig {

    @Autowired
    private JobBuilderFactory jobs;

    @Autowired
    private StepBuilderFactory steps;

    @Autowired
    private MarkToMarketTradesTasklet markToMarketTradesTasklet;

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(final JobRegistry jobRegistry) {
        final JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor = new JobRegistryBeanPostProcessor();
        jobRegistryBeanPostProcessor.setJobRegistry(jobRegistry);

        return jobRegistryBeanPostProcessor;
    }

    @Bean(name = EOD1_BATCH_JOB_NAME)
    public Job eod1Job() {
        return jobs.get(EOD1_BATCH_JOB_NAME)
            .start(mtmTrades())
            .build();
    }

    @Bean
    @JobScope
    public Step mtmTrades() {
        return steps.get("mtmTrades")
            .tasklet(markToMarketTradesTasklet)
            .build();
    }
}
