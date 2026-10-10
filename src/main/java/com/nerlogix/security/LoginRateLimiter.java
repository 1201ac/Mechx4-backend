package com.nerlogix.security;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long TIME_WINDOW_MS = 15 * 60 * 1000; // 15 minutes

    private final Map<String, AttemptInfo> attemptsMap = new ConcurrentHashMap<>();

    public boolean isAllowed(String ip) {
        if (ip == null || ip.isBlank()) return true;
        AttemptInfo info = attemptsMap.get(ip);
        if (info == null) return true;
        if (System.currentTimeMillis() - info.firstAttemptTime > TIME_WINDOW_MS) {
            attemptsMap.remove(ip);
            return true;
        }
        return info.count < MAX_FAILED_ATTEMPTS;
    }

    public void recordFailure(String ip) {
        if (ip == null || ip.isBlank()) return;
        long now = System.currentTimeMillis();
        attemptsMap.compute(ip, (key, val) -> {
            if (val == null || (now - val.firstAttemptTime > TIME_WINDOW_MS)) {
                return new AttemptInfo(1, now);
            }
            val.count++;
            return val;
        });
    }

    public void reset(String ip) {
        if (ip != null) {
            attemptsMap.remove(ip);
        }
    }

    private static class AttemptInfo {
        int count;
        long firstAttemptTime;

        AttemptInfo(int count, long firstAttemptTime) {
            this.count = count;
            this.firstAttemptTime = firstAttemptTime;
        }
    }
}
