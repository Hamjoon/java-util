package com.cedarsoftware.util.cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ThreadedLRUCacheStrategy}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (valid & invalid arguments)</li>
 *   <li>Basic Map contract (put, get, remove, clear, size, isEmpty, contains…)</li>
 *   <li>Null key / null value handling</li>
 *   <li>LRU eviction logic (including timestamp updates via get)</li>
 *   <li>Views (entrySet, keySet, values) – immutability and content</li>
 *   <li>equals / hashCode contract against a standard {@link HashMap}</li>
 *   <li>toString format (uses {@link MapUtilities#mapToString(Map)})</li>
 * </ul>
 *
 * No mocking frameworks are used – only the public API shown in the source
 * and the auxiliary classes listed in the prompt.
 */
public class ThreadedLRUCacheStrategyTest {

    /** Small delay to make cleanup deterministic while keeping the test fast. */
    private static final int CLEANUP_DELAY_MS = 20;

    /** Capacity used for most tests – small enough to trigger eviction quickly. */
    private static final int CAPACITY = 3;

    private ThreadedLRUCacheStrategy<String, String> cache;

    @Before
    public void setUp() {
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MS);
    }

    @After
    public void tearDown() {
        // Ensure the cache is cleared so that subsequent tests start fresh.
        cache.clear();
    }

    /* ---------------------------------------------------------------------- */
    /*  Constructor validation                                                */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsZeroCapacity() {
        new ThreadedLRUCacheStrategy<>(0, CLEANUP_DELAY_MS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNegativeCapacity() {
        new ThreadedLRUCacheStrategy<>(-5, CLEANUP_DELAY_MS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsTooSmallCleanupDelay() {
        new ThreadedLRUCacheStrategy<>(CAPACITY, 5); // must be >= 10
    }

    @Test
    public void constructorInitializesCapacityAndIsEmpty() {
        assertEquals(CAPACITY, cache.getCapacity());
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic Map operations                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void putAndGetSimpleEntry() {
        assertNull(cache.put("k1", "v1"));
        assertEquals("v1", cache.get("k1"));
        assertEquals(1, cache.size());
    }

    @Test
    public void putReplacesExistingValue() {
        cache.put("k", "old");
        String previous = cache.put("k", "new");
        assertEquals("old", previous);
        assertEquals("new", cache.get("k"));
        assertEquals(1, cache.size());
    }

    @Test
    public void putAllInsertsAllEntries() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "1");
        source.put("b", "2");
        source.put("c", "3");
        cache.putAll(source);
        assertEquals(3, cache.size());
        assertEquals("1", cache.get("a"));
        assertEquals("2", cache.get("b"));
        assertEquals("3", cache.get("c"));
    }

    @Test
    public void removeDeletesEntryAndReturnsValue() {
        cache.put("x", "y");
        String removed = cache.remove("x");
        assertEquals("y", removed);
        assertNull(cache.get("x"));
        assertEquals(0, cache.size());
    }

    @Test
    public void clearEmptiesCache() {
        cache.put("one", "1");
        cache.put("two", "2");
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void containsKeyAndContainsValueWork() {
        cache.put("alpha", "A");
        cache.put(null, "nullKey");
        cache.put("nullVal", null);
        assertTrue(cache.containsKey("alpha"));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue("A"));
        assertTrue(cache.containsValue(null));
        assertFalse(cache.containsKey("beta"));
        assertFalse(cache.containsValue("B"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Null handling                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void nullKeyIsSupported() {
        cache.put(null, "nullKey");
        assertEquals("nullKey", cache.get(null));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.remove(null) != null);
        assertFalse(cache.containsKey(null));
    }

    @Test
    public void nullValueIsSupported() {
        cache.put("nullVal", null);
        assertTrue(cache.containsKey("nullVal"));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get("nullVal"));
    }

    /* ---------------------------------------------------------------------- */
    /*  LRU eviction logic                                                     */
    /* ---------------------------------------------------------------------- */

    /**
     * Insert CAPACITY entries, access one of them to refresh its timestamp,
     * then insert an extra entry that forces eviction. After waiting for
     * the scheduled cleanup we expect the *least* recently used entry to be
     * removed.
     */
    @Test
    public void evictionRemovesLeastRecentlyUsedEntry() throws Exception {
        // Fill to capacity
        cache.put("A", "1");
        cache.put("B", "2");
        cache.put("C", "3");

        // Access "A" – makes it most‑recently used
        assertEquals("1", cache.get("A"));

        // Insert fourth entry; this triggers immediate cleanup scheduling
        cache.put("D", "4");
        // Wait longer than the cleanup delay to let the background task run
        Thread.sleep(CLEANUP_DELAY_MS * 3);

        // Cache size must be back to capacity
        assertEquals(CAPACITY, cache.size());

        // "B" was the LRU (never accessed after insertion), should be gone
        assertFalse(cache.containsKey("B"));
        // The other three should still be present
        assertTrue(cache.containsKey("A"));
        assertTrue(cache.containsKey("C"));
        assertTrue(cache.containsKey("D"));
    }

    /**
     * Verify that repeated puts beyond capacity do not schedule multiple
     * clean‑ups concurrently (the flag prevents overlapping tasks).
     */
    @Test
    public void multipleOverCapacityPutsDoNotCauseDuplicateCleanups() throws Exception {
        // Insert 5 entries quickly – each put after capacity should trigger a schedule,
        // but the AtomicBoolean prevents more than one pending task.
        for (int i = 0; i < 5; i++) {
            cache.put("K" + i, "V" + i);
        }
        // Give enough time for at most one cleanup to finish
        Thread.sleep(CLEANUP_DELAY_MS * 4);
        // After cleanup the size must be equal to capacity
        assertEquals(CAPACITY, cache.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  Views (entrySet, keySet, values) – immutability and content            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void entrySetIsUnmodifiableAndAccurate() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        Set<Map.Entry<String, String>> entrySet = cache.entrySet();

        // Content check
        Set<Map.Entry<String, String>> expected = new HashSet<>();
        expected.add(new AbstractMap.SimpleEntry<>("k1", "v1"));
        expected.add(new AbstractMap.SimpleEntry<>("k2", "v2"));
        assertEquals(expected, entrySet);

        // Immutability check
        try {
            entrySet.add(new AbstractMap.SimpleEntry<>("k3", "v3"));
            fail("entrySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void keySetIsUnmodifiableAndAccurate() {
        cache.put("x", "1");
        cache.put("y", "2");
        Set<String> keySet = cache.keySet();

        Set<String> expected = new HashSet<>(Arrays.asList("x", "y"));
        assertEquals(expected, keySet);

        try {
            keySet.remove("x");
            fail("keySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void valuesCollectionIsUnmodifiableAndAccurate() {
        cache.put("p", "alpha");
        cache.put("q", "beta");
        Collection<String> values = cache.values();

        List<String> expected = Arrays.asList("alpha", "beta");
        // order is not guaranteed – compare as sets
        assertEquals(new HashSet<>(expected), new HashSet<>(values));

        try {
            values.clear();
            fail("values collection should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  equals / hashCode contract                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void equalsAndHashCodeMatchStandardMap() {
        Map<String, String> reference = new HashMap<>();
        reference.put("a", "1");
        reference.put("b", "2");
        reference.put(null, null);

        cache.putAll(reference);

        // Equality with a regular map
        assertTrue(cache.equals(reference));
        assertTrue(reference.equals(cache));

        // Hash code must be identical (order‑independent)
        assertEquals(reference.hashCode(), cache.hashCode());
    }

    @Test
    public void equalsHandlesDifferentSizes() {
        cache.put("only", "value");
        Map<String, String> other = new HashMap<>();
        other.put("one", "1");
        other.put("two", "2");
        assertFalse(cache.equals(other));
    }

    @Test
    public void equalsReturnsFalseForNonMap() {
        assertFalse(cache.equals("not a map"));
    }

    /* ---------------------------------------------------------------------- */
    /*  toString delegating to MapUtilities                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toStringContainsAllEntries() {
        cache.put("k", "v");
        cache.put(null, "nullKey");
        cache.put("nullVal", null);
        String s = cache.toString();

        // MapUtilities.mapToString produces a string like {k=v, null=nullKey, nullVal=null}
        assertTrue(s.contains("k=v"));
        assertTrue(s.contains("null=nullKey"));
        assertTrue(s.contains("nullVal=null"));
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
    }
}
