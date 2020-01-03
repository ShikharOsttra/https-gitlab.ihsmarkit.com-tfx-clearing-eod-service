package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MONTHLY_TRADING_VOLUME_LEDGER_STEP_NAME;

import java.util.List;

import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.MonthlyTradingVolumeProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.MonthlyTradingVolumeReader;
import com.ihsmarkit.tfx.eod.model.ledger.MonthlyTradingVolumeItem;
import com.ihsmarkit.tfx.eod.support.ListItemWriter;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class MonthlyTradingVolumeLedgerConfig {

    private final LedgerStepFactory ledgerStepFactory;
    @Value("classpath:/ledger/sql/eod_ledger_monthly_trading_volume_insert.sql")
    private final Resource monthlyTradingVolumeLedgerSql;
    private final MonthlyTradingVolumeReader monthlyTradingVolumeReader;
    private final MonthlyTradingVolumeProcessor monthlyTradingVolumeProcessor;

    @Bean(MONTHLY_TRADING_VOLUME_LEDGER_STEP_NAME)
    protected Step monthlyTradingVolumeLedger() {
        return ledgerStepFactory
            .<List<ParticipantPositionEntity>, List<MonthlyTradingVolumeItem>>stepBuilder(MONTHLY_TRADING_VOLUME_LEDGER_STEP_NAME, 1)
            .reader(monthlyTradingVolumeReader)
            .processor(monthlyTradingVolumeProcessor)
            .writer(new ListItemWriter<>(monthlyTradingVolumeWriter()))
            .build();
    }

    @Bean
    protected ItemWriter<MonthlyTradingVolumeItem> monthlyTradingVolumeWriter() {
        return ledgerStepFactory.listWriter(monthlyTradingVolumeLedgerSql);
    }

}
