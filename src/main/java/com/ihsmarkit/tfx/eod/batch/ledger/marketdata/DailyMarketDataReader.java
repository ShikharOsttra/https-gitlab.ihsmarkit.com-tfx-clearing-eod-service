package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import static com.ihsmarkit.tfx.core.domain.type.TransactionType.FORCE_ALLOCATION;
import static com.ihsmarkit.tfx.core.domain.type.TransactionType.NEW_TRANSACTION_BY_CLHS;
import static com.ihsmarkit.tfx.core.domain.type.TransactionType.POSITION_OFFSET;
import static com.ihsmarkit.tfx.core.domain.type.TransactionType.REGULAR;
import static java.util.function.Predicate.not;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.domain.type.Side;
import com.ihsmarkit.tfx.core.domain.type.TransactionType;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarkedDataAggregated;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class DailyMarketDataReader implements ItemReader<Map<String, DailyMarkedDataAggregated>> {

    private static final Set<TransactionType> TRANSACTION_TYPES = Set.of(REGULAR, FORCE_ALLOCATION, NEW_TRANSACTION_BY_CLHS, POSITION_OFFSET);

    private final TradeRepository tradeRepository;
    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;
    private final OffsettedTradeMatchIdProvider offsettedTradeMatchIdProvider;
    private boolean isFinished;

    @Override
    @SuppressFBWarnings("USFW_UNSYNCHRONIZED_SINGLETON_FIELD_WRITES")
    public Map<String, DailyMarkedDataAggregated> read() {
        // workaround: https://stackoverflow.com/a/44278809/4760059
        if (isFinished) {
            return null;
        }
        isFinished = true;

        log.info("Read trades for Daily Market Data Ledger for trade date: {}", businessDate);

        return tradeRepository.findAllNovatedForTradeDateAndDirectionAndTransactionTypes(businessDate, Side.SELL, TRANSACTION_TYPES)
            .filter(not(offsettedByTransactionPredicate()))
            .collect(
                Collectors.groupingBy(
                    trade -> trade.getCurrencyPair().getCode(),
                    Collector.of(TradeHolder::new, TradeHolder::acceptTrade, TradeHolder::merge, DailyMarketDataReader::finisher)
                )
            );
    }

    private Predicate<TradeEntity> offsettedByTransactionPredicate() {
        return tradeEntity -> offsettedTradeMatchIdProvider.hasOffsettingMatchId(tradeEntity.getMatchingRef());
    }

    private static DailyMarkedDataAggregated finisher(final TradeHolder tradeHolder) {
        return DailyMarkedDataAggregated.builder()
            .openPriceTime(tradeHolder.getOpenTrade().getVersionTsp())
            .openPrice(tradeHolder.getOpenTrade().getSpotRate())

            .closePriceTime(tradeHolder.getCloseTrade().getVersionTsp())
            .closePrice(tradeHolder.getCloseTrade().getSpotRate())

            .lowPriceTime(tradeHolder.getLowTrade().getVersionTsp())
            .lowPrice(tradeHolder.getLowTrade().getSpotRate())

            .highPriceTime(tradeHolder.getHighTrade().getVersionTsp())
            .highPrice(tradeHolder.getHighTrade().getSpotRate())

            .shortPositionsAmount(tradeHolder.getTotalShortPositionsAmountSum())

            .build();
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TradeHolder {

        private static final Comparator<TradeEntity> OPEN_TRADE_COMPARATOR = Comparator.comparing(TradeEntity::getVersionTsp);
        private static final Comparator<TradeEntity> CLOSE_TRADE_COMPARATOR = OPEN_TRADE_COMPARATOR.reversed();
        private static final Comparator<TradeEntity> LOW_TRADE_COMPARATOR = Comparator.comparing(TradeEntity::getSpotRate);
        private static final Comparator<TradeEntity> HIGH_TRADE_COMPARATOR = LOW_TRADE_COMPARATOR.reversed();

        private TradeEntity openTrade;
        private TradeEntity closeTrade;
        private TradeEntity highTrade;
        private TradeEntity lowTrade;
        private BigDecimal totalShortPositionsAmountSum = BigDecimal.ZERO;

        void acceptTrade(final TradeEntity candidate) {
            openTrade = findMinBy(Stream.of(openTrade, candidate), OPEN_TRADE_COMPARATOR);
            closeTrade = findMinBy(Stream.of(closeTrade, candidate), CLOSE_TRADE_COMPARATOR);
            lowTrade = findMinBy(Stream.of(lowTrade, candidate), LOW_TRADE_COMPARATOR);
            highTrade = findMinBy(Stream.of(highTrade, candidate), HIGH_TRADE_COMPARATOR);
            // this should be total for "ShortPosition" only.
            if (candidate.getDirection() == Side.SELL) {
                totalShortPositionsAmountSum = totalShortPositionsAmountSum.add(candidate.getBaseAmount().getValue());
            }
        }

        static TradeHolder merge(final TradeHolder a, final TradeHolder b) {
            return new TradeHolder(
                findMinBy(concatTrades(a, b), OPEN_TRADE_COMPARATOR),
                findMinBy(concatTrades(a, b), CLOSE_TRADE_COMPARATOR),
                findMinBy(concatTrades(a, b), HIGH_TRADE_COMPARATOR),
                findMinBy(concatTrades(a, b), LOW_TRADE_COMPARATOR),
                a.getTotalShortPositionsAmountSum().add(b.getTotalShortPositionsAmountSum())
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
    }

}
