package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_LIST_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD2_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.SWAP_PNL_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TOTAL_VM_STEP_NAME;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.ihsmarkit.tfx.eod.batch.SwapPnLTasklet;
import com.ihsmarkit.tfx.eod.batch.TotalVariationMarginTasklet;
import com.ihsmarkit.tfx.eod.config.ledger.CollateralListLedgerConfig;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Configuration
@Import(CollateralListLedgerConfig.class)
public class EOD2JobConfig {

    private final JobBuilderFactory jobs;

    private final StepBuilderFactory steps;

    private final SwapPnLTasklet swapPnLTasklet;

    private final TotalVariationMarginTasklet totalVariationMarginTasklet;

    @Qualifier(COLLATERAL_LIST_LEDGER_STEP_NAME)
    private Step collateralListLedger;

    @Bean(name = EOD2_BATCH_JOB_NAME)
    public Job eod2Job() {
        return jobs.get(EOD2_BATCH_JOB_NAME)
            .start(swapPnL())
            .next(totalVM())

            //ledgers
            .next(collateralListLedger)

            .build();
    }

    private Step swapPnL() {
        return steps.get(SWAP_PNL_STEP_NAME)
            .tasklet(swapPnLTasklet)
            .build();
    }

    private Step totalVM() {
        return steps.get(TOTAL_VM_STEP_NAME)
            .tasklet(totalVariationMarginTasklet)
            .build();
    }

}
