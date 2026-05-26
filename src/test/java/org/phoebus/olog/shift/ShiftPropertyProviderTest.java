package org.phoebus.olog.shift;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Property;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShiftPropertyProviderTest {

    private static final String SHIFT_URL = "http://localhost:8080/Shift/resources";
    private static final String SHIFT_TYPE = "Operations";
    private static final long NO_CACHE = 0L; // TTL of 0ms effectively disables caching

    private RestTemplate mockRestTemplate;
    private ShiftPropertyProvider provider;

    @BeforeEach
    void setUp() {
        mockRestTemplate = mock(RestTemplate.class);
        provider = new ShiftPropertyProvider(SHIFT_URL, List.of(SHIFT_TYPE), mockRestTemplate,
                NO_CACHE, Clock.systemUTC());
    }

    @Test
    void testGetPropertyWithActiveShift() {
        ShiftType type = new ShiftType();
        type.setId(1);
        type.setName("Operations");

        Shift shift = new Shift();
        shift.setId(42);
        shift.setStatus("Active");
        shift.setOwner("jdoe");
        shift.setType(type);

        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenReturn(shift);

        Property property = provider.getProperty(mock(Log.class));

        assertNotNull(property);
        assertEquals("Shift", property.getName());
        assertEquals("42", attr(property, "Id"));
        assertEquals("Operations", attr(property, "Type"));
        assertEquals(SHIFT_URL + "/shift/Operations/42", attr(property, "URL"));
        assertEquals("jdoe", attr(property, "Owner"));
    }

    @Test
    void testGetPropertyWithInactiveShift() {
        Shift shift = new Shift();
        shift.setId(10);
        shift.setStatus("Closed");

        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenReturn(shift);

        assertNull(provider.getProperty(mock(Log.class)));
    }

    @Test
    void testGetPropertyWhenServiceReturnsNull() {
        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenReturn(null);

        assertNull(provider.getProperty(mock(Log.class)));
    }

    @Test
    void testGetPropertyWhenServiceThrowsException() {
        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenThrow(new RuntimeException("Connection refused"));

        assertNull(provider.getProperty(mock(Log.class)));
    }

    @Test
    void testGetPropertyWithNullShiftType() {
        Shift shift = new Shift();
        shift.setId(7);
        shift.setStatus("Active");
        shift.setType(null);

        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenReturn(shift);

        Property property = provider.getProperty(mock(Log.class));

        assertNotNull(property);
        assertEquals("Operations", attr(property, "Type"));
    }

    // -------------------------------------------------------------------------
    // Multiple shift types
    // -------------------------------------------------------------------------

    @Test
    void testMultipleTypes_firstTypeActive_returnsFirstType() {
        provider = new ShiftPropertyProvider(SHIFT_URL, List.of("Operations", "Safety"),
                mockRestTemplate, NO_CACHE, Clock.systemUTC());

        Shift ops = activeShift(1, "Operations");
        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/Operations", Shift.class))
                .thenReturn(ops);

        Property property = provider.getProperty(mock(Log.class));

        assertNotNull(property);
        assertEquals("Operations", attr(property, "Type"));
        verify(mockRestTemplate, never()).getForObject(SHIFT_URL + "/shift/Safety", Shift.class);
    }

    @Test
    void testMultipleTypes_firstTypeInactive_fallsThrough() {
        provider = new ShiftPropertyProvider(SHIFT_URL, List.of("Operations", "Safety"),
                mockRestTemplate, NO_CACHE, Clock.systemUTC());

        Shift inactiveOps = new Shift();
        inactiveOps.setStatus("Closed");
        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/Operations", Shift.class))
                .thenReturn(inactiveOps);

        Shift activeSafety = activeShift(2, "Safety");
        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/Safety", Shift.class))
                .thenReturn(activeSafety);

        Property property = provider.getProperty(mock(Log.class));

        assertNotNull(property);
        assertEquals("Safety", attr(property, "Type"));
    }

    @Test
    void testMultipleTypes_allInactive_returnsNull() {
        provider = new ShiftPropertyProvider(SHIFT_URL, List.of("Operations", "Safety"),
                mockRestTemplate, NO_CACHE, Clock.systemUTC());

        Shift closed = new Shift();
        closed.setStatus("Closed");
        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/Operations", Shift.class))
                .thenReturn(closed);
        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/Safety", Shift.class))
                .thenReturn(closed);

        assertNull(provider.getProperty(mock(Log.class)));
    }

    // -------------------------------------------------------------------------
    // Caching
    // -------------------------------------------------------------------------

    @Test
    void testCacheHit_secondCallDoesNotHitService() {
        AtomicLong nowMs = new AtomicLong(1000L);
        Clock testClock = clockAt(nowMs);

        provider = new ShiftPropertyProvider(SHIFT_URL, List.of(SHIFT_TYPE), mockRestTemplate,
                30_000L, testClock);

        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenReturn(activeShift(5, "Operations"));

        provider.getProperty(mock(Log.class));
        provider.getProperty(mock(Log.class));

        // Service called exactly once; second call served from cache
        verify(mockRestTemplate, times(1))
                .getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class);
    }

    @Test
    void testCacheExpiry_refetchesAfterTtl() {
        AtomicLong nowMs = new AtomicLong(1000L);
        Clock testClock = clockAt(nowMs);

        provider = new ShiftPropertyProvider(SHIFT_URL, List.of(SHIFT_TYPE), mockRestTemplate,
                5_000L, testClock);

        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenReturn(activeShift(5, "Operations"));

        provider.getProperty(mock(Log.class)); // fetches and caches

        nowMs.set(7000L); // advance clock past the 5s TTL

        provider.getProperty(mock(Log.class)); // should re-fetch

        verify(mockRestTemplate, times(2))
                .getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class);
    }

    @Test
    void testCacheNullResponse_doesNotHitServiceAgain() {
        AtomicLong nowMs = new AtomicLong(1000L);
        provider = new ShiftPropertyProvider(SHIFT_URL, List.of(SHIFT_TYPE), mockRestTemplate,
                30_000L, clockAt(nowMs));

        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenReturn(null);

        provider.getProperty(mock(Log.class));
        provider.getProperty(mock(Log.class));

        verify(mockRestTemplate, times(1))
                .getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class);
    }

    @Test
    void testExceptionIsNotCached_retriesOnNextCall() {
        provider = new ShiftPropertyProvider(SHIFT_URL, List.of(SHIFT_TYPE), mockRestTemplate,
                30_000L, Clock.systemUTC());

        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn(activeShift(3, "Operations"));

        assertNull(provider.getProperty(mock(Log.class)));       // exception, not cached
        assertNotNull(provider.getProperty(mock(Log.class)));    // succeeds on retry

        verify(mockRestTemplate, times(2))
                .getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Shift activeShift(int id, String typeName) {
        ShiftType type = new ShiftType();
        type.setId(id);
        type.setName(typeName);

        Shift shift = new Shift();
        shift.setId(id);
        shift.setStatus("Active");
        shift.setOwner("operator");
        shift.setType(type);
        return shift;
    }

    private static Clock clockAt(AtomicLong nowMs) {
        return new Clock() {
            public ZoneOffset getZone() { return ZoneOffset.UTC; }
            public Clock withZone(java.time.ZoneId zone) { return this; }
            public Instant instant() { return Instant.ofEpochMilli(nowMs.get()); }
        };
    }

    private String attr(Property property, String name) {
        return property.getAttributes().stream()
                .filter(a -> name.equals(a.getName()))
                .map(Attribute::getValue)
                .findFirst()
                .orElse(null);
    }
}
