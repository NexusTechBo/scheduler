package com.example.scheduler.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class DateTimeFormatsTest {
    @Test
    void shouldFormatShortDateAsDayMonthYearWithLeadingZeros() {
        assertThat(LocalDate.of(2026, 1, 5).format(DateTimeFormats.SHORT_DATE)).isEqualTo("05/01/2026");
    }

    @Test
    void shouldFormatTimeIn24HourClockWithLeadingZeros() {
        assertThat(LocalTime.of(8, 5).format(DateTimeFormats.TIME)).isEqualTo("08:05");
        assertThat(LocalTime.of(23, 59, 59).format(DateTimeFormats.TIME)).isEqualTo("23:59");
        assertThat(LocalTime.MIDNIGHT.format(DateTimeFormats.TIME)).isEqualTo("00:00");
    }

    @Test
    void shouldFormatDateTimeValuesWithBothFormats() {
        var value = LocalDateTime.of(2026, 11, 12, 10, 0);

        assertThat(value.format(DateTimeFormats.SHORT_DATE)).isEqualTo("12/11/2026");
        assertThat(value.format(DateTimeFormats.TIME)).isEqualTo("10:00");
    }

    @Test
    void shouldProduceSameTextRegardlessOfFormatterLocale() {
        var value = LocalDateTime.of(2026, 11, 12, 17, 30);

        for (var locale : new Locale[] {Locale.US, Locale.forLanguageTag("es"), Locale.forLanguageTag("ar-EG")}) {
            assertThat(value.format(DateTimeFormats.SHORT_DATE.withLocale(locale)))
                    .isEqualTo("12/11/2026");
            assertThat(value.format(DateTimeFormats.TIME.withLocale(locale))).isEqualTo("17:30");
        }
    }

    @Test
    void shouldParseBackWhatItFormats() {
        assertThat(LocalDate.parse("12/11/2026", DateTimeFormats.SHORT_DATE)).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(LocalTime.parse("17:30", DateTimeFormats.TIME)).isEqualTo(LocalTime.of(17, 30));
    }
}
