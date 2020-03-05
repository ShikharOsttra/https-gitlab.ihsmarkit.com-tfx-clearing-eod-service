package com.ihsmarkit.tfx.eod.service;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.ihsmarkit.tfx.common.math.BigDecimals.isEqualToZero;
import static com.ihsmarkit.tfx.common.math.BigDecimals.isGreaterThanZero;
import static com.ihsmarkit.tfx.common.streams.Streams.summingBigDecimal;
import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.CASH;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.DAY;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.FOLLOWING;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.TOTAL;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.BUY;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.MONTHLY_VOLUME_NET_BUY;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.MONTHLY_VOLUME_NET_SELL;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.NET;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SELL;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;
import static java.math.BigDecimal.ZERO;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
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
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.ihsmarkit.tfx.common.collectors.GuavaCollectors;
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
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.model.ParticipantMargin;
import com.ihsmarkit.tfx.eod.model.ParticipantPosition;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;

import io.vavr.Function3;
import io.vavr.Function4;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import lombok.RequiredArgsConstructor;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class EODCalculator {

    private static final Tuple3<Optional<BigDecimal>, Optional<BigDecimal>, Optional<BigDecimal>> EMPTY_TUPLE3 =
        Tuple.of(Optional.empty(), Optional.empty(), Optional.empty());
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

        return Optional.ofNullable(swapPointResolver.apply(trade.getCurrencyPair()))
            .map(swapPoint -> calc(trade, jpyRates, SWAP_POINT_UNIT.multiply(swapPoint)))
            .orElseGet(() -> ParticipantCurrencyPairAmount.of(trade.getParticipant(), trade.getCurrencyPair(), ZERO));
    }

    public ParticipantCurrencyPairAmount calculateSwapPoint(
        final ParticipantCurrencyPairAmount position,
        final Function<CurrencyPairEntity, BigDecimal> swapPointResolver,
        final Function<String, BigDecimal> jpyRates) {

        return Optional.ofNullable(swapPointResolver.apply(position.getCurrencyPair()))
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
            .collect(
                toMap(
                    ParticipantCurrencyPairAmount::getParticipant,
                    position -> jpyRates.apply(position.getCurrencyPair().getBaseCurrency())
                        .multiply(position.getAmount())
                        .multiply(marginRatioResolver.apply(position.getCurrencyPair(), position.getParticipant())
                            .multiply(MARGIN_RATIO_FACTOR))
                        .setScale(0, RoundingMode.CEILING).abs(),
                    BigDecimal::add
                )
            );
    }

    public Stream<ParticipantCurrencyPairAmount> calculateAndAggregateInitialMtm(
        final Stream<TradeEntity> trades,
        final Function<CurrencyPairEntity, BigDecimal> dsp,
        final Function<String, BigDecimal> jpyRates
    ) {
        return aggregateAndFlatten(
            trades
                .map(tradeOrPositionMapper::convertTrade)
                .map(essentials -> calculateMtmValue(essentials, dsp, jpyRates))
        );
    }

    public Stream<ParticipantCurrencyPairAmount> calculateAndAggregateSwapPnL(
        final Stream<TradeEntity> trades,
        final Function<CurrencyPairEntity, BigDecimal> swapPointResolver,
        final Function<String, BigDecimal> jpyRates
    ) {

        return aggregateAndFlatten(
            trades
                .map(tradeOrPositionMapper::convertTrade)
                .map(essentials -> calculateSwapPoint(essentials, swapPointResolver, jpyRates))
        );
    }

    private static Stream<ParticipantCurrencyPairAmount> aggregateAndFlatten(final Stream<? extends CcyParticipantAmount> ccyPairAmounts) {
        final var aggregatedTable = ccyPairAmounts
            .collect(
                GuavaCollectors.toTable(
                    CcyParticipantAmount::getParticipant,
                    CcyParticipantAmount::getCurrencyPair,
                    summingBigDecimal(CcyParticipantAmount::getAmount)
                )
            );
        return StreamEx.of(aggregatedTable.cellSet())
            .map(cell -> ParticipantCurrencyPairAmount.of(
                checkNotNull(cell.getRowKey()),
                checkNotNull(cell.getColumnKey()),
                checkNotNull(cell.getValue())
            ));
    }

    public Stream<ParticipantCurrencyPairAmount> aggregatePositions(final Stream<ParticipantPositionEntity> positions) {
        return aggregateAndFlatten(positions.map(tradeOrPositionMapper::convertPosition));
    }

    public Map<ParticipantEntity, Map<EodProductCashSettlementType, EnumMap<EodCashSettlementDateType, BigDecimal>>>
        aggregateRequiredMargin(final List<EodProductCashSettlementEntity> margins, final LocalDate businessDate) {

        final Optional<LocalDate> followingDate = margins.stream()
            .map(EodProductCashSettlementEntity::getSettlementDate)
            .filter(settlementDate -> settlementDate.isAfter(businessDate))
            .min(LocalDate::compareTo);

        return margins.stream()
            .collect(
                groupingBy(
                    EodProductCashSettlementEntity::getParticipant,
                    groupingBy(
                        EodProductCashSettlementEntity::getType,
                        () -> new EnumMap<>(EodProductCashSettlementType.class),
                        dateTypeCollector(
                            margin -> businessDate.isEqual(margin.getSettlementDate()),
                            margin -> followingDate
                                .map(date -> date.equals(margin.getSettlementDate()))
                                .orElse(Boolean.FALSE),
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
                EMPTY_TUPLE3,
                item -> {
                    final Optional<BigDecimal> value = mapper.andThen(Optional::of).apply(item);
                    return dayPredicate.test(item)
                        ? EMPTY_TUPLE3.update1(value)
                        : followingPredicate.test(item)
                            ? EMPTY_TUPLE3.update2(value)
                            : EMPTY_TUPLE3.update3(value);
                },
                (prev, next) ->
                    Tuple.of(
                        sumAll(prev._1(), next._1()),
                        sumAll(prev._2(), next._2()),
                        sumAll(prev._3(), next._3())
                    )
            ),
            result -> result.apply(finisher)
        );
    }

    private static EnumMap<EodCashSettlementDateType, BigDecimal> marginMap(final Optional<BigDecimal> day, final Optional<BigDecimal> following,
        final Optional<BigDecimal> future) {
        return new EnumMap<>(
            EntryStream.of(
                TOTAL, sumAll(day, future, following),
                DAY, day,
                FOLLOWING, following
            )
                .flatMapValues(Optional::stream)
                .toMap()
        );
    }

    public Stream<ParticipantCurrencyPairAmount> netAll(final Stream<? extends CcyParticipantAmount> trades) {
        return aggregateAndFlatten(trades);
    }

    public Stream<ParticipantPosition> netAllByBuySell(
        final Stream<TradeOrPositionEssentials> tradesToNet,
        final Stream<? extends CcyParticipantAmount> sodPositions
    ) {

        return mergeAndFlatten(
            StreamEx.of(sodPositions)
                .collect(
                    Tables.toTable(
                        CcyParticipantAmount::getParticipant,
                        CcyParticipantAmount::getCurrencyPair,
                        CcyParticipantAmount::getAmount,
                        HashBasedTable::create
                    )
                ),
            StreamEx.of(tradesToNet)
                .collect(
                    GuavaCollectors.toTable(
                        CcyParticipantAmount::getParticipant,
                        CcyParticipantAmount::getCurrencyPair,
                        twoWayCollector(
                            isGreaterThanZero(TradeOrPositionEssentials::getAmount),
                            CcyParticipantAmount::getAmount,
                            BuySellAmounts::new
                        )
                    )
                ),
            (participant, currencyPair, sod, buySell) ->
                Stream.of(
                    buySell
                        .map(BuySellAmounts::getBuy)
                        .filter(not(isEqualToZero()))
                        .map(buy -> ParticipantPosition.of(participant, currencyPair, buy, BUY)),
                    buySell
                        .map(BuySellAmounts::getSell)
                        .filter(not(isEqualToZero()))
                        .map(sell -> ParticipantPosition.of(participant, currencyPair, sell, SELL)),
                    sumAll(
                        buySell
                            .map(BuySellAmounts::getBuy),
                        buySell
                            .map(BuySellAmounts::getSell),
                        sod
                    )
                        .map(net -> ParticipantPosition.of(participant, currencyPair, net, NET))
                )
                    .flatMap(Optional::stream)
        );
    }

    public Stream<ParticipantPosition> netByBuySellForMonthlyVolumeReport(final Stream<TradeOrPositionEssentials> tradesToNet) {
        final var nettedBuySellAmounts = tradesToNet
            .collect(
                GuavaCollectors.toTable(
                    CcyParticipantAmount::getParticipant,
                    CcyParticipantAmount::getCurrencyPair,
                    twoWayCollector(
                        isGreaterThanZero(TradeOrPositionEssentials::getAmount),
                        CcyParticipantAmount::getAmount,
                        BuySellAmounts::new
                    )
                )
            );
        return StreamEx.of(nettedBuySellAmounts.cellSet())
            .flatMap(cell -> {
                final var participant = checkNotNull(cell.getRowKey());
                final var currencyPair = checkNotNull(cell.getColumnKey());
                final var buySellAmounts = checkNotNull(cell.getValue());
                final BigDecimal buyAmount = buySellAmounts.getBuy();
                final BigDecimal sellAmount = buySellAmounts.getSell();
                return Stream.concat(
                    isEqualToZero(buyAmount)
                        ? Stream.empty()
                        : Stream.of(ParticipantPosition.of(participant, currencyPair, buyAmount, MONTHLY_VOLUME_NET_BUY)),
                    isEqualToZero(sellAmount)
                        ? Stream.empty()
                        : Stream.of(ParticipantPosition.of(participant, currencyPair, sellAmount, MONTHLY_VOLUME_NET_SELL))
                );
            });
    }

    public Stream<ParticipantCurrencyPairAmount> calculateAndAggregateDailyMtm(final Stream<ParticipantPositionEntity> positions,
        final Function<CurrencyPairEntity, BigDecimal> dsp,
        final Function<String, BigDecimal> jpyRates) {

        return positions
            .map(tradeOrPositionMapper::convertPosition)
            .map(tradeOrPositionEssentials -> calculateMtmValue(tradeOrPositionEssentials, dsp, jpyRates));
    }

    public Map<CurrencyPairEntity, List<BalanceTrade>> rebalanceLPPositions(final Stream<ParticipantPositionEntity> positions,
        final Map<CurrencyPairEntity, Long> thresholds) {

        return EntryStream.of(
            positions
                .map(tradeOrPositionMapper::convertPosition)
                .collect(groupingBy(TradeOrPositionEssentials::getCurrencyPair))
        )
            .mapToValue((currencyPair, tradeOrPositionEssentials) ->
                rebalanceCalculator.rebalance(tradeOrPositionEssentials, thresholds.get(currencyPair), DEFAULT_ROUNDING)
            )
            .toMap();
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
                .filter(not(isEqualToZero()))
                .map(amount -> value.divide(amount, 2, RoundingMode.HALF_DOWN))
            );
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

    private static <R, C, LEFT, RIGHT, T> Stream<T> mergeAndFlatten(final Table<R, C, LEFT> left, final Table<R, C, RIGHT> right,
        final Function4<R, C, Optional<LEFT>, Optional<RIGHT>, Stream<T>> merger) {
        return StreamEx.of(left, right)
            .flatCollection(Table::cellSet)
            .mapToEntry(Table.Cell::getRowKey, Table.Cell::getColumnKey)
            .distinct()
            .flatMapKeyValue(
                (rowKey, columnKey) -> merger.apply(
                    rowKey,
                    columnKey,
                    Optional.ofNullable(left.get(rowKey, columnKey)),
                    Optional.ofNullable(right.get(rowKey, columnKey))
                )
            );
    }

    public static <T, R> Collector<T, ?, R> twoWayCollector(
        final Predicate<T> splittingPredicate,
        final Function<T, BigDecimal> mapper,
        final BiFunction<BigDecimal, BigDecimal, R> finisher) {
        return collectingAndThen(
            partitioningBy(splittingPredicate, summingBigDecimal(mapper)),
            map -> finisher.apply(
                map.getOrDefault(Boolean.TRUE, ZERO),
                map.getOrDefault(Boolean.FALSE, ZERO)
            )
        );
    }

    @SafeVarargs
    private static Optional<BigDecimal> sumAll(final Optional<BigDecimal>... values) {
        return Arrays.stream(values)
            .flatMap(Optional::stream)
            .reduce(BigDecimal::add);
    }
}
