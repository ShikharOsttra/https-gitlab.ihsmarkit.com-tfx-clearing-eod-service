package com.ihsmarkit.tfx.eod.batch.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.ihsmarkit.tfx.core.domain.type.CollateralProductType;
import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.core.domain.type.ParticipantType;

class LedgerFormattingUtilsTest {

    @Test
    void shouldFormatDate() {
        assertThat(LedgerFormattingUtils.formatDate(LocalDate.of(2019, 1, 1))).isEqualTo("2019/01/01");
    }

    @Test
    void shouldFormatDateTime() {
        assertThat(LedgerFormattingUtils.formatDateTime(LocalDateTime.of(2019, 1, 1, 12, 0, 0))).isEqualTo("2019/01/01 12:00:00");
    }

    @Test
    void shouldFormatMonthDay() {
        assertThat(LedgerFormattingUtils.formatMonthDay(MonthDay.of(7, 30))).isEqualTo("07/30");
    }

    @ParameterizedTest
    @MethodSource("enumFormatting")
    void shouldFormatEnum(Enum value, String message) {
        assertThat(LedgerFormattingUtils.formatEnum(value)).isEqualTo(message);
    }

    private static Stream enumFormatting() {

        return Stream.of(
            Arguments.of(ParticipantType.FX_BROKER, "FXB"),
            Arguments.of(ParticipantType.LIQUIDITY_PROVIDER, "LP"),
            Arguments.of(CollateralPurpose.MARGIN, "Margin"),
            Arguments.of(CollateralPurpose.MARKET_ENTRY_DEPOSIT, "Market entry deposit"),
            Arguments.of(CollateralPurpose.CLEARING_DEPOSIT, "Clearing deposit"),
            Arguments.of(CollateralPurpose.SPECIAL_PURPOSE_COLLATERAL, "Special purpose collateral"),
            Arguments.of(CollateralProductType.CASH, "Cash"),
            Arguments.of(CollateralProductType.LOG, "LG"),
            Arguments.of(CollateralProductType.BOND, "JGB"),
            Arguments.of(CollateralProductType.EQUITY, "Equities")
        );
    }

}