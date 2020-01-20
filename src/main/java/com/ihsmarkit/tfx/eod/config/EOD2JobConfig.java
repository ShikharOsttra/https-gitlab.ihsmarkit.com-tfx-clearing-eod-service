package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_BALANCE_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_LIST_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.DAILY_MARKET_DATA_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD2_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MARGIN_COLLATERAL_EXCESS_OR_DEFICIENCY;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MONTHLY_TRADING_VOLUME_LEDGER_FLOW_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.OPEN_POSITIONS_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.SWAP_PNL_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TOTAL_VM_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TRANSACTION_DIARY_LEDGER_FLOW_NAME;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.ihsmarkit.tfx.alert.client.domain.Eod2CompletedAlert;
import com.ihsmarkit.tfx.alert.client.domain.Eod2StartAlert;
import com.ihsmarkit.tfx.eod.batch.MarginCollateralExcessDeficiencyTasklet;
import com.ihsmarkit.tfx.eod.batch.SwapPnLTasklet;
import com.ihsmarkit.tfx.eod.batch.TotalVariationMarginTasklet;
import com.ihsmarkit.tfx.eod.config.ledger.CollateralBalanceLedgerConfig;
import com.ihsmarkit.tfx.eod.config.ledger.CollateralListLedgerConfig;
import com.ihsmarkit.tfx.eod.config.ledger.DailyMarketDataLedgerConfig;
import com.ihsmarkit.tfx.eod.config.ledger.LedgerStepFactory;
import com.ihsmarkit.tfx.eod.config.ledger.MonthlyTradingVolumeLedgerConfig;
import com.ihsmarkit.tfx.eod.config.ledger.OpenPositionsLedgerConfig;
import com.ihsmarkit.tfx.eod.config.ledger.TransactionDiaryLedgerConfig;
import com.ihsmarkit.tfx.eod.config.listeners.EodJobListenerFactory;
import com.ihsmarkit.tfx.eod.config.listeners.EodAlertStepListener;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Configuration
@Import({
    CollateralListLedgerConfig.class,
    CollateralBalanceLedgerConfig.class,
    TransactionDiaryLedgerConfig.class,
    OpenPositionsLedgerConfig.class,
    DailyMarketDataLedgerConfig.class,
    MonthlyTradingVolumeLedgerConfig.class
})
@ComponentScan(basePackageClasses = LedgerStepFactory.class)
public class EOD2JobConfig {

    private final JobBuilderFactory jobs;

    private final StepBuilderFactory steps;

    private final SwapPnLTasklet swapPnLTasklet;

    private final TotalVariationMarginTasklet totalVariationMarginTasklet;

    private final MarginCollateralExcessDeficiencyTasklet marginCollateralExcessDeficiencyTasklet;

    private final EodAlertStepListener eodAlertStepListener;

    private final EodJobListenerFactory eodJobListenerFactory;

    @Qualifier(DAILY_MARKET_DATA_LEDGER_STEP_NAME)
    private Step dailyMarkedDataLedger;
    @Qualifier(TRANSACTION_DIARY_LEDGER_FLOW_NAME)
    private final Flow transactionDiaryLedger;
    @Qualifier(OPEN_POSITIONS_LEDGER_STEP_NAME)
    private Step openPositionsLedger;
    @Qualifier(COLLATERAL_LIST_LEDGER_STEP_NAME)
    private final Step collateralListLedger;
    @Qualifier(COLLATERAL_BALANCE_LEDGER_STEP_NAME)
    private final Step collateralBalanceLedger;
    @Qualifier(MONTHLY_TRADING_VOLUME_LEDGER_FLOW_NAME)
    private Flow monthlyTradingVolumeLedger;


    @Bean(name = EOD2_BATCH_JOB_NAME)
    public Job eod2Job() {
        return jobs.get(EOD2_BATCH_JOB_NAME)
            .listener(eodJobListenerFactory.listener(Eod2StartAlert::of, Eod2CompletedAlert::of))
            .flow(swapPnL())
            .next(totalVM())
            .next(marginCollateralExcessOrDeficiency())
            //ledgers
            .next(dailyMarkedDataLedger)
            .next(transactionDiaryLedger)
            .next(openPositionsLedger)
            .next(collateralListLedger)
            .next(collateralBalanceLedger)
            .next(monthlyTradingVolumeLedger)
            .end()
            .build();
    }

    private Step swapPnL() {
        return steps.get(SWAP_PNL_STEP_NAME)
            .listener(eodAlertStepListener)
            .tasklet(swapPnLTasklet)
            .build();
    }

    private Step totalVM() {
        return steps.get(TOTAL_VM_STEP_NAME)
            .listener(eodAlertStepListener)
            .tasklet(totalVariationMarginTasklet)
            .build();
    }

    private Step marginCollateralExcessOrDeficiency() {
        return steps.get(MARGIN_COLLATERAL_EXCESS_OR_DEFICIENCY)
            .listener(eodAlertStepListener)
            .tasklet(marginCollateralExcessDeficiencyTasklet)
            .build();
    }

}
