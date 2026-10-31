package com.example.scheduler.middleware;

import com.example.scheduler.config.RateLimitProperties;
import com.example.scheduler.config.RateLimitProperties.KeyStrategy;
import com.example.scheduler.config.RateLimitProperties.Limit;
import com.example.scheduler.config.RateLimitProperties.Policy;
import com.example.scheduler.exception.ErrorResponseWriter;
import com.example.scheduler.util.LogKeys;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final long MAX_WAIT_SHOWN_IN_SECONDS = 120;

    private final ErrorResponseWriter errorResponseWriter;
    private final boolean enabled;
    private final List<PathPattern> excludedPaths;
    private final List<CompiledPolicy> policies;

    public RateLimitFilter(RateLimitProperties properties, ErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
        this.enabled = properties.enabled();
        var parser = PathPatternParser.defaultInstance;
        this.excludedPaths = properties.excludedPaths().stream().map(parser::parse).toList();
        this.policies = properties.policies().stream()
                .map(policy -> CompiledPolicy.of(policy, parser, properties.cacheMaxSize()))
                .toList();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        if (!enabled || HttpMethod.OPTIONS.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        var path = PathContainer.parsePath(request.getRequestURI().substring(request.getContextPath().length()));
        if (matchesAny(excludedPaths, path)) {
            chain.doFilter(request, response);
            return;
        }
        var policy = policies.stream().filter(p -> p.matches(request.getMethod(), path)).findFirst().orElse(null);
        if (policy == null) {
            chain.doFilter(request, response);
            return;
        }
        var probe = policy.buckets().get(resolveKey(policy.key(), request), _ -> policy.newBucket()).tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader(REMAINING_HEADER, Long.toString(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }
        var retryAfterSeconds = Math.max(1L, Math.ceilDiv(probe.getNanosToWaitForRefill(), 1_000_000_000L));
        logExceeded(request, policy.name(), retryAfterSeconds);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setHeader(REMAINING_HEADER, "0");
        if (retryAfterSeconds > MAX_WAIT_SHOWN_IN_SECONDS)
            errorResponseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "rateLimit.exceededMinutes",
                    Math.ceilDiv(retryAfterSeconds, 60));
        else
            errorResponseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "rateLimit.exceeded",
                    retryAfterSeconds);
    }

    private static String resolveKey(KeyStrategy strategy, HttpServletRequest request) {
        if (strategy == KeyStrategy.USER) {
            var clinicId = request.getAttribute(LogKeys.ATTR_CLINIC_ID);
            var userId = request.getAttribute(LogKeys.ATTR_USER_ID);
            if (clinicId != null && userId != null && !LogKeys.GUEST_USER.equals(userId))
                return "user:" + clinicId + ":" + userId;
        }
        return "ip:" + clientIpKey(request.getRemoteAddr());
    }

    private static String clientIpKey(String remoteAddr) {
        var zoneIndex = remoteAddr.indexOf('%');
        var literal = zoneIndex >= 0 ? remoteAddr.substring(0, zoneIndex) : remoteAddr;
        try {
            var address = InetAddress.ofLiteral(literal);
            if (address instanceof Inet6Address) {
                var bytes = address.getAddress();
                Arrays.fill(bytes, 8, 16, (byte) 0);
                return InetAddress.getByAddress(bytes).getHostAddress() + "/64";
            }
            return address.getHostAddress();
        } catch (IllegalArgumentException | IOException _) {
        }
        return remoteAddr;
    }

    private static void logExceeded(HttpServletRequest request, String policy, long retryAfterSeconds) {
        var method = request.getMethod();
        var path = request.getRequestURI();
        var clientIp = request.getRemoteAddr();
        log.atWarn()
                .addKeyValue("policy", policy)
                .addKeyValue("method", method)
                .addKeyValue("path", path)
                .addKeyValue("clientIp", clientIp)
                .addKeyValue("retryAfterSeconds", retryAfterSeconds)
                .log(
                        "Rate limit exceeded policy={} {} {} ip={} retryAfter={}s clinic={} user={}",
                        policy,
                        method,
                        path,
                        clientIp,
                        retryAfterSeconds,
                        Objects.toString(request.getAttribute(LogKeys.ATTR_CLINIC_ID), "-"),
                        Objects.toString(request.getAttribute(LogKeys.ATTR_USER_ID), "-")
                );
    }

    private static boolean matchesAny(List<PathPattern> patterns, PathContainer path) {
        return patterns.stream().anyMatch(pattern -> pattern.matches(path));
    }

    private record CompiledPolicy(String name, Set<String> methods, List<PathPattern> paths, KeyStrategy key,
                                  List<Bandwidth> bandwidths, Cache<String, Bucket> buckets) {

        static CompiledPolicy of(Policy policy, PathPatternParser parser, long cacheMaxSize) {
            var longestPeriod = policy.limits().stream().map(Limit::period).max(Comparator.naturalOrder()).orElseThrow();
            Cache<String, Bucket> buckets = Caffeine.newBuilder()
                    .maximumSize(cacheMaxSize)
                    .expireAfterAccess(longestPeriod)
                    .build();
            var bandwidths = policy.limits().stream()
                    .map(limit -> Bandwidth.builder()
                            .capacity(limit.capacity())
                            .refillGreedy(limit.capacity(), limit.period())
                            .build())
                    .toList();
            return new CompiledPolicy(
                    policy.name(),
                    Set.copyOf(policy.methods()),
                    policy.paths().stream().map(parser::parse).toList(),
                    policy.key(),
                    bandwidths,
                    buckets
            );
        }

        boolean matches(String method, PathContainer path) {
            return (methods.isEmpty() || methods.contains(method)) && matchesAny(paths, path);
        }

        Bucket newBucket() {
            var builder = Bucket.builder();
            bandwidths.forEach(builder::addLimit);
            return builder.build();
        }
    }
}
