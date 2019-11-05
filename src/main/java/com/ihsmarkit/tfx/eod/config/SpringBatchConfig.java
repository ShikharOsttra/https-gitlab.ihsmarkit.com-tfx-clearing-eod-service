package com.ihsmarkit.tfx.eod.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.ihsmarkit.tfx.eod.batch.MarkToMarketTradesTasklet;
import com.ihsmarkit.tfx.eod.batch.NettingTasklet;

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

    @Autowired
    private NettingTasklet nettingTasklet;

    @Bean(name = "eod1Job")
    public Job eod1Job() {
        return jobs.get("eod1Job")
            .start(mtmTrades())
            .next(netTrades())
            .build();
    }

    @Bean
    @JobScope
    public Step mtmTrades() {
        return steps.get("mtmTrades")
            .tasklet(markToMarketTradesTasklet)
            .build();
    }

    @Bean
    @JobScope
    public Step netTrades() {
        return steps.get("netTrades")
                .tasklet(nettingTasklet)
                .build();
    }
}
