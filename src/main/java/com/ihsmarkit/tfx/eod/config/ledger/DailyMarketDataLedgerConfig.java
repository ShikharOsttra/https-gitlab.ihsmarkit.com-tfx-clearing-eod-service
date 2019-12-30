package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.DAILY_MARKET_DATA_LEDGER_STEP_NAME;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ihsmarkit.tfx.eod.batch.ledger.RecordDateSetter;
import com.ihsmarkit.tfx.eod.batch.ledger.marketdata.DailyMarketDataReader;
import com.ihsmarkit.tfx.eod.batch.ledger.marketdata.DailyMarketDataWriter;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarkedDataProjection;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class DailyMarketDataLedgerConfig {

    private final StepBuilderFactory steps;
    private final RecordDateSetter recordDateSetter;
    private final DailyMarketDataReader dailyMarketDataReader;
    private final DailyMarketDataWriter dailyMarketDataWriter;

    @Bean(DAILY_MARKET_DATA_LEDGER_STEP_NAME)
    protected Step dailyMarketDataLedgerStep() {
        return steps.get(DAILY_MARKET_DATA_LEDGER_STEP_NAME)
            .listener(recordDateSetter)
            // we need to read all records before aggregation
            .<DailyMarkedDataProjection, DailyMarkedDataProjection>chunk(Integer.MAX_VALUE)
            .reader(dailyMarketDataReader)
            .writer(dailyMarketDataWriter)
            .build();
    }
}
