package com.example.scheduler.middleware;

import com.example.scheduler.config.rateLimiting.RateLimitBandwidth;
import com.example.scheduler.config.rateLimiting.RateLimitKeyStrategy;
import com.example.scheduler.config.rateLimiting.RateLimitPolicy;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

record CompiledRateLimitPolicy(
        String name,
        Set<String> methods,
        List<PathPattern> paths,
        RateLimitKeyStrategy key,
        List<Bandwidth> bandwidths,
        Cache<String, Bucket> buckets) {
    static CompiledRateLimitPolicy of(RateLimitPolicy policy, PathPatternParser parser, long cacheMaxSize) {
        var longestPeriod = policy.limits().stream()
                .map(RateLimitBandwidth::period)
                .max(Comparator.naturalOrder())
                .orElseThrow();
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
        return new CompiledRateLimitPolicy(
                policy.name(),
                Set.copyOf(policy.methods()),
                policy.paths().stream().map(parser::parse).toList(),
                policy.key(),
                bandwidths,
                buckets);
    }

    boolean matches(String method, PathContainer path) {
        return (methods.isEmpty() || methods.contains(method)) && RateLimitFilter.matchesAny(paths, path);
    }

    Bucket newBucket() {
        var builder = Bucket.builder();
        bandwidths.forEach(builder::addLimit);
        return builder.build();
    }
}
