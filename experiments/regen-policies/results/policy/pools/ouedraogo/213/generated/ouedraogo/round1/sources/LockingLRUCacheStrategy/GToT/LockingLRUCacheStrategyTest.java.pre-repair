package com.cedarsoftware.util.cache;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LockingLRUCacheStrategy}.
 * Covers normal operation, edge cases and expected exception scenarios.
 */
public class LockingLRUCacheStrategyTest {

    private LockingLRUCacheStrategy<String, String> cache;
    private static final int CAPACITY = 3;

    @Before
    public void setUp() {
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    /* ---------------------------------------------------------------------- */
    /*  Constructor & basic getters                                           */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeCapacityThrows() {
        new LockingLRUCacheStrategy<String, String>(-1);
    }

    @Test
    public void testGetCapacity() {
        assertEquals(CAPACITY, cache.getCapacity());
    }

    /* ---------------------------------------------------------------------- */
    /*  put() / get() basic behavior                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetBasic() {
        assertNull(cache.put("a", "alpha"));
        assertEquals("alpha", cache.get("a"));
    }

    @Test
    public void testPutUpdatesExistingValue() {
        cache.put("b", "beta");
        assertEquals("beta", cache.put("b", "beta2"));
        assertEquals("beta2", cache.get("b"));
    }

    @Test
    public void testPutNullKeyAndValue() {
        // Both key and value may be null according to the class Javadoc.
        assertNull(cache.put(null, null));
        assertNull(cache.get(null));
        cache.put("c", null);
        assertNull(cache.get("c"));
        cache.put(null, "nullKey");
        assertEquals("nullKey", cache.get(null));
    }

    @Test
    public void testGetNonExistingReturnsNull() {
        assertNull(cache.get("nonexistent"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Eviction policy (LRU)                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEvictionWhenCapacityExceeded() {
        cache.put("1", "one");
        cache.put("2", "two");
        cache.put("3", "three");
        // At capacity now; access "1" to make it most‑recently used.
        assertEquals("one", cache.get("1"));
        // Adding a fourth entry should evict the least‑recently used ("2").
        cache.put("4", "four");
        assertNull(cache.get("2")); // evicted
        assertEquals("one", cache.get("1")); // still present
        assertEquals("three", cache.get("3"));
        assertEquals("four", cache.get("4"));
    }

    /* ---------------------------------------------------------------------- */
    /*  remove() / clear()                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRemoveExisting() {
        cache.put("x", "ex");
        assertEquals("ex", cache.remove("x"));
        assertNull(cache.get("x"));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testRemoveNonExistingReturnsNull() {
        assertNull(cache.remove("missing"));
    }

    @Test
    public void testClearEmptiesCache() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertNull(cache.get("a"));
        assertNull(cache.get("b"));
    }

    /* ---------------------------------------------------------------------- */
    /*  size(), isEmpty(), containsKey(), containsValue()                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(cache.isEmpty());
        cache.put("k1", "v1");
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());
        cache.put("k2", "v2");
        assertEquals(2, cache.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        cache.put("key", "value");
        assertTrue(cache.containsKey("key"));
        assertFalse(cache.containsKey("other"));
        assertTrue(cache.containsValue("value"));
        assertFalse(cache.containsValue("other"));
    }

    @Test
    public void testContainsValueWithNull() {
        cache.put("nullKey", null);
        assertTrue(cache.containsValue(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  entrySet(), keySet(), values() – order must reflect LRU (most recent first) */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEntrySetOrder() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");
        // Access "a" to move it to the head.
        cache.get("a");
        List<String> keysInOrder = new ArrayList<>();
        for (Map.Entry<String, String> e : cache.entrySet()) {
            keysInOrder.add(e.getKey());
        }
        assertEquals(Arrays.asList("a", "c", "b"), keysInOrder);
    }

    @Test
    public void testKeySetOrder() {
        cache.put("1", "one");
        cache.put("2", "two");
        cache.put("3", "three");
        cache.get("2"); // make "2" most recent
        List<String> keyList = new ArrayList<>(cache.keySet());
        assertEquals(Arrays.asList("2", "3", "1"), keyList);
    }

    @Test
    public void testValuesOrder() {
        cache.put("x", "X");
        cache.put("y", "Y");
        cache.put("z", "Z");
        cache.get("y"); // most recent
        List<String> values = new ArrayList<>(cache.values());
        assertEquals(Arrays.asList("Y", "Z", "X"), values);
    }

    /* ---------------------------------------------------------------------- */
    /*  putAll() and null handling                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAllCopiesAllEntries() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("a", "A");
        source.put("b", "B");
        source.put("c", "C");
        cache.putAll(source);
        assertEquals(3, cache.size());
        assertEquals("A", cache.get("a"));
        assertEquals("B", cache.get("b"));
        assertEquals("C", cache.get("c"));
    }

    @Test(expected = NullPointerException.class)
    public void testPutAllNullMapThrows() {
        cache.putAll(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  equals() and hashCode() consistency                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");

        Map<String, String> other = new LinkedHashMap<>();
        other.put("k1", "v1");
        other.put("k2", "v2");

        assertTrue(cache.equals(other));
        assertEquals(other.entrySet().hashCode(), cache.hashCode());
        // modify other – equality should break
        other.put("k3", "v3");
        assertFalse(cache.equals(other));
    }

    /* ---------------------------------------------------------------------- */
    /*  toString() – formatting and self‑reference handling                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringBasicFormatting() {
        cache.put("one", "1");
        cache.put("two", "2");
        String s = cache.toString();
        // Order is most recent first; after two puts "two" is most recent.
        assertTrue(s.startsWith("{two=2, one=1"));
        assertTrue(s.endsWith("}"));
    }

    @Test
    public void testToStringSelfReferencePlaceholder() {
        // Insert the cache itself as a value and as a key.
        cache.put("selfKey", cache);
        cache.put(cache, "selfValue");
        String s = cache.toString();

        // Both occurrences should be replaced with the placeholder.
        assertTrue(s.contains("selfKey=(this Collection)"));
        assertTrue(s.contains("(this Collection)=selfValue"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Additional edge cases – handling of duplicate puts and capacity 0     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCapacityZeroCacheNeverStores() {
        LockingLRUCacheStrategy<String, String> zeroCache = new LockingLRUCacheStrategy<>(0);
        zeroCache.put("a", "A");
        assertTrue(zeroCache.isEmpty());
        assertNull(zeroCache.get("a"));
    }

    @Test
    public void testPutSameKeyMultipleTimesDoesNotGrowBeyondCapacity() {
        cache.put("x", "1");
        cache.put("y", "2");
        cache.put("z", "3");
        cache.put("x", "4"); // update existing
        cache.put("y", "5"); // update existing
        assertEquals(CAPACITY, cache.size());
        // Ensure the most recent order reflects the latest accesses.
        List<String> keys = new ArrayList<>(cache.keySet());
        assertEquals(Arrays.asList("y", "x", "z"), keys);
    }
}
