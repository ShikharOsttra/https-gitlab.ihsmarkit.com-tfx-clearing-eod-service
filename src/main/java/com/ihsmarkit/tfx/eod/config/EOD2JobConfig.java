package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_LIST_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD2_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.SWAP_PNL_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TOTAL_VM_STEP_NAME;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.eod.batch.SwapPnLTasklet;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralListLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralListQueryProvider;
import com.ihsmarkit.tfx.eod.model.ledger.CollateralListItem;
import com.ihsmarkit.tfx.eod.batch.TotalVariationMarginTasklet;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@AllArgsConstructor
@Configuration
public class EOD2JobConfig {

    private final JobBuilderFactory jobs;

    private final StepBuilderFactory steps;

    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;

    private final SwapPnLTasklet swapPnLTasklet;

    private final TotalVariationMarginTasklet totalVariationMarginTasklet;

    @Value("${eod.ledger.collateral.list.chunk.size:1000}")
    private final int collateralListChunkSize;
    private final CollateralListLedgerProcessor collateralListProcessor;
    @Value("classpath:/ledger/sql/eod_ledger_collateral_list_insert.sql")
    private final Resource collateralListLedgerSql;

    @Bean(name = EOD2_BATCH_JOB_NAME)
    public Job eod2Job() {
        return jobs.get(EOD2_BATCH_JOB_NAME)
            .start(swapPnL())
            .next(totalVM())

            //ledgers
            .next(collateralListLedger())

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

    private Step collateralListLedger() {
        return steps.get(COLLATERAL_LIST_LEDGER_STEP_NAME)
            .<CollateralBalanceEntity, CollateralListItem>chunk(collateralListChunkSize)
            .reader(collateralListReader())
            .processor(collateralListProcessor)
            .writer(collateralListWriter())
            .build();
    }

    @Bean
    protected ItemReader<CollateralBalanceEntity> collateralListReader() {
        //todo: hibernate cursor ?
        return new JpaPagingItemReaderBuilder<CollateralBalanceEntity>()
            .pageSize(collateralListChunkSize)
            .entityManagerFactory(entityManagerFactory)
            .saveState(false)
            .transacted(false)
            .queryProvider(new CollateralListQueryProvider())
            .build();
    }

    @SuppressWarnings("unchecked")
    @Bean
    @SneakyThrows
    protected ItemWriter<CollateralListItem> collateralListWriter() {
        return new JdbcBatchItemWriterBuilder<CollateralListItem>()
            .beanMapped()
            .sql(IOUtils.toString(collateralListLedgerSql.getInputStream()))
            .dataSource(dataSource)
            .build();
    }

}
