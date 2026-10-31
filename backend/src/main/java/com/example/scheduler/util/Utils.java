package com.example.scheduler.util;

import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.MessageKeys;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

public final class Utils {
    public static final String USERNAME_DOMAIN = ".com";
    private static final String DEFAULT_PHONE_REGION = "BO";
    private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");

    private Utils() {}

    public static String normalizePhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) return null;
        var phoneUtil = PhoneNumberUtil.getInstance();
        try {
            var parsed = phoneUtil.parse(phoneNumber, DEFAULT_PHONE_REGION);
            if (phoneUtil.isValidNumber(parsed))
                return phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException _) {
        }
        throw new BadRequestException(MessageKeys.VALIDATION_PHONE_INVALID);
    }

    public static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    public static String usernameSuffix(String clinicSlug) {
        return "@" + clinicSlug + USERNAME_DOMAIN;
    }

    public static String stripControlChars(String value) {
        return CONTROL_CHARS.matcher(value).replaceAll("");
    }

    public static String escapeHtml(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value, StandardCharsets.UTF_8.name());
    }
}
