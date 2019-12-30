package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_LIST_LEDGER_STEP_NAME;

import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;

import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralListLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralListQueryProvider;
import com.ihsmarkit.tfx.eod.model.ledger.CollateralListItem;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class CollateralListLedgerConfig {

    @Value("${eod.ledger.collateral.list.chunk.size:1000}")
    private final int collateralListChunkSize;
    @Value("${eod.ledger.collateral.list.concurrency.limit:1}")
    private final int collateralListConcurrencyLimit;
    @Value("classpath:/ledger/sql/eod_ledger_collateral_list_insert.sql")
    private final Resource collateralListLedgerSql;

    private final CollateralListLedgerProcessor collateralListProcessor;
    private final LedgerStepFactory ledgerStepFactory;

    @Bean(COLLATERAL_LIST_LEDGER_STEP_NAME)
    Step collateralListLedger() {
        return ledgerStepFactory.<CollateralBalanceEntity, CollateralListItem>stepBuilder(COLLATERAL_LIST_LEDGER_STEP_NAME, collateralListChunkSize)
            .reader(collateralListReader())
            .processor(collateralListProcessor)
            .writer(collateralListWriter())
            .taskExecutor(collateralListTaskExecutor())
            .build();
    }

    @Bean
    TaskExecutor collateralListTaskExecutor() {
        return ledgerStepFactory.taskExecutor(collateralListConcurrencyLimit);
    }

    @Bean
    ItemReader<CollateralBalanceEntity> collateralListReader() {
        return ledgerStepFactory.listReader(new CollateralListQueryProvider(), collateralListChunkSize);
    }

    @Bean
    ItemWriter<CollateralListItem> collateralListWriter() {
        return ledgerStepFactory.listWriter(collateralListLedgerSql);
    }
}
