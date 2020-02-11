package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimal;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.common.streams.Streams;
import com.ihsmarkit.tfx.core.dl.entity.FxSpotProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.DailySettlementPriceRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarkedDataAggregated;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarketDataEnriched;
import com.ihsmarkit.tfx.eod.service.CurrencyPairSwapPointService;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class DailyMarketDataProcessor implements ItemProcessor<Map<String, DailyMarkedDataAggregated>, List<DailyMarketDataEnriched>> {

    private final CurrencyPairSwapPointService currencyPairSwapPointService;
    private final DailySettlementPriceRepository dailySettlementPriceRepository;
    private final FXSpotProductService fxSpotProductService;
    private final ParticipantPositionRepository participantPositionRepository;
    private final ClockService clockService;
    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;
    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    @Override
    public List<DailyMarketDataEnriched> process(final Map<String, DailyMarkedDataAggregated> aggregatedMap) {
        log.debug("Enrich Daily Market Data Ledger. Aggregated trades data: {}", aggregatedMap);

        final Function<String, BigDecimal> swapPointsMapper = getSwapPointsMapper();
        final Map<String, DailySettlementPriceEntity> dspMap = getDspMap();
        final Map<String, BigDecimal> currencyPairOpenPosition = getOpenPositionAmount();

        return aggregatedMap.entrySet().stream()
            .map(entry -> mapToEnrichedItem(entry, swapPointsMapper, dspMap, currencyPairOpenPosition))
            .collect(Collectors.toUnmodifiableList());
    }

    private DailyMarketDataEnriched mapToEnrichedItem(
        final Map.Entry<String, DailyMarkedDataAggregated> aggregatedEntry, final Function<String, BigDecimal> swapPointMapper,
        final Map<String, DailySettlementPriceEntity> dspMap, final Map<String, BigDecimal> currencyPairOpenPosition
    ) {
        final String currencyPairCode = aggregatedEntry.getKey();
        final int priceScale = fxSpotProductService.getScaleForCurrencyPair(currencyPairCode);
        final DailyMarkedDataAggregated aggregated = aggregatedEntry.getValue();
        final DailySettlementPriceEntity dsp = dspMap.get(currencyPairCode);
        final FxSpotProductEntity fxSpotProduct = fxSpotProductService.getFxSpotProduct(currencyPairCode);
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
            .openPrice(formatBigDecimal(aggregated.getOpenPrice(), priceScale))
            .openPriceTime(formatTime(clockService.utcTimeToServerTime(aggregated.getOpenPriceTime())))
            .highPrice(formatBigDecimal(aggregated.getHighPrice(), priceScale))
            .highPriceTime(formatTime(clockService.utcTimeToServerTime(aggregated.getHighPriceTime())))
            .lowPrice(formatBigDecimal(aggregated.getLowPrice(), priceScale))
            .lowPriceTime(formatTime(clockService.utcTimeToServerTime(aggregated.getLowPriceTime())))
            .closePrice(formatBigDecimal(aggregated.getClosePrice(), priceScale))
            .closePriceTime(formatTime(clockService.utcTimeToServerTime(aggregated.getClosePriceTime())))
            .swapPoint(formatBigDecimal(swapPointMapper.apply(currencyPairCode)))
            .previousDsp(formatBigDecimal(previousDsp))
            .currentDsp(formatBigDecimal(currentDsp))
            .dspChange(formatBigDecimal(currentDsp.subtract(previousDsp), priceScale))
            .tradingVolumeAmount(formatBigDecimal(aggregated.getShortPositionsAmount()))
            // todo: rounding?
            .tradingVolumeAmountInUnit(formatBigDecimal(aggregated.getShortPositionsAmount().divide(tradingUnit)))
            .openPositionAmount(formatBigDecimal(openPositionAmount))
            // todo: rounding?
            .openPositionAmountInUnit(formatBigDecimal(openPositionAmount.divide(tradingUnit)))
            .build();
    }

    private Function<String, BigDecimal> getSwapPointsMapper() {
        return currencyPairCode -> currencyPairSwapPointService.getSwapPoint(businessDate, currencyPairCode);
    }

    private Map<String, DailySettlementPriceEntity> getDspMap() {
        return dailySettlementPriceRepository.findAllByBusinessDate(businessDate).stream()
            .collect(Collectors.toUnmodifiableMap(item -> item.getCurrencyPair().getCode(), Function.identity()));
    }

    private Map<String, BigDecimal> getOpenPositionAmount() {
        return
            Stream.of(ParticipantPositionType.NET, ParticipantPositionType.REBALANCING)
                .map(positionType -> participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(positionType, businessDate))
                .flatMap(Collection::stream)

                .collect(
                    Collectors.groupingBy(
                        item -> item.getCurrencyPair().getCode(),
                        Collectors.groupingBy(
                            ParticipantPositionEntity::getParticipant,
                            Streams.summingBigDecimal(item -> item.getAmount().getValue())
                        )
                    )
                ).entrySet().stream()
                    .collect(
                        Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().values().stream().map(BigDecimal.ZERO::min).collect(Streams.summingBigDecimal(BigDecimal::abs))
                        )
                    );
    }

    private static BigDecimal getDspValue(@Nullable final DailySettlementPriceEntity dsp, final Function<DailySettlementPriceEntity, BigDecimal> extractor) {
        return Optional.ofNullable(dsp)
            .map(extractor)
            .orElse(BigDecimal.ZERO);
    }
}
