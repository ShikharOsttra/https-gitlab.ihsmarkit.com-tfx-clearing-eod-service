package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.ITEM_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.SWAP_POINTS_DECIMAL_PLACES;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimal;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimalRoundTo1Jpy;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatTime;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.FLOOR;

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

        return Stream.concat(

            aggregatedMap.entrySet().stream()
                .map(entry -> mapToEnrichedItem(entry, swapPointsMapper, dspMap, currencyPairOpenPosition)),

            Stream.of(aggregatedMap.entrySet().stream()
                .map(entry -> mapToTotal(entry, currencyPairOpenPosition))
                .reduce(Total::add)
                .map(this::mapToEnrichedItem)
                .orElseGet(() -> mapToEnrichedItem(Total.of(ZERO, ZERO))))

        ).collect(Collectors.toUnmodifiableList());
    }

    private Total mapToTotal(final Map.Entry<String, DailyMarkedDataAggregated> aggregatedEntry, final Map<String, BigDecimal> currencyPairOpenPosition) {
        return Total.of(
            convertToTradingUnit(aggregatedEntry.getValue().getShortPositionsAmount(), aggregatedEntry.getKey()),
            convertToTradingUnit(currencyPairOpenPosition.getOrDefault(aggregatedEntry.getKey(), ZERO), aggregatedEntry.getKey())
        );
    }

    private DailyMarketDataEnriched mapToEnrichedItem(final Total total) {
        return DailyMarketDataEnriched.builder()
            .businessDate(businessDate)
            .currencyPairCode(TOTAL)
            .tradingVolumeAmountInUnit(formatBigDecimalRoundTo1Jpy(total.getTradingVolumeAmountInUnit()))
            .openPositionAmountInUnit(formatBigDecimalRoundTo1Jpy(total.getOpenPositionAmountInUnit()))
            .orderId(Long.MAX_VALUE)
            .recordType(TOTAL_RECORD_TYPE)
            .build();
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
        // todo: do we expect nulls in any of the below vars?
        final BigDecimal openPositionAmount = currencyPairOpenPosition.getOrDefault(currencyPairCode, ZERO);
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
            .swapPoint(formatBigDecimal(swapPointMapper.apply(currencyPairCode), SWAP_POINTS_DECIMAL_PLACES))
            .previousDsp(formatBigDecimal(previousDsp, priceScale))
            .currentDsp(formatBigDecimal(currentDsp, priceScale))
            .dspChange(formatBigDecimal(currentDsp.subtract(previousDsp), priceScale))
            .tradingVolumeAmount(formatBigDecimalRoundTo1Jpy(aggregated.getShortPositionsAmount()))
            .tradingVolumeAmountInUnit(formatBigDecimalRoundTo1Jpy(convertToTradingUnit(aggregated.getShortPositionsAmount(), currencyPairCode)))
            .openPositionAmount(formatBigDecimalRoundTo1Jpy(openPositionAmount))
            .openPositionAmountInUnit(formatBigDecimalRoundTo1Jpy(convertToTradingUnit(openPositionAmount, currencyPairCode)))
            .recordType(ITEM_RECORD_TYPE)
            .orderId(Long.parseLong(fxSpotProduct.getProductNumber()))
            .build();
    }

    private BigDecimal convertToTradingUnit(final BigDecimal amount, final String currencyPairCode) {
        return amount.setScale(0, FLOOR)
            .divide(BigDecimal.valueOf(fxSpotProductService.getFxSpotProduct(currencyPairCode).getTradingUnit()));
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
                        entry -> entry.getValue().values().stream().map(ZERO::min).collect(Streams.summingBigDecimal(BigDecimal::abs))
                    )
                );
    }

    private static BigDecimal getDspValue(@Nullable final DailySettlementPriceEntity dsp, final Function<DailySettlementPriceEntity, BigDecimal> extractor) {
        return Optional.ofNullable(dsp)
            .map(extractor)
            .orElse(ZERO);
    }


    @lombok.Value(staticConstructor = "of")
    private static class Total {

        private final BigDecimal tradingVolumeAmountInUnit;
        private final BigDecimal openPositionAmountInUnit;

        Total add(final Total total) {
            return Total.of(
                this.tradingVolumeAmountInUnit.add(total.tradingVolumeAmountInUnit),
                this.openPositionAmountInUnit.add(total.openPositionAmountInUnit)
            );
        }
    }
}
