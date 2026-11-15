package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link TTLCache}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors and Map‑interface methods.</li>
 *   <li>Edge‑case handling such as illegal arguments, null keys/values, TTL expiration,
 *       LRU eviction, iterator misuse, and equality contracts.</li>
 *   <li>Behaviour of static {@code shutdown()}.</li>
 * </ul>
 *
 * The scheduler used by {@code TTLCache} is a shared daemon thread.  After each test
 * suite we shut it down to avoid interference with other tests.
 */
public class TTLCacheTest {

    private static final long SHORT_TTL = 50L;          // 50 ms – for expiration tests
    private static final long LONG_TTL  = 10_000L;       // 10 s – for normal operation
    private static final long CLEANUP_INTERVAL = 60_000L; // default interval, not used in tests

    private TTLCache<String, String> cache;

    @Before
    public void setUp() {
        // default cache used by most tests – long TTL so it does not expire during the test
        cache = new TTLCache<>(LONG_TTL);
    }

    @After
    public void tearDown() {
        // Clear the cache and shut down the shared scheduler to avoid thread leakage.
        if (cache != null) {
            cache.clear();
        }
        TTLCache.shutdown();
    }

    /* ----------------------------------------------------------------------
     *  PUBLIC CONSTRUCTORS
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructorWithOnlyTTL() {
        TTLCache<Integer, Integer> c = new TTLCache<>(100);
        assertEquals(0, c.size());
        assertTrue(c.isEmpty());
    }

    @Test
    public void testConstructorWithTTLAndMaxSize() {
        TTLCache<Integer, Integer> c = new TTLCache<>(100, 2);
        assertEquals(0, c.size());
        assertTrue(c.isEmpty());
    }

    @Test
    public void testConstructorWithAllParameters() {
        TTLCache<Integer, Integer> c = new TTLCache<>(100, 2, 200);
        assertEquals(0, c.size());
        assertTrue(c.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroTTL() {
        new TTLCache<>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeTTL() {
        new TTLCache<>(-5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsTooSmallCleanupInterval() {
        new TTLCache<>(100, -1, 5);
    }

    /* ----------------------------------------------------------------------
     *  BASIC MAP OPERATIONS
     * ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGet() {
        assertNull(cache.put("a", "alpha"));
        assertEquals("alpha", cache.get("a"));
        assertEquals(1, cache.size());

        // overwrite existing key
        assertEquals("alpha", cache.put("a", "beta"));
        assertEquals("beta", cache.get("a"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testRemove() {
        cache.put("x", "xray");
        cache.put("y", "yankee");
        assertEquals("xray", cache.remove("x"));
        assertNull(cache.get("x"));
        assertEquals(1, cache.size());

        // removing non‑existent key returns null
        assertNull(cache.remove("nonexistent"));
    }

    @Test
    public void testClear() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertTrue(cache.keySet().isEmpty());
        assertTrue(cache.values().isEmpty());
        assertTrue(cache.entrySet().isEmpty());
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        cache.put("one", "1");
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        cache.put("k", "v");
        assertTrue(cache.containsKey("k"));
        assertTrue(cache.containsValue("v"));

        assertFalse(cache.containsKey("missing"));
        assertFalse(cache.containsValue("missing"));
    }

    @Test
    public void testPutAll() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "1");
        source.put("b", "2");
        cache.putAll(source);
        assertEquals(2, cache.size());
        assertEquals("1", cache.get("a"));
        assertEquals("2", cache.get("b"));
    }

    @Test
    public void testKeySet() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        Set<String> keys = cache.keySet();
        assertEquals(new HashSet<>(Arrays.asList("k1", "k2")), keys);
    }

    @Test
    public void testValues() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        Collection<String> vals = cache.values();
        assertTrue(vals.containsAll(Arrays.asList("v1", "v2")));
        assertEquals(2, vals.size());
    }

    @Test
    public void testEntrySetIterationAndRemoval() {
        cache.put("one", "1");
        cache.put("two", "2");
        cache.put("three", "3");

        Set<Map.Entry<String, String>> entrySet = cache.entrySet();
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();

        // Remove the entry with key "two" via iterator
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("two".equals(e.getKey())) {
                it.remove();
                break;
            }
        }

        assertEquals(2, cache.size());
        assertFalse(cache.containsKey("two"));
    }

    @Test(expected = IllegalStateException.class)
    public void testEntryIteratorRemoveWithoutNext() {
        cache.put("a", "A");
        Iterator<Map.Entry<String, String>> it = cache.entrySet().iterator();
        // Directly call remove – should throw IllegalStateException
        it.remove();
    }

    /* ----------------------------------------------------------------------
     *  NULL KEY / NULL VALUE SUPPORT
     * ---------------------------------------------------------------------- */

    @Test
    public void testNullKeyAndNullValue() {
        // null key
        assertNull(cache.put(null, "nullKey"));
        assertEquals("nullKey", cache.get(null));
        assertTrue(cache.containsKey(null));

        // null value
        assertNull(cache.put("nullValue", null));
        assertNull(cache.get("nullValue"));
        assertTrue(cache.containsKey("nullValue"));
        assertTrue(cache.containsValue(null));
    }

    /* ----------------------------------------------------------------------
     *  TIME‑TO‑LIVE (TTL) EXPIRATION
     * ---------------------------------------------------------------------- */

    @Test
    public void testEntryExpiresAfterTTL() throws InterruptedException {
        TTLCache<String, String> shortCache = new TTLCache<>(SHORT_TTL);
        shortCache.put("temp", "data");
        assertEquals("data", shortCache.get("temp"));
        // Wait longer than TTL
        Thread.sleep(SHORT_TTL + 20);
        // The entry should be considered expired and removed on access
        assertNull(shortCache.get("temp"));
        assertFalse(shortCache.containsKey("temp"));
        assertEquals(0, shortCache.size());
    }

    @Test
    public void testContainsKeyRemovesExpiredEntry() throws InterruptedException {
        TTLCache<String, String> shortCache = new TTLCache<>(SHORT_TTL);
        shortCache.put("expireMe", "value");
        Thread.sleep(SHORT_TTL + 20);
        assertFalse(shortCache.containsKey("expireMe"));
        assertEquals(0, shortCache.size());
    }

    /* ----------------------------------------------------------------------
     *  LRU EVICTION WHEN maxSize IS SPECIFIED
     * ---------------------------------------------------------------------- */

    @Test
    public void testLRUEvictionWhenMaxSizeExceeded() {
        TTLCache<String, String> lruCache = new TTLCache<>(LONG_TTL, 2);
        lruCache.put("first", "1");
        lruCache.put("second", "2");
        // Access "first" to make it most‑recently used
        assertEquals("1", lruCache.get("first"));
        // Adding third entry should evict "second" (least recently used)
        lruCache.put("third", "3");

        assertTrue(lruCache.containsKey("first"));
        assertFalse(lruCache.containsKey("second"));
        assertTrue(lruCache.containsKey("third"));
        assertEquals(2, lruCache.size());
    }

    @Test
    public void testLRUEvictionWithZeroMaxSize() {
        TTLCache<String, String> zeroCache = new TTLCache<>(LONG_TTL, 0);
        zeroCache.put("a", "A");
        assertFalse(zeroCache.containsKey("a"));
        assertEquals(0, zeroCache.size());
    }

    /* ----------------------------------------------------------------------
     *  EQUALITY, HASHCODE, TOSTRING
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        TTLCache<String, String> c1 = new TTLCache<>(LONG_TTL);
        TTLCache<String, String> c2 = new TTLCache<>(LONG_TTL);

        c1.put("x", "1");
        c1.put("y", "2");
        c2.put("x", "1");
        c2.put("y", "2");

        assertTrue(c1.equals(c2));
        assertTrue(c2.equals(c1));
        assertEquals(c1.hashCode(), c2.hashCode());

        // Different content
        c2.put("z", "3");
        assertFalse(c1.equals(c2));
    }

    @Test
    public void testEqualsWithNonMapAndNull() {
        assertFalse(cache.equals(null));
        assertFalse(cache.equals("not a map"));
    }

    @Test
    public void testToStringFormat() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        String s = cache.toString();

        // The string must start with '{' and end with '}' and contain both entries
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("k1=v1"));
        assertTrue(s.contains("k2=v2"));
        // Order is not guaranteed – just check comma separation when more than one entry
        if (cache.size() > 1) {
            assertTrue(s.contains(", "));
        }
    }

    /* ----------------------------------------------------------------------
     *  STATIC SHUTDOWN METHOD
     * ---------------------------------------------------------------------- */

    @Test
    public void testShutdownDoesNotThrow() {
        // The method is void and should not throw an exception even if called multiple times.
        TTLCache.shutdown();
        TTLCache.shutdown(); // second call – should be a no‑op
    }
}
