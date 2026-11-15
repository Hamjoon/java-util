package com.cedarsoftware.util.cache;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ThreadedLRUCacheStrategy}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Constructor argument validation</li>
 *   <li>Basic Map operations (put, get, remove, clear, size, isEmpty)</li>
 *   <li>LRU eviction when capacity is exceeded</li>
 *   <li>Timestamp refresh on access</li>
 *   <li>containsKey / containsValue semantics (including {@code null})</li>
 *   <li>Views returned by {@code entrySet()}, {@code keySet()} and {@code values()}
 *       (unmodifiable nature)</li>
 *   <li>Equality and hash‑code contract against a plain {@link java.util.HashMap}</li>
 *   <li>{@code toString()} delegating to {@link com.cedarsoftware.util.MapUtilities}</li>
 *   <li>putAll handling</li>
 * </ul>
 * <p>
 * The shared scheduler used by the cache is shut down once all tests have finished.
 */
public class ThreadedLRUCacheStrategyTest {

    private static final int CAPACITY = 3;
    private static final int CLEANUP_DELAY_MS = 10; // minimum allowed value

    private ThreadedLRUCacheStrategy<String, String> cache;

    @Before
    public void setUp() {
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MS);
    }

    @AfterClass
    public static void tearDownClass() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    /*** Constructor validation ***/
    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsZeroCapacity() {
        new ThreadedLRUCacheStrategy<>(0, CLEANUP_DELAY_MS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNegativeCapacity() {
        new ThreadedLRUCacheStrategy<>(-5, CLEANUP_DELAY_MS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsTooSmallDelay() {
        new ThreadedLRUCacheStrategy<>(CAPACITY, 5);
    }

    /*** Basic put / get / remove semantics ***/
    @Test
    public void putAndGetWork() {
        assertNull(cache.put("k1", "v1"));
        assertEquals("v1", cache.get("k1"));
        assertNull(cache.get("missing"));
    }

    @Test
    public void putReturnsPreviousValue() {
        cache.put("k", "old");
        assertEquals("old", cache.put("k", "new"));
        assertEquals("new", cache.get("k"));
    }

    @Test
    public void removeReturnsValueAndActuallyRemoves() {
        cache.put("a", "alpha");
        assertEquals("alpha", cache.remove("a"));
        assertNull(cache.get("a"));
        assertNull(cache.remove("non‑existent"));
    }

    @Test
    public void clearEmptiesCache() {
        cache.put("x", "X");
        cache.put("y", "Y");
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    /*** Capacity and LRU eviction ***/
    @Test
    public void exceedsCapacityTriggersEviction() throws InterruptedException {
        // Fill cache beyond capacity; the oldest entry should be evicted after cleanup runs
        cache.put("1", "one");   // oldest
        cache.put("2", "two");
        cache.put("3", "three");
        cache.put("4", "four");  // triggers immediate cleanup scheduling

        // Give the scheduler time to run (delay is 10 ms)
        Thread.sleep(50);

        // Size must be back to the configured capacity
        assertEquals(CAPACITY, cache.size());

        // The least‑recently‑used key ("1") must have been removed
        assertNull(cache.get("1"));
        assertEquals("two", cache.get("2"));
        assertEquals("three", cache.get("3"));
        assertEquals("four", cache.get("4"));
    }

    @Test
    public void accessUpdatesTimestampSoRecentlyAccessedItemSurvivesEviction() throws Exception {
        cache.put("A", "A1");
        cache.put("B", "B1");
        cache.put("C", "C1");

        // Access "A" to refresh its timestamp
        assertEquals("A1", cache.get("A"));

        // Add a fourth entry -> triggers eviction after delay
        cache.put("D", "D1");
        Thread.sleep(50);

        // "B" should be evicted because it was the least‑recently used
        assertNull(cache.get("B"));
        assertNotNull(cache.get("A"));
        assertNotNull(cache.get("C"));
        assertNotNull(cache.get("D"));
    }

    /*** containsKey / containsValue ***/
    @Test
    public void containsKeyAndContainsValueBehaveCorrectly() {
        cache.put(null, "nullKey");
        cache.put("nullVal", null);
        cache.put("x", "y");

        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsKey("x"));
        assertFalse(cache.containsKey("missing"));

        assertTrue(cache.containsValue("y"));
        assertTrue(cache.containsValue(null));
        assertFalse(cache.containsValue("absent"));
    }

    /*** Views – entrySet, keySet, values – must be unmodifiable ***/
    @Test
    public void entrySetIsUnmodifiableAndReflectsCurrentContent() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        Set<Map.Entry<String, String>> entries = cache.entrySet();

        // Verify content
        assertEquals(2, entries.size());
        assertTrue(entries.contains(new java.util.AbstractMap.SimpleEntry<>("k1", "v1")));

        // Attempt to modify – should throw UnsupportedOperationException
        try {
            entries.add(new java.util.AbstractMap.SimpleEntry<>("k3", "v3"));
            fail("Expected UnsupportedOperationException when adding to entrySet");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void keySetIsUnmodifiable() {
        cache.put("a", "1");
        Set<String> keys = cache.keySet();
        assertEquals(Collections.singleton("a"), keys);
        try {
            keys.remove("a");
            fail("Expected UnsupportedOperationException when removing from keySet");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void valuesIsUnmodifiable() {
        cache.put("k", "v");
        java.util.Collection<String> vals = cache.values();
        assertEquals(Collections.singleton("v"), vals);
        try {
            vals.clear();
            fail("Expected UnsupportedOperationException when clearing values collection");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    /*** putAll ***/
    @Test
    public void putAllCopiesAllEntries() {
        Map<String, String> source = new HashMap<>();
        source.put("one", "1");
        source.put("two", "2");
        source.put("three", "3");
        cache.putAll(source);
        assertEquals(3, cache.size());
        assertEquals("1", cache.get("one"));
        assertEquals("2", cache.get("two"));
        assertEquals("3", cache.get("three"));
    }

    /*** Equality and hashCode against a regular Map ***/
    @Test
    public void equalsAndHashCodeMatchPlainMap() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put(null, null);

        Map<String, String> plain = new HashMap<>();
        plain.put("a", "A");
        plain.put("b", "B");
        plain.put(null, null);

        assertTrue(cache.equals(plain));
        assertTrue(plain.equals(cache));
        assertEquals(cache.hashCode(), plain.hashCode());
    }

    /*** toString delegates to MapUtilities.mapToString ***/
    @Test
    public void toStringContainsAllEntries() {
        cache.put("x", "X");
        cache.put("y", "Y");
        String s = cache.toString();
        assertTrue(s.contains("x"));
        assertTrue(s.contains("X"));
        assertTrue(s.contains("y"));
        assertTrue(s.contains("Y"));
        // The format is map‑like, e.g. {x=X, y=Y}
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
    }
}
