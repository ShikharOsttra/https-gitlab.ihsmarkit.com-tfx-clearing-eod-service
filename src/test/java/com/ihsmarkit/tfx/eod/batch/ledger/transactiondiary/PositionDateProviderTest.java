package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.core.domain.type.CurrencyPairFamily.NON_NZD;
import static com.ihsmarkit.tfx.core.domain.type.SummerTimeSettingSource.USER;
import static com.ihsmarkit.tfx.core.domain.type.TradingHoursType.REGULAR;
import static com.ihsmarkit.tfx.core.domain.type.TradingHoursType.SUMMER;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.entity.SummerTimeSettingEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradingHoursEntity;
import com.ihsmarkit.tfx.core.dl.repository.SummerTimeSettingRepository;
import com.ihsmarkit.tfx.core.dl.repository.TradingHoursRepository;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.core.domain.type.TradingHoursType;

@ExtendWith(MockitoExtension.class)
class PositionDateProviderTest {

    private static final LocalTime REGULAR_MONDAY_CLOSE_TIME = LocalTime.of(7, 1);
    private static final LocalTime REGULAR_TUESDAY_CLOSE_TIME = LocalTime.of(7, 2);
    private static final LocalTime REGULAR_WEDNESDAY_CLOSE_TIME = LocalTime.of(7, 3);
    private static final LocalTime REGULAR_THURSDAY_CLOSE_TIME = LocalTime.of(7, 4);
    private static final LocalTime REGULAR_FRIDAY_CLOSE_TIME = LocalTime.of(7, 5);

    private static final LocalTime SUMMER_MONDAY_CLOSE_TIME = LocalTime.of(8, 1);
    private static final LocalTime SUMMER_TUESDAY_CLOSE_TIME = LocalTime.of(8, 2);
    private static final LocalTime SUMMER_WEDNESDAY_CLOSE_TIME = LocalTime.of(8, 3);
    private static final LocalTime SUMMER_THURSDAY_CLOSE_TIME = LocalTime.of(8, 4);
    private static final LocalTime SUMMER_FRIDAY_CLOSE_TIME = LocalTime.of(8, 5);

    @Mock
    private SummerTimeSettingRepository summerTimeSettingRepository;

    @Mock
    private TradingHoursRepository tradingHoursRepository;

    @Mock
    private CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    @ParameterizedTest
    @MethodSource("positionDatesDataProvider")
    void shouldCalculateDates(final LocalDate businessDate, final LocalDateTime sodDate, final LocalDateTime netDate) {
        when(summerTimeSettingRepository.findByCurrencyPairFamilyFailFast(NON_NZD))
            .thenReturn(SummerTimeSettingEntity.builder()
                .currencyPairFamily(NON_NZD)
                .startDate(LocalDate.of(2020, 3, 16))
                .endDate(LocalDate.of(2020, 3, 22))
                .source(USER)
                .build()
            );

        when(tradingHoursRepository.findAll()).thenReturn(List.of(
            tradingHours(REGULAR, MONDAY, REGULAR_MONDAY_CLOSE_TIME),
            tradingHours(REGULAR, TUESDAY, REGULAR_TUESDAY_CLOSE_TIME),
            tradingHours(REGULAR, WEDNESDAY, REGULAR_WEDNESDAY_CLOSE_TIME),
            tradingHours(REGULAR, THURSDAY, REGULAR_THURSDAY_CLOSE_TIME),
            tradingHours(REGULAR, FRIDAY, REGULAR_FRIDAY_CLOSE_TIME),
            tradingHours(SUMMER, MONDAY, SUMMER_MONDAY_CLOSE_TIME),
            tradingHours(SUMMER, TUESDAY, SUMMER_TUESDAY_CLOSE_TIME),
            tradingHours(SUMMER, WEDNESDAY, SUMMER_WEDNESDAY_CLOSE_TIME),
            tradingHours(SUMMER, THURSDAY, SUMMER_THURSDAY_CLOSE_TIME),
            tradingHours(SUMMER, FRIDAY, SUMMER_FRIDAY_CLOSE_TIME)
        ));

        when(calendarTradingSwapPointRepository.findPreviousTradingDateFailFast(any())).thenAnswer(
            invocation -> ((LocalDate) invocation.getArgument(0)).with(
                temporal -> DayOfWeek.from(temporal) == MONDAY
                            ? temporal.with(TemporalAdjusters.previous(FRIDAY))
                            : temporal.minus(1, ChronoUnit.DAYS)
            )
        );

        final PositionDateProvider positionDateProvider = new PositionDateProvider(
            businessDate,
            summerTimeSettingRepository,
            tradingHoursRepository,
            calendarTradingSwapPointRepository
        );

        assertThat(positionDateProvider.getSodDate()).isEqualTo(sodDate);
        assertThat(positionDateProvider.getNetDate()).isEqualTo(netDate);
    }

    private static TradingHoursEntity tradingHours(final TradingHoursType type, final DayOfWeek dayOfWeek, final LocalTime closeTime) {
        return TradingHoursEntity.builder()
            .type(type)
            .currencyPairFamily(NON_NZD)
            .dayOfWeek(dayOfWeek)
            .startTime(LocalTime.now())
            .closeTime(closeTime)
            .acceptanceLimitTime(LocalTime.now())
            .build();
    }

    private static Stream<Arguments> positionDatesDataProvider() {
        return Stream.of(
            Arguments.of(
                LocalDate.of(2020, 3, 16),
                LocalDateTime.of(LocalDate.of(2020, 3, 14), REGULAR_FRIDAY_CLOSE_TIME),
                LocalDateTime.of(LocalDate.of(2020, 3, 17), SUMMER_MONDAY_CLOSE_TIME)
            ),
            Arguments.of(
                LocalDate.of(2020, 3, 17),
                LocalDateTime.of(LocalDate.of(2020, 3, 17), SUMMER_MONDAY_CLOSE_TIME),
                LocalDateTime.of(LocalDate.of(2020, 3, 18), SUMMER_TUESDAY_CLOSE_TIME)
            ),
            Arguments.of(
                LocalDate.of(2020, 3, 18),
                LocalDateTime.of(LocalDate.of(2020, 3, 18), SUMMER_TUESDAY_CLOSE_TIME),
                LocalDateTime.of(LocalDate.of(2020, 3, 19), SUMMER_WEDNESDAY_CLOSE_TIME)
            ),
            Arguments.of(
                LocalDate.of(2020, 3, 19),
                LocalDateTime.of(LocalDate.of(2020, 3, 19), SUMMER_WEDNESDAY_CLOSE_TIME),
                LocalDateTime.of(LocalDate.of(2020, 3, 20), SUMMER_THURSDAY_CLOSE_TIME)
            ),
            Arguments.of(
                LocalDate.of(2020, 3, 20),
                LocalDateTime.of(LocalDate.of(2020, 3, 20), SUMMER_THURSDAY_CLOSE_TIME),
                LocalDateTime.of(LocalDate.of(2020, 3, 21), SUMMER_FRIDAY_CLOSE_TIME)
            ),
            Arguments.of(
                LocalDate.of(2020, 3, 23),
                LocalDateTime.of(LocalDate.of(2020, 3, 21), SUMMER_FRIDAY_CLOSE_TIME),
                LocalDateTime.of(LocalDate.of(2020, 3, 24), REGULAR_MONDAY_CLOSE_TIME)
            ),
            Arguments.of(
                LocalDate.of(2020, 3, 24),
                LocalDateTime.of(LocalDate.of(2020, 3, 24), REGULAR_MONDAY_CLOSE_TIME),
                LocalDateTime.of(LocalDate.of(2020, 3, 25), REGULAR_TUESDAY_CLOSE_TIME)
            ),
            Arguments.of(
                LocalDate.of(2020, 3, 25),
                LocalDateTime.of(LocalDate.of(2020, 3, 25), REGULAR_TUESDAY_CLOSE_TIME),
                LocalDateTime.of(LocalDate.of(2020, 3, 26), REGULAR_WEDNESDAY_CLOSE_TIME)
            ),
            Arguments.of(
                LocalDate.of(2020, 3, 26),
                LocalDateTime.of(LocalDate.of(2020, 3, 26), REGULAR_WEDNESDAY_CLOSE_TIME),
                LocalDateTime.of(LocalDate.of(2020, 3, 27), REGULAR_THURSDAY_CLOSE_TIME)
            ),
            Arguments.of(
                LocalDate.of(2020, 3, 27),
                LocalDateTime.of(LocalDate.of(2020, 3, 27), REGULAR_THURSDAY_CLOSE_TIME),
                LocalDateTime.of(LocalDate.of(2020, 3, 28), REGULAR_FRIDAY_CLOSE_TIME)
            )
        );
    }

}