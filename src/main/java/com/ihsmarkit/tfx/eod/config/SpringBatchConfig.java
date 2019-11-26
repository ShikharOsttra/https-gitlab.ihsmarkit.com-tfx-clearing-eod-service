package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD1_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MTM_TRADES_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.NET_TRADES_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.REBALANCE_POSITIONS_STEP_NAME;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.ihsmarkit.tfx.eod.batch.MarkToMarketTradesTasklet;
import com.ihsmarkit.tfx.eod.batch.NettingTasklet;
import com.ihsmarkit.tfx.eod.batch.RebalancingTasklet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;

@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@Configuration
@ComponentScan(basePackages = { "com.ihsmarkit.tfx.eod.batch", "com.ihsmarkit.tfx.eod.service", "com.ihsmarkit.tfx.eod.mapper", "com.ihsmarkit.tfx.eod.config"})
@EnableBatchProcessing
@AllArgsConstructor
public class SpringBatchConfig {

    private final JobBuilderFactory jobs;

    private final StepBuilderFactory steps;

    private final MarkToMarketTradesTasklet markToMarketTradesTasklet;

    private final NettingTasklet nettingTasklet;

    private final RebalancingTasklet rebalancingTasklet;

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
            .next(netTrades())
            .next(rebalancePositions())
            .build();
    }

    @Bean
    @JobScope
    public Step mtmTrades() {
        return steps.get(MTM_TRADES_STEP_NAME)
            .tasklet(markToMarketTradesTasklet)
            .build();
    }

    @Bean
    @JobScope
    public Step netTrades() {
        return steps.get(NET_TRADES_STEP_NAME)
                .tasklet(nettingTasklet)
                .build();
    }

    @Bean
    @JobScope
    public Step rebalancePositions() {
        return steps.get(REBALANCE_POSITIONS_STEP_NAME)
            .tasklet(rebalancingTasklet)
            .build();
    }
}
