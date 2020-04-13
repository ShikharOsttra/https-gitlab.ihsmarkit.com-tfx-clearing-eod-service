package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.NET_TRANSACTION_DIARY_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TRANSACTION_DIARY_LEDGER_FLOW_NAME;

import java.time.LocalDate;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.HibernateCursorItemReader;
import org.springframework.batch.item.database.builder.HibernateCursorItemReaderBuilder;
import org.springframework.batch.item.database.orm.JpaQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantAndCurrencyPairQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.NETTransactionDiaryLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.SODTransactionDiaryLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.TradeListQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.TradeTransactionDiaryLedgerProcessor;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class TransactionDiaryLedgerConfig {

    private final LedgerStepFactory ledgerStepFactory;

    @Value("${eod.ledger.transaction.diary.chunk.size:1000}")
    private final int transactionDiaryChunkSize;
    @Value("${eod.ledger.transaction.diary.concurrency.limit:1}")
    private final int transactionDiaryConcurrencyLimit;
    @Value("classpath:/ledger/sql/eod_ledger_transaction_diary_insert.sql")
    private final Resource transactionDiaryLedgerSql;

    private final TradeTransactionDiaryLedgerProcessor tradeTransactionDiaryLedgerProcessor;
    private final SODTransactionDiaryLedgerProcessor sodTransactionDiaryLedgerProcessor;
    private final NETTransactionDiaryLedgerProcessor netTransactionDiaryLedgerProcessor;

    private final TradeListQueryProvider tradeListQueryProvider;
    private final ParticipantAndCurrencyPairQueryProvider participantAndCurrencyPairQueryProvider;

    @Bean
    TaskExecutor transactionDiaryTaskExecutor() {
        final SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(transactionDiaryConcurrencyLimit);
        return taskExecutor;
    }

    @Bean(TRANSACTION_DIARY_LEDGER_FLOW_NAME)
    Flow transactionDiaryLedger(final @Qualifier(TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME) Step tradeTransactionDiaryLedger) {
        return new FlowBuilder<SimpleFlow>(TRANSACTION_DIARY_LEDGER_FLOW_NAME)
            .start(sodTransactionDiaryLedger())
            .next(tradeTransactionDiaryLedger)
            .next(netTransactionDiaryLedger())
            .build();
    }

    @Bean(TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME)
    Step tradeTransactionDiaryLedger() {
        return getStep(
            TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME,
            transactionDiaryReader(tradeListQueryProvider, false),
            tradeTransactionDiaryLedgerProcessor,
            transactionDiaryWriter(),
            transactionDiaryTaskExecutor()
        );
    }

//    @Bean
//    @StepScope
//    HibernateCursorItemReader<TradeEntity> tradeReader(@Value("#{jobParameters['businessDate']}") LocalDate businessDate,
//        EntityManagerFactory entityManagerFactory) {
//        return new HibernateCursorItemReaderBuilder<TradeEntity>()
//            .entityClass(TradeEntity.class)
//            .useStatelessSession(true)
//            .saveState(false)
//            .queryString(
//                "FROM TradeEntity trade " +
//                    "JOIN FETCH trade.currencyPair " +
//                    "JOIN FETCH trade.originator originator " +
//                    "JOIN FETCH originator.participant " +
//                    "JOIN FETCH trade.counterparty counterparty " +
//                    "JOIN FETCH counterparty.participant " +
//                    "WHERE trade.tradeDate=:date and trade.clearingStatus=com.ihsmarkit.tfx.core.domain.type.ClearingStatus.NOVATED")
//            .parameterValues(Map.of("date", businessDate))
//            .sessionFactory(entityManagerFactory.unwrap(SessionFactory.class))
//            .build();
//    }

    @Bean(SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME)
    Step sodTransactionDiaryLedger() {
        return getStep(
            SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME,
            transactionDiaryReader(participantAndCurrencyPairQueryProvider, true),
            sodTransactionDiaryLedgerProcessor,
            transactionDiaryWriter(),
            transactionDiaryTaskExecutor()
        );
    }

    @Bean(NET_TRANSACTION_DIARY_LEDGER_STEP_NAME)
    Step netTransactionDiaryLedger() {
        return getStep(
            NET_TRANSACTION_DIARY_LEDGER_STEP_NAME,
            transactionDiaryReader(participantAndCurrencyPairQueryProvider, true),
            netTransactionDiaryLedgerProcessor,
            transactionDiaryWriter(),
            transactionDiaryTaskExecutor()
        );
    }

    @Bean
    <O> ItemWriter<O> transactionDiaryWriter() {
        return ledgerStepFactory.listWriter(transactionDiaryLedgerSql);
    }

    private <T, O> Step getStep(
        final String transactionDiaryLedgerStepName,
        final ItemReader<T> tradeEntityItemReader,
        final ItemProcessor<T, O> transactionDiaryLedgerProcessor,
        final ItemWriter<O> writer,
        final TaskExecutor taskExecutor
    ) {
        return ledgerStepFactory.<T, O>stepBuilder(transactionDiaryLedgerStepName, transactionDiaryChunkSize)
            .reader(tradeEntityItemReader)
            .processor(transactionDiaryLedgerProcessor)
            .writer(writer)
            .taskExecutor(taskExecutor)
            .build();
    }

    private <T> ItemReader<T> transactionDiaryReader(final JpaQueryProvider queryProvider, final boolean transacted) {
        return ledgerStepFactory.<T>listReaderBuilder(queryProvider, transactionDiaryChunkSize)
            .transacted(transacted)
            .build();
    }
}
