package com.ihsmarkit.tfx.eod.model;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;

class PositionListTest {

    private static final ParticipantEntity PARTICIPANT_A = aParticipantEntityBuilder().name("A").build();
    private static final ParticipantEntity PARTICIPANT_B = aParticipantEntityBuilder().name("B").build();
    private static final ParticipantEntity PARTICIPANT_C = aParticipantEntityBuilder().name("C").build();

    @Test
    void shouldCollectPositions() {
        PositionList positions = Stream.of(
            new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(-12)),
            new RawPositionData(PARTICIPANT_B, BigDecimal.valueOf(-1)),
            new RawPositionData(PARTICIPANT_C, BigDecimal.valueOf(3))
        ).map(PositionList.PositionListBuilder::of)
            .reduce(PositionList.PositionListBuilder::combine)
            .get()
            .build();

        assertThat(positions.getNet()).isEqualByComparingTo(BigDecimal.valueOf(-10));
        assertThat(positions.getPositions())
            .extracting(RawPositionData::getParticipant, RawPositionData::getAmount)
            .containsExactlyInAnyOrder(
                Tuple.tuple(PARTICIPANT_A, BigDecimal.valueOf(-12)),
                Tuple.tuple(PARTICIPANT_B, BigDecimal.valueOf(-1)),
                Tuple.tuple(PARTICIPANT_C, BigDecimal.valueOf(3))
            );
    }

    @Test
    void shouldCorrectlyHandleEmpty() {
        PositionList empty = PositionList.PositionListBuilder.empty().build();
        assertThat(empty.getNet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(empty.getPositions()).isEmpty();
    }

    @Test
    void shouldCorrectlyCombineTwoEmpty() {
        PositionList empty = PositionList.PositionListBuilder.empty()
            .combine(PositionList.PositionListBuilder.empty())
            .build();
        assertThat(empty.getNet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(empty.getPositions()).isEmpty();
    }

    @Test
    void shouldCorrectlyConvertSingle() {
        PositionList sole = PositionList.PositionListBuilder.of(
            new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(-12))
        ).build();

        assertThat(sole.getNet()).isEqualByComparingTo(BigDecimal.valueOf(-12));
        assertThat(sole.getPositions())
            .extracting(RawPositionData::getParticipant, RawPositionData::getAmount)
            .containsOnly(
                Tuple.tuple(PARTICIPANT_A, BigDecimal.valueOf(-12))
            );
    }

    @Test
    void shouldCorrectlyCombineWithEmpty() {
        PositionList sole = PositionList.PositionListBuilder.of(
            new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(-12))
        ).combine(PositionList.PositionListBuilder.empty())
        .build();

        assertThat(sole.getNet()).isEqualByComparingTo(BigDecimal.valueOf(-12));
        assertThat(sole.getPositions())
            .extracting(RawPositionData::getParticipant, RawPositionData::getAmount)
            .containsOnly(
                Tuple.tuple(PARTICIPANT_A, BigDecimal.valueOf(-12))
            );
    }

    @Test
    void shouldCorrectlyCombineEmptyWithFilled() {
        PositionList sole = PositionList.PositionListBuilder.empty()
            .combine(
                PositionList.PositionListBuilder.of(
                    new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(-12))
                )
            ).build();

        assertThat(sole.getNet()).isEqualByComparingTo(BigDecimal.valueOf(-12));
        assertThat(sole.getPositions())
            .extracting(RawPositionData::getParticipant, RawPositionData::getAmount)
            .containsOnly(
                Tuple.tuple(PARTICIPANT_A, BigDecimal.valueOf(-12))
            );
    }

    @Test
    void shouldCorrectlyCombineTwo() {
        PositionList sole = PositionList.PositionListBuilder.of(
            new RawPositionData(PARTICIPANT_A, BigDecimal.valueOf(-11))
        ).combine(
            PositionList.PositionListBuilder.of(
                new RawPositionData(PARTICIPANT_B, BigDecimal.valueOf(12))
            )
        ).build();

        assertThat(sole.getNet()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(sole.getPositions())
            .extracting(RawPositionData::getParticipant, RawPositionData::getAmount)
            .containsExactlyInAnyOrder(
                Tuple.tuple(PARTICIPANT_A, BigDecimal.valueOf(-11)),
                Tuple.tuple(PARTICIPANT_B, BigDecimal.valueOf(12))
            );
    }
}