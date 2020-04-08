package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.PARTICIPANT_TOTAL_CONTEXT_KEY;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TFX_TOTAL_CONTEXT_KEY;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_LIST_LEDGER_STEP_NAME;

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

import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralListInputProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralListMapProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralListQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralListTotalProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain.CollateralListParticipantTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain.CollateralListTfxTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain.CollateralListWriteItem;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.BigDecimalTotalValue;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.MapTotalHolder;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TotalWriterListener;

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

    private final CollateralListInputProcessor inputProcessor;
    private final CollateralListMapProcessor mapProcessor;
    private final LedgerStepFactory ledgerStepFactory;

    @Bean(COLLATERAL_LIST_LEDGER_STEP_NAME)
    Step collateralListLedger() {
        return ledgerStepFactory.<CollateralBalanceEntity, CollateralListWriteItem>stepBuilder(COLLATERAL_LIST_LEDGER_STEP_NAME, collateralListChunkSize)
            .reader(collateralListReader())
            .processor(collateralListItemProcessor())
            .writer(collateralListWriter())
            .taskExecutor(collateralListTaskExecutor())
            .stream(collateralListParticipantTotalHolder())
            .stream(collateralListTfxTotalHolder())
            .listener(collateralListTotalWriterListener())
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
    @StepScope
    ItemProcessor<CollateralBalanceEntity, CollateralListWriteItem> collateralListItemProcessor() {
        return LedgerStepFactory.compositeProcessor(
            inputProcessor,
            collateralListTotalProcessor(),
            mapProcessor
        );
    }

    @Bean
    @StepScope
    CollateralListTotalProcessor collateralListTotalProcessor() {
        return new CollateralListTotalProcessor(
            collateralListTfxTotalHolder(),
            collateralListParticipantTotalHolder()
        );
    }

    @Bean
    @StepScope
    MapTotalHolder<CollateralListTfxTotalKey, BigDecimalTotalValue> collateralListTfxTotalHolder() {
        return new MapTotalHolder<>(TFX_TOTAL_CONTEXT_KEY, false);
    }

    @Bean
    @StepScope
    MapTotalHolder<CollateralListParticipantTotalKey, BigDecimalTotalValue> collateralListParticipantTotalHolder() {
        return new MapTotalHolder<>(PARTICIPANT_TOTAL_CONTEXT_KEY, false);
    }

    @Bean
    TotalWriterListener<CollateralListWriteItem> collateralListTotalWriterListener() {
        return new TotalWriterListener<>(
            collateralListParticipantTotalHolder(), mapProcessor::mapToParticipantTotal,
            collateralListTfxTotalHolder(), mapProcessor::mapToTfxTotal,
            collateralListWriter()
        );
    }

    @Bean
    ItemWriter<CollateralListWriteItem> collateralListWriter() {
        return ledgerStepFactory.listWriter(collateralListLedgerSql);
    }
}
