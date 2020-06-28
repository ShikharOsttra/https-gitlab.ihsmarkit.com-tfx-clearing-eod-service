package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.DAILY_MARKET_DATA_LEDGER_STEP_NAME;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.ihsmarkit.tfx.eod.batch.ledger.marketdata.DailyMarketDataProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.marketdata.DailyMarketDataReader;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarkedDataAggregated;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarketDataEnriched;
import com.ihsmarkit.tfx.eod.support.ListItemWriter;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@AllArgsConstructor
@Slf4j
public class DailyMarketDataLedgerConfig {

    private final DailyMarketDataReader dailyMarketDataReader;
    private final DailyMarketDataProcessor dailyMarketDataProcessor;
    @Value("classpath:/ledger/sql/eod_daily_market_data_insert.sql")
    private final Resource dailyMarketDataSqlInsert;
    private final LedgerStepFactory ledgerStepFactory;

    @PersistenceContext
    private EntityManager entityManager;

    @Bean(DAILY_MARKET_DATA_LEDGER_STEP_NAME)
    Step dailyMarketDataLedgerStep() {
        return ledgerStepFactory.<Map<String, DailyMarkedDataAggregated>, List<DailyMarketDataEnriched>>stepBuilder(DAILY_MARKET_DATA_LEDGER_STEP_NAME, 1)
            .reader(dailyMarketDataReader)
            .processor(dailyMarketDataProcessor)
            .writer(new ListItemWriter<>(dailyMarkedDataWriter()))
            .listener(new EntityManagerClearListener(entityManager))
            .build();
    }

    @Bean
    ItemWriter<DailyMarketDataEnriched> dailyMarkedDataWriter() {
        return ledgerStepFactory.listWriter(dailyMarketDataSqlInsert);
    }
}
