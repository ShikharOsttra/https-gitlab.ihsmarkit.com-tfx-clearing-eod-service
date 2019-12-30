package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_BALANCE_LEDGER_STEP_NAME;

import java.util.List;

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
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.RecordDateSetter;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.CollateralBalanceLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.ParticipantQueryProvider;
import com.ihsmarkit.tfx.eod.model.ledger.CollateralBalanceItem;
import com.ihsmarkit.tfx.eod.support.ListItemWriter;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Configuration
@AllArgsConstructor
public class CollateralBalanceLedgerConfig {
    //todo use BaseLedgerConfig
    private final StepBuilderFactory steps;

    private final EntityManagerFactory entityManagerFactory;

    private final DataSource dataSource;

    @Value("${eod.ledger.collateral.balance.chunk.size:10}")
    private final int collateralBalanceChunkSize;

    @Value("${eod.ledger.collateral.balance.concurrency.limit:1}")
    private final int collateralBalanceConcurrencyLimit;

    private final CollateralBalanceLedgerProcessor collateralBalanceLedgerProcessor;

    @Value("classpath:/ledger/sql/eod_ledger_collateral_balance_insert.sql")
    private final Resource collateralBalanceLedgerSql;

    private final RecordDateSetter recordDateSetter;

    @Bean(COLLATERAL_BALANCE_LEDGER_STEP_NAME)
    Step collateralBalanceLedger() {
        return steps.get(COLLATERAL_BALANCE_LEDGER_STEP_NAME)
            .listener(recordDateSetter)
            .<ParticipantEntity, List<CollateralBalanceItem>>chunk(collateralBalanceChunkSize)
            .reader(collateralBalanceReader())
            .processor(collateralBalanceLedgerProcessor)
            .writer(collateralBalanceListWriter())
            .taskExecutor(collateralBalanceTaskExecutor())
            .build();
    }

    @Bean
    TaskExecutor collateralBalanceTaskExecutor() {
        final SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(collateralBalanceConcurrencyLimit);
        return taskExecutor;
    }

    @Bean
    ItemReader<ParticipantEntity> collateralBalanceReader() {
        return new JpaPagingItemReaderBuilder<ParticipantEntity>()
            .pageSize(collateralBalanceChunkSize)
            .entityManagerFactory(entityManagerFactory)
            .saveState(false)
            .transacted(false)
            .queryProvider(new ParticipantQueryProvider())
            .build();
    }

    @Bean
    ItemWriter<List<CollateralBalanceItem>> collateralBalanceListWriter() {
        return new ListItemWriter<>(collateralBalanceWriter());
    }

    @Bean
    @SneakyThrows
    ItemWriter<CollateralBalanceItem> collateralBalanceWriter() {
        return new JdbcBatchItemWriterBuilder<CollateralBalanceItem>()
            .beanMapped()
            .sql(IOUtils.toString(collateralBalanceLedgerSql.getInputStream()))
            .dataSource(dataSource)
            .build();
    }
}
