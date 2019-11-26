package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class PositionList {

    private final BigDecimal net;
    private final List<RawPositionData> positions;

    @RequiredArgsConstructor
    @Getter
    static class PositionListBuilder {
        private final BigDecimal total;
        private final Stream<RawPositionData> positions;

        public static PositionListBuilder empty() {
            return new PositionListBuilder(BigDecimal.ZERO, Stream.of());
        }

        public static PositionListBuilder of(final RawPositionData position) {
            return new PositionListBuilder(position.getAmount(), Stream.of(position));
        }

        public PositionListBuilder combine(final PositionListBuilder other) {
            return new PositionListBuilder(
                total.add(other.total),
                Stream.concat(positions, other.positions)
            );
        }

        public PositionList build() {
            return new PositionList(total, positions.collect(Collectors.toList()));
        }
    }

}
