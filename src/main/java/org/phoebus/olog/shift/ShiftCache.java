package org.phoebus.olog.shift;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe TTL cache for shift service responses, keyed by shift type.
 * Caches null responses (service returned nothing) to avoid hammering a
 * down service on every log entry. Exceptions are never cached.
 */
class ShiftCache {

    static class CacheEntry {
        final Shift shift; // null means the service returned null/404
        final long timestamp;

        CacheEntry(Shift shift, long timestamp) {
            this.shift = shift;
            this.timestamp = timestamp;
        }
    }

    private final long ttlMs;
    private final Clock clock;
    private final ConcurrentHashMap<String, CacheEntry> entries = new ConcurrentHashMap<>();

    ShiftCache(long ttlMs, Clock clock) {
        this.ttlMs = ttlMs;
        this.clock = clock;
    }

    /**
     * Returns the cached entry for the given shift type, or null on cache miss / expiry.
     * A non-null return with a null {@code entry.shift} means the service returned nothing.
     */
    CacheEntry lookup(String type) {
        CacheEntry entry = entries.get(type);
        if (entry == null) return null;
        if (clock.millis() - entry.timestamp >= ttlMs) {
            entries.remove(type);
            return null;
        }
        return entry;
    }

    void store(String type, Shift shift) {
        entries.put(type, new CacheEntry(shift, clock.millis()));
    }

    void invalidate(String type) {
        entries.remove(type);
    }

    void invalidateAll() {
        entries.clear();
    }
}
