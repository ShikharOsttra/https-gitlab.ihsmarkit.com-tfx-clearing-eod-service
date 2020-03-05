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

import com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.OpenPositionsLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.OpenPositionsQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.openpositions.total.OpenPositionsTotalProcessor;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ledger.OpenPositionsListItem;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class OpenPositionsLedgerConfig {

    private final OpenPositionsLedgerProcessor openPositionsLedgerProcessor;
    private final OpenPositionsQueryProvider openPositionsQueryProvider;
    private final OpenPositionsTotalProcessor openPositionsTotalProcessor;
    private final LedgerStepFactory ledgerStepFactory;

    @Value("${eod.ledger.open.positions.chunk.size:1000}")
    private final int openPositionsChunkSize;
    @Value("classpath:/ledger/sql/eod_ledger_open_positions_insert.sql")
    private final Resource openPositionsLedgerSql;

    @Bean(OPEN_POSITIONS_LEDGER_STEP_NAME)
    Step openPositionsLedger() {
        return ledgerStepFactory.<ParticipantAndCurrencyPair, OpenPositionsListItem<String>>stepBuilder(OPEN_POSITIONS_LEDGER_STEP_NAME, openPositionsChunkSize)
            .reader(openPositionsLedgerReader())
            .processor(openPositionsLedgerItemProcessor())
            .writer(openPositionsLedgerWriter())
            .build();
    }

    @Bean
    JpaPagingItemReader<ParticipantAndCurrencyPair> openPositionsLedgerReader() {
        return ledgerStepFactory.<ParticipantAndCurrencyPair>listReaderBuilder(openPositionsQueryProvider, openPositionsChunkSize)
            .transacted(true)
            .build();
    }

    @Bean
    @StepScope
    ItemProcessor<ParticipantAndCurrencyPair, OpenPositionsListItem<String>> openPositionsLedgerItemProcessor() {
        return openPositionsTotalProcessor.wrapItemProcessor(
            openPositionsLedgerProcessor,
            openPositionsListItem ->
                OpenPositionsListItem.<String>builder()
                    .businessDate(openPositionsListItem.getBusinessDate())
                    .tradeDate(openPositionsListItem.getTradeDate())
                    .recordDate(openPositionsListItem.getRecordDate())
                    .participantCode(openPositionsListItem.getParticipantCode())
                    .participantName(openPositionsListItem.getParticipantName())
                    .participantType(openPositionsListItem.getParticipantType())
                    .currencyNo(openPositionsListItem.getCurrencyNo())
                    .currencyCode(openPositionsListItem.getCurrencyCode())
                    .shortPositionPreviousDay(openPositionsListItem.getShortPositionPreviousDay())
                    .longPositionPreviousDay(openPositionsListItem.getLongPositionPreviousDay())
                    .sellTradingAmount(openPositionsListItem.getSellTradingAmount())
                    .buyTradingAmount(openPositionsListItem.getBuyTradingAmount())
                    .shortPosition(openPositionsListItem.getShortPosition())
                    .longPosition(openPositionsListItem.getLongPosition())
                    .settlementDate(openPositionsListItem.getSettlementDate())
                    .orderId(openPositionsListItem.getOrderId())
                    .recordType(openPositionsListItem.getRecordType())

                    .initialMtmAmount(LedgerFormattingUtils.formatBigDecimal(openPositionsListItem.getInitialMtmAmount()))
                    .dailyMtmAmount(LedgerFormattingUtils.formatBigDecimal(openPositionsListItem.getDailyMtmAmount()))
                    .swapPoint(LedgerFormattingUtils.formatBigDecimal(openPositionsListItem.getSwapPoint()))
                    .totalVariationMargin(LedgerFormattingUtils.formatBigDecimal(openPositionsListItem.getTotalVariationMargin()))
                    .build()
        );
    }

    @Bean
    @StepScope
    ItemWriter<OpenPositionsListItem<String>> openPositionsLedgerWriter() {
        return openPositionsTotalProcessor.wrapItemWriter(openPositionsJdbcLedgerWriter());
    }

    @Bean
    ItemWriter<OpenPositionsListItem<String>> openPositionsJdbcLedgerWriter() {
        return ledgerStepFactory.listWriter(openPositionsLedgerSql);
    }

}
