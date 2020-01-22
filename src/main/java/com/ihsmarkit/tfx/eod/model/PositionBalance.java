package com.ihsmarkit.tfx.eod.model;

import static java.util.function.UnaryOperator.identity;
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

import com.ihsmarkit.tfx.common.streams.Streams;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.eod.service.Slicer;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PositionBalance {

    private static final Comparator<RawPositionData> BY_AMOUNT_AND_PARTICIPANT_CODE  = Comparator
        .comparing((RawPositionData position) -> position.getAmount().abs(), BigDecimal::compareTo).reversed()
        .thenComparing(position -> position.getParticipant().getCode());

    private final PositionList sell;
    private final PositionList buy;

    public static PositionBalance of(final Stream<RawPositionData> positions) {

        final Map<Boolean, PositionList.PositionListBuilder> separatedByDirection = positions
            .filter(position -> position.getAmount().compareTo(BigDecimal.ZERO) != 0)
            .map(PositionList.PositionListBuilder::of)
            .collect(
                partitioningBy(
                    position -> position.getTotal().compareTo(BigDecimal.ZERO) > 0,
                    collectingAndThen(
                        reducing(PositionList.PositionListBuilder::combine),
                        optionalPositionListBuilder -> optionalPositionListBuilder.orElseGet(PositionList.PositionListBuilder::empty)
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
                trade -> Stream.of(
                    new RawPositionData(trade.getOriginator(), trade.getAmount()),
                    new RawPositionData(trade.getCounterparty(), trade.getAmount().negate())
                )
            ).collect(
                groupingBy(
                    RawPositionData::getParticipant,
                    Streams.summingBigDecimal(RawPositionData::getAmount)
                )
            );

        return PositionBalance.of(
            Stream.concat(
                sell.getPositions().stream(),
                buy.getPositions().stream()
            ).map(
                position -> new RawPositionData(
                    position.getParticipant(),
                    Optional.ofNullable(positionChanges.get(position.getParticipant()))
                        .map(position.getAmount()::add)
                        .orElseGet(position::getAmount)
                )
            )
        );
    }

    public Stream<BalanceTrade> rebalance(final int rounding) {

        if (buy.getNet().compareTo(sell.getNet().abs()) >= 0) {
            return rebalanceImpl(buy, sell, rounding, BigDecimal::negate);
        } else {
            return rebalanceImpl(sell, buy, rounding, identity());
        }
    }

    public Stream<BalanceTrade> allocateResidual() {
        if (buy.getNet().compareTo(sell.getNet().abs()) >= 0) {
            return allocateResidual(buy, sell, BigDecimal::negate);
        } else {
            return allocateResidual(sell, buy, identity());
        }
    }

    private Stream<BalanceTrade> allocateResidual(
        final PositionList from,
        final PositionList to,
        final UnaryOperator<BigDecimal> amountAdjuster
    ) {

        final Optional<RawPositionData> fromPosition = from.getPositions().stream().sorted(BY_AMOUNT_AND_PARTICIPANT_CODE).findFirst();

        if (fromPosition.isPresent()) {
            final RawPositionData other = fromPosition.get();
            return to.getPositions().stream()
                .map(position -> new BalanceTrade(other.getParticipant(), position.getParticipant(), amountAdjuster.apply(position.getAmount().abs())));
        } else {
            return Stream.of();
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
            new Slicer<>(toReduce, (RawPositionData position) -> position.getAmount().abs());

        return from.getPositions().stream().sorted(BY_AMOUNT_AND_PARTICIPANT_CODE)
            .flatMap(
                position -> slicer.produce(
                    position.getAmount()
                        .abs()
                        .multiply(to.getNet().abs())
                        .divide(from.getNet().abs(), RoundingMode.FLOOR)
                        .setScale(-rounding, RoundingMode.FLOOR),
                    (other, slice) -> new BalanceTrade(position.getParticipant(), other.getParticipant(), amountAdjuster.apply(slice))
                )
            );
    }

}
