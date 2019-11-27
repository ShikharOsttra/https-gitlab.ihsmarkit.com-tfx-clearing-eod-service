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
import com.ihsmarkit.tfx.eod.model.CcyParticipantAmount;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.model.PositionBalance;
import com.ihsmarkit.tfx.eod.model.RawPositionData;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EODCalculator {

    private static final int DEFAULT_ROUNDING = 5;

    private final TradeOrPositionEssentialsMapper tradeOrPositionMapper;

    private ParticipantCurrencyPairAmount calculateMtmValue(final TradeOrPositionEssentials trade, final Map<CurrencyPairEntity, BigDecimal> dsp,
                                                            final Map<String, BigDecimal> jpyRates) {

        final var currencyPair = trade.getCurrencyPair();
        final var valueCurrency = currencyPair.getValueCurrency();

        final var rate = dsp.get(currencyPair);

        final var mtmAmount = rate.subtract(trade.getSpotRate())
            .multiply(trade.getAmount())
            .multiply(JPY.equals(valueCurrency) ? BigDecimal.ONE : jpyRates.get(valueCurrency))
            .setScale(0, RoundingMode.FLOOR);

        return ParticipantCurrencyPairAmount.of(trade.getParticipant(), currencyPair, mtmAmount);
    }

    public Stream<ParticipantCurrencyPairAmount> calculateAndAggregateInitialMtm(
        final Stream<TradeEntity> trades,
        final Map<CurrencyPairEntity,
            BigDecimal> dsp
    ) {

        final Map<String, BigDecimal> jpyRates = getJpyRatesFromDsp(dsp);

        return flatten(
            aggregate(
                trades
                    .map(tradeOrPositionMapper::convertTrade)
                    .map(essentials -> calculateMtmValue(essentials, dsp, jpyRates))
            )
        );

    }

    private Map<ParticipantEntity, Map<CurrencyPairEntity, BigDecimal>> aggregate(final Stream<? extends CcyParticipantAmount> input) {
        return input
            .collect(
                Collectors.groupingBy(
                    CcyParticipantAmount::getParticipant,
                    Collectors.groupingBy(
                        CcyParticipantAmount::getCurrencyPair,
                        Collectors.reducing(BigDecimal.ZERO, CcyParticipantAmount::getAmount, BigDecimal::add)
                    )
                )
            );
    }

    private Stream<ParticipantCurrencyPairAmount> flatten(final Map<ParticipantEntity, Map<CurrencyPairEntity, BigDecimal>> input) {
        return input.entrySet().stream()
            .flatMap(participantBalance -> participantBalance.getValue().entrySet().stream()
                .map(ccyPairBalances -> ParticipantCurrencyPairAmount.of(participantBalance.getKey(), ccyPairBalances.getKey(), ccyPairBalances.getValue()))
            );
    }

    public Stream<ParticipantCurrencyPairAmount> aggregatePositions(final Stream<ParticipantPositionEntity> positions) {
        return flatten(aggregate(positions.map(tradeOrPositionMapper::convertPosition)));
    }

    public Stream<ParticipantCurrencyPairAmount> netAllTtrades(final Stream<? extends CcyParticipantAmount> trades) {
        return flatten(aggregate(trades));
    }

    public Stream<ParticipantCurrencyPairAmount> calculateAndAggregateDailyMtm(final Collection<ParticipantPositionEntity> positions,
                                                                               final Map<CurrencyPairEntity, BigDecimal> dsp) {

        final Map<String, BigDecimal> jpyRates = getJpyRatesFromDsp(dsp);

        return positions.stream()
            .map(tradeOrPositionMapper::convertPosition)
            .map(t -> calculateMtmValue(t, dsp, jpyRates));
    }

    public Map<CurrencyPairEntity, List<BalanceTrade>> rebalanceLPPositions(final Stream<ParticipantPositionEntity> positions) {

        return positions
            .map(tradeOrPositionMapper::convertPosition)
            .collect(Collectors.groupingBy(
                TradeOrPositionEssentials::getCurrencyPair,
                Collectors.toList()
            )).entrySet().stream()
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> rebalanceSingleCurrency(entry.getValue(), DEFAULT_ROUNDING) //FIXME: Rounding by ccy
                    )
                );

    }

    private List<BalanceTrade> rebalanceSingleCurrency(final List<TradeOrPositionEssentials> list, final int rounding) {

        PositionBalance balance = PositionBalance.of(
            list.stream()
                .map(position -> new RawPositionData(position.getParticipant(), position.getAmount()))
        );

        final BigDecimal threshold = BigDecimal.TEN.pow(rounding);
        final List<BalanceTrade> trades = new ArrayList<>();

        int tradesInIteration = Integer.MAX_VALUE;

        while (tradesInIteration > 0 && balance.getBuy().getNet().min(balance.getSell().getNet().abs()).compareTo(threshold) > 0) {
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
