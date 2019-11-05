package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;

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
import com.ihsmarkit.tfx.eod.model.ParticipantPositionForPair;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeMtmCalculator {

    private final TradeOrPositionEssentialsMapper tradeOrPositionMapper;

    private ParticipantPositionForPair calculateMtmValue(final TradeOrPositionEssentials trade, final Map<CurrencyPairEntity, BigDecimal> dsp,
                                                         final Map<String, BigDecimal> jpyRates) {

        final var currencyPair = trade.getCurrencyPair();
        final var valueCurrency = currencyPair.getValueCurrency();

        final var rate = dsp.get(currencyPair);

        final var mtmAmount = rate.subtract(trade.getSpotRate())
            .multiply(trade.getBaseAmount())
            .multiply(JPY.equals(valueCurrency) ? BigDecimal.ONE : jpyRates.get(valueCurrency))
            .setScale(0, RoundingMode.FLOOR);

        return ParticipantPositionForPair.of(trade.getParticipant(), currencyPair, mtmAmount);
    }

    public Stream<ParticipantPositionForPair> calculateAndAggregateInitialMtm(final Stream<TradeEntity> trades, final Map<CurrencyPairEntity, BigDecimal> dsp) {

        final Map<String, BigDecimal> jpyRates = getJpyRatesFromDsp(dsp);

        return trades
            .map(tradeOrPositionMapper::convertTrade)
            .map(essentials -> calculateMtmValue(essentials, dsp, jpyRates))
            .collect(
                Collectors.groupingBy(
                    ParticipantPositionForPair::getParticipant,
                    Collectors.groupingBy(
                        ParticipantPositionForPair::getCurrencyPair,
                        Collectors.reducing(BigDecimal.ZERO, ParticipantPositionForPair::getAmount, BigDecimal::add)
                    )
                )
            ).entrySet().stream()
            .flatMap(participantBalance -> participantBalance.getValue().entrySet().stream()
                .map(ccyPairBalances -> ParticipantPositionForPair.of(participantBalance.getKey(), ccyPairBalances.getKey(), ccyPairBalances.getValue()))
            );
    }

    public Stream<ParticipantPositionForPair> calculateAndAggregateDailyMtm(final Collection<ParticipantPositionEntity> positions,
        final Map<CurrencyPairEntity, BigDecimal> dsp) {

        final Map<String, BigDecimal> jpyRates = getJpyRatesFromDsp(dsp);

        return positions.stream()
            .map(tradeOrPositionMapper::convertPosition)
            .map(t -> calculateMtmValue(t, dsp, jpyRates));
    }

    private Map<String, BigDecimal> getJpyRatesFromDsp(final Map<CurrencyPairEntity, BigDecimal> dailySettlementPrices) {
        return dailySettlementPrices.entrySet().stream()
            .filter(a -> JPY.equals(a.getKey().getValueCurrency()))
            .collect(Collectors.toMap(
                ccyPairBalance -> ccyPairBalance.getKey().getBaseCurrency(),
                Map.Entry::getValue
            ));
    }
}
