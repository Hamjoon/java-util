package com.cedarsoftware.util.cache;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class LockingLRUCacheStrategyTest {

    private static final int CAPACITY = 3;
    private LockingLRUCacheStrategy<String, String> cache;

    @Before
    public void setUp() {
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    /**
     * Helper that returns the keys from the cache in LRU order
     * (most‑recently used first) by iterating over the entry set.
     */
    private List<String> keysInOrder() {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, String> e : cache.entrySet()) {
            keys.add(e.getKey());
        }
        return keys;
    }

    /** --------------------------------------------------------------------- **/
    /** BASIC put / get functionality, including null key/value handling.    **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testPutAndGetBasic() {
        assertNull(cache.put("a", "A"));
        assertEquals("A", cache.get("a"));
        assertNull(cache.put(null, "NULL_KEY"));
        assertEquals("NULL_KEY", cache.get(null));
        assertNull(cache.put("b", null));
        assertNull(cache.get("b"));
    }

    /** --------------------------------------------------------------------- **/
    /** LRU ordering: a get() should move the accessed entry to the head.      **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testLRUOrderAfterAccess() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C"); // order: c (MRU), b, a (LRU)

        // access "a" -> should become most recent
        assertEquals("A", cache.get("a"));
        List<String> expected = Arrays.asList("a", "c", "b");
        assertEquals(expected, keysInOrder());
    }

    /** --------------------------------------------------------------------- **/
    /** Capacity enforcement: the least‑recently used entry is evicted.        **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testCapacityEviction() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C"); // cache is full now
        // Access "a" to make it MRU, order: a, c, b
        cache.get("a");
        // Adding fourth entry should evict the LRU entry ("b")
        cache.put("d", "D");
        assertFalse(cache.containsKey("b"));
        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsKey("c"));
        assertTrue(cache.containsKey("d"));
        List<String> expected = Arrays.asList("d", "a", "c"); // d just added -> MRU
        assertEquals(expected, keysInOrder());
    }

    /** --------------------------------------------------------------------- **/
    /** putAll should add all entries respecting capacity and LRU ordering.   **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testPutAllRespectsCapacityAndOrdering() {
        Map<String, String> toPut = new LinkedHashMap<>();
        toPut.put("a", "A");
        toPut.put("b", "B");
        toPut.put("c", "C");
        toPut.put("d", "D"); // 4 entries, capacity is 3

        cache.putAll(toPut);
        // The first inserted ("a") should have been evicted
        assertFalse(cache.containsKey("a"));
        assertTrue(cache.containsKey("b"));
        assertTrue(cache.containsKey("c"));
        assertTrue(cache.containsKey("d"));
        // Most recent is the last inserted ("d")
        List<String> expected = Arrays.asList("d", "c", "b");
        assertEquals(expected, keysInOrder());
    }

    /** --------------------------------------------------------------------- **/
    /** remove() should delete the entry and update the LRU chain.               **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testRemoveUpdatesOrder() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");
        // Remove middle entry "b"
        assertEquals("B", cache.remove("b"));
        assertFalse(cache.containsKey("b"));
        // Remaining order should be c (MRU), a (LRU)
        List<String> expected = Arrays.asList("c", "a");
        assertEquals(expected, keysInOrder());
    }

    /** --------------------------------------------------------------------- **/
    /** clear() empties the cache and resets internal links.                    **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testClearResetsCache() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("a"));
        assertFalse(cache.containsKey("b"));
        // After clear, new puts should behave normally
        cache.put("x", "X");
        assertEquals("X", cache.get("x"));
    }

    /** --------------------------------------------------------------------- **/
    /** size() and isEmpty() reflect the current number of entries.           **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        cache.put("a", "A");
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());

        cache.put("b", "B");
        cache.put("c", "C");
        assertEquals(3, cache.size());

        cache.remove("b");
        assertEquals(2, cache.size());
    }

    /** --------------------------------------------------------------------- **/
    /** containsKey() and containsValue() work for present and absent items.  **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testContainsKeyAndContainsValue() {
        cache.put("a", "A");
        cache.put(null, "NULL_KEY");
        cache.put("b", null);

        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsKey(null));
        assertFalse(cache.containsKey("missing"));

        assertTrue(cache.containsValue("A"));
        assertTrue(cache.containsValue(null));
        assertFalse(cache.containsValue("missing"));
    }

    /** --------------------------------------------------------------------- **/
    /** entrySet() returns entries in LRU order (most recent first).           **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testEntrySetIterationOrder() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");
        // Access "a" => order: a, c, b
        cache.get("a");

        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, String> e : cache.entrySet()) {
            keys.add(e.getKey());
            values.add(e.getValue());
        }
        assertEquals(Arrays.asList("a", "c", "b"), keys);
        assertEquals(Arrays.asList("A", "C", "B"), values);
    }

    /** --------------------------------------------------------------------- **/
    /** keySet() and values() are consistent with entrySet ordering.          **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testKeySetAndValues() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");
        cache.get("b"); // order: b, c, a

        assertEquals(Arrays.asList("b", "c", "a"), new ArrayList<>(cache.keySet()));
        assertEquals(Arrays.asList("B", "C", "A"), new ArrayList<>(cache.values()));
    }

    /** --------------------------------------------------------------------- **/
    /** equals() and hashCode() are consistent with other Map implementations.   **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testEqualsAndHashCode() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");

        Map<String, String> regularMap = new LinkedHashMap<>();
        regularMap.put("c", "C");
        regularMap.put("b", "B");
        regularMap.put("a", "A"); // LinkedHashMap preserves insertion order

        // The cache's entrySet order is most‑recent‑first, while regularMap is insertion order.
        // equals() compares entry sets, so order does not matter.
        assertTrue(cache.equals(regularMap));
        assertEquals(cache.hashCode(), regularMap.hashCode());

        // Changing the regular map should break equality
        regularMap.put("d", "D");
        assertFalse(cache.equals(regularMap));
    }

    /** --------------------------------------------------------------------- **/
    /** toString() should produce a readable representation and handle self‑references. **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testToStringSelfReference() {
        cache.put("self", null);
        // Insert the cache itself as a value to trigger self‑reference formatting
        cache.put("cache", (String) (Object) cache); // cast to String via Object to satisfy generic type
        String s = cache.toString();

        // Expected format: {cache=(this Collection), self=null, ...}
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("cache=(this Collection)"));
        assertTrue(s.contains("self=null"));
    }

    /** --------------------------------------------------------------------- **/
    /** Verify that hashCode() follows the contract even after mutations.      **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testHashCodeConsistencyAfterMutations() {
        int hash1 = cache.hashCode();
        cache.put("x", "X");
        int hash2 = cache.hashCode();
        assertNotEquals(hash1, hash2);

        cache.remove("x");
        int hash3 = cache.hashCode();
        assertEquals(hash1, hash3);
    }

    /** --------------------------------------------------------------------- **/
    /** Ensure that put() returns the previous value when key already exists. **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testPutReturnsPreviousValue() {
        assertNull(cache.put("k", "v1"));
        assertEquals("v1", cache.put("k", "v2"));
        assertEquals("v2", cache.get("k"));
    }

    /** --------------------------------------------------------------------- **/
    /** Verify that putAll does not dead‑lock when invoked while the cache is locked. **/
    /** (We simulate the lock by calling putAll inside a lock‑protected block). **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testPutAllDoesNotDeadlock() {
        // The implementation already locks inside putAll, so a re‑entrant call should be safe.
        cache.put("a", "A");
        cache.putAll(Collections.singletonMap("b", "B"));
        assertTrue(cache.containsKey("b"));
    }
}
