package com.example.scheduler.exchange.contract;

import com.example.scheduler.util.LogUtils;
import java.util.List;
import org.jspecify.annotations.NonNull;

public record BrevoEmailRequest(BrevoContact sender, List<BrevoContact> to, String subject, String htmlContent) {

    @Override
    public @NonNull String toString() {
        var recipients = to == null
                ? List.of()
                : to.stream()
                        .map(c -> c == null || c.email() == null ? null : LogUtils.maskEmail(c.email()))
                        .toList();
        return "BrevoEmailRequest[sender=" + (sender == null ? null : sender.email()) + ", to=" + recipients
                + ", subject=" + LogUtils.sanitizeForLog(subject) + ", htmlContent=<"
                + (htmlContent == null ? 0 : htmlContent.length()) + " chars>]";
    }
}
