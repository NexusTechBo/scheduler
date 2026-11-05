package com.example.scheduler.util;

import java.time.format.DateTimeFormatter;

/** Shared user-facing date and time formats, so error messages and notifications show the same text. */
public final class DateTimeFormats {
    /** Numeric date, for example {@code 12/11/2026}. */
    public static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** 24-hour time, for example {@code 10:00}. */
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private DateTimeFormats() {}
}
