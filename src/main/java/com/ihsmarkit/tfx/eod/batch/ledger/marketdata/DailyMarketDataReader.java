package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarkedDataAggregated;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class DailyMarketDataReader implements ItemReader<Map<String, DailyMarkedDataAggregated>> {

    private static final Comparator<TradeEntity> OPEN_TRADE_COMPARATOR = Comparator.comparing(TradeEntity::getVersionTsp);
    private static final Comparator<TradeEntity> CLOSE_TRADE_COMPARATOR = Comparator.comparing(TradeEntity::getVersionTsp).reversed();
    private static final Comparator<TradeEntity> LOW_TRADE_COMPARATOR = Comparator.comparing(DailyMarketDataReader::valueAmountMapper);
    private static final Comparator<TradeEntity> HIGH_TRADE_COMPARATOR = Comparator.comparing(DailyMarketDataReader::valueAmountMapper).reversed();

    private final TradeRepository tradeRepository;
    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;
    private boolean isFinished;

    @Override
    @SuppressFBWarnings("USFW_UNSYNCHRONIZED_SINGLETON_FIELD_WRITES")
    public Map<String, DailyMarkedDataAggregated> read() {
        // workaround: https://stackoverflow.com/a/44278809/4760059
        if (isFinished) {
            return null;
        }
        isFinished = true;

        log.debug("Read trades for Daily Market Data Ledger");

        return tradeRepository.findAllNovatedForTradeDate(businessDate)
            .collect(
                Collectors.groupingBy(
                    trade -> trade.getCurrencyPair().getCode(),
                    Collector.of(TradeHolder::new, DailyMarketDataReader::addTrade, DailyMarketDataReader::merge, DailyMarketDataReader::finisher)
                )
            );
    }

    private static DailyMarkedDataAggregated finisher(final TradeHolder tradeHolder) {
        return DailyMarkedDataAggregated.builder()
            .openPriceTime(tradeHolder.getOpenTrade().getVersionTsp())
            .openPrice(tradeHolder.getOpenTrade().getValueAmount().getValue())

            .closePriceTime(tradeHolder.getCloseTrade().getVersionTsp())
            .closePrice(tradeHolder.getCloseTrade().getValueAmount().getValue())

            .lowPriceTime(tradeHolder.getLowTrade().getVersionTsp())
            .lowPrice(tradeHolder.getLowTrade().getValueAmount().getValue())

            .highPriceTime(tradeHolder.getHighTrade().getVersionTsp())
            .highPrice(tradeHolder.getHighTrade().getValueAmount().getValue())

            .tradingVolumeAmount(tradeHolder.getTotalBaseAmountSum())
            .currencyPairCode(tradeHolder.getCurrencyPairCode())

            .build();
    }

    private static void addTrade(final TradeHolder aggregate, final TradeEntity nextTrade) {
        aggregate.setOpenTrade(findMinBy(Stream.of(aggregate.openTrade, nextTrade), OPEN_TRADE_COMPARATOR));
        aggregate.setCloseTrade(findMinBy(Stream.of(aggregate.closeTrade, nextTrade), CLOSE_TRADE_COMPARATOR));
        aggregate.setLowTrade(findMinBy(Stream.of(aggregate.lowTrade, nextTrade), LOW_TRADE_COMPARATOR));
        aggregate.setHighTrade(findMinBy(Stream.of(aggregate.highTrade, nextTrade), HIGH_TRADE_COMPARATOR));
        aggregate.setCurrencyPairCode(nextTrade.getCurrencyPair().getCode());

        final BigDecimal totalBaseAmount = aggregate.getTotalBaseAmountSum() == null
            ? BigDecimal.ZERO
            : aggregate.getTotalBaseAmountSum();

        aggregate.setTotalBaseAmountSum(totalBaseAmount.add(nextTrade.getBaseAmount().getValue()));
    }

    private static TradeHolder merge(final TradeHolder a, final TradeHolder b) {
        return new TradeHolder(
            findMinBy(concatTrades(a, b), OPEN_TRADE_COMPARATOR),
            findMinBy(concatTrades(a, b), CLOSE_TRADE_COMPARATOR),
            findMinBy(concatTrades(a, b), HIGH_TRADE_COMPARATOR),
            findMinBy(concatTrades(a, b), LOW_TRADE_COMPARATOR),
            a.getTotalBaseAmountSum().add(b.getTotalBaseAmountSum()),
            a.getCurrencyPairCode()
        );
    }

    private static TradeEntity findMinBy(
        final Stream<? extends TradeEntity> stream, final Comparator<TradeEntity> comparator
    ) {
        return stream
            .filter(Objects::nonNull)
            .min(comparator)
            .orElseThrow(() -> new IllegalStateException("Can't find min item by comparator"));
    }

    private static Stream<TradeEntity> concatTrades(final TradeHolder a, final TradeHolder b) {
        return Stream.of(
            a.openTrade, a.closeTrade, a.highTrade, a.lowTrade,
            b.openTrade, b.closeTrade, b.highTrade, b.lowTrade
        );
    }

    private static BigDecimal valueAmountMapper(final TradeEntity trade) {
        return trade.getValueAmount().getValue();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TradeHolder {
        private TradeEntity openTrade;
        private TradeEntity closeTrade;
        private TradeEntity highTrade;
        private TradeEntity lowTrade;
        @Nullable
        private BigDecimal totalBaseAmountSum;
        private String currencyPairCode;
    }

}
