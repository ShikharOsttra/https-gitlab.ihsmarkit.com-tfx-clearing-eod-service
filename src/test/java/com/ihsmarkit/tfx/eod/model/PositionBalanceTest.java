package com.ihsmarkit.tfx.eod.model;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;

class PositionBalanceTest {

    private static final ParticipantEntity PARTICIPANT_A = aParticipantEntityBuilder().name("A").code("1").build();
    private static final ParticipantEntity PARTICIPANT_B = aParticipantEntityBuilder().name("B").code("2").build();
    private static final ParticipantEntity PARTICIPANT_C = aParticipantEntityBuilder().name("C").code("3").build();
    private static final ParticipantEntity PARTICIPANT_D = aParticipantEntityBuilder().name("D").code("4").build();

    @Test
    void shouldBuildFromSellPositions() {
        PositionBalance balance = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(-12)),
                new RawPositionData(PARTICIPANT_B, BigDecimal.valueOf(-1))
            )
        );

        assertThat(balance.getBuy().getNet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getBuy().getPositions()).isEmpty();

        assertThat(balance.getSell().getNet()).isEqualByComparingTo(BigDecimal.valueOf(-13));
        assertThat(balance.getSell().getPositions())
            .extracting(RawPositionData::getParticipant, RawPositionData::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, BigDecimal.valueOf(-12)),
                tuple(PARTICIPANT_B, BigDecimal.valueOf(-1))
            );
    }

    @Test
    void shouldHandleEmptyPositions() {
        PositionBalance balance = PositionBalance.of(Stream.empty());

        assertThat(balance.getSell().getNet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getSell().getPositions()).isEmpty();
        assertThat(balance.getBuy().getNet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getBuy().getPositions()).isEmpty();
    }

    @Test
    void shouldBuildFromBuyPositions() {
        PositionBalance balance = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(12)),
                new RawPositionData(PARTICIPANT_B, BigDecimal.valueOf(1))
            )
        );

        assertThat(balance.getSell().getNet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getSell().getPositions()).isEmpty();

        assertThat(balance.getBuy().getNet()).isEqualByComparingTo(BigDecimal.valueOf(13));
        assertThat(balance.getBuy().getPositions())
            .extracting(RawPositionData::getParticipant, RawPositionData::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, BigDecimal.valueOf(12)),
                tuple(PARTICIPANT_B, BigDecimal.valueOf(1))
            );
    }

    @Test
    void shouldBuildFromPositions() {
        PositionBalance balance = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(-12)),
                new RawPositionData(PARTICIPANT_B, BigDecimal.valueOf(-1)),
                new RawPositionData(PARTICIPANT_C, BigDecimal.valueOf(-3)),
                new RawPositionData(PARTICIPANT_D, BigDecimal.valueOf(21))
            )
        );

        assertThat(balance.getBuy().getNet()).isEqualByComparingTo(BigDecimal.valueOf(21));
        assertThat(balance.getBuy().getPositions())
            .extracting(RawPositionData::getParticipant, RawPositionData::getAmount)
            .containsOnly(
                tuple(PARTICIPANT_D, BigDecimal.valueOf(21))
            );

        assertThat(balance.getSell().getNet()).isEqualByComparingTo(BigDecimal.valueOf(-16));
        assertThat(balance.getSell().getPositions())
            .extracting(RawPositionData::getParticipant, RawPositionData::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, BigDecimal.valueOf(-12)),
                tuple(PARTICIPANT_B, BigDecimal.valueOf(-1)),
                tuple(PARTICIPANT_C, BigDecimal.valueOf(-3))
            );
    }

    @Test
    void shouldProcessTrades() {
        PositionBalance balance = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(-12)),
                new RawPositionData(PARTICIPANT_B, BigDecimal.valueOf(-1)),
                new RawPositionData(PARTICIPANT_C, BigDecimal.valueOf(-3)),
                new RawPositionData(PARTICIPANT_D, BigDecimal.valueOf(21))
            )
        );

        PositionBalance newBalance = balance.applyTrades(
            Stream.of(
                new BalanceTrade(PARTICIPANT_D, PARTICIPANT_A, BigDecimal.valueOf(-5)),
                new BalanceTrade(PARTICIPANT_D, PARTICIPANT_C, BigDecimal.valueOf(-2))
            )
        );

        assertThat(newBalance.getBuy().getNet()).isEqualByComparingTo(BigDecimal.valueOf(14));
        assertThat(newBalance.getBuy().getPositions())
            .extracting(RawPositionData::getParticipant, RawPositionData::getAmount)
            .containsOnly(
                tuple(PARTICIPANT_D, BigDecimal.valueOf(14))
            );

        assertThat(newBalance.getSell().getNet()).isEqualByComparingTo(BigDecimal.valueOf(-9));
        assertThat(newBalance.getSell().getPositions())
            .extracting(RawPositionData::getParticipant, RawPositionData::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, BigDecimal.valueOf(-7)),
                tuple(PARTICIPANT_B, BigDecimal.valueOf(-1)),
                tuple(PARTICIPANT_C, BigDecimal.valueOf(-1))
            );
    }

    @Test
    void shouldBalanceTwoPositions() {
        Stream<BalanceTrade> trades = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(-12)),
                new RawPositionData(PARTICIPANT_D, BigDecimal.valueOf(21))
            )
        ).rebalance(0);

        assertThat(trades)
            .extracting(BalanceTrade::getOriginator, BalanceTrade::getCounterparty, BalanceTrade::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_D, PARTICIPANT_A, BigDecimal.valueOf(-12))
            );
    }

    @Test
    void shouldBalanceTwoPositionsReducingSell() {
        Stream<BalanceTrade> trades = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(12)),
                new RawPositionData(PARTICIPANT_D, BigDecimal.valueOf(-21))
            )
        ).rebalance(0);

        assertThat(trades)
            .extracting(BalanceTrade::getOriginator, BalanceTrade::getCounterparty, BalanceTrade::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_D, PARTICIPANT_A, BigDecimal.valueOf(12))
            );
    }

    @Test
    void shouldBalanceFourPositions() {
        Stream<BalanceTrade> trades = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(-12)),
                new RawPositionData(PARTICIPANT_B, BigDecimal.valueOf(-1)),
                new RawPositionData(PARTICIPANT_C, BigDecimal.valueOf(-3)),
                new RawPositionData(PARTICIPANT_D, BigDecimal.valueOf(21))
            )
        ).rebalance(0);

        assertThat(trades)
            .extracting(BalanceTrade::getOriginator, BalanceTrade::getCounterparty, BalanceTrade::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_D, PARTICIPANT_A, BigDecimal.valueOf(-12)),
                tuple(PARTICIPANT_D, PARTICIPANT_B, BigDecimal.valueOf(-1)),
                tuple(PARTICIPANT_D, PARTICIPANT_C, BigDecimal.valueOf(-3))
            );
    }

    @Test
    void shouldBalanceFourToZero() {
        Stream<BalanceTrade> trades = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_B, BigDecimal.valueOf(-11)),
                new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(-13)),
                new RawPositionData(PARTICIPANT_C, BigDecimal.valueOf(3)),
                new RawPositionData(PARTICIPANT_D, BigDecimal.valueOf(21))
            )
        ).rebalance(0);

        assertThat(trades)
            .extracting(BalanceTrade::getOriginator, BalanceTrade::getCounterparty, BalanceTrade::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_D, PARTICIPANT_A, BigDecimal.valueOf(-13)),
                tuple(PARTICIPANT_D, PARTICIPANT_B, BigDecimal.valueOf(-8)),
                tuple(PARTICIPANT_C, PARTICIPANT_B, BigDecimal.valueOf(-3))
            );
    }

    @Test
    void shouldSortAccordingToParticipantCodeOnLargerSide() {
        Stream<BalanceTrade> trades = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_B, BigDecimal.valueOf(4)),
                new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(4)),
                new RawPositionData(PARTICIPANT_C, BigDecimal.valueOf(-3)),
                new RawPositionData(PARTICIPANT_D, BigDecimal.valueOf(-2))
            )
        ).rebalance(0);

        assertThat(trades)
            .extracting(BalanceTrade::getOriginator, BalanceTrade::getCounterparty, BalanceTrade::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_B, PARTICIPANT_C, BigDecimal.valueOf(-1)),
                tuple(PARTICIPANT_A, PARTICIPANT_C, BigDecimal.valueOf(-2)),
                tuple(PARTICIPANT_B, PARTICIPANT_D, BigDecimal.valueOf(-1))
            );
    }

    @Test
    void shouldSortAccordingToParticipantCodeOnSmallerrSide() {
        Stream<BalanceTrade> trades = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_B, BigDecimal.valueOf(4)),
                new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(5)),
                new RawPositionData(PARTICIPANT_C, BigDecimal.valueOf(-4)),
                new RawPositionData(PARTICIPANT_D, BigDecimal.valueOf(-4))
            )
        ).rebalance(0);

        assertThat(trades)
            .extracting(BalanceTrade::getOriginator, BalanceTrade::getCounterparty, BalanceTrade::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, PARTICIPANT_C, BigDecimal.valueOf(-4)),
                tuple(PARTICIPANT_B, PARTICIPANT_D, BigDecimal.valueOf(-3))
            );
    }

    @Test
    void shouldReturnEmptyOnOneSidedBalance() {
        Stream<BalanceTrade> trades = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_B, BigDecimal.ZERO),
                new RawPositionData(PARTICIPANT_C, BigDecimal.valueOf(4)),
                new RawPositionData(PARTICIPANT_D, BigDecimal.valueOf(4))
            )
        ).rebalance(0);

        assertThat(trades)
            .isEmpty();
    }

    @Test
    void shouldRound() {
        Stream<BalanceTrade> trades = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_B, BigDecimal.valueOf(44)),
                new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(55)),
                new RawPositionData(PARTICIPANT_C, BigDecimal.valueOf(-41)),
                new RawPositionData(PARTICIPANT_D, BigDecimal.valueOf(-42))
            )
        ).rebalance(1);

        assertThat(trades)
            .extracting(BalanceTrade::getOriginator, BalanceTrade::getCounterparty, a -> a.getAmount().intValue())
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, PARTICIPANT_D, -40),
                tuple(PARTICIPANT_B, PARTICIPANT_D, -2),
                tuple(PARTICIPANT_B, PARTICIPANT_C, -28)
            );
    }

    @Test
    void shouldRebalanceItself() {
        PositionBalance balance = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_B, BigDecimal.valueOf(44)),
                new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(55)),
                new RawPositionData(PARTICIPANT_C, BigDecimal.valueOf(-41)),
                new RawPositionData(PARTICIPANT_D, BigDecimal.valueOf(-42))
            )
        );

        balance = balance.applyTrades(balance.rebalance(1));

        assertThat(balance.getBuy().getNet()).isEqualByComparingTo(BigDecimal.valueOf(29));
        assertThat(balance.getBuy().getPositions())
            .extracting(RawPositionData::getParticipant, RawPositionData::getAmount)
            .containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, BigDecimal.valueOf(15)),
                tuple(PARTICIPANT_B, BigDecimal.valueOf(14))
            );

        assertThat(balance.getSell().getNet()).isEqualByComparingTo(BigDecimal.valueOf(-13));
        assertThat(balance.getSell().getPositions())
            .extracting(RawPositionData::getParticipant, RawPositionData::getAmount)
            .containsOnly(
                tuple(PARTICIPANT_C, BigDecimal.valueOf(-13))
            );
    }

    @Test
    void shouldNotBalanceDueToRounding() {
        PositionBalance balance = PositionBalance.of(
            Stream.of(
                new RawPositionData(PARTICIPANT_B, BigDecimal.valueOf(14)),
                new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(15)),
                new RawPositionData(PARTICIPANT_C, BigDecimal.valueOf(-13))
           )
        );

        assertThat(balance.rebalance(1)).isEmpty();
    }
}