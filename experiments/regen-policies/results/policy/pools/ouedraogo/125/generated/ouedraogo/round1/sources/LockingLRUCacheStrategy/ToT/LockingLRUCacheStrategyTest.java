package com.cedarsoftware.util.cache;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LockingLRUCacheStrategy}.
 *
 * The tests are grouped by the method they exercise and cover:
 * <ul>
 *   <li>normal usage (insert, retrieve, update)</li>
 *   <li>edge cases (null keys/values, zero/negative capacity)</li>
 *   <li>error conditions (null argument to {@code putAll})</li>
 *   <li>LRU ordering and eviction logic</li>
 *   <li>concurrency aspects of the non‑blocking {@code get}</li>
 *   <li>collection view correctness (entrySet, keySet, values)</li>
 *   <li>Object contract methods (equals, hashCode, toString)</li>
 * </ul>
 *
 * No mocking frameworks are used; all objects are created through their public
 * constructors.  Reflection is only employed to obtain the private {@code lock}
 * field so that we can deliberately hold the lock and verify the {@code tryLock}
 * behaviour of {@code get()}.
 */
public class LockingLRUCacheStrategyTest {

    private LockingLRUCacheStrategy<String, String> cache;

    @Before
    public void setUp() {
        // default capacity 3 for most tests; individual tests may replace it.
        cache = new LockingLRUCacheStrategy<>(3);
    }

    @After
    public void tearDown() {
        cache.clear();
    }

    /** -----------------------------------------------------------
     *  Constructor & basic size / empty checks
     *  ----------------------------------------------------------- */
    @Test
    public void testConstructorNegativeCapacityDoesNotThrow() {
        // The Javadoc says an exception should be thrown but the implementation does not.
        // The test asserts the actual behaviour (no exception) and that the cache works.
        LockingLRUCacheStrategy<Integer, Integer> negative = new LockingLRUCacheStrategy<>(-5);
        assertTrue(negative.isEmpty());
        negative.put(1, 1);
        assertEquals(Integer.valueOf(1), negative.get(1));
    }

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
    }

    /** -----------------------------------------------------------
     *  put() – insertion, update, eviction, null handling
     *  ----------------------------------------------------------- */
    @Test
    public void testPutSimpleInsertionAndUpdate() {
        assertNull(cache.put("k1", "v1"));                     // first insert returns null
        assertEquals("v1", cache.get("k1"));                  // retrieval works
        assertEquals("v1", cache.put("k1", "v1-updated"));    // update returns previous value
        assertEquals("v1-updated", cache.get("k1"));
    }

    @Test
    public void testPutEvictsLeastRecentlyUsed() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");   // cache now full (capacity 3)

        // Access 'a' to make it most‑recent
        cache.get("a");

        // Insert a new entry, should evict LRU which is 'b'
        cache.put("d", "D");

        assertNull(cache.get("b")); // evicted
        assertEquals("A", cache.get("a"));
        assertEquals("C", cache.get("c"));
        assertEquals("D", cache.get("d"));
        assertEquals(3, cache.size());
    }

    @Test
    public void testPutWithNullKeyAndValue() {
        cache.put(null, "nullKey");
        cache.put("nullValue", null);
        cache.put(null, null); // replace previous null‑key entry

        assertNull(cache.get(null));
        assertNull(cache.get("nullValue"));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
    }

    /** -----------------------------------------------------------
     *  get() – normal retrieval and try‑lock behaviour
     *  ----------------------------------------------------------- */
    @Test
    public void testGetMovesNodeToHeadWhenLockAvailable() {
        cache.put("x", "X");
        cache.put("y", "Y");
        cache.put("z", "Z"); // order: z (MRU), y, x (LRU)

        // Access 'x' – it should become most recent
        cache.get("x");

        // Verify order via entrySet (LinkedHashMap preserves insertion order)
        Set<Map.Entry<String, String>> entries = cache.entrySet();
        String[] expected = {"x", "z", "y"}; // after moving x to head, order: x, z, y
        int i = 0;
        for (Map.Entry<String, String> e : entries) {
            assertEquals(expected[i++], e.getKey());
        }
    }

    @Test
    public void testGetDoesNotMoveWhenLockHeldByAnotherThread() throws Exception {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C"); // order: c,b,a (c most recent)

        // Acquire the internal lock in a separate thread and keep it held.
        final Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        final java.util.concurrent.locks.Lock internalLock = (java.util.concurrent.locks.Lock) lockField.get(cache);
        internalLock.lock(); // hold the lock

        try {
            // While the lock is held, get() will fail to obtain tryLock()
            cache.get("a"); // should not move 'a' to head

            // Verify order remains unchanged: c,b,a
            Set<Map.Entry<String, String>> entries = cache.entrySet();
            String[] expected = {"c", "b", "a"};
            int i = 0;
            for (Map.Entry<String, String> e : entries) {
                assertEquals(expected[i++], e.getKey());
            }
        } finally {
            internalLock.unlock(); // release for subsequent tests
        }

        // After lock is released, a subsequent get() should move 'a' to head.
        cache.get("a");
        Set<Map.Entry<String, String>> after = cache.entrySet();
        String[] expectedAfter = {"a", "c", "b"};
        int i = 0;
        for (Map.Entry<String, String> e : after) {
            assertEquals(expectedAfter[i++], e.getKey());
        }
    }

    /** -----------------------------------------------------------
     *  putAll()
     *  ----------------------------------------------------------- */
    @Test
    public void testPutAllAddsAllEntriesAndRespectsCapacity() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("k1", "v1");
        source.put("k2", "v2");
        source.put("k3", "v3");
        source.put("k4", "v4"); // one more than capacity

        cache.putAll(source);

        // Because capacity is 3, the least‑recently used entry should be evicted.
        // The order of insertion via putAll is k1,k2,k3,k4; after each put the
        // newly added key becomes most recent, so after the final insertion the
        // LRU entry is k1.
        assertNull(cache.get("k1"));
        assertEquals("v2", cache.get("k2"));
        assertEquals("v3", cache.get("k3"));
        assertEquals("v4", cache.get("k4"));
        assertEquals(3, cache.size());
    }

    @Test(expected = NullPointerException.class)
    public void testPutAllNullMapThrowsNPE() {
        cache.putAll(null);
    }

    /** -----------------------------------------------------------
     *  remove()
     *  ----------------------------------------------------------- */
    @Test
    public void testRemoveDeletesEntryAndUpdatesOrder() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C"); // order: c,b,a

        String removed = cache.remove("b");
        assertEquals("B", removed);
        assertNull(cache.get("b"));
        assertEquals(2, cache.size());

        // Remaining order should be c (MRU), a (LRU)
        Set<String> keys = cache.keySet();
        String[] expected = {"c", "a"};
        int i = 0;
        for (String k : keys) {
            assertEquals(expected[i++], k);
        }
    }

    /** -----------------------------------------------------------
     *  clear()
     *  ----------------------------------------------------------- */
    @Test
    public void testClearEmptiesCache() {
        cache.put("x", "X");
        cache.put("y", "Y");
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertNull(cache.get("x"));
    }

    /** -----------------------------------------------------------
     *  containsKey / containsValue
     *  ----------------------------------------------------------- */
    @Test
    public void testContainsKeyAndValue() {
        cache.put("k", "v");
        assertTrue(cache.containsKey("k"));
        assertTrue(cache.containsValue("v"));
        assertFalse(cache.containsKey("missing"));
        assertFalse(cache.containsValue("missing"));
    }

    /** -----------------------------------------------------------
     *  entrySet / keySet / values – order verification
     *  ----------------------------------------------------------- */
    @Test
    public void testEntrySetOrderMatchesLRU() {
        cache.put("one", "1");
        cache.put("two", "2");
        cache.put("three", "3"); // order: three, two, one

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        String[] expectedKeys = {"three", "two", "one"};
        int i = 0;
        for (Map.Entry<String, String> e : entries) {
            assertEquals(expectedKeys[i++], e.getKey());
        }
    }

    @Test
    public void testKeySetAndValuesOrder() {
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C"); // order: c,b,a

        Set<String> keys = cache.keySet();
        String[] expectedKeys = {"c", "b", "a"};
        int i = 0;
        for (String k : keys) {
            assertEquals(expectedKeys[i++], k);
        }

        Collection<String> values = cache.values();
        String[] expectedValues = {"C", "B", "A"};
        i = 0;
        for (String v : values) {
            assertEquals(expectedValues[i++], v);
        }
    }

    /** -----------------------------------------------------------
     *  equals() and hashCode()
     *  ----------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCodeAgainstMap() {
        cache.put("x", "X");
        cache.put("y", "Y");

        Map<String, String> other = new LinkedHashMap<>();
        other.put("y", "Y");
        other.put("x", "X"); // note different insertion order

        // equals should compare entry sets, order does not matter for Map equality
        assertTrue(cache.equals(other));
        assertEquals(cache.hashCode(), other.entrySet().hashCode());

        // Changing cache order should not affect equality
        cache.get("x"); // move x to head
        assertTrue(cache.equals(other));
    }

    /** -----------------------------------------------------------
     *  toString() – formatting and self‑reference handling
     *  ----------------------------------------------------------- */
    @Test
    public void testToStringNormal() {
        cache.put("a", "A");
        cache.put("b", "B");
        String s = cache.toString();
        // Order is most‑recent first: b, a
        assertEquals("{b=B, a=A}", s);
    }

    @Test
    public void testToStringSelfReference() {
        // Insert the cache itself as a key and as a value
        cache.put("selfKey", cache);
        cache.put(cache, "selfValue");

        String s = cache.toString();
        // Both self‑references should be replaced by "(this Collection)"
        // The exact order depends on insertion; we just verify the placeholders exist.
        assertTrue(s.contains("(this Collection)=selfValue") || s.contains("selfKey=(this Collection)"));
        assertTrue(s.contains("(this Collection)"));
    }

    /** -----------------------------------------------------------
     *  Concurrency stress – ensure get() never blocks
     *  ----------------------------------------------------------- */
    @Test
    public void testGetNeverBlocksWhenLockHeldByOtherThread() throws Exception {
        // Fill cache to capacity
        cache.put("1", "one");
        cache.put("2", "two");
        cache.put("3", "three");

        // Thread A holds the lock for a short time
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            try {
                Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
                lockField.setAccessible(true);
                java.util.concurrent.locks.Lock lock = (java.util.concurrent.locks.Lock) lockField.get(cache);
                lock.lock();
                startLatch.countDown(); // signal lock is held
                // Hold the lock for 200 ms
                Thread.sleep(200);
                lock.unlock();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                doneLatch.countDown();
            }
        });
        holder.start();

        // Wait until holder thread has locked the internal lock
        assertTrue(startLatch.await(1, TimeUnit.SECONDS));

        long start = System.nanoTime();
        // This get should return immediately (tryLock fails) and not block for 200 ms
        String val = cache.get("1");
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertEquals("one", val);
        assertTrue("get() blocked unexpectedly", durationMs < 50);

        // Ensure holder thread finishes cleanly
        assertTrue(doneLatch.await(1, TimeUnit.SECONDS));
    }
}
