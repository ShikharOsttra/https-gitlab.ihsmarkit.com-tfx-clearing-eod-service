package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TFX_TOTAL_CONTEXT_KEY;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_BALANCE_LEDGER_STEP_NAME;

import java.util.List;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.CollateralBalanceInputProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.CollateralBalanceMapProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.CollateralBalanceTotalProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.ParticipantQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.CollateralBalanceTotal;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain.CollateralBalanceWriteItem;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.SingleValueTotalHolder;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TotalWriterListener;
import com.ihsmarkit.tfx.eod.support.ListItemWriter;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class CollateralBalanceLedgerConfig {

    @Value("${eod.ledger.collateral.balance.chunk.size:10}")
    private final int collateralBalanceChunkSize;
    @Value("${eod.ledger.collateral.balance.concurrency.limit:1}")
    private final int collateralBalanceConcurrencyLimit;
    @Value("classpath:/ledger/sql/eod_ledger_collateral_balance_insert.sql")
    private final Resource collateralBalanceLedgerSql;

    private final CollateralBalanceInputProcessor inputProcessor;
    private final CollateralBalanceMapProcessor mapProcessor;
    private final LedgerStepFactory ledgerStepFactory;

    @Bean(COLLATERAL_BALANCE_LEDGER_STEP_NAME)
    Step collateralBalanceLedger() {
        return ledgerStepFactory
            .<ParticipantEntity, List<CollateralBalanceWriteItem>>stepBuilder(COLLATERAL_BALANCE_LEDGER_STEP_NAME, collateralBalanceChunkSize)
            .reader(collateralBalanceReader())
            .processor(collateralBalanceLedgerProcessor())
            .writer(collateralBalanceListWriter())
            .taskExecutor(collateralBalanceTaskExecutor())
            .stream(collateralBalanceTfxTotalHolder())
            .listener(collateralBalanceTotalWriterListener())
            .build();
    }

    @Bean
    TaskExecutor collateralBalanceTaskExecutor() {
        return ledgerStepFactory.taskExecutor(collateralBalanceConcurrencyLimit);
    }

    @Bean
    ItemReader<ParticipantEntity> collateralBalanceReader() {
        return ledgerStepFactory.listReader(new ParticipantQueryProvider(), collateralBalanceChunkSize);
    }

    @Bean
    ItemProcessor<ParticipantEntity, List<CollateralBalanceWriteItem>> collateralBalanceLedgerProcessor() {
        return LedgerStepFactory.compositeProcessor(
            inputProcessor,
            collateralBalanceTotalProcessor(),
            mapProcessor
        );
    }

    @Bean
    ItemWriter<List<CollateralBalanceWriteItem>> collateralBalanceListWriter() {
        return new ListItemWriter<>(collateralBalanceWriter());
    }

    @Bean
    CollateralBalanceTotalProcessor collateralBalanceTotalProcessor() {
        return new CollateralBalanceTotalProcessor(collateralBalanceTfxTotalHolder());
    }

    @Bean
    @StepScope
    SingleValueTotalHolder<CollateralBalanceTotal> collateralBalanceTfxTotalHolder() {
        return new SingleValueTotalHolder<>(TFX_TOTAL_CONTEXT_KEY, false);
    }

    @Bean
    TotalWriterListener<CollateralBalanceWriteItem> collateralBalanceTotalWriterListener() {
        return new TotalWriterListener<>(
            collateralBalanceTfxTotalHolder(), mapProcessor::mapToTfxTotal,
            collateralBalanceWriter()
        );
    }

    @Bean
    ItemWriter<CollateralBalanceWriteItem> collateralBalanceWriter() {
        return ledgerStepFactory.listWriter(collateralBalanceLedgerSql);
    }
}
