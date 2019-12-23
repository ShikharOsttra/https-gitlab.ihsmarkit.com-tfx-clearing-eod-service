package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TRANSACTION_DIARY_LEDGER_STEP_NAME;

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

import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.RecordDateSetter;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.TradeListQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.TransactionDiaryLedgerProcessor;
import com.ihsmarkit.tfx.eod.model.ledger.TransactionDiary;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Configuration
@AllArgsConstructor
public class TransactionDiaryLedgerConfig {

    private final StepBuilderFactory steps;

    private final EntityManagerFactory entityManagerFactory;

    private final DataSource dataSource;

    @Value("${eod.ledger.transaction.diary.chunk.size:1000}")
    private final int transactionDiaryChunkSize;

    private final TransactionDiaryLedgerProcessor transactionDiaryLedgerProcessor;

    @Value("classpath:/ledger/sql/eod_ledger_transaction_diary_insert.sql")
    private final Resource tradeDiaryLedgerSql;

    private final RecordDateSetter recordDateSetter;

    private final TradeListQueryProvider queryProvider;

    @Bean(TRANSACTION_DIARY_LEDGER_STEP_NAME)
    protected Step tradeDiaryLedger() {
        return steps.get(TRANSACTION_DIARY_LEDGER_STEP_NAME)
            .listener(recordDateSetter)
            .<TradeEntity, TransactionDiary>chunk(transactionDiaryChunkSize)
            .reader(transactionDiaryReader())
            .processor(transactionDiaryLedgerProcessor)
            .writer(tradeDiaryWriter())
            .build();
    }

    @Bean
    protected ItemReader<TradeEntity> transactionDiaryReader() {
        //todo: hibernate cursor ?
        return new JpaPagingItemReaderBuilder<TradeEntity>()
            .pageSize(transactionDiaryChunkSize)
            .entityManagerFactory(entityManagerFactory)
            .saveState(false)
            .transacted(false)
            .queryProvider(queryProvider)
            .build();
    }

    @SuppressWarnings("unchecked")
    @Bean
    @SneakyThrows
    protected ItemWriter<TransactionDiary> tradeDiaryWriter() {
        return new JdbcBatchItemWriterBuilder<TransactionDiary>()
            .beanMapped()
            .sql(IOUtils.toString(tradeDiaryLedgerSql.getInputStream()))
            .dataSource(dataSource)
            .build();
    }
}
