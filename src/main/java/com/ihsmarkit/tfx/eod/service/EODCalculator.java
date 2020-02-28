package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.common.streams.Streams.summingBigDecimal;
import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.CASH;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.DAY;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.FOLLOWING;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.TOTAL;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.BUY;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.NET;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SELL;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;
import static java.math.BigDecimal.ZERO;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
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
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import com.google.common.collect.Streams;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.MarginAlertConfigurationEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.MarginAlertLevel;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.BalanceContribution;
import com.ihsmarkit.tfx.eod.model.BalanceTrade;
import com.ihsmarkit.tfx.eod.model.BuySellAmounts;
import com.ihsmarkit.tfx.eod.model.CcyParticipantAmount;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.model.ParticipantMargin;
import com.ihsmarkit.tfx.eod.model.ParticipantPosition;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;

import io.vavr.Function3;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class EODCalculator {

    private static final int DEFAULT_ROUNDING = 5;
    private static final BigDecimal SWAP_POINT_UNIT = BigDecimal.ONE.scaleByPowerOfTen(-3);
    private static final BigDecimal MARGIN_RATIO_FACTOR = BigDecimal.ONE.scaleByPowerOfTen(-2);

    private final TradeOrPositionEssentialsMapper tradeOrPositionMapper;

    private final SingleCurrencyRebalanceCalculator rebalanceCalculator;

    private BigDecimal getJpyAmount(final CurrencyPairEntity currencyPair, final BigDecimal amount, final Function<String, BigDecimal> jpyRates) {
        return Optional.of(currencyPair)
            .map(CurrencyPairEntity::getValueCurrency)
            .filter(not(JPY::equals))
            .map(jpyRates)
            .map(amount::multiply)
            .orElse(amount);
    }

    private ParticipantCurrencyPairAmount calc(
        final ParticipantCurrencyPairAmount position,
        final Function<String, BigDecimal> jpyRates,
        final BigDecimal multiplier
    ) {

        return ParticipantCurrencyPairAmount.of(
            position.getParticipant(),
            position.getCurrencyPair(),
            getJpyAmount(position.getCurrencyPair(), position.getAmount(), jpyRates)
                .multiply(multiplier)
                .setScale(0, RoundingMode.FLOOR)
        );

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

        return Optional
            .ofNullable(swapPointResolver.apply(trade.getCurrencyPair()))
            .map(swapPoint -> calc(trade, jpyRates, SWAP_POINT_UNIT.multiply(swapPoint)))
            .orElseGet(() -> ParticipantCurrencyPairAmount.of(trade.getParticipant(), trade.getCurrencyPair(), ZERO));
    }

    public ParticipantCurrencyPairAmount calculateSwapPoint(
        final ParticipantCurrencyPairAmount position,
        final Function<CurrencyPairEntity, BigDecimal> swapPointResolver,
        final Function<String, BigDecimal> jpyRates) {

        return Optional
            .ofNullable(swapPointResolver.apply(position.getCurrencyPair()))
            .map(swapPoint -> calc(position, jpyRates, SWAP_POINT_UNIT.multiply(swapPoint)))
            .orElseGet(() -> ParticipantCurrencyPairAmount.of(position.getParticipant(), position.getCurrencyPair(), ZERO));
    }

    public ParticipantCurrencyPairAmount calculateSwapPoint(
        final ParticipantPositionEntity participantPosition,
        final Function<CurrencyPairEntity, BigDecimal> swapPointResolver,
        final Function<String, BigDecimal> jpyRates) {

        return calculateSwapPoint(tradeOrPositionMapper.convertPosition(participantPosition), swapPointResolver, jpyRates);
    }

    public ParticipantCurrencyPairAmount calculateSwapPoint(
        final TradeEntity participantPosition,
        final Function<CurrencyPairEntity, BigDecimal> swapPointResolver,
        final Function<String, BigDecimal> jpyRates) {

        return calculateSwapPoint(tradeOrPositionMapper.convertTrade(participantPosition), swapPointResolver, jpyRates);
    }

    public Map<ParticipantEntity, BigDecimal> calculateRequiredInitialMargin(
        final Stream<ParticipantPositionEntity> positions,
        final BiFunction<CurrencyPairEntity, ParticipantEntity, BigDecimal> marginRatioResolver,
        final Function<String, BigDecimal> jpyRates
    ) {
        return aggregatePositions(positions)
            .map(position -> ImmutablePair.of(
                position.getParticipant(),
                jpyRates.apply(position.getCurrencyPair().getBaseCurrency())
                    .multiply(position.getAmount())
                    .multiply(marginRatioResolver.apply(position.getCurrencyPair(), position.getParticipant())
                        .multiply(MARGIN_RATIO_FACTOR))
                    .setScale(0, RoundingMode.CEILING).abs()
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

    private Stream<ParticipantCurrencyPairAmount> flatten(final Map<ParticipantEntity, Map<CurrencyPairEntity, BigDecimal>> input) {
        return input.entrySet().stream()
            .flatMap(participantBalance -> participantBalance.getValue().entrySet().stream()
                .map(ccyPairBalances -> ParticipantCurrencyPairAmount.of(participantBalance.getKey(), ccyPairBalances.getKey(), ccyPairBalances.getValue()))
            );
    }

    public Stream<ParticipantCurrencyPairAmount> aggregatePositions(final Stream<ParticipantPositionEntity> positions) {
        return flatten(aggregate(positions.map(tradeOrPositionMapper::convertPosition)));
    }

    public Map<ParticipantEntity, Map<EodProductCashSettlementType, EnumMap<EodCashSettlementDateType, BigDecimal>>>
                            aggregateRequiredMargin(final List<EodProductCashSettlementEntity> margins, final LocalDate businessDate) {

        final Optional<LocalDate> followingDate = margins.stream().filter(margin -> margin.getSettlementDate().isAfter(businessDate))
            .map(EodProductCashSettlementEntity::getSettlementDate).min(LocalDate::compareTo);

        return margins.stream()
            .collect(
                groupingBy(
                    EodProductCashSettlementEntity::getParticipant,
                    groupingBy(
                        EodProductCashSettlementEntity::getType,
                        () -> new EnumMap<>(EodProductCashSettlementType.class),
                        dateTypeCollector(
                            margin -> businessDate.isEqual(margin.getSettlementDate()),
                            margin -> followingDate.map(date -> date.equals(margin.getSettlementDate())).orElse(Boolean.FALSE),
                            margin -> margin.getAmount().getValue(),
                            EODCalculator::marginMap
                        )
                    )
                )
            );
    }

    private static <T, R> Collector<T, ?, R> dateTypeCollector(
        final Predicate<T> dayPredicate,
        final Predicate<T> followingPredicate,
        final Function<T, BigDecimal> mapper,
        final Function3<Optional<BigDecimal>, Optional<BigDecimal>, Optional<BigDecimal>, R> finisher) {

        return collectingAndThen(
            reducing(
                ImmutableTriple.of(Optional.<BigDecimal>empty(), Optional.<BigDecimal>empty(), Optional.<BigDecimal>empty()),
                o -> dayPredicate.test(o)
                    ? ImmutableTriple.of(mapper.andThen(Optional::of).apply(o), Optional.<BigDecimal>empty(), Optional.<BigDecimal>empty())
                    : followingPredicate.test(o)
                        ? ImmutableTriple.of(Optional.<BigDecimal>empty(), mapper.andThen(Optional::of).apply(o), Optional.<BigDecimal>empty())
                        : ImmutableTriple.of(Optional.<BigDecimal>empty(), Optional.<BigDecimal>empty(), mapper.andThen(Optional::of).apply(o)),
                (a, b) -> ImmutableTriple.of(sumAll(a.left, b.left), sumAll(a.middle, b.middle), sumAll(a.right, b.right))
            ),
            res -> finisher.apply(res.left, res.middle, res.right)
        );
    }


    private static EnumMap<EodCashSettlementDateType, BigDecimal> marginMap(final Optional<BigDecimal> day, final Optional<BigDecimal> following,
        final Optional<BigDecimal> future) {
        return new EnumMap<EodCashSettlementDateType, BigDecimal>(
            Stream.of(
                sumAll(day, future, following).map(margin -> Pair.of(TOTAL, margin)),
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
                    buySell
                        .map(BuySellAmounts::getBuy)
                        .filter(EODCalculator::isNotEqualToZero)
                        .map(buy -> ParticipantPosition.of(key.getParticipant(), key.getCurrencyPair(), buy, BUY)),
                    buySell
                        .map(BuySellAmounts::getSell)
                        .filter(EODCalculator::isNotEqualToZero)
                        .map(sell -> ParticipantPosition.of(key.getParticipant(), key.getCurrencyPair(), sell, SELL)),
                    sumAll(buySell.map(BuySellAmounts::getBuy), buySell.map(BuySellAmounts::getSell), sod)
                        .map(net -> ParticipantPosition.of(key.getParticipant(), key.getCurrencyPair(), net, NET))
                ).flatMap(Optional::stream)
        ).flatMap(identity());
    }

    public Stream<ParticipantCurrencyPairAmount> calculateAndAggregateDailyMtm(final Collection<ParticipantPositionEntity> positions,
        final Function<CurrencyPairEntity, BigDecimal> dsp,
        final Function<String, BigDecimal> jpyRates) {

        return positions.stream()
            .map(tradeOrPositionMapper::convertPosition)
            .map(t -> calculateMtmValue(t, dsp, jpyRates));
    }

    public Map<CurrencyPairEntity, List<BalanceTrade>> rebalanceLPPositions(final Stream<ParticipantPositionEntity> positions,
        final Map<CurrencyPairEntity, Long> thresholds) {

        return positions
            .map(tradeOrPositionMapper::convertPosition)
            .collect(groupingBy(
                TradeOrPositionEssentials::getCurrencyPair,
                Collectors.toList()
            )).entrySet().stream()
            .collect(
                toMap(
                    Map.Entry::getKey,
                    entry -> rebalanceCalculator.rebalance(entry.getValue(), thresholds.get(entry.getKey()), DEFAULT_ROUNDING)
                )
            );
    }

    public Stream<ParticipantMargin> calculateParticipantMargin(
        final Map<ParticipantEntity, BigDecimal> requiredInitialMargin,
        final Map<ParticipantEntity, EnumMap<EodCashSettlementDateType, BigDecimal>> dayCashSettlement,
        final Map<ParticipantEntity, BalanceContribution> deposits,
        final Map<Long, List<MarginAlertConfigurationEntity>> marginAlertConfigurationsProvider
    ) {
        return Stream.of(requiredInitialMargin, dayCashSettlement, deposits)
            .map(Map::keySet)
            .flatMap(Set::stream)
            .distinct()
            .map(
                participant -> createEodParticipantMargin(
                    participant,
                    Optional.ofNullable(marginAlertConfigurationsProvider.get(participant.getId()))
                        .orElseThrow(() -> new IllegalStateException("Unable to resolve margin alert configuration for participant: " + participant.getCode())),
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
                    BalanceContribution::new
                )
            )
        );

    }

    private ParticipantMargin createEodParticipantMargin(
        final ParticipantEntity participant,
        final List<MarginAlertConfigurationEntity> marginAlertConfigurations,
        final Optional<BigDecimal> requiredInitialMargin,
        final Optional<EnumMap<EodCashSettlementDateType, BigDecimal>> dayCashSettlement,
        final Optional<BalanceContribution> balance) {

        final Optional<BigDecimal> pnl = dayCashSettlement.map(vm -> vm.get(TOTAL));
        final Optional<BigDecimal> todaySettlement = dayCashSettlement.map(vm -> vm.get(DAY));
        final Optional<BigDecimal> nextDaySettlement = dayCashSettlement.map(vm -> vm.get(FOLLOWING));
        final Optional<BigDecimal> cashCollateral = balance.map(BalanceContribution::getCashBalanceContribution);
        final Optional<BigDecimal> logCollateral = balance.map(BalanceContribution::getLogBalanceContribution);
        final Optional<BigDecimal> effectiveMargin = calculateEffectiveMarginRatio(cashCollateral, logCollateral, pnl, requiredInitialMargin);

        return ParticipantMargin.builder()
            .participant(participant)
            .initialMargin(requiredInitialMargin)
            .marginRatio(effectiveMargin)
            .marginAlertLevel(toMarginAlertLevel(marginAlertConfigurations, effectiveMargin))
            .pnl(pnl)
            .cashCollateralAmount(cashCollateral)
            .logCollateralAmount(logCollateral)
            .todaySettlement(todaySettlement)
            .nextDaySettlement(nextDaySettlement)
            .requiredAmount(sumAll(requiredInitialMargin, pnl.map(BigDecimal::negate)))
            .totalDeficit(sumAll(cashCollateral, logCollateral, pnl, requiredInitialMargin.map(BigDecimal::negate)))
            .cashDeficit(sumAll(cashCollateral, todaySettlement, requiredInitialMargin.map(BigDecimal::negate)))
            .build();
    }

    private static Optional<BigDecimal> calculateEffectiveMarginRatio(
        final Optional<BigDecimal> cashCollateral,
        final Optional<BigDecimal> logCollateral,
        final Optional<BigDecimal> pnl,
        final Optional<BigDecimal> initialMargin
    ) {
        return cashCollateral
            .flatMap(value -> logCollateral.map(value::add))
            .flatMap(value -> pnl.map(value::add))
            .flatMap(value -> initialMargin
                .filter(not(ZERO::equals))
                .map(amount -> value.divide(amount, 2, RoundingMode.HALF_DOWN)));
    }

    private static Optional<MarginAlertLevel> toMarginAlertLevel(
        final List<MarginAlertConfigurationEntity> marginAlertConfigurations,
        final Optional<BigDecimal> effectiveMargin
    ) {
        return effectiveMargin.flatMap(value -> marginAlertConfigurations.stream()
            .filter(configuration -> configuration.getTriggerLevel().compareTo(value) <= 0)
            .max(Comparator.comparing(MarginAlertConfigurationEntity::getTriggerLevel))
            .map(MarginAlertConfigurationEntity::getLevel)
        );
    }

    private static <K, T, L, R> Stream<T> mergeAndFlatten(final Map<K, L> left, final Map<K, R> right, final Merger<K, T, L, R> merger) {
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
        final Predicate<T> isBuyPredicate,
        final Function<T, BigDecimal> mapper,
        final BiFunction<BigDecimal, BigDecimal, R> finisher) {
        return collectingAndThen(
            groupingBy(isBuyPredicate::test, summingBigDecimal(mapper)),
            map -> finisher.apply(map.getOrDefault(Boolean.TRUE, ZERO), map.getOrDefault(Boolean.FALSE, ZERO))
        );
    }

    private static Optional<BigDecimal> sumAll(final Optional<BigDecimal>... values) {
        return Arrays.stream(values)
            .flatMap(Optional::stream)
            .reduce(BigDecimal::add);
    }

    private static boolean isNotEqualToZero(final BigDecimal value) {
        return !isEqualToZero(value);
    }

    private static boolean isEqualToZero(final BigDecimal value) {
        return ZERO.compareTo(value) == 0;
    }

    @FunctionalInterface
    interface Merger<K, T, L, R> {

        T merge(K key, Optional<L> left, Optional<R> right);
    }
}
