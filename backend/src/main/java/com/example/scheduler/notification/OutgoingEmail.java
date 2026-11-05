package com.example.scheduler.notification;

import com.example.scheduler.util.LogUtils;
import org.jspecify.annotations.NonNull;

public record OutgoingEmail(String to, String subject, String html) {

    @Override
    public @NonNull String toString() {
        return "OutgoingEmail[to=" + (to == null ? null : LogUtils.maskEmail(to)) + ", subject="
                + LogUtils.sanitizeForLog(subject) + ", html=<" + (html == null ? 0 : html.length()) + " chars>]";
    }
}
