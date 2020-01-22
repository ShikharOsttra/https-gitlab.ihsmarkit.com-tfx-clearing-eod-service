package com.ihsmarkit.tfx.eod.batch.ledger;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class LedgerFormattingUtils {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("ledger/messages/messages", Locale.ROOT);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static String formatDate(@Nullable final LocalDate date) {
        return safeFormat(date, DATE_FORMATTER::format);
    }

    public static String formatDateTime(@Nullable final LocalDateTime dateTime) {
        return safeFormat(dateTime, DATE_TIME_FORMATTER::format);
    }

    public static String formatTime(@Nullable final LocalDateTime dateTime) {
        return safeFormat(dateTime, TIME_FORMATTER::format);
    }

    public static String formatMonthDay(@Nullable final MonthDay monthDay) {
        return safeFormat(monthDay, MONTH_DAY_FORMATTER::format);
    }

    public static String formatEnum(@Nullable final Enum enumValue) {
        return safeFormat(enumValue, value -> RESOURCE_BUNDLE.getString(value.getClass().getName() + "." + value.name()));
    }

    public static String formatBigDecimal(@Nullable final BigDecimal bigDecimal) {
        return safeFormat(bigDecimal, BigDecimal::toPlainString);
    }

    public static String quote(final String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        } else {
            return "\"" + value + "\"";
        }
    }

    private static <T> String safeFormat(@Nullable final T value, final Function<T, String> mappingFunction) {
        return value == null ? EMPTY : mappingFunction.apply(value);
    }
}
