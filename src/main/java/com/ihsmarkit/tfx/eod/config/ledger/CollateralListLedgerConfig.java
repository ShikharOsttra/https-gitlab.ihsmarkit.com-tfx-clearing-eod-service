package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_LIST_LEDGER_STEP_NAME;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.batch.core.Step;
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
import com.ihsmarkit.tfx.eod.batch.ledger.RecordDateSetter;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralListLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralListQueryProvider;
import com.ihsmarkit.tfx.eod.model.ledger.CollateralListItem;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Configuration
@AllArgsConstructor
public class CollateralListLedgerConfig {

    private final StepBuilderFactory steps;

    private final EntityManagerFactory entityManagerFactory;

    private final DataSource dataSource;

    @Value("${eod.ledger.collateral.list.chunk.size:1000}")
    private final int collateralListChunkSize;

    private final CollateralListLedgerProcessor collateralListProcessor;

    @Value("classpath:/ledger/sql/eod_ledger_collateral_list_insert.sql")
    private final Resource collateralListLedgerSql;

    private final RecordDateSetter recordDateSetter;

    @Bean(COLLATERAL_LIST_LEDGER_STEP_NAME)
    protected Step collateralListLedger() {
        return steps.get(COLLATERAL_LIST_LEDGER_STEP_NAME)
            .listener(recordDateSetter)
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
