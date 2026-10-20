package com.example.scheduler.util;

import org.springframework.util.StringUtils;

public final class Utils {

    private Utils() {}

    public static String normalizePhoneNumber(String phoneNumber) {
        return StringUtils.hasText(phoneNumber) ? phoneNumber : null;
    }
}
