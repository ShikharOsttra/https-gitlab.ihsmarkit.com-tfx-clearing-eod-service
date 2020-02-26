package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.OPEN_POSITIONS_LEDGER_STEP_NAME;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.ihsmarkit.tfx.eod.batch.ledger.ItemWriterWithTotal;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.OpenPositionsLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.OpenPositionsQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.total.OpenPositionTotalSupplier;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ledger.OpenPositionsListItem;
import com.ihsmarkit.tfx.eod.support.BeforeStepListener;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class OpenPositionsLedgerConfig {

    private final OpenPositionsLedgerProcessor openPositionsLedgerProcessor;
    private final OpenPositionsQueryProvider openPositionsQueryProvider;
    private final LedgerStepFactory ledgerStepFactory;
    private final OpenPositionTotalSupplier openPositionTotalSupplier;

    @Value("${eod.ledger.open.positions.chunk.size:1000}")
    private final int openPositionsChunkSize;
    @Value("classpath:/ledger/sql/eod_ledger_open_positions_insert.sql")
    private final Resource openPositionsLedgerSql;

    @Bean(OPEN_POSITIONS_LEDGER_STEP_NAME)
    protected Step openPositionsLedger() {
        return ledgerStepFactory.<ParticipantAndCurrencyPair, OpenPositionsListItem>stepBuilder(OPEN_POSITIONS_LEDGER_STEP_NAME, openPositionsChunkSize,
            Set.of(initTotalMapListener()))
            .reader(openPositionsLedgerReader())
            .processor(openPositionsLedgerProcessor)
            .writer(openPositionsLedgerWriter())
            .build();
    }

    @Bean
    protected JpaPagingItemReader<ParticipantAndCurrencyPair> openPositionsLedgerReader() {
        return ledgerStepFactory.<ParticipantAndCurrencyPair>listReaderBuilder(openPositionsQueryProvider, openPositionsChunkSize)
            .transacted(true)
            .build();
    }

    @Bean
    protected ItemWriter<OpenPositionsListItem> openPositionsLedgerWriter() {
        return new ItemWriterWithTotal<>(
            openPositionTotalSupplier,
            openPositionsJdbcLedgerWriter()
        );
    }

    @Bean
    protected ItemWriter<OpenPositionsListItem> openPositionsJdbcLedgerWriter() {
        return ledgerStepFactory.listWriter(openPositionsLedgerSql);
    }

    private BeforeStepListener initTotalMapListener() {
        return new BeforeStepListener(stepExecution -> stepExecution.getExecutionContext().put("total", new ConcurrentHashMap<>()));
    }
}
