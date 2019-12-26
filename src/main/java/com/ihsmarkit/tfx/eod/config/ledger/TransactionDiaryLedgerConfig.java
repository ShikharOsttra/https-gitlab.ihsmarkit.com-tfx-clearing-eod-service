package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.NET_TRANSACTION_DIARY_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.orm.JpaQueryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.ihsmarkit.tfx.eod.batch.ledger.RecordDateSetter;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.NETQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.NETTransactionDiaryLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.SODQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.SODTransactionDiaryLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.TradeListQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.TradeTransactionDiaryLedgerProcessor;
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

    private final TradeTransactionDiaryLedgerProcessor tradeTransactionDiaryLedgerProcessor;
    private final SODTransactionDiaryLedgerProcessor sodTransactionDiaryLedgerProcessor;
    private final NETTransactionDiaryLedgerProcessor netTransactionDiaryLedgerProcessor;

    @Value("classpath:/ledger/sql/eod_ledger_transaction_diary_insert.sql")
    private final Resource transactionDiaryLedgerSql;

    private final RecordDateSetter recordDateSetter;

    private final TradeListQueryProvider tradeListQueryProvider;
    private final SODQueryProvider sodQueryProvider;
    private final NETQueryProvider netQueryProvider;

    @Bean(TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME)
    protected Step tradeTransactionDiaryLedger() {
        return getStep(TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME, transactionDiaryReader(tradeListQueryProvider), tradeTransactionDiaryLedgerProcessor);
    }

    @Bean(SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME)
    protected Step sodTransactionDiaryLedger() {
        return getStep(SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME, transactionDiaryReader(sodQueryProvider), sodTransactionDiaryLedgerProcessor);
    }

    @Bean(NET_TRANSACTION_DIARY_LEDGER_STEP_NAME)
    protected Step netTransactionDiaryLedger() {
        return getStep(NET_TRANSACTION_DIARY_LEDGER_STEP_NAME, transactionDiaryReader(netQueryProvider), netTransactionDiaryLedgerProcessor);
    }

    @Bean
    @SneakyThrows
    protected ItemWriter<TransactionDiary> transactionDiaryWriter() {
        return new JdbcBatchItemWriterBuilder<TransactionDiary>()
            .beanMapped()
            .sql(IOUtils.toString(transactionDiaryLedgerSql.getInputStream()))
            .dataSource(dataSource)
            .build();
    }

    private <T> Step getStep(final String transactionDiaryLedgerStepName, final ItemReader<T> tradeEntityItemReader,
        final TransactionDiaryLedgerProcessor<T> transactionDiaryLedgerProcessor) {
        return steps.get(transactionDiaryLedgerStepName)
            .listener(recordDateSetter)
            .<T, TransactionDiary>chunk(transactionDiaryChunkSize)
            .reader(tradeEntityItemReader)
            .processor(transactionDiaryLedgerProcessor)
            .writer(transactionDiaryWriter())
            .build();
    }

    private <T> ItemReader<T> transactionDiaryReader(final JpaQueryProvider queryProvider) {
        //todo: hibernate cursor ?
        return new JpaPagingItemReaderBuilder<T>()
            .pageSize(transactionDiaryChunkSize)
            .entityManagerFactory(entityManagerFactory)
            .saveState(false)
            .transacted(false)
            .queryProvider(queryProvider)
            .build();
    }
}
