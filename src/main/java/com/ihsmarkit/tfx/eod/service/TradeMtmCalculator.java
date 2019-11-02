package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.MarkToMarketTrade;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeMtmCalculator {

    private final TradeOrPositionEssentialsMapper tradeOrPositionMapper;

    private MarkToMarketTrade calculateMtmValue(final TradeOrPositionEssentials trade, final Map<CurrencyPairEntity, BigDecimal> dsp,
        final Map<String, BigDecimal> jpyRates) {

        final var currencyPair = trade.getCurrencyPair();
        final var valueCurrency = currencyPair.getValueCurrency();

        final var rate = dsp.get(currencyPair);

        final var mtmAmount = rate.subtract(trade.getSpotRate())
            .multiply(trade.getBaseAmount())
            .multiply("JPY".equals(valueCurrency) ? BigDecimal.ONE : jpyRates.get(valueCurrency))
            .setScale(0, RoundingMode.FLOOR);

        return MarkToMarketTrade.of(trade.getParticipant(), currencyPair, mtmAmount);
    }

    public Stream<MarkToMarketTrade> calculateAndAggregateInitialMtm(final Stream<TradeEntity> trades, final Map<CurrencyPairEntity, BigDecimal> dsp) {

        final Map<String, BigDecimal> jpyRates = getJpyRatesFromDsp(dsp);

        return trades
            .map(tradeOrPositionMapper::convertTrade)
            .map(t -> calculateMtmValue(t, dsp, jpyRates))
            .collect(
                Collectors.groupingBy(
                    MarkToMarketTrade::getParticipant,
                    Collectors.groupingBy(
                        MarkToMarketTrade::getCurrencyPair,
                        Collectors.reducing(BigDecimal.ZERO, MarkToMarketTrade::getAmount, BigDecimal::add)
                    )
                )
            ).entrySet().stream()
            .flatMap(a -> a.getValue().entrySet().stream().map(b -> MarkToMarketTrade.of(a.getKey(), b.getKey(), b.getValue())));
    }

    public Stream<MarkToMarketTrade> calculateAndAggregateDailyMtm(final Collection<ParticipantPositionEntity> positions,
        final Map<CurrencyPairEntity, BigDecimal> dsp) {

        final Map<String, BigDecimal> jpyRates = getJpyRatesFromDsp(dsp);

        return positions.stream()
            .map(tradeOrPositionMapper::convertPosition)
            .map(t -> calculateMtmValue(t, dsp, jpyRates));
    }

    private Map<String, BigDecimal> getJpyRatesFromDsp(final Map<CurrencyPairEntity, BigDecimal> dailySettlementPrices) {
        return dailySettlementPrices.entrySet().stream()
            .filter(a -> "JPY".equals(a.getKey().getValueCurrency()))
            .collect(Collectors.toMap(
                a -> a.getKey().getBaseCurrency(),
                Map.Entry::getValue
            ));
    }
}
