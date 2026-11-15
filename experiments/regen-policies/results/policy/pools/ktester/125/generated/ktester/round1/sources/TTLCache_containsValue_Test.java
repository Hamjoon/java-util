package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_containsValue_Test {

    private TTLCache<Integer, String> ttlCache;

    /**
     * Simple POJO with proper equals()/hashCode()
     */
    static class Point {

        final int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Point))
                return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /**
     * Value object whose equals() always throws
     */
    static class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("equals() failure");
        }
    }

    /**
     * Create a fresh cache before each test.  A long TTL is used so that
     * entries do not expire unintentionally (except in the explicit
     * expiration test where we provide a short TTL).
     */
    @BeforeEach
    void setupBeforeEach() {
        // TTL = 10 seconds, no size limit, default cleanup interval (60 seconds)
        ttlCache = new TTLCache<>(10_000L, -1);
        // Use a long TTL so that normal entries never expire during these tests.
        ttlCache = new TTLCache<>(10_000L, -1);
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            ttlCache.clear();
        }
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    /**
     * Shut down the shared background purge thread after all tests have run.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
        TTLCache.shutdown();
    }

    private static Stream<Arguments> presentValueProvider() {
        return Stream.of(Arguments.of("apple", true, "Cache contains a single entry (key \"a\" → \"apple\")."), Arguments.of("banana", true, "Cache contains multiple entries (\"a\" → \"apple\", \"b\" → \"banana\")."));
    }

    private static Stream<Arguments> absentValueProvider() {
        return Stream.of(Arguments.of("orange", false, "Cache holds entries \"a\"→\"apple\" and \"b\"→\"banana\"; \"orange\" is absent."));
    }

    @Test
    void containsValue_returnsTrueWhenValueExists() {
        ttlCache.put(1, "Alpha");
        ttlCache.put(2, "Beta");
        ttlCache.put(3, "Gamma");
        assertTrue(ttlCache.containsValue("Alpha"), "Cache should contain value \"Alpha\"");
        assertTrue(ttlCache.containsValue("Beta"), "Cache should contain value \"Beta\"");
        assertTrue(ttlCache.containsValue("Gamma"), "Cache should contain value \"Gamma\"");
        assertFalse(ttlCache.containsValue("Delta"), "Cache should not contain value \"Delta\"");
    }

    @Test
    void containsValue_handlesNullValues() {
        ttlCache.put(10, null);
        ttlCache.put(20, "NotNull");
        assertTrue(ttlCache.containsValue(null), "Cache should report containing a null value");
        assertTrue(ttlCache.containsValue("NotNull"), "Cache should report containing \"NotNull\"");
        assertFalse(ttlCache.containsValue("Missing"), "Cache should not report a missing value");
    }

    @Test
    void containsValue_returnsFalseAfterEntryExpires() throws InterruptedException {
        // Short TTL (200 ms) and a short cleanup interval (50 ms) to force quick expiration.
        TTLCache<Integer, String> shortLivedCache = new TTLCache<>(200L, -1, 50L);
        shortLivedCache.put(99, "Ephemeral");
        // Value should be present immediately.
        assertTrue(shortLivedCache.containsValue("Ephemeral"), "Value should be present before TTL expires");
        // Wait longer than the TTL so the background task can purge the entry.
        Thread.sleep(350L);
        // After expiration the cache must no longer report the value.
        assertFalse(shortLivedCache.containsValue("Ephemeral"), "Value should not be present after TTL expiration");
        // Clean up the temporary cache instance.
        shortLivedCache.clear();
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("presentValueProvider")
    void containsValue_valuePresent(String searchValue, boolean expected, String description) {
        // arrange
        ttlCache.put(1, "apple");
        ttlCache.put(2, "banana");
        // act & assert
        assertEquals(expected, ttlCache.containsValue(searchValue), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("absentValueProvider")
    void containsValue_valueAbsent(String searchValue, boolean expected, String description) {
        ttlCache.put(1, "apple");
        ttlCache.put(2, "banana");
        assertEquals(expected, ttlCache.containsValue(searchValue), description);
    }

    @Test
    void containsValue_nullValuePresent() {
        ttlCache.put(3, null);
        ttlCache.put(4, "notNull");
        assertTrue(ttlCache.containsValue(null), "Cache contains a mapping with null value");
    }

    @Test
    void containsValue_nullValueAbsent() {
        ttlCache.put(5, "onlyNonNull");
        assertFalse(ttlCache.containsValue(null), "Cache has no null values, should return false");
    }

    @Test
    void containsValue_emptyCache() {
        assertFalse(ttlCache.containsValue("any"), "Empty cache must return false for any value");
    }

    @Test
    void containsValue_customObjectEquality() {
        Point stored = new Point(5, 10);
        ttlCache.put(6, stored);
        // different instance but equal
        Point query = new Point(5, 10);
        assertTrue(ttlCache.containsValue(query), "Equal custom objects should be recognised");
    }

    @Test
    void containsValue_differentTypeMismatch() {
        ttlCache.put(7, "stringValue");
        assertFalse(ttlCache.containsValue(12345), "Searching with Integer should not match String values");
    }

    @Test
    void containsValue_entryNodeNull_NPE() throws Exception {
        // Obtain the private cacheMap field
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Integer, TTLCache.CacheEntry<Integer, String>> map = (ConcurrentHashMap<Integer, TTLCache.CacheEntry<Integer, String>>) mapField.get(ttlCache);
        // Build a CacheEntry with a null node (using reflection)
        Class<?> entryClass = Class.forName("com.cedarsoftware.util.TTLCache$CacheEntry");
        Object entry = entryClass.getDeclaredConstructor().newInstance();
        // Set its 'node' field to null explicitly (it is already null, but we set it for clarity)
        Field nodeField = entryClass.getDeclaredField("node");
        nodeField.setAccessible(true);
        nodeField.set(entry, null);
        // Insert the malformed entry directly into the map
        map.put(999, (TTLCache.CacheEntry<Integer, String>) entry);
        // The method should now throw NPE when dereferencing entry.node.value
        assertThrows(NullPointerException.class, () -> ttlCache.containsValue("any"), "containsValue must propagate NPE when entry.node is null");
    }

    @Test
    void containsValue_cacheMapNull_NPE() throws Exception {
        // Set the private cacheMap field to null via reflection
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(ttlCache, null);
        // Expect NPE when method tries to call cacheMap.values()
        assertThrows(NullPointerException.class, () -> ttlCache.containsValue("any"), "containsValue must propagate NPE when cacheMap is null");
    }

    @Test
    void containsValue_valueEqualsThrowsException() {
        BadEquals bad = new BadEquals();
        ttlCache.put(8, bad);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ttlCache.containsValue("trigger"), "Exception from equals() should propagate");
        assertEquals("equals() failure", ex.getMessage());
    }
}
