package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
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
@SuppressWarnings("PMD.TooManyMethods")
public class EODCalculator {

    private static final int DEFAULT_ROUNDING = 5;
    private static final BigDecimal SWAP_POINT_UNIT = BigDecimal.ONE.scaleByPowerOfTen(-3);

    private final TradeOrPositionEssentialsMapper tradeOrPositionMapper;

    private BigDecimal getJpyAmount(final CurrencyPairEntity currencyPair, final BigDecimal amount, final Function<String, BigDecimal> jpyRates) {
        return Optional.of(currencyPair)
            .map(CurrencyPairEntity::getValueCurrency)
            .filter(Predicate.not(JPY::equals))
            .map(jpyRates)
            .map(amount::multiply)
            .orElse(amount);
    }

    private ParticipantCurrencyPairAmount calc(
        final TradeOrPositionEssentials trade,
        final Function<String, BigDecimal> jpyRates,
        final BigDecimal multiplier
    ) {

        return ParticipantCurrencyPairAmount.of(
            trade.getParticipant(),
            trade.getCurrencyPair(),
            getJpyAmount(trade.getCurrencyPair(), trade.getAmount(), jpyRates)
                .multiply(multiplier)
                .setScale(0, RoundingMode.FLOOR)
        );

    }

    private ParticipantCurrencyPairAmount calculateMtmValue(
        final TradeOrPositionEssentials trade,
        final Function<CurrencyPairEntity, BigDecimal> dsp,
        final Function<String, BigDecimal> jpyRates) {

        return calc(
            trade,
            jpyRates,
            dsp.apply(trade.getCurrencyPair()).subtract(trade.getSpotRate())
        );
    }

    public ParticipantCurrencyPairAmount calculateInitialMtmValue(
        final TradeEntity trade, final Function<CurrencyPairEntity, BigDecimal> dsp, final Function<String, BigDecimal> jpyRates) {
        return calculateMtmValue(tradeOrPositionMapper.convertTrade(trade), dsp, jpyRates);
    }

    public ParticipantCurrencyPairAmount calculateDailyMtmValue(
        final ParticipantPositionEntity positionEntity, final Function<CurrencyPairEntity, BigDecimal> dsp, final Function<String, BigDecimal> jpyRates) {
        return calculateMtmValue(tradeOrPositionMapper.convertPosition(positionEntity), dsp, jpyRates);
    }

    private ParticipantCurrencyPairAmount calculateSwapPoint(
        final TradeOrPositionEssentials trade,
        final Function<CurrencyPairEntity, BigDecimal> swapPointResolver,
        final Function<String, BigDecimal> jpyRates) {

        return calc(
            trade,
            jpyRates,
            SWAP_POINT_UNIT.multiply(swapPointResolver.apply(trade.getCurrencyPair()))
        );
    }

    public ParticipantCurrencyPairAmount calculateSwapPoint(
        final TradeEntity trade,
        final Function<CurrencyPairEntity, BigDecimal> swapPointResolver,
        final Function<String, BigDecimal> jpyRates) {

        return calculateSwapPoint(tradeOrPositionMapper.convertTrade(trade), swapPointResolver, jpyRates);
    }

    public Stream<ParticipantCurrencyPairAmount> calculateAndAggregateInitialMtm(
        final Stream<TradeEntity> trades,
        final Function<CurrencyPairEntity, BigDecimal> dsp,
        final Function<String, BigDecimal> jpyRates
    ) {

        return flatten(
            aggregate(
                trades
                    .map(tradeOrPositionMapper::convertTrade)
                    .map(essentials -> calculateMtmValue(essentials, dsp, jpyRates))
            )
        );
    }

    public Stream<ParticipantCurrencyPairAmount> calculateAndAggregateSwapPnL(
        final Stream<TradeEntity> trades,
        final Function<CurrencyPairEntity, BigDecimal> swapPointResolver,
        final Function<String, BigDecimal> jpyRates
    ) {

        return flatten(
            aggregate(
                trades
                    //TODO: use it
                    .map(tradeOrPositionMapper::convertTrade)
                    .map(essentials -> calculateSwapPoint(essentials, swapPointResolver, jpyRates))
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

    public Stream<ParticipantCurrencyPairAmount> netAll(final Stream<? extends CcyParticipantAmount> trades) {
        return flatten(aggregate(trades));
    }

    public Stream<ParticipantCurrencyPairAmount> calculateAndAggregateDailyMtm(final Collection<ParticipantPositionEntity> positions,
        final Function<CurrencyPairEntity, BigDecimal> dsp,
        final Function<String, BigDecimal> jpyRates) {

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

}
