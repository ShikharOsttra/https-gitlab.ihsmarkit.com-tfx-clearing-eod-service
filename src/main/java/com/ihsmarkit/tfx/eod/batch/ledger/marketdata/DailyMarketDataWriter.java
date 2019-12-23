package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.eod.batch.ledger.marketdata.model.DailyMarkedDataProjection;
import com.ihsmarkit.tfx.eod.batch.ledger.marketdata.model.DailyMarketDataAggregate;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@StepScope
public class DailyMarketDataWriter implements ItemWriter<DailyMarkedDataProjection> {

    private final DailyMarketDataAggregator dailyMarketDataAggregator;
    private final JdbcBatchItemWriter<DailyMarketDataAggregate> writer;

    @SneakyThrows
    public DailyMarketDataWriter(
        final DailyMarketDataAggregator dailyMarketDataAggregator,
        final DataSource dataSource,
        @Value("classpath:/ledger/sql/eod_daily_market_data_insert.sql")
        final Resource dailyMarketDataSqlInsert
    ) {
        this.dailyMarketDataAggregator = dailyMarketDataAggregator;
        this.writer = new JdbcBatchItemWriterBuilder<DailyMarketDataAggregate>()
            .beanMapped()
            .sql(IOUtils.toString(dailyMarketDataSqlInsert.getInputStream()))
            .dataSource(dataSource)
            .build();
        this.writer.afterPropertiesSet();
    }


    @Override
    public void write(final List<? extends DailyMarkedDataProjection> items) throws Exception {
        final List<DailyMarketDataAggregate> aggregatedMarketData = dailyMarketDataAggregator.aggregate(items);
        log.debug("Persisting Daily Market Data:\n{}", aggregatedMarketData);

        writer.write(aggregatedMarketData);
    }
}
