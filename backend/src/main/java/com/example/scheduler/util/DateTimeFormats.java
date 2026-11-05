package com.example.scheduler.util;

import java.time.format.DateTimeFormatter;

public final class DateTimeFormats {
    public static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private DateTimeFormats() {}
}
