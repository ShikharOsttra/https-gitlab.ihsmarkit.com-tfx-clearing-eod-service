package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.PARTICIPANT_TOTAL_CONTEXT_KEY;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TFX_TOTAL_CONTEXT_KEY;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.OPEN_POSITIONS_LEDGER_STEP_NAME;

import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantAndCurrencyPairQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.MapTotalHolder;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TotalWriterListener;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.RebalanceInputProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.RebalanceMapProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.RebalanceTotalProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceParticipantTotal;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceParticipantTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceTfxTotal;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceWriteItem;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@AllArgsConstructor
public class RebalanceLedgerConfig {

    private final RebalanceInputProcessor inputProcessor;
    private final RebalanceMapProcessor mapProcessor;

    private final ParticipantAndCurrencyPairQueryProvider participantAndCurrencyPairQueryProvider;

    private final LedgerStepFactory ledgerStepFactory;

    @Value("${eod.ledger.open.positions.chunk.size:1000}")
    private final int rebalanceChunkSize;
    @Value("classpath:/ledger/sql/eod_ledger_open_positions_insert.sql")
    private final Resource rebalanceLedgerSql;

    @Bean(OPEN_POSITIONS_LEDGER_STEP_NAME)
    Step rebalanceLedger() {
        return ledgerStepFactory.<ParticipantAndCurrencyPair, RebalanceWriteItem>stepBuilder(OPEN_POSITIONS_LEDGER_STEP_NAME, rebalanceChunkSize)
            .reader(rebalanceLedgerReader())
            .processor(rebalanceLedgerItemProcessor())
            .writer(rebalanceLedgerWriter())
            .stream(rebalanceTfxTotalHolder())
            .stream(rebalanceParticipantTotalHolder())
            .listener(rebalanceTotalWriterListener())
            .build();
    }

    @Bean
    JpaPagingItemReader<ParticipantAndCurrencyPair> rebalanceLedgerReader() {
        return ledgerStepFactory.<ParticipantAndCurrencyPair>listReaderBuilder(participantAndCurrencyPairQueryProvider, rebalanceChunkSize)
            .transacted(true)
            .build();
    }

    @Bean
    @StepScope
    ItemProcessor<ParticipantAndCurrencyPair, RebalanceWriteItem> rebalanceLedgerItemProcessor() {
        return LedgerStepFactory.compositeProcessor(
            inputProcessor,
            rebalanceTotalProcessor(),
            mapProcessor
        );
    }

    @Bean
    @StepScope
    RebalanceTotalProcessor rebalanceTotalProcessor() {
        return new RebalanceTotalProcessor(
            rebalanceTfxTotalHolder(),
            rebalanceParticipantTotalHolder()
        );
    }

    @Bean
    @StepScope
    MapTotalHolder<String, RebalanceTfxTotal> rebalanceTfxTotalHolder() {
        return new MapTotalHolder<>(TFX_TOTAL_CONTEXT_KEY, false);
    }

    @Bean
    @StepScope
    MapTotalHolder<RebalanceParticipantTotalKey, RebalanceParticipantTotal> rebalanceParticipantTotalHolder() {
        return new MapTotalHolder<>(PARTICIPANT_TOTAL_CONTEXT_KEY, false);
    }

    @Bean
    TotalWriterListener<RebalanceWriteItem> rebalanceTotalWriterListener() {
        return new TotalWriterListener<>(
            rebalanceParticipantTotalHolder(), mapProcessor::mapToParticipantTotal,
            rebalanceTfxTotalHolder(), mapProcessor::mapToTfxTotal,
            rebalanceLedgerWriter()
        );
    }

    @Bean
    ItemWriter<RebalanceWriteItem> rebalanceLedgerWriter() {
        return ledgerStepFactory.listWriter(rebalanceLedgerSql);
    }

}
