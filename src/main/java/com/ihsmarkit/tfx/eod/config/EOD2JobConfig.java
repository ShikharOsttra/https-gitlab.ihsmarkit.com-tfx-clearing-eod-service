package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_BALANCE_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_LIST_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD2_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MARGIN_COLLATERAL_EXCESS_OR_DEFICIENCY;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.OPEN_POSITIONS_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.NET_TRANSACTION_DIARY_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.SWAP_PNL_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TOTAL_VM_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.ihsmarkit.tfx.eod.batch.MarginCollateralExcessDeficiencyTasklet;
import com.ihsmarkit.tfx.eod.batch.SwapPnLTasklet;
import com.ihsmarkit.tfx.eod.batch.TotalVariationMarginTasklet;
import com.ihsmarkit.tfx.eod.config.ledger.CollateralBalanceLedgerConfig;
import com.ihsmarkit.tfx.eod.config.ledger.CollateralListLedgerConfig;
import com.ihsmarkit.tfx.eod.config.ledger.OpenPositionsLedgerConfig;
import com.ihsmarkit.tfx.eod.config.ledger.TransactionDiaryLedgerConfig;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Configuration
@Import({ CollateralListLedgerConfig.class, CollateralBalanceLedgerConfig.class, TransactionDiaryLedgerConfig.class, OpenPositionsLedgerConfig.class})

public class EOD2JobConfig {

    private final JobBuilderFactory jobs;

    private final StepBuilderFactory steps;

    private final SwapPnLTasklet swapPnLTasklet;

    private final TotalVariationMarginTasklet totalVariationMarginTasklet;

    private final MarginCollateralExcessDeficiencyTasklet marginCollateralExcessDeficiencyTasklet;

    @Qualifier(COLLATERAL_LIST_LEDGER_STEP_NAME)
    private final Step collateralListLedger;
    @Qualifier(COLLATERAL_BALANCE_LEDGER_STEP_NAME)
    private final Step collateralBalanceLedger;
    @Qualifier(TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME)
    private final Step tradeTransactionDiaryLedger;
    @Qualifier(SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME)
    private final Step sodTransactionDiaryLedger;
    @Qualifier(NET_TRANSACTION_DIARY_LEDGER_STEP_NAME)
    private final Step netTransactionDiaryLedger;

    @Qualifier(OPEN_POSITIONS_LEDGER_STEP_NAME)
    private Step openPositionsLedger;

    @Bean(name = EOD2_BATCH_JOB_NAME)
    public Job eod2Job() {
        return jobs.get(EOD2_BATCH_JOB_NAME)
            .start(swapPnL())
            .next(totalVM())
            .next(marginCollateralExcessOrDeficiency())
            //ledgers
            .next(sodTransactionDiaryLedger)
            .next(tradeTransactionDiaryLedger)
            .next(netTransactionDiaryLedger)
            .next(collateralListLedger)
            .next(collateralBalanceLedger)
            .next(openPositionsLedger)
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

    private Step marginCollateralExcessOrDeficiency() {
        return steps.get(MARGIN_COLLATERAL_EXCESS_OR_DEFICIENCY)
            .tasklet(marginCollateralExcessDeficiencyTasklet)
            .build();
    }

}
