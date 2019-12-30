package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.domain.type.ClearingStatus;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarkedDataProjection;

@Component
@StepScope
public class DailyMarketDataReader extends JdbcCursorItemReader<DailyMarkedDataProjection> {

    private static final String TRADE_ID = "trade_id";
    private static final String CURRENCY_PAIR_ID = "currency_pair_id";
    private static final String PRODUCT_CODE = "product_code";
    private static final String PRODUCT_NUMBER = "product_number";
    private static final String VERSION_TIMESTAMP = "version_timestamp";
    private static final String VALUE_AMOUNT_VALUE = "value_amount_value";

    private static final String SQL_QUERY =
        "SELECT t.id AS trade_id, t.currency_pair_id AS currency_pair_id, t.product_code AS product_code, product.product_number as product_number, "
            + "t.version_tsp AS version_timestamp, t.value_ccy_amt AS value_amount_value"
            + " FROM trade t "
            + " JOIN fx_spot_product product ON product.id = t.currency_pair_id "
            + " WHERE t.trade_date = ? AND t.clearing_status = ?";

    public DailyMarketDataReader(
        final DataSource dataSource,
        @Value("#{jobParameters['businessDate']}")
        final LocalDate businessDate
    ) {
        setDataSource(dataSource);
        setSql(SQL_QUERY);
        setRowMapper(new DailyMarkedDataRowMapper());
        setPreparedStatementSetter(ps -> {
            ps.setDate(1, Date.valueOf(businessDate));
            ps.setString(2, ClearingStatus.NOVATED.name());
        });
    }

    private static class DailyMarkedDataRowMapper implements RowMapper<DailyMarkedDataProjection> {

        @Override
        public DailyMarkedDataProjection mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return DailyMarkedDataProjection.builder()
                .tradeId(rs.getLong(TRADE_ID))
                .currencyPairId(rs.getLong(CURRENCY_PAIR_ID))
                .currencyPairCode(rs.getString(PRODUCT_CODE))
                .productNumber(rs.getString(PRODUCT_NUMBER))
                .valueAmount(rs.getBigDecimal(VALUE_AMOUNT_VALUE))
                .versionTsp(rs.getTimestamp(VERSION_TIMESTAMP).toLocalDateTime())
                .build();
        }
    }

}
