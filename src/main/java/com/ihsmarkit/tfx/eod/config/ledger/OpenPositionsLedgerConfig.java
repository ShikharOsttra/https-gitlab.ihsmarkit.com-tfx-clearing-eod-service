package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.OPEN_POSITIONS_LEDGER_STEP_NAME;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantAndCurrencyPairQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.MapTotalHolder;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TotalWriterListener;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.OpenPositionsInputProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.OpenPositionsMapProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.OpenPositionsTotalProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain.OpenPositionsParticipantTotal;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain.OpenPositionsParticipantTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain.OpenPositionsTfxTotal;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.domain.OpenPositionsWriteItem;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class OpenPositionsLedgerConfig {

    private final OpenPositionsInputProcessor inputProcessor;
    private final OpenPositionsMapProcessor mapProcessor;

    private final ParticipantAndCurrencyPairQueryProvider participantAndCurrencyPairQueryProvider;

    private final LedgerStepFactory ledgerStepFactory;

    @Value("${eod.ledger.open.positions.chunk.size:1000}")
    private final int openPositionsChunkSize;
    @Value("classpath:/ledger/sql/eod_ledger_open_positions_insert.sql")
    private final Resource openPositionsLedgerSql;

    @Bean(OPEN_POSITIONS_LEDGER_STEP_NAME)
    Step openPositionsLedger() {
        return ledgerStepFactory.<ParticipantAndCurrencyPair, OpenPositionsWriteItem>stepBuilder(OPEN_POSITIONS_LEDGER_STEP_NAME, openPositionsChunkSize)
            .reader(openPositionsLedgerReader())
            .processor(openPositionsLedgerItemProcessor())
            .writer(openPositionsLedgerWriter())
            .stream(tfxTotalHolder())
            .stream(participantTotalHolder())
            .listener(totalWriterListener())
            .build();
    }

    @Bean
    JpaPagingItemReader<ParticipantAndCurrencyPair> openPositionsLedgerReader() {
        return ledgerStepFactory.<ParticipantAndCurrencyPair>listReaderBuilder(participantAndCurrencyPairQueryProvider, openPositionsChunkSize)
            .transacted(true)
            .build();
    }

    @Bean
    @StepScope
    ItemProcessor<ParticipantAndCurrencyPair, OpenPositionsWriteItem> openPositionsLedgerItemProcessor() {
        return LedgerStepFactory.compositeProcessor(
            inputProcessor,
            openPositionsTotalProcessor(),
            mapProcessor
        );
    }

    @Bean
    @StepScope
    OpenPositionsTotalProcessor openPositionsTotalProcessor() {
        return new OpenPositionsTotalProcessor(
            tfxTotalHolder(),
            participantTotalHolder()
        );
    }

    @Bean
    @StepScope
    MapTotalHolder<String, OpenPositionsTfxTotal> tfxTotalHolder() {
        return new MapTotalHolder<>("tfxTotal");
    }

    @Bean
    @StepScope
    MapTotalHolder<OpenPositionsParticipantTotalKey, OpenPositionsParticipantTotal> participantTotalHolder() {
        return new MapTotalHolder<>("participantTotal");
    }

    @Bean
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    TotalWriterListener<OpenPositionsWriteItem> totalWriterListener() {
        return new TotalWriterListener<>(
            openPositionsTotalProcessor()::getParticipantTotal, mapProcessor::mapToParticipantTotal,
            openPositionsTotalProcessor()::getTfxTotal, mapProcessor::mapToTfxTotal,
            openPositionsLedgerWriter()
        );
    }

    @Bean
    ItemWriter<OpenPositionsWriteItem> openPositionsLedgerWriter() {
        return ledgerStepFactory.listWriter(openPositionsLedgerSql);
    }

}
