package com.nerlogix.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class SystemCacheService {

    private static final long DEFAULT_TTL_MS = 20_000L; // 20 seconds TTL

    private static class CacheEntry {
        final Object value;
        final long expiresAt;

        CacheEntry(Object value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public <T> T getOrCompute(String key, Supplier<T> supplier) {
        return getOrCompute(key, DEFAULT_TTL_MS, supplier);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrCompute(String key, long ttlMs, Supplier<T> supplier) {
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(key);
        if (entry != null && entry.expiresAt > now) {
            return (T) entry.value;
        }

        T value = supplier.get();
        cache.put(key, new CacheEntry(value, now + ttlMs));
        return value;
    }

    public void evict(String key) {
        cache.remove(key);
    }

    public void evictDashboardAndOverview() {
        cache.remove("dashboard.summary");
        cache.remove("dashboard.districts");
        cache.remove("dashboard.supply");
        cache.remove("dashboard.charts");
        cache.remove("dashboard.movement");
        cache.remove("dashboard.emergency");
        cache.remove("public.overview");
    }

    public void evictRoadRelated() {
        evictDashboardAndOverview();
        cache.remove("map.segments");
    }

    public void evictIncidentRelated() {
        evictRoadRelated();
    }

    public void evictWeatherRelated() {
        cache.remove("weather.districts");
        evictDashboardAndOverview();
    }

    public void evictEmergencyRelated() {
        evictDashboardAndOverview();
    }

    public void evictAll() {
        cache.clear();
    }
}
