package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MONTHLY_TRADING_VOLUME_LEDGER_FLOW_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MONTHLY_TRADING_VOLUME_LEDGER_STEP_NAME;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantAndCurrencyPairQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.LastTradingDateInMonthDecider;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.MonthlyTradingVolumeProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.MonthlyTradingVolumeTotalProcessor;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ledger.MonthlyTradingVolumeItem;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class MonthlyTradingVolumeLedgerConfig {

    private final LedgerStepFactory ledgerStepFactory;
    @Value("classpath:/ledger/sql/eod_ledger_monthly_trading_volume_insert.sql")
    private final Resource monthlyTradingVolumeLedgerSql;
    @Value("${eod.ledger.monthly.trading.volume.chunk.size:1000}")
    private final int monthlyTradingVolumeChunkSize;
    private final ParticipantAndCurrencyPairQueryProvider participantAndCurrencyPairQueryProvider;
    private final MonthlyTradingVolumeProcessor monthlyTradingVolumeProcessor;
    private final MonthlyTradingVolumeTotalProcessor monthlyTradingVolumeTotalProcessor;
    private final LastTradingDateInMonthDecider lastTradingDateInMonthDecider;

    @Bean(MONTHLY_TRADING_VOLUME_LEDGER_FLOW_NAME)
    Flow monthlyTradingVolumeLedgerFlow() {
        return new FlowBuilder<SimpleFlow>(MONTHLY_TRADING_VOLUME_LEDGER_FLOW_NAME)
            .start(lastTradingDateInMonthDecider).on(TRUE.toString()).to(monthlyTradingVolumeLedger())
            .from(lastTradingDateInMonthDecider).on(FALSE.toString()).end()
            .build();
    }

    @Bean(MONTHLY_TRADING_VOLUME_LEDGER_STEP_NAME)
    Step monthlyTradingVolumeLedger() {
        return ledgerStepFactory.<ParticipantAndCurrencyPair, MonthlyTradingVolumeItem<String>>stepBuilder(MONTHLY_TRADING_VOLUME_LEDGER_STEP_NAME,
            monthlyTradingVolumeChunkSize)
            .reader(monthlyTradingVolumeReader())
            .processor(monthlyTradingVolumeItemProcessor())
            .writer(monthlyTradingVolumeWriter())
            .build();
    }

    @Bean
    JpaPagingItemReader<ParticipantAndCurrencyPair> monthlyTradingVolumeReader() {
        return ledgerStepFactory.<ParticipantAndCurrencyPair>listReaderBuilder(participantAndCurrencyPairQueryProvider, monthlyTradingVolumeChunkSize)
            .transacted(true)
            .build();
    }

    @Bean
    @StepScope
    ItemProcessor<ParticipantAndCurrencyPair, MonthlyTradingVolumeItem<String>> monthlyTradingVolumeItemProcessor() {
        return monthlyTradingVolumeTotalProcessor.wrapItemProcessor(
            monthlyTradingVolumeProcessor,
            monthlyTradingVolumeItem ->
                MonthlyTradingVolumeItem.<String>builder()
                    .businessDate(monthlyTradingVolumeItem.getBusinessDate())
                    .tradeDate(monthlyTradingVolumeItem.getTradeDate())
                    .recordDate(monthlyTradingVolumeItem.getRecordDate())
                    .participantCode(monthlyTradingVolumeItem.getParticipantCode())
                    .participantName(monthlyTradingVolumeItem.getParticipantName())
                    .participantType(monthlyTradingVolumeItem.getParticipantType())
                    .currencyPairNumber(monthlyTradingVolumeItem.getCurrencyPairNumber())
                    .currencyPairCode(monthlyTradingVolumeItem.getCurrencyPairCode())
                    .recordType(monthlyTradingVolumeItem.getRecordType())
                    .orderId(monthlyTradingVolumeItem.getOrderId())

                    .buyTradingVolumeInUnit(monthlyTradingVolumeItem.getBuyTradingVolumeInUnit().toString())
                    .sellTradingVolumeInUnit(monthlyTradingVolumeItem.getSellTradingVolumeInUnit().toString())
                    .build()
        );
    }

    @Bean
    @StepScope
    ItemWriter<MonthlyTradingVolumeItem<String>> monthlyTradingVolumeWriter() {
        return monthlyTradingVolumeTotalProcessor.wrapItemWriter(monthlyTradingVolumeJdbcWriter());
    }

    @Bean
    ItemWriter<MonthlyTradingVolumeItem<String>> monthlyTradingVolumeJdbcWriter() {
        return ledgerStepFactory.listWriter(monthlyTradingVolumeLedgerSql);
    }
}
