package com.example.scheduler.util;

import com.example.scheduler.exception.BadRequestException;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.springframework.util.StringUtils;

public final class Utils {
    private static final String DEFAULT_PHONE_REGION = "BO";

    private Utils() {}

    public static String normalizePhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) return null;
        var phoneUtil = PhoneNumberUtil.getInstance();
        try {
            var parsed = phoneUtil.parse(phoneNumber, DEFAULT_PHONE_REGION);
            if (phoneUtil.isValidNumber(parsed))
                return phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException _) {}
        throw new BadRequestException("validation.phone.invalid");
    }

    public static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    public static String usernameSuffix(String clinicSlug) {
        return "@" + clinicSlug + ".com";
    }
}
