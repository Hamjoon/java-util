package com.cedarsoftware.util.cache;

import org.junit.Test;
import org.junit.Before;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class LockingLRUCacheStrategyTest {

    private LockingLRUCacheStrategy<String, String> cache;

    @Before
    public void setUp() {
        cache = new LockingLRUCacheStrategy<>(3);
    }

    @Test
    public void testPutAndGetBasicFunctionality() {
        assertNull(cache.put("A", "1"));
        assertNull(cache.put("B", "2"));
        assertEquals("1", cache.get("A"));
        assertEquals("2", cache.get("B"));
        assertEquals(2, cache.size());
    }

    @Test
    public void testEvictionWhenCapacityExceeded() {
        cache.put("A", "1");
        cache.put("B", "2");
        cache.put("C", "3"); // cache is now full (capacity 3)

        // Access "A" to make it most-recently used
        cache.get("A");

        // Adding fourth entry should evict the least-recently used entry ("B")
        cache.put("D", "4");

        assertTrue(cache.containsKey("A"));
        assertTrue(cache.containsKey("C"));
        assertTrue(cache.containsKey("D"));
        assertFalse(cache.containsKey("B"));
        assertEquals(3, cache.size());

        // Verify order via toString (most-recent first)
        // Expected order: D (just added), A (touched), C
        String expected = "{D=4, A=1, C=3}";
        assertEquals(expected, cache.toString());
    }

    @Test
    public void testGetMovesNodeToHeadWhenLockIsFree() {
        cache.put("A", "1");
        cache.put("B", "2");
        cache.put("C", "3"); // order: C, B, A (most recent first)

        // Access B; it should become most recent
        cache.get("B");

        // Expected order: B, C, A
        String expected = "{B=2, C=3, A=1}";
        assertEquals(expected, cache.toString());
    }

    @Test
    public void testGetDoesNotReorderWhenLockIsHeldByAnotherThread() throws Exception {
        cache.put("A", "1");
        cache.put("B", "2");
        cache.put("C", "3"); // order: C, B, A

        // Capture order before contested get
        String before = cache.toString();

        // Set up a thread that holds the internal lock
        final CountDownLatch lockAcquired = new CountDownLatch(1);
        final CountDownLatch releaseLock = new CountDownLatch(1);
        Thread locker = new Thread(() -> {
            try {
                Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
                lockField.setAccessible(true);
                java.util.concurrent.locks.Lock internalLock = (java.util.concurrent.locks.Lock) lockField.get(cache);
                internalLock.lock();
                lockAcquired.countDown(); // signal that lock is held
                // Wait until main thread finishes the get
                releaseLock.await();
                internalLock.unlock();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        locker.start();

        // Ensure the locker thread has acquired the lock
        assertTrue(lockAcquired.await(1, TimeUnit.SECONDS));

        // This get should fail to acquire the tryLock and therefore not move the node
        cache.get("A");

        // Release the lock so the locker thread can finish
        releaseLock.countDown();
        locker.join();

        // Order must be unchanged
        String after = cache.toString();
        assertEquals(before, after);
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
        assertTrue(cache.containsValue("nullKey"));
    }

    @Test
    public void testContainsValue() {
        cache.put("X", "valueX");
        cache.put("Y", "valueY");

        assertTrue(cache.containsValue("valueX"));
        assertFalse(cache.containsValue("valueZ"));
    }

    @Test
    public void testEntrySetKeySetAndValuesOrder() {
        cache.put("One", "1");
        cache.put("Two", "2");
        cache.put("Three", "3"); // order: Three, Two, One

        // Access "One" to make it most recent
        cache.get("One"); // order now: One, Three, Two

        // entrySet
        List<String> entryStrings = new ArrayList<>();
        for (Map.Entry<String, String> e : cache.entrySet()) {
            entryStrings.add(e.getKey() + "=" + e.getValue());
        }
        assertEquals(Arrays.asList("One=1", "Three=3", "Two=2"), entryStrings);

        // keySet
        List<String> keys = new ArrayList<>(cache.keySet());
        assertEquals(Arrays.asList("One", "Three", "Two"), keys);

        // values
        List<String> values = new ArrayList<>(cache.values());
        assertEquals(Arrays.asList("1", "3", "2"), values);
    }

    @Test
    public void testClearEmptiesCache() {
        cache.put("A", "1");
        cache.put("B", "2");
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals("{}", cache.toString());
    }

    @Test
    public void testEqualsAndHashCode() {
        LockingLRUCacheStrategy<String, String> cache1 = new LockingLRUCacheStrategy<>(2);
        LockingLRUCacheStrategy<String, String> cache2 = new LockingLRUCacheStrategy<>(2);

        cache1.put("K1", "V1");
        cache1.put("K2", "V2");
        cache2.put("K1", "V1");
        cache2.put("K2", "V2");

        // Access order differs, but Map equality is based on entries, not order
        assertEquals(cache1, cache2);
        assertEquals(cache1.hashCode(), cache2.hashCode());

        // Compare to a regular HashMap with same entries
        Map<String, String> map = new HashMap<>();
        map.put("K1", "V1");
        map.put("K2", "V2");
        assertEquals(map, cache1);
        assertEquals(cache1, map);
    }

    @Test
    public void testToStringSelfReferenceInKeyAndValue() {
        // Use the cache itself as a key
        cache.put("selfKey", "value");
        cache.put(null, null); // add a null entry to ensure map is not empty
        cache.put("another", "anotherValue");

        cache.put(cache.toString(), "dummy"); // just to have some other data

        // Put the cache itself as a key
        cache.put(cache.toString(), "ignored"); // ensure we have a key that is not the cache
        cache.put((String) null, "ignored"); // no effect

        // Now insert self-reference
        cache.put("selfRef", null);
        cache.put(null, null); // ensure null entries don't interfere
        cache.put(cache.toString(), cache.toString()); // not self yet

        // Direct self-reference
        cache.put("self", null);
        cache.put("self", null); // redundant but ok

        // Actually store the cache as a key
        cache.put((String) (Object) cache, "valueForSelfKey");

        // Store the cache as a value
        cache.put("keyForSelfValue", (String) (Object) cache);

        String result = cache.toString();
        assertTrue("Self reference in key should be replaced",
                result.contains("(this Collection)=valueForSelfKey"));
        assertTrue("Self reference in value should be replaced",
                result.contains("keyForSelfValue=(this Collection)"));
    }

    @Test
    public void testPutAllAddsAllEntries() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("A", "1");
        source.put("B", "2");
        source.put("C", "3");

        cache.putAll(source);
        assertEquals(3, cache.size());
        assertEquals("1", cache.get("A"));
        assertEquals("2", cache.get("B"));
        assertEquals("3", cache.get("C"));
    }

    @Test
    public void testRemoveDeletesEntryAndUpdatesOrder() {
        cache.put("X", "1");
        cache.put("Y", "2");
        cache.put("Z", "3"); // order: Z, Y, X

        // Remove Y
        String removed = cache.remove("Y");
        assertEquals("2", removed);
        assertFalse(cache.containsKey("Y"));
        assertEquals(2, cache.size());

        // Remaining order should be Z (most recent), X
        assertEquals("{Z=3, X=1}", cache.toString());
    }
}
