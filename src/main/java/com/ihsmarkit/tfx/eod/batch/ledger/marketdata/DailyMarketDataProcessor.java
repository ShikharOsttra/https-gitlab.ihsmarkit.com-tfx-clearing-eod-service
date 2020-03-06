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
import static com.ihsmarkit.tfx.eod.batch.ledger.OrderUtils.buildOrderId;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.FLOOR;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import one.util.streamex.EntryStream;

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

            EntryStream.of(aggregatedMap)
                .mapKeyValue((currencyPairCode, aggregatedDailyMarketData) ->
                    mapToEnrichedItem(currencyPairCode, aggregatedDailyMarketData, swapPointsMapper, dspMap, currencyPairOpenPosition)
                ),

            Stream.of(
                EntryStream.of(aggregatedMap)
                    .mapKeyValue((currencyPairCode, aggregatedDailyMarketData) ->
                        mapToTotal(currencyPairCode, aggregatedDailyMarketData, currencyPairOpenPosition)
                    )
                    .reduce(Total::add)
                    .map(this::mapToEnrichedItem)
                    .orElseGet(() -> mapToEnrichedItem(Total.of(ZERO, ZERO)))
            )

        ).collect(Collectors.toUnmodifiableList());
    }

    private Total mapToTotal(final String currencyPairCode, final DailyMarkedDataAggregated aggregatedDailyMarketData,
        final Map<String, BigDecimal> currencyPairOpenPosition) {
        return Total.of(
            convertToTradingUnit(aggregatedDailyMarketData.getShortPositionsAmount(), currencyPairCode),
            convertToTradingUnit(currencyPairOpenPosition.getOrDefault(currencyPairCode, ZERO), currencyPairCode)
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
        final String currencyPairCode, final DailyMarkedDataAggregated aggregatedDailyMarketData, final Function<String, BigDecimal> swapPointMapper,
        final Map<String, DailySettlementPriceEntity> dspMap, final Map<String, BigDecimal> currencyPairOpenPosition
    ) {
        final int priceScale = fxSpotProductService.getScaleForCurrencyPair(currencyPairCode);
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
            .openPrice(formatBigDecimal(aggregatedDailyMarketData.getOpenPrice(), priceScale))
            .openPriceTime(formatTime(clockService.utcTimeToServerTime(aggregatedDailyMarketData.getOpenPriceTime())))
            .highPrice(formatBigDecimal(aggregatedDailyMarketData.getHighPrice(), priceScale))
            .highPriceTime(formatTime(clockService.utcTimeToServerTime(aggregatedDailyMarketData.getHighPriceTime())))
            .lowPrice(formatBigDecimal(aggregatedDailyMarketData.getLowPrice(), priceScale))
            .lowPriceTime(formatTime(clockService.utcTimeToServerTime(aggregatedDailyMarketData.getLowPriceTime())))
            .closePrice(formatBigDecimal(aggregatedDailyMarketData.getClosePrice(), priceScale))
            .closePriceTime(formatTime(clockService.utcTimeToServerTime(aggregatedDailyMarketData.getClosePriceTime())))
            .swapPoint(formatBigDecimal(swapPointMapper.apply(currencyPairCode), SWAP_POINTS_DECIMAL_PLACES))
            .previousDsp(formatBigDecimal(previousDsp, priceScale))
            .currentDsp(formatBigDecimal(currentDsp, priceScale))
            .dspChange(formatBigDecimal(currentDsp.subtract(previousDsp), priceScale))
            .tradingVolumeAmount(formatBigDecimalRoundTo1Jpy(aggregatedDailyMarketData.getShortPositionsAmount()))
            .tradingVolumeAmountInUnit(formatBigDecimalRoundTo1Jpy(convertToTradingUnit(aggregatedDailyMarketData.getShortPositionsAmount(), currencyPairCode)))
            .openPositionAmount(formatBigDecimalRoundTo1Jpy(openPositionAmount))
            .openPositionAmountInUnit(formatBigDecimalRoundTo1Jpy(convertToTradingUnit(openPositionAmount, currencyPairCode)))
            .recordType(ITEM_RECORD_TYPE)
            .orderId(buildOrderId(fxSpotProduct.getProductNumber()))
            .build();
    }

    private BigDecimal convertToTradingUnit(final BigDecimal amount, final String currencyPairCode) {
        return amount.setScale(0, FLOOR)
            .divide(BigDecimal.valueOf(fxSpotProductService.getFxSpotProduct(currencyPairCode).getTradingUnit()))
            .setScale(0, FLOOR);
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
            EntryStream.of(
                Stream.of(ParticipantPositionType.NET, ParticipantPositionType.REBALANCING)
                    .flatMap(positionType -> participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(positionType, businessDate))
                    .collect(
                        Collectors.groupingBy(
                            item -> item.getCurrencyPair().getCode(),
                            Collectors.groupingBy(
                                ParticipantPositionEntity::getParticipant,
                                Streams.summingBigDecimal(item -> item.getAmount().getValue())
                            )
                        )
                    )
            )
                .mapValues(Map::values)
                .mapValues(values ->
                    values.stream()
                        .map(ZERO::min)
                        .collect(Streams.summingBigDecimal(BigDecimal::abs))
                )
                .toMap();
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
