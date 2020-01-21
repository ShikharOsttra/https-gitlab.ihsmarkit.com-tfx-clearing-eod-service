package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.NET_TRANSACTION_DIARY_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TRANSACTION_DIARY_LEDGER_FLOW_NAME;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.orm.JpaQueryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.NETQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.NETTransactionDiaryLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.SODQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.SODTransactionDiaryLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.TradeListQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary.TradeTransactionDiaryLedgerProcessor;
import com.ihsmarkit.tfx.eod.support.ListItemWriter;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class TransactionDiaryLedgerConfig {

    private final LedgerStepFactory ledgerStepFactory;

    @Value("${eod.ledger.transaction.diary.chunk.size:1000}")
    private final int transactionDiaryChunkSize;
    @Value("classpath:/ledger/sql/eod_ledger_transaction_diary_insert.sql")
    private final Resource transactionDiaryLedgerSql;

    private final TradeTransactionDiaryLedgerProcessor tradeTransactionDiaryLedgerProcessor;
    private final SODTransactionDiaryLedgerProcessor sodTransactionDiaryLedgerProcessor;
    private final NETTransactionDiaryLedgerProcessor netTransactionDiaryLedgerProcessor;

    private final TradeListQueryProvider tradeListQueryProvider;
    private final SODQueryProvider sodQueryProvider;
    private final NETQueryProvider netQueryProvider;

    @Bean(TRANSACTION_DIARY_LEDGER_FLOW_NAME)
    Flow transactionDiaryLedger() {
        return new FlowBuilder<SimpleFlow>(TRANSACTION_DIARY_LEDGER_FLOW_NAME)
            .start(sodTransactionDiaryLedger())
            .next(tradeTransactionDiaryLedger())
            .next(netTransactionDiaryLedger())
            .build();
    }

    @Bean(TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME)
    Step tradeTransactionDiaryLedger() {
        return getStep(
            TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME,
            transactionDiaryReader(tradeListQueryProvider),
            tradeTransactionDiaryLedgerProcessor,
            new ListItemWriter<>(transactionDiaryWriter())
        );
    }

    @Bean(SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME)
    Step sodTransactionDiaryLedger() {
        return getStep(
            SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME,
            transactionDiaryReader(sodQueryProvider),
            sodTransactionDiaryLedgerProcessor,
            transactionDiaryWriter()
        );
    }

    @Bean(NET_TRANSACTION_DIARY_LEDGER_STEP_NAME)
    Step netTransactionDiaryLedger() {
        return getStep(
            NET_TRANSACTION_DIARY_LEDGER_STEP_NAME,
            transactionDiaryReader(netQueryProvider),
            netTransactionDiaryLedgerProcessor,
            transactionDiaryWriter()
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
        final ItemWriter<O> writer
    ) {
        return ledgerStepFactory.<T, O>stepBuilder(transactionDiaryLedgerStepName, transactionDiaryChunkSize)
            .reader(tradeEntityItemReader)
            .processor(transactionDiaryLedgerProcessor)
            .writer(writer)
            .build();
    }

    private <T> ItemReader<T> transactionDiaryReader(final JpaQueryProvider queryProvider) {
        return ledgerStepFactory.listReader(queryProvider, transactionDiaryChunkSize);
    }
}
