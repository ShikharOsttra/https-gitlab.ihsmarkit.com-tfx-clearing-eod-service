package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.BalanceTrade;
import com.ihsmarkit.tfx.eod.model.ParticipantPositionForPair;
import com.ihsmarkit.tfx.eod.model.PositionBalance;
import com.ihsmarkit.tfx.eod.model.RawPositionData;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EODCalculator {

    private static final int DEFAULT_ROUNDING = 5;
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

        return flatten(trades
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
            ));
    }

    private Stream<ParticipantPositionForPair> flatten(final Map<ParticipantEntity, Map<CurrencyPairEntity, BigDecimal>> input) {
        return input.entrySet().stream()
            .flatMap(participantBalance -> participantBalance.getValue().entrySet().stream()
                .map(ccyPairBalances -> ParticipantPositionForPair.of(participantBalance.getKey(), ccyPairBalances.getKey(), ccyPairBalances.getValue()))
            );
    }

    public Stream<ParticipantPositionForPair> netAllTtrades(final Stream<TradeOrPositionEssentials> trades) {
        return flatten(trades
            .collect(
                Collectors.groupingBy(
                    TradeOrPositionEssentials::getParticipant,
                    Collectors.groupingBy(
                        TradeOrPositionEssentials::getCurrencyPair,
                        Collectors.reducing(BigDecimal.ZERO, TradeOrPositionEssentials::getBaseAmount, BigDecimal::add)
                    )
                )
            ));
    }

    public Stream<ParticipantPositionForPair> calculateAndAggregateDailyMtm(final Collection<ParticipantPositionEntity> positions,
        final Map<CurrencyPairEntity, BigDecimal> dsp) {

        final Map<String, BigDecimal> jpyRates = getJpyRatesFromDsp(dsp);

        return positions.stream()
            .map(tradeOrPositionMapper::convertPosition)
            .map(t -> calculateMtmValue(t, dsp, jpyRates));
    }

    public Stream<BalanceTrade> rebalanceLPPositions(final Collection<ParticipantPositionEntity> positions) {

        final Stream<BalanceTrade> balanceTrades = positions.stream()
            .map(tradeOrPositionMapper::convertPosition)
            .collect(Collectors.groupingBy(
                TradeOrPositionEssentials::getCurrencyPair,
                Collectors.toList()
            )).entrySet().stream()
            .flatMap(e -> rebalanceSingleCurrency(e.getValue(), DEFAULT_ROUNDING).stream());

        return balanceTrades;
    }


    private List<BalanceTrade> rebalanceSingleCurrency(final List<TradeOrPositionEssentials> list, final int rounding) {

        PositionBalance balance = PositionBalance.of(
            list.stream()
                .map(p -> new RawPositionData(p.getParticipant(), p.getBaseAmount()))
        );

        final BigDecimal threshold = BigDecimal.TEN.pow(rounding);
        final List<BalanceTrade> trades = new ArrayList<>();

        int tradesInIteration = Integer.MAX_VALUE;

        while (tradesInIteration > 0 && balance.getBuy().getNet().max(balance.getSell().getNet()).compareTo(threshold) > 0) {
            final List<BalanceTrade> iterationTrades = balance.rebalance(rounding).collect(Collectors.toList());
            tradesInIteration = iterationTrades.size();
            trades.addAll(iterationTrades);
            balance = balance.applyTrades(iterationTrades.stream());
        }

        return trades;

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
