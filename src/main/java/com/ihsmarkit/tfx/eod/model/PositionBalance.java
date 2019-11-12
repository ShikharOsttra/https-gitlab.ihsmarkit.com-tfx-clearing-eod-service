package com.ihsmarkit.tfx.eod.model;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.reducing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.eod.service.Slicer;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PositionBalance {

    private static final Comparator<RawPositionData> BY_AMOUNT_AND_PARTICIPANT_CODE  = Comparator
        .comparing((RawPositionData t) -> t.getAmount().abs(), BigDecimal::compareTo).reversed()
        .thenComparing(t -> t.getParticipant().getCode());

    private final PositionList sell;
    private final PositionList buy;

    public static PositionBalance of(final Stream<RawPositionData> positions) {

        final Map<Boolean, PositionList.PositionListBuilder> separatedByDirection = positions
            .filter(a -> a.getAmount().compareTo(BigDecimal.ZERO) != 0)
            .map(PositionList.PositionListBuilder::of)
            .collect(
                partitioningBy(
                    position -> position.getTotal().compareTo(BigDecimal.ZERO) > 0,
                    collectingAndThen(
                        reducing(PositionList.PositionListBuilder::combine),
                        o -> o.orElseGet(PositionList.PositionListBuilder::empty)
                    )
                )
            );

        return
            PositionBalance.builder()
                .sell(
                    Optional.ofNullable(separatedByDirection.get(Boolean.FALSE))
                        .orElseGet(PositionList.PositionListBuilder::empty)
                        .build()
                ).buy(
                    Optional.ofNullable(separatedByDirection.get(Boolean.TRUE))
                        .orElseGet(PositionList.PositionListBuilder::empty)
                        .build()
                ).build();
    }

    public PositionBalance applyTrades(final Stream<BalanceTrade> trades) {
        final Map<ParticipantEntity, BigDecimal> positionChanges = trades
            .flatMap(
                t -> Stream.of(
                    new RawPositionData(t.getOriginator(), t.getAmount()),
                    new RawPositionData(t.getCounterpart(), t.getAmount().negate())
                )
            ).collect(
                groupingBy(
                    RawPositionData::getParticipant,
                    reducing(BigDecimal.ZERO, RawPositionData::getAmount, BigDecimal::add)
                )
            );

        return PositionBalance.of(
            Stream.concat(
                sell.getPositions().stream(),
                buy.getPositions().stream()
            ).map(
                t -> new RawPositionData(
                    t.getParticipant(),
                    Optional.ofNullable(positionChanges.get(t.getParticipant()))
                        .map(t.getAmount()::add)
                        .orElseGet(t::getAmount)
                )
            )
        );
    }

    public Stream<BalanceTrade> rebalance(final int rounding) {

        if (buy.getNet().compareTo(sell.getNet().abs()) >= 0) {
            return rebalanceImpl(buy, sell, rounding, BigDecimal::negate);
        } else {
            return rebalanceImpl(sell, buy, rounding, a -> a);
        }

    }

    private Stream<BalanceTrade> rebalanceImpl(
        final PositionList from,
        final PositionList to,
        final int rounding,
        final UnaryOperator<BigDecimal> amountAdjuster
    ) {

        final Queue<RawPositionData> toReduce = new PriorityQueue<>(BY_AMOUNT_AND_PARTICIPANT_CODE);
        toReduce.addAll(to.getPositions());

        final Slicer<RawPositionData> slicer =
            new Slicer<>(toReduce, (RawPositionData t) -> t.getAmount().abs());

        return from.getPositions().stream().sorted(BY_AMOUNT_AND_PARTICIPANT_CODE)
            .flatMap(
                t -> slicer.produce(
                    t.getAmount()
                        .abs()
                        .multiply(to.getNet().abs())
                        .divide(from.getNet().abs(), RoundingMode.FLOOR)
                        .setScale(-rounding, RoundingMode.FLOOR),
                    (a, b) -> new BalanceTrade(t.getParticipant(), a.getParticipant(), amountAdjuster.apply(b))
                )
            );
    }


}
