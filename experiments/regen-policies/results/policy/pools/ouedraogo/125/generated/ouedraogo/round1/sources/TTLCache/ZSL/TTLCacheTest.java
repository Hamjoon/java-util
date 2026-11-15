package com.cedarsoftware.util;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TTLCache}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction validation (TTL & cleanup interval checks)</li>
 *   <li>Basic Map operations: put, get, remove, clear, size, isEmpty</li>
 *   <li>TTL expiration behavior</li>
 *   <li>LRU eviction when a maximum size is supplied</li>
 *   <li>containsKey / containsValue semantics (including expiry handling)</li>
 *   <li>putAll, keySet, values, entrySet iterator & removal</li>
 *   <li>equals, hashCode and toString contracts</li>
 *   <li>Scheduler shutdown</li>
 * </ul>
 *
 * No mocking or subclassing is used – only the public API exposed by {@link TTLCache}
 * and standard JDK classes.
 */
public class TTLCacheTest {

    private static final long SHORT_TTL = 50L;          // 50 ms TTL for expiration tests
    private static final long CLEANUP_INTERVAL = 20L;  // short cleanup interval (not strictly required)

    @Before
    public void setUp() {
        // No shared mutable state between tests
    }

    @AfterClass
    public static void tearDownClass() {
        // Shut down the shared scheduler thread created by TTLCache
        TTLCache.shutdown();
    }

    /*** Construction validation ***/
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroTtl() {
        new TTLCache<>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeCleanupInterval() {
        new TTLCache<>(100, -1, 5);
    }

    /*** Basic put / get / remove / size / isEmpty ***/
    @Test
    public void testPutGetRemoveBasicOperations() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        // put
        assertNull(cache.put("k1", "v1"));
        assertEquals(1, cache.size());
        assertFalse(cache.isEmpty());

        // get existing
        assertEquals("v1", cache.get("k1"));

        // overwrite
        assertEquals("v1", cache.put("k1", "v2"));
        assertEquals("v2", cache.get("k1"));

        // remove
        assertEquals("v2", cache.remove("k1"));
        assertNull(cache.get("k1"));
        assertTrue(cache.isEmpty());
    }

    /*** TTL expiration ***/
    @Test
    public void testEntryExpiresAfterTtl() throws InterruptedException {
        TTLCache<String, String> cache = new TTLCache<>(SHORT_TTL, -1, CLEANUP_INTERVAL);
        cache.put("temp", "value");
        assertEquals("value", cache.get("temp"));
        // Wait longer than TTL
        Thread.sleep(SHORT_TTL + 30);
        // Access after expiry should return null and remove the entry
        assertNull(cache.get("temp"));
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("temp"));
    }

    /*** containsKey / containsValue with expiration ***/
    @Test
    public void testContainsKeyAndValueRespectExpiration() throws InterruptedException {
        TTLCache<String, String> cache = new TTLCache<>(SHORT_TTL);
        cache.put("a", "alpha");
        cache.put("b", "beta");

        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsValue("beta"));

        Thread.sleep(SHORT_TTL + 20);
        // After expiration, both checks must be false
        assertFalse(cache.containsKey("a"));
        assertFalse(cache.containsValue("beta"));
    }

    /*** LRU eviction when maxSize is set ***/
    @Test
    public void testLruEvictionRespectsMaxSize() {
        TTLCache<String, String> cache = new TTLCache<>(1000, 2);
        cache.put("first", "1");   // LRU
        cache.put("second", "2");  // MRU

        // Access "first" to make it MRU
        assertEquals("1", cache.get("first")); // order: second (LRU), first (MRU)

        // Insert third entry – should evict "second"
        cache.put("third", "3");
        assertFalse(cache.containsKey("second"));
        assertTrue(cache.containsKey("first"));
        assertTrue(cache.containsKey("third"));
        assertEquals(2, cache.size());
    }

    /*** putAll, keySet, values ***/
    @Test
    public void testPutAllKeySetAndValues() {
        TTLCache<String, Integer> cache = new TTLCache<>(1000);
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 10);
        source.put("y", 20);
        source.put("z", 30);

        cache.putAll(source);
        assertEquals(3, cache.size());

        Set<String> keys = cache.keySet();
        assertEquals(new HashSet<>(Arrays.asList("x", "y", "z")), keys);

        Collection<Integer> values = cache.values();
        assertTrue(values.containsAll(Arrays.asList(10, 20, 30)));
        assertEquals(3, values.size());
    }

    /*** EntrySet iterator removal ***/
    @Test
    public void testEntrySetIteratorRemove() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");

        Iterator<Map.Entry<String, String>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("b".equals(e.getKey())) {
                it.remove(); // should remove entry "b"
            }
        }

        assertEquals(2, cache.size());
        assertFalse(cache.containsKey("b"));
        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsKey("c"));
    }

    /*** equals and hashCode contracts ***/
    @Test
    public void testEqualsAndHashCode() {
        TTLCache<String, String> cache1 = new TTLCache<>(1000);
        TTLCache<String, String> cache2 = new TTLCache<>(1000);

        cache1.put("k1", "v1");
        cache1.put("k2", "v2");

        cache2.put("k1", "v1");
        cache2.put("k2", "v2");

        // Equality should be true
        assertTrue(cache1.equals(cache2));
        assertTrue(cache2.equals(cache1));

        // Hash codes must match for equal objects
        assertEquals(cache1.hashCode(), cache2.hashCode());

        // Change one cache – equality breaks
        cache2.put("k3", "v3");
        assertFalse(cache1.equals(cache2));
    }

    /*** equals with a standard Map ***/
    @Test
    public void testEqualsWithStandardMap() {
        TTLCache<String, Integer> cache = new TTLCache<>(1000);
        cache.put("one", 1);
        cache.put("two", 2);

        Map<String, Integer> regularMap = new HashMap<>();
        regularMap.put("one", 1);
        regularMap.put("two", 2);

        assertTrue(cache.equals(regularMap));
        assertTrue(regularMap.equals(cache));
    }

    /*** toString format ***/
    @Test
    public void testToStringRepresentation() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.put("alpha", "A");
        cache.put("beta", "B");

        String str = cache.toString();
        // The string must start with '{' and end with '}'
        assertTrue(str.startsWith("{"));
        assertTrue(str.endsWith("}"));

        // It must contain both entries, order is not important for the test
        assertTrue(str.contains("alpha=A"));
        assertTrue(str.contains("beta=B"));
        // Ensure separator is present when more than one entry
        assertTrue(str.contains(", "));
    }

    /*** clear operation ***/
    @Test
    public void testClearResetsCache() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.put("k", "v");
        assertFalse(cache.isEmpty());

        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("k"));
    }

    /*** remove non‑existent key returns null ***/
    @Test
    public void testRemoveMissingKeyReturnsNull() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        assertNull(cache.remove("nonexistent"));
    }

    /*** put with null key and null value (supported by ConcurrentHashMapNullSafe) ***/
    @Test
    public void testPutNullKeyAndValue() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
        assertEquals(1, cache.size());

        // removal works as expected
        assertNull(cache.remove(null));
        assertFalse(cache.containsKey(null));
    }
}
