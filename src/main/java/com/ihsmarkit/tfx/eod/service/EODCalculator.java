package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.CASH;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.DAY;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.FOLLOWING;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.TOTAL;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.BUY;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.NET;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SELL;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;
import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import com.google.common.collect.Streams;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.BalanceContribution;
import com.ihsmarkit.tfx.eod.model.BalanceTrade;
import com.ihsmarkit.tfx.eod.model.BuySellAmounts;
import com.ihsmarkit.tfx.eod.model.CcyParticipantAmount;
import com.ihsmarkit.tfx.eod.model.DayAndTotalCashSettlement;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.model.ParticipantMargin;
import com.ihsmarkit.tfx.eod.model.ParticipantPosition;
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
        final ParticipantPositionEntity participantPosition,
        final Function<CurrencyPairEntity, BigDecimal> swapPointResolver,
        final Function<String, BigDecimal> jpyRates) {

        return calculateSwapPoint(tradeOrPositionMapper.convertPosition(participantPosition), swapPointResolver, jpyRates);
    }

    public Map<ParticipantEntity, BigDecimal> calculateRequiredInitialMargin(
        final Stream<ParticipantPositionEntity> positions,
        final BiFunction<CurrencyPairEntity, ParticipantEntity, BigDecimal> marginRatioResolver,
        final Function<String, BigDecimal> jpyRates
    ) {
        return aggregatePositions(positions)
            .map(position -> ImmutablePair.of(
                position.getParticipant(),
                getJpyAmount(position.getCurrencyPair(), position.getAmount().abs(), jpyRates)
                    .multiply(marginRatioResolver.apply(position.getCurrencyPair(), position.getParticipant()))
                    .setScale(0, RoundingMode.CEILING)
            )).collect(
                toMap(ImmutablePair::getLeft, ImmutablePair::getRight, BigDecimal::add)
            );
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
                    .map(tradeOrPositionMapper::convertTrade)
                    .map(essentials -> calculateSwapPoint(essentials, swapPointResolver, jpyRates))
            )
        );
    }

    private Map<ParticipantEntity, Map<CurrencyPairEntity, BigDecimal>> aggregate(final Stream<? extends CcyParticipantAmount> input) {
        return aggregate(input, reducing(ZERO, CcyParticipantAmount::getAmount, BigDecimal::add));
    }

    private <R> Map<ParticipantEntity, Map<CurrencyPairEntity, R>> aggregate(
        final Stream<? extends CcyParticipantAmount> input,
        final Collector<CcyParticipantAmount, ?, R> collector
    ) {
        return input
            .collect(
                groupingBy(
                    CcyParticipantAmount::getParticipant,
                    groupingBy(CcyParticipantAmount::getCurrencyPair, collector)
                )
            );
    }

    private <T> Stream<ParticipantCurrencyPairAmount> flatten(final Map<ParticipantEntity, Map<CurrencyPairEntity, BigDecimal>> input) {
        return input.entrySet().stream()
            .flatMap(participantBalance -> participantBalance.getValue().entrySet().stream()
                .map(ccyPairBalances -> ParticipantCurrencyPairAmount.of(participantBalance.getKey(), ccyPairBalances.getKey(), ccyPairBalances.getValue()))
            );
    }

    public Stream<ParticipantCurrencyPairAmount> aggregatePositions(final Stream<ParticipantPositionEntity> positions) {
        return flatten(aggregate(positions.map(tradeOrPositionMapper::convertPosition)));
    }

    public Map<ParticipantEntity, Map<EodProductCashSettlementType, EnumMap<EodCashSettlementDateType, BigDecimal>>>
                            aggregateRequiredMargin(final Stream<EodProductCashSettlementEntity> margins, final LocalDate businessDate) {
        return margins
            .collect(
                groupingBy(
                    EodProductCashSettlementEntity::getParticipant,
                    groupingBy(
                        EodProductCashSettlementEntity::getType,
                        () -> new EnumMap<EodProductCashSettlementType, EnumMap<EodCashSettlementDateType, BigDecimal>>(EodProductCashSettlementType.class),
                        twoWayCollector(
                            margin -> businessDate.isEqual(margin.getSettlementDate()),
                            margin -> margin.getAmount().getValue(),
                            EODCalculator::marginMap
                        )
                    )
                )
            );
    }

    private static EnumMap<EodCashSettlementDateType, BigDecimal> marginMap(final Optional<BigDecimal> day, final Optional<BigDecimal> following) {
        return new EnumMap<EodCashSettlementDateType, BigDecimal>(
            Stream.of(
                sumAll(day, following).map(margin -> Pair.of(TOTAL, margin)),
                day.map(margin -> Pair.of(DAY, margin)),
                following.map(margin -> Pair.of(FOLLOWING, margin))
            ).flatMap(Optional::stream).collect(toMap(Pair::getLeft, Pair::getRight))
        );
    }

    public Stream<ParticipantCurrencyPairAmount> netAll(final Stream<? extends CcyParticipantAmount> trades) {
        return flatten(aggregate(trades));
    }

    public Stream<ParticipantPosition> netAllByBuySell(
        final Stream<TradeOrPositionEssentials> tradesToNet,
        final Stream<? extends CcyParticipantAmount> positions
    ) {

        return mergeAndFlatten(
            positions.collect(
                toMap(pos -> new ParticipantAndCurrencyPair(pos.getParticipant(), pos.getCurrencyPair()), CcyParticipantAmount::getAmount)
            ),
            tradesToNet.collect(
                groupingBy(
                    pos -> new ParticipantAndCurrencyPair(pos.getParticipant(), pos.getCurrencyPair()),
                    twoWayCollector(
                        ccyParticipantAmount -> ccyParticipantAmount.getAmount().compareTo(ZERO) > 0,
                        CcyParticipantAmount::getAmount,
                        BuySellAmounts::new
                    )
                )
            ),
            (key, sod, buySell) ->
                Stream.of(
                    buySell.flatMap(BuySellAmounts::getBuy).map(buy -> ParticipantPosition.of(key.getParticipant(), key.getCurrencyPair(), buy, BUY)),
                    buySell.flatMap(BuySellAmounts::getSell).map(sell -> ParticipantPosition.of(key.getParticipant(), key.getCurrencyPair(), sell, SELL)),
                    sumAll(buySell.flatMap(BuySellAmounts::getBuy), buySell.flatMap(BuySellAmounts::getSell), sod)
                        .map(net -> ParticipantPosition.of(key.getParticipant(), key.getCurrencyPair(), net, NET))
                ).flatMap(Optional::stream)
        ).flatMap(x -> x);
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
            .collect(groupingBy(
                TradeOrPositionEssentials::getCurrencyPair,
                Collectors.toList()
            )).entrySet().stream()
                .collect(
                    toMap(
                        Map.Entry::getKey,
                        entry -> rebalanceSingleCurrency(entry.getValue(), DEFAULT_ROUNDING) //FIXME: Rounding by ccy
                    )
                );
    }

    public Stream<ParticipantMargin> calculateParticipantMargin(final Map<ParticipantEntity, BigDecimal> requiredInitialMargin,
                                                                final Map<ParticipantEntity, DayAndTotalCashSettlement> dayCashSettlement,
                                                                final Map<ParticipantEntity, BalanceContribution> deposits) {
        return Stream.of(requiredInitialMargin, dayCashSettlement, deposits)
            .map(Map::keySet)
            .flatMap(Set::stream)
            .distinct()
            .map(
                participant -> createEodParticipantMargin(
                    participant,
                    Optional.ofNullable(requiredInitialMargin.get(participant)),
                    Optional.ofNullable(dayCashSettlement.get(participant)),
                    Optional.ofNullable(deposits.get(participant))
                )
            );
    }

    public Map<ParticipantEntity, BalanceContribution> calculateDeposits(
        final Stream<CollateralBalanceEntity> balances,
        final Function<CollateralBalanceEntity, BigDecimal> evaluator
    ) {
        return balances.collect(
            groupingBy(
                CollateralBalanceEntity::getParticipant,
                twoWayCollector(
                    balance -> balance.getProduct().getType() == CASH,
                    evaluator,
                    (cash, nonCash) -> new BalanceContribution(sumAll(nonCash, cash).orElse(ZERO), cash.orElse(ZERO))
                )
            )
        );

    }

    public Map<ParticipantEntity, DayAndTotalCashSettlement> aggregateDayAndTotalCashSettlement(
        final Map<ParticipantEntity, Map<EodProductCashSettlementType, EnumMap<EodCashSettlementDateType, BigDecimal>>> aggregated
    ) {
        return aggregated.entrySet().stream()
            .flatMap(
                byParticipant -> Optional.ofNullable(byParticipant.getValue().get(TOTAL_VM))
                    .map(byType -> new DayAndTotalCashSettlement(Optional.ofNullable(byType.get(DAY)), byType.get(TOTAL)))
                    .map(amount -> ImmutablePair.of(byParticipant.getKey(), amount))
                    .stream()
            ).collect(
            toMap(Pair::getLeft, Pair::getRight)
        );
    }

    @SuppressWarnings("unchecked")
    private ParticipantMargin createEodParticipantMargin(
        final ParticipantEntity participant,
        final Optional<BigDecimal> requiredInitialMargin,
        final Optional<DayAndTotalCashSettlement> dayCashSettlement,
        final Optional<BalanceContribution> balance) {

        return ParticipantMargin.builder()
            .participant(participant)
            .initialMargin(requiredInitialMargin)
            .requiredAmount(sumAll(requiredInitialMargin, dayCashSettlement.map(DayAndTotalCashSettlement::getTotal).map(BigDecimal::negate)))
            .totalDeficit(
                sumAll(
                    balance.map(BalanceContribution::getTotalBalanceContribution),
                    dayCashSettlement.map(DayAndTotalCashSettlement::getTotal),
                    requiredInitialMargin.map(BigDecimal::negate)
                ))
            .cashDeficit(
                sumAll(
                    balance.map(BalanceContribution::getCashBalanceContribution),
                    dayCashSettlement.flatMap(DayAndTotalCashSettlement::getDay),
                    requiredInitialMargin.map(BigDecimal::negate)
                ))
            .build();
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

    public static <K, T, L, R> Stream<T> mergeAndFlatten(final Map<K, L> left, final Map<K, R> right, final Merger<K, T, L, R> merger) {
        return Streams.concat(left.keySet().stream(), right.keySet().stream())
            .distinct()
            .map(
                key -> merger.merge(
                    key,
                    Optional.ofNullable(left.get(key)),
                    Optional.ofNullable(right.get(key))
                )
            );
    }

    public static <T, R> Collector<T, ?, R> twoWayCollector(
        final Predicate<T> predicate,
        final Function<T, BigDecimal> mapper,
        final BiFunction<Optional<BigDecimal>, Optional<BigDecimal>, R> finisher) {

        return collectingAndThen(
            reducing(
                ImmutablePair.of(Optional.<BigDecimal>empty(), Optional.<BigDecimal>empty()),
                o -> predicate.test(o)
                    ? ImmutablePair.of(mapper.andThen(Optional::of).apply(o), Optional.<BigDecimal>empty())
                    : ImmutablePair.of(Optional.<BigDecimal>empty(), mapper.andThen(Optional::of).apply(o)
                ),
                (a, b) -> ImmutablePair.of(sumAll(a.getLeft(), b.getLeft()), sumAll(a.getRight(), b.getRight()))
            ),
            res -> finisher.apply(res.getLeft(), res.getRight())
        );
    }

    private static Optional<BigDecimal> sumAll(final Optional<BigDecimal>...values) {
        return Arrays
            .stream(values)
            .flatMap(Optional::stream)
            .reduce(BigDecimal::add);
    }

    @FunctionalInterface
    interface Merger<K, T, L, R> {
        T merge(K key, Optional<L> left, Optional<R> right);
    }
}
