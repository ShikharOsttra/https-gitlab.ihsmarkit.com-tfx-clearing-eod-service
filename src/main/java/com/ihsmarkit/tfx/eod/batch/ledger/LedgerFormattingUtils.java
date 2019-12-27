package com.ihsmarkit.tfx.eod.batch.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class LedgerFormattingUtils {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("ledger/messages/messages", Locale.ROOT);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static String formatDate(final LocalDate date) {
        return DATE_FORMATTER.format(date);
    }

    public static String formatDateTime(final LocalDateTime dateTime) {
        return DATE_TIME_FORMATTER.format(dateTime);
    }

    public static String formatTime(final LocalDateTime dateTime) {
        return TIME_FORMATTER.format(dateTime);
    }

    public static String formatMonthDay(final MonthDay monthDay) {
        return MONTH_DAY_FORMATTER.format(monthDay);
    }

    public static String formatTime(final LocalTime time) {
        return TIME_FORMATTER.format(time);
    }

    public static String formatEnum(final Enum value) {
        return RESOURCE_BUNDLE.getString(value.getClass().getName() + "." + value.name());
    }

    public static String formatBigDecimal(final BigDecimal bigDecimal) {
        // todo: formatting, rounding?
        return bigDecimal.toString();
    }
}
