package com.ihsmarkit.tfx.eod.batch.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
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

    private static Stream<Arguments> enumFormatting() {

        return Stream.of(
            Arguments.of(ParticipantType.FX_BROKER, "FXB"),
            Arguments.of(ParticipantType.LIQUIDITY_PROVIDER, "LP"),
            Arguments.of(CollateralPurpose.MARGIN, "Margin"),
            Arguments.of(CollateralPurpose.MARKET_ENTRY_DEPOSIT, "Market Entry Deposit"),
            Arguments.of(CollateralPurpose.CLEARING_DEPOSIT, "Clearing Deposit"),
            Arguments.of(CollateralPurpose.SPECIAL_PURPOSE_COLLATERAL, "Special Purpose Collateral"),
            Arguments.of(CollateralProductType.CASH, "Cash"),
            Arguments.of(CollateralProductType.LOG, "LG"),
            Arguments.of(CollateralProductType.BOND, "JGB"),
            Arguments.of(CollateralProductType.EQUITY, "Equities")
        );
    }

    @ParameterizedTest
    @MethodSource("nullFormatting")
    void shouldFormatNull(final Function<Object, String> formatMethod) {
        assertThat(formatMethod.apply(null)).isEmpty();
    }

    private static Stream<Arguments> nullFormatting() {
        return Stream.of(
            Arguments.of((Function<LocalDate, String>) LedgerFormattingUtils::formatDate),
            Arguments.of((Function<LocalDateTime, String>) LedgerFormattingUtils::formatDateTime),
            Arguments.of((Function<LocalDateTime, String>) LedgerFormattingUtils::formatTime),
            Arguments.of((Function<MonthDay, String>) LedgerFormattingUtils::formatMonthDay),
            Arguments.of((Function<Enum<?>, String>) LedgerFormattingUtils::formatEnum),
            Arguments.of((Function<BigDecimal, String>) LedgerFormattingUtils::formatBigDecimal),
            Arguments.of((Function<BigDecimal, String>) LedgerFormattingUtils::formatBigDecimalRoundTo1Jpy)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "123.2331234, 3, 123.233",
        "123.2335, 3, 123.234",
        "0.000123, 4, 0.0001"
    })
    void shouldFormatBigDecimalWithSpecifiedNumberOfDecimalPlaces(final BigDecimal passed, final int decimalPlaces, final String expected) {
        assertThat(LedgerFormattingUtils.formatBigDecimal(passed, decimalPlaces)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "123.00, 123",
        "123.11, 123",
        "123.56, 123",
        "-123.56, -124",
        "-123.11, -124"
    })
    void shouldFormatBigDecimalWith1JpyRule(final BigDecimal passed, final String expected) {
        assertThat(LedgerFormattingUtils.formatBigDecimalRoundTo1Jpy(passed)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "123.00, 123",
        "123.01, 123.01",
        "123.10, 123.10"
    })
    void shouldFormatBigDecimalStripZero(final BigDecimal passed, final String expected) {
        assertThat(LedgerFormattingUtils.formatBigDecimalStripZero(passed)).isEqualTo(expected);
    }

    @Test
    void shouldFormatYearMonth() {
        assertThat(LedgerFormattingUtils.formatYearMonth(LocalDate.of(2020, 4, 9))).isEqualTo("2020/04");
    }
}