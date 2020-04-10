package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.PARTICIPANT_TOTAL_CONTEXT_KEY;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TFX_TOTAL_CONTEXT_KEY;
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
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.MapTotalHolder;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.TotalWriterListener;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.LastTradingDateInMonthDecider;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.MonthlyTradingVolumeInputProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.MonthlyTradingVolumeMapProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.MonthlyTradingVolumeTotalProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.domain.MonthlyTradingVolumeParticipantTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.domain.MonthlyTradingVolumeWriteItem;
import com.ihsmarkit.tfx.eod.model.BuySellAmounts;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;

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
    private final MonthlyTradingVolumeMapProcessor mapProcessor;
    private final MonthlyTradingVolumeInputProcessor inputProcessor;
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
        return ledgerStepFactory.<ParticipantAndCurrencyPair, MonthlyTradingVolumeWriteItem>stepBuilder(MONTHLY_TRADING_VOLUME_LEDGER_STEP_NAME,
            monthlyTradingVolumeChunkSize)
            .reader(monthlyTradingVolumeReader())
            .processor(monthlyTradingVolumeItemProcessor())
            .writer(monthlyTradingVolumeWriter())
            .stream(monthlyTradingVolumeTfxTotalHolder())
            .stream(monthlyTradingVolumeParticipantTotalHolder())
            .listener(monthlyTradingVolumeTotalWriterListener())
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
    ItemProcessor<ParticipantAndCurrencyPair, MonthlyTradingVolumeWriteItem> monthlyTradingVolumeItemProcessor() {
        return LedgerStepFactory.compositeProcessor(
            inputProcessor,
            monthlyTradingVolumeTotalProcessor(),
            mapProcessor
        );
    }

    @Bean
    @StepScope
    MonthlyTradingVolumeTotalProcessor monthlyTradingVolumeTotalProcessor() {
        return new MonthlyTradingVolumeTotalProcessor(
            monthlyTradingVolumeTfxTotalHolder(),
            monthlyTradingVolumeParticipantTotalHolder()
        );
    }

    @Bean
    @StepScope
    MapTotalHolder<String, BuySellAmounts> monthlyTradingVolumeTfxTotalHolder() {
        return new MapTotalHolder<>(TFX_TOTAL_CONTEXT_KEY, false);
    }

    @Bean
    @StepScope
    MapTotalHolder<MonthlyTradingVolumeParticipantTotalKey, BuySellAmounts> monthlyTradingVolumeParticipantTotalHolder() {
        return new MapTotalHolder<>(PARTICIPANT_TOTAL_CONTEXT_KEY, false);
    }

    @Bean
    TotalWriterListener<MonthlyTradingVolumeWriteItem> monthlyTradingVolumeTotalWriterListener() {
        return new TotalWriterListener<>(
            monthlyTradingVolumeTfxTotalHolder(), mapProcessor::mapTfxTotal,
            monthlyTradingVolumeParticipantTotalHolder(), mapProcessor::mapParticipantTotal,
            monthlyTradingVolumeWriter()
        );
    }

    @Bean
    ItemWriter<MonthlyTradingVolumeWriteItem> monthlyTradingVolumeWriter() {
        return ledgerStepFactory.listWriter(monthlyTradingVolumeLedgerSql);
    }
}
