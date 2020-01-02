package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.DAILY_MARKET_DATA_LEDGER_STEP_NAME;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.ihsmarkit.tfx.eod.batch.ledger.RecordDateSetter;
import com.ihsmarkit.tfx.eod.batch.ledger.marketdata.DailyMarketDataProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.marketdata.DailyMarketDataReader;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarkedDataAggregated;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarketDataEnriched;
import com.ihsmarkit.tfx.eod.support.ListItemWriter;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Configuration
@AllArgsConstructor
public class DailyMarketDataLedgerConfig {

    private final StepBuilderFactory steps;
    private final RecordDateSetter recordDateSetter;
    private final DailyMarketDataReader dailyMarketDataReader;
    private final DailyMarketDataProcessor dailyMarketDataProcessor;
    private final DataSource dataSource;
    @Value("classpath:/ledger/sql/eod_daily_market_data_insert.sql")
    private final Resource dailyMarketDataSqlInsert;

    @Bean(DAILY_MARKET_DATA_LEDGER_STEP_NAME)
    protected Step dailyMarketDataLedgerStep() {
        return steps.get(DAILY_MARKET_DATA_LEDGER_STEP_NAME)
            .listener(recordDateSetter)
            .<Map<String, DailyMarkedDataAggregated>, List<DailyMarketDataEnriched>>chunk(1)
            .reader(dailyMarketDataReader)
            .processor(dailyMarketDataProcessor)
            .writer(new ListItemWriter<>(dailyMarkedDataWriter()))
            .build();
    }

    @SneakyThrows
    @Bean
    protected ItemWriter<DailyMarketDataEnriched> dailyMarkedDataWriter() {
        return new JdbcBatchItemWriterBuilder<DailyMarketDataEnriched>()
            .beanMapped()
            .sql(IOUtils.toString(dailyMarketDataSqlInsert.getInputStream()))
            .dataSource(dataSource)
            .build();
    }
}
