package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CASH_BALANCE_UPDATE_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CASH_BALANCE_UPDATE_NOTIFY_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CASH_BALANCE_UPDATE_STEP_NAME;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ihsmarkit.tfx.eod.batch.CashCollateralBalanceUpdateTasklet;
import com.ihsmarkit.tfx.eod.batch.CollateralBalanceUpdateNotifierTasklet;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Configuration
public class CashCollateralBalanceUpdateJobConfig {

    private final JobBuilderFactory jobs;

    private final StepBuilderFactory steps;

    private final CashCollateralBalanceUpdateTasklet cashCollateralBalanceUpdateTasklet;

    private final CollateralBalanceUpdateNotifierTasklet collateralBalanceUpdateNotifierTasklet;

    @Bean(name = CASH_BALANCE_UPDATE_BATCH_JOB_NAME)
    public Job cashBalanceUpdateJob() {
        return jobs.get(CASH_BALANCE_UPDATE_BATCH_JOB_NAME)
            .start(cashBalanceUpdate())
            .next(collateralBalanceUpdateNotify())
            .build();
    }

    private Step cashBalanceUpdate() {
        return steps.get(CASH_BALANCE_UPDATE_STEP_NAME)
            .tasklet(cashCollateralBalanceUpdateTasklet)
            .build();
    }

    private Step collateralBalanceUpdateNotify() {
        return steps.get(CASH_BALANCE_UPDATE_NOTIFY_STEP_NAME)
            .tasklet(collateralBalanceUpdateNotifierTasklet)
            .build();
    }
}
