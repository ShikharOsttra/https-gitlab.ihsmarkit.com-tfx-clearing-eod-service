package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.CASH;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.DAY;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.FOLLOWING;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.TOTAL;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;
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
import com.ihsmarkit.tfx.eod.model.CcyParticipantAmount;
import com.ihsmarkit.tfx.eod.model.DayAndTotalCashSettlement;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.model.ParticipantMargin;
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
        return input
            .collect(
                groupingBy(
                    CcyParticipantAmount::getParticipant,
                    groupingBy(
                        CcyParticipantAmount::getCurrencyPair,
                        reducing(ZERO, CcyParticipantAmount::getAmount, BigDecimal::add)
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
                safeSum(day, following).map(margin -> Pair.of(TOTAL, margin)),
                day.map(margin -> Pair.of(DAY, margin)),
                following.map(margin -> Pair.of(FOLLOWING, margin))
            ).flatMap(Optional::stream).collect(toMap(Pair::getLeft, Pair::getRight))
        );
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
                    (cash, nonCash) -> new BalanceContribution(safeSum(nonCash, cash).orElse(ZERO), cash.orElse(ZERO))
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

    private ParticipantMargin createEodParticipantMargin(
        final ParticipantEntity participant,
        final Optional<BigDecimal> requiredInitialMargin,
        final Optional<DayAndTotalCashSettlement> dayCashSettlement,
        final Optional<BalanceContribution> balance) {

        return ParticipantMargin.builder()
            .participant(participant)
            .initialMargin(requiredInitialMargin)
            .requiredAmount(safeDiff(requiredInitialMargin, dayCashSettlement.flatMap(DayAndTotalCashSettlement::getDay)))
            .totalDeficit(safeDiff(balance.map(BalanceContribution::getTotalBalanceContribution), dayCashSettlement.map(DayAndTotalCashSettlement::getTotal)))
            .cashDeficit(safeDiff(balance.map(BalanceContribution::getCashBalanceContribution), dayCashSettlement.flatMap(DayAndTotalCashSettlement::getDay)))
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
                (a, b) -> ImmutablePair.of(safeSum(a.getLeft(), b.getLeft()), safeSum(a.getRight(), b.getRight()))
            ),
            res -> finisher.apply(res.getLeft(), res.getRight())
        );
    }

    public static Optional<BigDecimal> safeSum(final Optional<BigDecimal> left, final Optional<BigDecimal> right) {
        return
            left
                .flatMap(l -> right.map(l::add))
                .or(() -> left)
                .or(() -> right);
    }

    private static Optional<BigDecimal> safeDiff(final Optional<BigDecimal> left, final Optional<BigDecimal> right) {
        return safeSum(left, right.map(BigDecimal::negate));
    }
}
