package com.cedarsoftware.util.cache;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LockingLRUCacheStrategy}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic put / get semantics, including null keys/values</li>
 *   <li>LRU ordering after puts and gets</li>
 *   <li>capacity eviction</li>
 *   <li>remove, clear, size, isEmpty, containsKey, containsValue</li>
 *   <li>putAll behavior</li>
 *   <li>entrySet, keySet, values views</li>
 *   <li>equals, hashCode and toString (including self‑reference handling)</li>
 * </ul>
 */
public class LockingLRUCacheStrategyTest {

    private LockingLRUCacheStrategy<String, String> cache;
    private static final int CAPACITY = 3;

    @Before
    public void setUp() {
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    /* -------------------------------------------------
     *  Constructor / basic getters
     * ------------------------------------------------- */
    @Test
    public void testCapacityIsReturnedCorrectly() {
        assertEquals(CAPACITY, cache.getCapacity());
    }

    @Test
    public void testNegativeCapacityIsAllowedByCurrentImplementation() {
        // The Javadoc says an exception should be thrown, but the code does not enforce it.
        // The test documents the actual behaviour.
        LockingLRUCacheStrategy<Object, Object> negative = new LockingLRUCacheStrategy<>(-5);
        assertEquals(-5, negative.getCapacity());
    }

    /* -------------------------------------------------
     *  put / get and LRU ordering
     * ------------------------------------------------- */
    @Test
    public void testPutAndGetSimple() {
        assertNull(cache.put("A", "Alpha"));
        assertEquals("Alpha", cache.get("A"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testPutUpdatesExistingKeyAndMovesToHead() {
        cache.put("A", "Alpha");
        cache.put("B", "Beta");
        cache.put("C", "Gamma");

        // Access B -> should become most recent
        cache.get("B");

        List<String> keysInOrder = keysFromEntrySet(cache);
        assertEquals(Arrays.asList("B", "C", "A"), keysInOrder);
    }

    @Test
    public void testPutEvictsLeastRecentlyUsedWhenCapacityExceeded() {
        cache.put("A", "Alpha"); // LRU order: A
        cache.put("B", "Beta");  // B, A
        cache.put("C", "Gamma"); // C, B, A
        cache.put("D", "Delta"); // should evict A -> D, C, B

        assertEquals(3, cache.size());
        assertFalse(cache.containsKey("A"));
        assertTrue(cache.containsKey("D"));

        List<String> keysInOrder = keysFromEntrySet(cache);
        assertEquals(Arrays.asList("D", "C", "B"), keysInOrder);
    }

    @Test
    public void testGetMovesNodeToHeadWhenLockAcquired() {
        cache.put("A", "Alpha");
        cache.put("B", "Beta");
        cache.put("C", "Gamma"); // order: C, B, A

        // tryLock will succeed in single‑threaded test
        cache.get("A"); // A becomes most recent

        List<String> keysInOrder = keysFromEntrySet(cache);
        assertEquals(Arrays.asList("A", "C", "B"), keysInOrder);
    }

    @Test
    public void testNullKeyAndNullValueSupported() {
        cache.put(null, "nullKey");
        cache.put("nullValue", null);

        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsKey("nullValue"));
        assertEquals("nullKey", cache.get(null));
        assertNull(cache.get("nullValue"));
        assertTrue(cache.containsValue(null));
    }

    /* -------------------------------------------------
     *  remove / clear
     * ------------------------------------------------- */
    @Test
    public void testRemoveExistingEntry() {
        cache.put("X", "ex");
        cache.put("Y", "why");

        String removed = cache.remove("X");
        assertEquals("ex", removed);
        assertFalse(cache.containsKey("X"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testRemoveNonExistingEntryReturnsNull() {
        assertNull(cache.remove("nonexistent"));
    }

    @Test
    public void testClearEmptiesCache() {
        cache.put("A", "Alpha");
        cache.put("B", "Beta");
        cache.clear();

        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("A"));
    }

    /* -------------------------------------------------
     *  containsKey / containsValue
     * ------------------------------------------------- */
    @Test
    public void testContainsKeyAndContainsValue() {
        cache.put("K1", "V1");
        cache.put("K2", null);
        cache.put(null, "Vnull");

        assertTrue(cache.containsKey("K1"));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue("V1"));
        assertTrue(cache.containsValue(null));
        assertFalse(cache.containsKey("missing"));
        assertFalse(cache.containsValue("missing"));
    }

    /* -------------------------------------------------
     *  putAll
     * ------------------------------------------------- */
    @Test
    public void testPutAllCopiesAllEntriesAndRespectsCapacity() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("A", "Alpha");
        source.put("B", "Beta");
        source.put("C", "Gamma");
        source.put("D", "Delta"); // one more than capacity

        cache.putAll(source);

        // capacity is 3, so the least recently used (the first inserted, "A") should be evicted
        assertEquals(3, cache.size());
        assertFalse(cache.containsKey("A"));
        assertTrue(cache.containsKey("D"));
        // order should be D (most recent), C, B
        List<String> keys = keysFromEntrySet(cache);
        assertEquals(Arrays.asList("D", "C", "B"), keys);
    }

    /* -------------------------------------------------
     *  entrySet / keySet / values
     * ------------------------------------------------- */
    @Test
    public void testEntrySetReflectsCurrentOrder() {
        cache.put("first", "1");
        cache.put("second", "2");
        cache.put("third", "3"); // order: third, second, first

        Set<Map.Entry<String, String>> entrySet = cache.entrySet();
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();

        Map.Entry<String, String> e1 = it.next();
        Map.Entry<String, String> e2 = it.next();
        Map.Entry<String, String> e3 = it.next();

        assertEquals("third", e1.getKey());
        assertEquals("second", e2.getKey());
        assertEquals("first", e3.getKey());
    }

    @Test
    public void testKeySetPreservesLRUOrder() {
        cache.put("A", "a");
        cache.put("B", "b");
        cache.put("C", "c"); // order: C, B, A

        Set<String> keys = cache.keySet();
        Iterator<String> it = keys.iterator();

        assertEquals("C", it.next());
        assertEquals("B", it.next());
        assertEquals("A", it.next());
    }

    @Test
    public void testValuesPreservesLRUOrder() {
        cache.put("A", "a");
        cache.put("B", "b");
        cache.put("C", "c"); // order: C, B, A

        Collection<String> values = cache.values();
        Iterator<String> it = values.iterator();

        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
    }

    /* -------------------------------------------------
     *  equals / hashCode
     * ------------------------------------------------- */
    @Test
    public void testEqualsWithAnotherMapHavingSameEntries() {
        cache.put("X", "x");
        cache.put("Y", "y");

        Map<String, String> other = new HashMap<>();
        other.put("X", "x");
        other.put("Y", "y");

        assertTrue(cache.equals(other));
        assertTrue(other.equals(cache)); // Map's equals is symmetric
    }

    @Test
    public void testEqualsReturnsFalseForDifferentContent() {
        cache.put("A", "1");
        Map<String, String> other = Collections.singletonMap("A", "2");
        assertFalse(cache.equals(other));
    }

    @Test
    public void testHashCodeMatchesManualComputation() {
        cache.put("K1", "V1");
        cache.put("K2", "V2"); // order: K2, K1

        int expected = 1;
        // K2
        expected = 31 * expected + "K2".hashCode();
        expected = 31 * expected + "V2".hashCode();
        // K1
        expected = 31 * expected + "K1".hashCode();
        expected = 31 * expected + "V1".hashCode();

        assertEquals(expected, cache.hashCode());
    }

    /* -------------------------------------------------
     *  toString and self‑reference handling
     * ------------------------------------------------- */
    @Test
    public void testToStringFormatsEntriesInLRUOrder() {
        cache.put("A", "alpha");
        cache.put("B", "beta");
        cache.put("C", "gamma"); // order: C, B, A

        String s = cache.toString();
        assertEquals("{C=gamma, B=beta, A=alpha}", s);
    }

    @Test
    public void testToStringHandlesSelfReferenceGracefully() {
        // key is the cache itself, value is a regular string
        cache.put("normal", "value");
        cache.put(null, null); // also test null handling
        // Insert self‑reference
        cache.put("selfKey", null);
        // Directly manipulate the internal node to create a self‑reference value
        // (we cannot access Node, so we use the public API)
        cache.put("selfValue", null);
        // Now replace the value with the cache itself using put (overwrites)
        cache.put("selfValue", (String) (Object) cache); // unsafe cast just for test purposes

        String result = cache.toString();

        // The self‑reference should appear as "(this Collection)"
        assertTrue(result.contains("selfValue=(this Collection)"));
        // Normal entries should appear unchanged
        assertTrue(result.contains("normal=value"));
    }

    /* -------------------------------------------------
     *  Helper utilities
     * ------------------------------------------------- */
    /**
     * Returns the list of keys as they appear when iterating over {@link LockingLRUCacheStrategy#entrySet()}.
     */
    private List<String> keysFromEntrySet(LockingLRUCacheStrategy<String, String> cache) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, String> e : cache.entrySet()) {
            keys.add(e.getKey());
        }
        return keys;
    }
}
