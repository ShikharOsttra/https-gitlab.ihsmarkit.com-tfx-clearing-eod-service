package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.OPEN_POSITIONS_LEDGER_STEP_NAME;

import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.OpenPositionsLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.OpenPositionsQueryProvider;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ledger.OpenPositionsListItem;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class OpenPositionsLedgerConfig {

    private final OpenPositionsLedgerProcessor openPositionsLedgerProcessor;
    private final OpenPositionsQueryProvider openPositionsQueryProvider;
    private final BaseLedgerConfigFactory baseLedgerConfigFactory;

    @Value("${eod.ledger.open.positions.chunk.size:1000}")
    private final int openPositionsChunkSize;
    @Value("classpath:/ledger/sql/eod_ledger_open_positions_insert.sql")
    private final Resource openPositionsLedgerSql;

    @Bean(OPEN_POSITIONS_LEDGER_STEP_NAME)
    protected Step openPositionsLedger() {
        return baseLedgerConfigFactory.<ParticipantAndCurrencyPair, OpenPositionsListItem>stepBuilder(OPEN_POSITIONS_LEDGER_STEP_NAME, openPositionsChunkSize)
            .reader(openPositionsLedgerReader())
            .processor(openPositionsLedgerProcessor)
            .writer(openPositionsLedgerWriter())
            .build();
    }

    @Bean
    protected JpaPagingItemReader<ParticipantAndCurrencyPair> openPositionsLedgerReader() {
        return baseLedgerConfigFactory.<ParticipantAndCurrencyPair>listReaderBuilder(openPositionsQueryProvider, openPositionsChunkSize)
            .transacted(true)
            .build();
    }

    @Bean
    protected ItemWriter<OpenPositionsListItem> openPositionsLedgerWriter() {
        return baseLedgerConfigFactory.listWriter(openPositionsLedgerSql);
    }
}
