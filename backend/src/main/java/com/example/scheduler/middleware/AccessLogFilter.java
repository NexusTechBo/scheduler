package com.example.scheduler.middleware;

import com.example.scheduler.util.LogKeys;
import com.example.scheduler.util.LogUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(AccessLogFilter.ORDER)
public class AccessLogFilter extends OncePerRequestFilter {
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
    private static final Pattern STATIC_RESOURCE =
            Pattern.compile(".*\\.(css|js|map|png|jpe?g|gif|svg|ico|woff2?|ttf|html)$");

    @Value("${security.system-paths:}")
    private String[] systemPaths;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        var traceId = MDC.get(LogKeys.TRACE_ID);
        if (traceId != null) response.setHeader(LogKeys.REQUEST_ID_HEADER, traceId);
        var path = request.getRequestURI();
        if (isExcluded(path) || HttpMethod.OPTIONS.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        var start = System.nanoTime();
        var failed = false;
        try {
            chain.doFilter(request, response);
        } catch (Throwable e) {
            failed = true;
            throw e;
        } finally {
            log(
                    request,
                    path,
                    failed ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : response.getStatus(),
                    (System.nanoTime() - start) / 1_000_000);
        }
    }

    private static void log(HttpServletRequest request, String path, int status, long durationMs) {
        var method = request.getMethod();
        var clinicId = request.getAttribute(LogKeys.ATTR_CLINIC_ID);
        var userId = request.getAttribute(LogKeys.ATTR_USER_ID);
        var role = request.getAttribute(LogKeys.ATTR_ROLE);
        var event = log.atLevel(levelFor(status))
                .addKeyValue(LogKeys.METHOD, method)
                .addKeyValue(LogKeys.PATH, path)
                .addKeyValue("status", status)
                .addKeyValue("durationMs", durationMs);
        if (clinicId != null) event = event.addKeyValue(LogKeys.CLINIC_ID, clinicId);
        if (userId != null) event = event.addKeyValue(LogKeys.USER_ID, userId);
        if (role != null) event = event.addKeyValue(LogKeys.ROLE, role);
        event.log(
                "{} {} {} {}ms clinic={} user={} role={}",
                method,
                path,
                status,
                durationMs,
                LogUtils.orDash(clinicId),
                LogUtils.orDash(userId),
                LogUtils.orDash(role));
    }

    private static Level levelFor(int status) {
        if (status >= 500) return Level.ERROR;
        if (status >= 400) return Level.WARN;
        return Level.INFO;
    }

    private boolean isExcluded(String path) {
        return Arrays.stream(systemPaths).anyMatch(path::startsWith)
                || STATIC_RESOURCE.matcher(path).matches();
    }
}
