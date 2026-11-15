package com.cedarsoftware.util.cache;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link LockingLRUCacheStrategy}.
 * The tests cover:
 * <ul>
 *   <li>basic put / get / remove behaviour</li>
 *   <li>LRU ordering and eviction when capacity is exceeded</li>
 *   <li>null keys and values</li>
 *   <li>view collections (keySet, values, entrySet)</li>
 *   <li>containsKey / containsValue</li>
 *   <li>clear, size, isEmpty</li>
 *   <li>equals, hashCode and toString</li>
 *   <li>putAll delegation</li>
 *   <li>the non‑blocking tryLock path of {@code get}</li>
 * </ul>
 */
public class LockingLRUCacheStrategyTest {

    private LockingLRUCacheStrategy<String, String> cache;

    @Before
    public void setUp() {
        // capacity 3 is small enough to provoke eviction quickly
        cache = new LockingLRUCacheStrategy<>(3);
    }

    /** Helper that returns the order of keys as shown by {@code toString()} (most‑recent → least‑recent). */
    private String orderFromToString(LockingLRUCacheStrategy<String, String> c) {
        // toString format: {k3=v3, k2=v2, k1=v1}
        String s = c.toString();
        // strip surrounding braces
        s = s.substring(1, s.length() - 1).trim();
        if (s.isEmpty()) {
            return "";
        }
        // keep only the keys (ignore values)
        StringBuilder sb = new StringBuilder();
        for (String pair : s.split(",\\s*")) {
            String key = pair.split("=")[0];
            sb.append(key).append(",");
        }
        // remove trailing comma
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    @Test
    public void testBasicPutGetRemove() {
        assertNull(cache.put("a", "A"));
        assertEquals("A", cache.get("a"));
        assertEquals("A", cache.remove("a"));
        assertNull(cache.get("a"));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testNullKeyAndValueHandling() {
        // null key
        assertNull(cache.put(null, "nullKey"));
        assertEquals("nullKey", cache.get(null));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue("nullKey"));
        assertEquals("nullKey", cache.remove(null));
        assertFalse(cache.containsKey(null));

        // null value
        assertNull(cache.put("k", null));
        assertNull(cache.get("k"));
        assertTrue(cache.containsKey("k"));
        assertTrue(cache.containsValue(null));
        assertNull(cache.remove("k"));
    }

    @Test
    public void testLRUOrderingAndEviction() {
        cache.put("one", "1");
        cache.put("two", "2");
        cache.put("three", "3");
        // order should be: three, two, one (most recent first)
        assertEquals("three,two,one", orderFromToString(cache));

        // Access "one" -> becomes most recent
        assertEquals("1", cache.get("one"));
        assertEquals("one,three,two", orderFromToString(cache));

        // Insert fourth entry, capacity is 3 → least recent ("two") evicted
        cache.put("four", "4");
        assertFalse(cache.containsKey("two"));
        assertEquals("four,one,three", orderFromToString(cache));
        assertEquals(3, cache.size());

        // Verify that the evicted node is truly gone
        assertNull(cache.get("two"));
    }

    @Test
    public void testPutAllDelegatesCorrectly() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "A");
        source.put("b", "B");
        source.put("c", "C");
        cache.putAll(source);
        assertEquals(3, cache.size());
        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsKey("b"));
        assertTrue(cache.containsKey("c"));
        // order after putAll should be: c, b, a (last entry added becomes most recent)
        assertEquals("c,b,a", orderFromToString(cache));
    }

    @Test
    public void testViewsReflectCurrentState() {
        cache.put("x", "X");
        cache.put("y", "Y");
        cache.put("z", "Z");

        Set<String> keys = cache.keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.containsAll(java.util.Arrays.asList("x", "y", "z")));

        java.util.Collection<String> values = cache.values();
        assertEquals(3, values.size());
        assertTrue(values.containsAll(java.util.Arrays.asList("X", "Y", "Z")));

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        assertEquals(3, entries.size());
        for (Map.Entry<String, String> e : entries) {
            assertEquals(e.getKey(), e.getValue().substring(0, 1));
        }
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        cache.put("k1", "v1");
        cache.put("k2", null);
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsKey("k2"));
        assertFalse(cache.containsKey("missing"));

        assertTrue(cache.containsValue("v1"));
        assertTrue(cache.containsValue(null));
        assertFalse(cache.containsValue("nope"));
    }

    @Test
    public void testClearResetsCache() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertEquals("", orderFromToString(cache));
        assertFalse(cache.containsKey("a"));
    }

    @Test
    public void testEqualsAndHashCodeConsistency() {
        cache.put("p", "P");
        cache.put("q", "Q");
        cache.put("r", "R");

        Map<String, String> other = new java.util.LinkedHashMap<>();
        other.put("r", "R");
        other.put("q", "Q");
        other.put("p", "P");

        assertTrue(cache.equals(other));
        assertEquals(other.entrySet(), cache.entrySet());
        assertEquals(other.hashCode(), cache.hashCode());
    }

    @Test
    public void testToStringFormattingAndSelfReference() {
        cache.put("self", null);
        // Force a self‑reference by putting the cache itself as a value
        cache.put("c", cache.toString()); // normal value
        cache.put("selfRef", (String) (Object) cache); // unsafe cast just to store the reference

        String s = cache.toString();
        // The self reference should be rendered as "(this Collection)"
        assertTrue(s.contains("(this Collection)"));
        // Ensure normal entries appear correctly
        assertTrue(s.contains("self=null"));
        assertTrue(s.contains("c=" + cache.toString()));
    }

    @Test
    public void testGetTryLockFailsDoesNotReorder() throws Exception {
        // Populate cache
        cache.put("first", "1");
        cache.put("second", "2");
        cache.put("third", "3");
        assertEquals("third,second,first", orderFromToString(cache));

        // Acquire the internal lock so that tryLock will fail
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        java.util.concurrent.locks.Lock internalLock = (java.util.concurrent.locks.Lock) lockField.get(cache);
        internalLock.lock(); // now the lock is held by this thread

        try {
            // get should return the value but NOT move the node to head
            assertEquals("1", cache.get("first"));
            // order must stay unchanged
            assertEquals("third,second,first", orderFromToString(cache));
        } finally {
            internalLock.unlock(); // release for other tests
        }

        // After releasing, a subsequent get should reorder
        assertEquals("1", cache.get("first"));
        assertEquals("first,third,second", orderFromToString(cache));
    }

    @Test
    public void testRemoveUpdatesOrderAndSize() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");
        assertEquals(3, cache.size());

        // Remove middle element
        assertEquals("B", cache.remove("b"));
        assertEquals(2, cache.size());
        assertFalse(cache.containsKey("b"));
        // Order should now be: c, a
        assertEquals("c,a", orderFromToString(cache));

        // Remove non‑existent key returns null
        assertNull(cache.remove("missing"));
    }

    @Test
    public void testSizeIsEmptyBehavior() {
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        cache.put("x", "X");
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());
    }

    @Test
    public void testPutUpdatesExistingEntryAndMovesToHead() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3");
        assertEquals("k3,k2,k1", orderFromToString(cache));

        // Update k1's value; should move to head
        assertEquals("v1", cache.put("k1", "v1-updated"));
        assertEquals("k1,k3,k2", orderFromToString(cache));
        assertEquals("v1-updated", cache.get("k1"));
    }

    @Test
    public void testConcurrentHashMapNullSafeUsesProvidedInitialCapacity() {
        // The constructor with capacity should not throw and should accept null keys/values.
        LockingLRUCacheStrategy<Integer, Integer> intCache = new LockingLRUCacheStrategy<>(5);
        assertNull(intCache.put(null, 10));
        assertEquals(Integer.valueOf(10), intCache.get(null));
        assertNull(intCache.put(20, null));
        assertNull(intCache.get(20));
        assertEquals(2, intCache.size());
    }
}
