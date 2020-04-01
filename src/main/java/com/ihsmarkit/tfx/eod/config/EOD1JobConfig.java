package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD1_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MTM_TRADES_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.NET_TRADES_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.REBALANCE_POSITIONS_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.ROLL_POSITIONS_STEP_NAME;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ihsmarkit.tfx.alert.client.domain.Eod1CompletedAlert;
import com.ihsmarkit.tfx.alert.client.domain.Eod1StartAlert;
import com.ihsmarkit.tfx.eod.batch.MarkToMarketTradesTasklet;
import com.ihsmarkit.tfx.eod.batch.NettingTasklet;
import com.ihsmarkit.tfx.eod.batch.PositionRollTasklet;
import com.ihsmarkit.tfx.eod.batch.RebalancingTasklet;
import com.ihsmarkit.tfx.eod.config.listeners.EodJobListenerFactory;

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

    private final EodJobListenerFactory eodJobListenerFactory;

    @Bean(name = EOD1_BATCH_JOB_NAME)
    public Job eod1Job() {
        return jobs.get(EOD1_BATCH_JOB_NAME)
            .listener(eodJobListenerFactory.listener(Eod1StartAlert::of, Eod1CompletedAlert::of))
            .start(mtmTrades())
            .next(netTrades())
            .next(rebalancePositions())
            .next(rollPositions())
            .build();
    }

    private Step mtmTrades() {
        return createStep(MTM_TRADES_STEP_NAME, markToMarketTradesTasklet);
    }

    private Step netTrades() {
        return createStep(NET_TRADES_STEP_NAME, nettingTasklet);
    }

    private Step rebalancePositions() {
        return createStep(REBALANCE_POSITIONS_STEP_NAME, rebalancingTasklet);
    }

    private Step rollPositions() {
        return createStep(ROLL_POSITIONS_STEP_NAME, positionRollTasklet);
    }

    private Step createStep(final String stepName, final Tasklet stepTasklet) {
        return steps.get(stepName)
            .tasklet(stepTasklet)
            .build();
    }
}
