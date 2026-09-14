package com.severus.finance.security;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class RateLimiter {

    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 60;

    public boolean isBlocked(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null) {
            return false;
        }
        if (Instant.now().getEpochSecond() - info.firstAttemptEpoch > WINDOW_SECONDS) {
            attempts.remove(key);
            return false;
        }
        return info.count >= MAX_ATTEMPTS;
    }

    public void recordFailedAttempt(String key) {
        long now = Instant.now().getEpochSecond();
        attempts.compute(key, (k, info) -> {
            if (info == null || (now - info.firstAttemptEpoch > WINDOW_SECONDS)) {
                return new AttemptInfo(now, 1);
            }
            return new AttemptInfo(info.firstAttemptEpoch, info.count + 1);
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private record AttemptInfo(long firstAttemptEpoch, int count) {}
}
