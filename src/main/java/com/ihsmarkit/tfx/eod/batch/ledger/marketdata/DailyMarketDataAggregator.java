package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimal;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatTime;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.FxSpotProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodSwapPointEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
import com.ihsmarkit.tfx.core.dl.repository.FxSpotProductRepository;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository.TradeTotalAmountCurrencyPair;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodSwapPointRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.DailySettlementPriceRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.batch.ledger.marketdata.model.DailyMarkedDataProjection;
import com.ihsmarkit.tfx.eod.batch.ledger.marketdata.model.DailyMarketDataAggregate;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Component
@StepScope
@RequiredArgsConstructor
public class DailyMarketDataAggregator {

    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    private final EodSwapPointRepository eodSwapPointRepository;
    private final DailySettlementPriceRepository dailySettlementPriceRepository;
    private final TradeRepository tradeRepository;
    private final FxSpotProductRepository fxSpotProductRepository;
    private final ParticipantPositionRepository participantPositionRepository;
    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;
    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    @SneakyThrows
    public List<DailyMarketDataAggregate> aggregate(final List<? extends DailyMarkedDataProjection> items) {
        final Function<String, BigDecimal> swapPointsMapper = getSwapPointsMapper();
        final Function<String, BigDecimal> currentDspMapper = getDspMapper(dailySettlementPriceRepository::findAllByBusinessDate);
        final Function<String, BigDecimal> previousDspMapper = getDspMapper(dailySettlementPriceRepository::findPreviousDailySettlementPrice);
        final Map<String, BigDecimal> currencyPairTotalMap = getValueTotal();
        final Map<String, BigDecimal> currencyPairOpenPosition = getOpenPositionAmount();
        final Map<String, Long> tradingUnitPerCurrencyPair = getTradingUnit();

        return items.stream()
            .collect(Collectors.groupingBy(DailyMarkedDataProjection::getCurrencyPairCode))
            .values().stream()
            .map(list -> aggregateItemsPerCurrencyPair(
                list, swapPointsMapper, currentDspMapper, previousDspMapper, currencyPairTotalMap, tradingUnitPerCurrencyPair, currencyPairOpenPosition
            ))
            .sorted(Comparator.comparing(DailyMarketDataAggregate::getCurrencyNumber))
            .collect(Collectors.toUnmodifiableList());
    }

    private DailyMarketDataAggregate aggregateItemsPerCurrencyPair(
        final List<? extends DailyMarkedDataProjection> items, final Function<String, BigDecimal> swapPointMapper,
        final Function<String, BigDecimal> currentDspMapper, final Function<String, BigDecimal> previousDspMapper,
        final Map<String, BigDecimal> currencyPairTotalMap, final Map<String, Long> tradingUnitPerCurrencyPair,
        final Map<String, BigDecimal> currencyPairOpenPosition
    ) {
        final DailyMarkedDataProjection open = findMinBy(items, Comparator.comparing(DailyMarkedDataProjection::getVersionTsp));
        final DailyMarkedDataProjection close = findMinBy(items, Comparator.comparing(DailyMarkedDataProjection::getVersionTsp).reversed());
        final DailyMarkedDataProjection low = findMinBy(items, Comparator.comparing(DailyMarkedDataProjection::getValueAmount));
        final DailyMarkedDataProjection high = findMinBy(items, Comparator.comparing(DailyMarkedDataProjection::getValueAmount).reversed());
        final String currencyPairCode = open.getCurrencyPairCode();
        final BigDecimal currentDsp = currentDspMapper.apply(currencyPairCode);
        final BigDecimal previousDsp = previousDspMapper.apply(currencyPairCode);
        final BigDecimal tradingVolumeAmount = currencyPairTotalMap.get(currencyPairCode);
        // todo: do we expect nulls here?
        final BigDecimal openPositionAmount = currencyPairOpenPosition.getOrDefault(currencyPairCode, BigDecimal.ZERO);
        final long productCodeTradingUnit = tradingUnitPerCurrencyPair.get(currencyPairCode);

        return DailyMarketDataAggregate.builder()
            .businessDate(Date.valueOf(businessDate))
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .currencyNumber(open.getProductNumber())
            .currencyPairCode(open.getCurrencyPairCode())
            .openPrice(formatBigDecimal(open.getValueAmount()))
            .openPriceTime(formatTime(open.getVersionTsp().toLocalTime()))
            .highPrice(formatBigDecimal(high.getValueAmount()))
            .highPriceTime(formatTime(high.getVersionTsp().toLocalTime()))
            .lowPrice(formatBigDecimal(low.getValueAmount()))
            .lowPriceTime(formatTime(low.getVersionTsp().toLocalTime()))
            .closePrice(formatBigDecimal(close.getValueAmount()))
            .closePriceTime(formatTime(close.getVersionTsp().toLocalTime()))
            .swapPoint(formatBigDecimal(swapPointMapper.apply(currencyPairCode)))
            .previousDsp(formatBigDecimal(previousDsp))
            .currentDsp(formatBigDecimal(currentDsp))
            .dspChange(formatBigDecimal(currentDsp.subtract(previousDsp)))
            .tradingVolumeAmount(formatBigDecimal(tradingVolumeAmount))
            // todo: rounding?
            .tradingVolumeAmountInUnit(formatBigDecimal(tradingVolumeAmount.divide(BigDecimal.valueOf(productCodeTradingUnit))))
            .openPositionAmount(formatBigDecimal(openPositionAmount))
            // todo: rounding?
            .openPositionAmountInUit(formatBigDecimal(openPositionAmount.divide(BigDecimal.valueOf(productCodeTradingUnit))))
            .build();
    }

    private Function<String, BigDecimal> getSwapPointsMapper() {
        final Map<String, BigDecimal> swapPointPerCurrencyPair = eodSwapPointRepository.findAllByDateOrderedByProductNumber(businessDate).stream()
            .collect(Collectors.toUnmodifiableMap(item -> item.getCurrencyPair().getCode(), EodSwapPointEntity::getSwapPoint));

        // todo: remove default ZERO once swap points will be setuped for each EOD
        return currencyPairCode -> swapPointPerCurrencyPair.getOrDefault(currencyPairCode, BigDecimal.ZERO).multiply(THOUSAND);
    }

    private Function<String, BigDecimal> getDspMapper(final Function<LocalDate, List<DailySettlementPriceEntity>> dspSupplier) {
        final Map<String, BigDecimal> dailySettlementPricePerCurrency = dspSupplier.apply(businessDate).stream()
            .collect(Collectors.toUnmodifiableMap(item -> item.getCurrencyPair().getCode(), DailySettlementPriceEntity::getDailySettlementPrice));

        // todo: remove ZERO once DSP will be setuped for each EOD
        return currencyPairCode -> dailySettlementPricePerCurrency.getOrDefault(currencyPairCode, BigDecimal.ZERO);
    }

    private Map<String, BigDecimal> getValueTotal() {
        return tradeRepository.findTotalBaseAmountPerCurrencyPairForBusinessDate(businessDate).stream()
            .collect(Collectors.toUnmodifiableMap(TradeTotalAmountCurrencyPair::getProductCode, TradeTotalAmountCurrencyPair::getTotal));
    }

    private Map<String, Long> getTradingUnit() {
        return fxSpotProductRepository.findAllOrderByProductNumberAsc().stream()
            .collect(Collectors.toMap(item -> item.getCurrencyPair().getCode(), FxSpotProductEntity::getTradingUnit));
    }

    private Map<String, BigDecimal> getOpenPositionAmount() {
        return participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(ParticipantPositionType.NET, businessDate).stream()
            .collect(Collectors.groupingBy(
                item -> item.getCurrencyPair().getCode(),
                Collectors.reducing(BigDecimal.ZERO, item -> item.getAmount().getValue(), BigDecimal::add)
            ));
    }

    private static DailyMarkedDataProjection findMinBy(
        final List<? extends DailyMarkedDataProjection> list, final Comparator<DailyMarkedDataProjection> comparator
    ) {
        return list.stream()
            .min(comparator)
            .orElseThrow(() -> new IllegalStateException("Can't find min item by comparator"));
    }

}
