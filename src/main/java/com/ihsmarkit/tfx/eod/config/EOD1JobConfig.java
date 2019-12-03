package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD1_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MTM_TRADES_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.NET_TRADES_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.REBALANCE_POSITIONS_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.ROLL_POSITIONS_STEP_NAME;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ihsmarkit.tfx.eod.batch.MarkToMarketTradesTasklet;
import com.ihsmarkit.tfx.eod.batch.NettingTasklet;
import com.ihsmarkit.tfx.eod.batch.PositionRollTasklet;
import com.ihsmarkit.tfx.eod.batch.RebalancingTasklet;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Configuration
public class EOD1JobConfig {

    private final JobBuilderFactory jobs;

    private final StepBuilderFactory steps;

    private final MarkToMarketTradesTasklet markToMarketTradesTasklet;

    private final NettingTasklet nettingTasklet;

    private final RebalancingTasklet rebalancingTasklet;

    private final PositionRollTasklet positionRollTasklet;
    @Bean(name = EOD1_BATCH_JOB_NAME)
    public Job eod1Job() {
        return jobs.get(EOD1_BATCH_JOB_NAME)
            .start(mtmTrades())
            .next(netTrades())
            .next(rebalancePositions())
            .next(rollPositions())
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

    @Bean
    @JobScope
    public Step rollPositions() {
        return steps.get(ROLL_POSITIONS_STEP_NAME)
            .tasklet(positionRollTasklet)
            .build();
    }
}
