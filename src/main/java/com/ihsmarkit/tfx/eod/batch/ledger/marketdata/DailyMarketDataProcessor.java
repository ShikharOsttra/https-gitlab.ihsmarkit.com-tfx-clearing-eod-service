package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimal;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.common.streams.Streams;
import com.ihsmarkit.tfx.core.dl.entity.FxSpotProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.EodSwapPointEntity;
import com.ihsmarkit.tfx.core.dl.repository.FxSpotProductRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.DailySettlementPriceRepository;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.EodSwapPointRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarkedDataAggregated;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarketDataEnriched;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class DailyMarketDataProcessor implements ItemProcessor<Map<String, DailyMarkedDataAggregated>, List<DailyMarketDataEnriched>> {

    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    private final EodSwapPointRepository eodSwapPointRepository;
    private final DailySettlementPriceRepository dailySettlementPriceRepository;
    private final FxSpotProductRepository fxSpotProductRepository;
    private final ParticipantPositionRepository participantPositionRepository;
    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;
    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    @Override
    public List<DailyMarketDataEnriched> process(final Map<String, DailyMarkedDataAggregated> aggregatedMap) throws Exception {
        log.debug("Enrich Daily Market Data Ledger. Aggregated trades data: {}", aggregatedMap);

        final Function<String, BigDecimal> swapPointsMapper = getSwapPointsMapper();
        final Map<String, DailySettlementPriceEntity> dspMap = getDspMap();
        final Map<String, BigDecimal> currencyPairOpenPosition = getOpenPositionAmount();
        final Map<String, FxSpotProductEntity> fxSpotProductsMap = getFxSpotProductsMap();

        return aggregatedMap.entrySet().stream()
            .map(entry -> mapToEnrichedItem(entry, swapPointsMapper, dspMap, fxSpotProductsMap, currencyPairOpenPosition))
            .collect(Collectors.toUnmodifiableList());
    }

    private DailyMarketDataEnriched mapToEnrichedItem(
        final Map.Entry<String, DailyMarkedDataAggregated> aggregatedEntry, final Function<String, BigDecimal> swapPointMapper,
        final Map<String, DailySettlementPriceEntity> dspMap, final Map<String, FxSpotProductEntity> fxSpotProductsMap,
        final Map<String, BigDecimal> currencyPairOpenPosition
    ) {
        final String currencyPairCode = aggregatedEntry.getKey();
        final DailyMarkedDataAggregated aggregated = aggregatedEntry.getValue();
        final DailySettlementPriceEntity dsp = dspMap.get(currencyPairCode);
        final FxSpotProductEntity fxSpotProduct = fxSpotProductsMap.get(currencyPairCode);
        final BigDecimal tradingUnit = BigDecimal.valueOf(fxSpotProduct.getTradingUnit());
        // todo: do we expect nulls in any of the below vars?
        final BigDecimal openPositionAmount = currencyPairOpenPosition.getOrDefault(currencyPairCode, BigDecimal.ZERO);
        final BigDecimal currentDsp = getDspValue(dsp, DailySettlementPriceEntity::getDailySettlementPrice);
        final BigDecimal previousDsp = getDspValue(dsp, DailySettlementPriceEntity::getPreviousDailySettlementPrice);

        return DailyMarketDataEnriched.builder()
            .businessDate(businessDate)
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .currencyNumber(fxSpotProduct.getProductNumber())
            .currencyPairCode(currencyPairCode)
            .openPrice(formatBigDecimal(aggregated.getOpenPrice()))
            .openPriceTime(formatTime(aggregated.getOpenPriceTime()))
            .highPrice(formatBigDecimal(aggregated.getHighPrice()))
            .highPriceTime(formatTime(aggregated.getHighPriceTime()))
            .lowPrice(formatBigDecimal(aggregated.getLowPrice()))
            .lowPriceTime(formatTime(aggregated.getLowPriceTime()))
            .closePrice(formatBigDecimal(aggregated.getClosePrice()))
            .closePriceTime(formatTime(aggregated.getClosePriceTime()))
            .swapPoint(formatBigDecimal(swapPointMapper.apply(currencyPairCode)))
            .previousDsp(formatBigDecimal(previousDsp))
            .currentDsp(formatBigDecimal(currentDsp))
            .dspChange(formatBigDecimal(currentDsp.subtract(previousDsp)))
            .tradingVolumeAmount(formatBigDecimal(aggregated.getTradingVolumeAmount()))
            // todo: rounding?
            .tradingVolumeAmountInUnit(formatBigDecimal(aggregated.getTradingVolumeAmount().divide(tradingUnit)))
            .openPositionAmount(formatBigDecimal(openPositionAmount))
            // todo: rounding?
            .openPositionAmountInUnit(formatBigDecimal(openPositionAmount.divide(tradingUnit)))
            .build();
    }

    private Function<String, BigDecimal> getSwapPointsMapper() {
        final Map<String, BigDecimal> swapPointPerCurrencyPair = eodSwapPointRepository.findAllByDateOrderedByProductNumber(businessDate).stream()
            .collect(Collectors.toUnmodifiableMap(item -> item.getCurrencyPair().getCode(), EodSwapPointEntity::getSwapPoint));

        // todo: remove default ZERO once swap points will be setuped for each EOD
        return currencyPairCode -> swapPointPerCurrencyPair.getOrDefault(currencyPairCode, BigDecimal.ZERO).multiply(THOUSAND);
    }

    private Map<String, DailySettlementPriceEntity> getDspMap() {
        return dailySettlementPriceRepository.findAllByBusinessDate(businessDate).stream()
            .collect(Collectors.toUnmodifiableMap(item -> item.getCurrencyPair().getCode(), Function.identity()));
    }

    private Map<String, FxSpotProductEntity> getFxSpotProductsMap() {
        return fxSpotProductRepository.findAllOrderByProductNumberAsc().stream()
            .collect(Collectors.toMap(item -> item.getCurrencyPair().getCode(), Function.identity()));
    }

    private Map<String, BigDecimal> getOpenPositionAmount() {
        return participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(ParticipantPositionType.NET, businessDate).stream()
            .collect(Collectors.groupingBy(
                item -> item.getCurrencyPair().getCode(),
                Streams.summingBigDecimal(item -> item.getAmount().getValue())
            ));
    }

    private static BigDecimal getDspValue(@Nullable final DailySettlementPriceEntity dsp, final Function<DailySettlementPriceEntity, BigDecimal> extractor) {
        return Optional.ofNullable(dsp)
            .map(extractor)
            .orElse(BigDecimal.ZERO);
    }
}
