package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_containsValue_Test {

    private TTLCache<Integer, String> ttlCache;

    /**
     * Create a fresh cache for each test. The default TTL (10 seconds) and
     * no‑size‑limit configuration are sufficient for the majority of tests.
     * A short‑TTL cache is created in the specific test that needs expiration.
     */
    @BeforeEach
    void setupBeforeEach() {
        // 10 s TTL, unlimited size
        ttlCache = new TTLCache<>(10_000L, -1);
    }

    /**
     * Clear the reference after each test to aid GC and to avoid accidental reuse.
     */
    @AfterEach
    void teardownAfterEach() {
        ttlCache = null;
    }

    /**
     * Shut down the shared background purge thread used by all TTLCache instances.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    /**
     * Supplies the arguments for {@link #containsValue_positiveMatches(Object, boolean)}.
     */
    private static Stream<Arguments> providePositiveMatches() {
        return Stream.of(// Cache contains a single entry with key "key1" and value "value1".
        // The method should find the matching value and return true.
        Arguments.of("value1", true), // Cache contains an entry whose stored value is null.
        // Passing null to containsValue should match via Objects.equals and return true.
        Arguments.of(null, true));
    }

    @Test
    void containsValue_returnsTrueWhenValueIsPresent() {
        ttlCache.put(1, "alpha");
        ttlCache.put(2, "beta");
        ttlCache.put(3, "gamma");
        assertTrue(ttlCache.containsValue("beta"), "containsValue should return true for a value that exists in the cache");
    }

    @Test
    void containsValue_returnsFalseWhenValueIsAbsent() {
        ttlCache.put(1, "alpha");
        ttlCache.put(2, "beta");
        assertFalse(ttlCache.containsValue("delta"), "containsValue should return false when the value is not stored in the cache");
    }

    @Test
    void containsValue_handlesNullValuesCorrectly() {
        ttlCache.put(1, null);
        ttlCache.put(2, "notNull");
        // Null is stored – should be found
        assertTrue(ttlCache.containsValue(null), "containsValue should return true for a stored null value");
        // Remove the null entry and verify the result changes
        ttlCache.remove(1);
        assertFalse(ttlCache.containsValue(null), "containsValue should return false after the null entry has been removed");
    }

    @Test
    void containsValue_returnsFalseAfterEntryExpires() throws InterruptedException {
        // Very short TTL (200 ms) and a quick cleanup interval (50 ms)
        TTLCache<Integer, String> shortTtlCache = new TTLCache<>(200L, -1, 50L);
        shortTtlCache.put(1, "temp");
        assertTrue(shortTtlCache.containsValue("temp"), "Value must be present immediately after insertion");
        // Wait long enough for the entry to expire and the purge task to run
        Thread.sleep(350L);
        assertFalse(shortTtlCache.containsValue("temp"), "Value should no longer be reported after its TTL has elapsed");
    }

    /**
     * Parameterized test covering positive matches:
     *   - a regular non‑null value that is present,
     *   - a null value that is present.
     */
    @ParameterizedTest(name = "[{index}] containsValue({0}) → {1}")
    @MethodSource("providePositiveMatches")
    void containsValue_positiveMatches(Object value, boolean expected) {
        // Use a String‑keyed cache for readability
        TTLCache<String, String> stringCache = new TTLCache<>(10_000L, -1);
        // Populate the cache with a regular entry and a null entry
        stringCache.put("key1", "value1");
        stringCache.put("keyNull", null);
        boolean actual = stringCache.containsValue(value);
        assertEquals(expected, actual, () -> "containsValue should return " + expected + " for value: " + value);
    }

    /**
     * Verifies that a deliberately nullified internal map leads to a
     * {@link NullPointerException}. The cacheMap field is private, so we
     * manipulate it via reflection.
     */
    @Test
    void containsValue_nullCacheMap_throwsNullPointerException() throws Exception {
        // Insert a regular entry so the cache is otherwise functional.
        ttlCache.put(1, "any");
        // Use reflection to set the private 'cacheMap' field to null.
        Field cacheMapField = TTLCache.class.getDeclaredField("cacheMap");
        cacheMapField.setAccessible(true);
        cacheMapField.set(ttlCache, null);
        // The next call must throw NPE because cacheMap is null.
        assertThrows(NullPointerException.class, () -> ttlCache.containsValue("any"), "containsValue should throw NullPointerException when internal cacheMap is null");
    }
}
