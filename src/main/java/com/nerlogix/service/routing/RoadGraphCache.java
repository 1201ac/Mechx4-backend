package com.nerlogix.service.routing;

import com.nerlogix.entity.RoadSegment;
import com.nerlogix.repository.RoadSegmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RoadGraphCache {

    private static final Logger log = LoggerFactory.getLogger(RoadGraphCache.class);
    private static final long CACHE_TTL_MS = 60_000L; // 60 seconds

    private final RoadSegmentRepository roadSegmentRepository;
    private final AtomicReference<CacheEntry> cache = new AtomicReference<>(null);

    public RoadGraphCache(RoadSegmentRepository roadSegmentRepository) {
        this.roadSegmentRepository = roadSegmentRepository;
    }

    public List<RoadSegment> getSegments() {
        CacheEntry current = cache.get();
        long now = Instant.now().toEpochMilli();
        if (current != null && (now - current.timestamp) < CACHE_TTL_MS) {
            return current.segments;
        }

        synchronized (this) {
            current = cache.get();
            if (current != null && (now - current.timestamp) < CACHE_TTL_MS) {
                return current.segments;
            }
            log.debug("RoadGraphCache miss or expired. Reloading road segments from DB.");
            List<RoadSegment> fresh = Collections.unmodifiableList(roadSegmentRepository.findAll());
            cache.set(new CacheEntry(fresh, Instant.now().toEpochMilli()));
            return fresh;
        }
    }

    public void evict() {
        log.info("Evicting RoadGraphCache.");
        cache.set(null);
    }

    private static class CacheEntry {
        final List<RoadSegment> segments;
        final long timestamp;

        CacheEntry(List<RoadSegment> segments, long timestamp) {
            this.segments = segments;
            this.timestamp = timestamp;
        }
    }
}
