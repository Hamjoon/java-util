package com.cedarsoftware.util.cache;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class LockingLRUCacheStrategyTest {

    private LockingLRUCacheStrategy<Integer, String> cache;

    @Before
    public void setUp() {
        cache = new LockingLRUCacheStrategy<>(3);
    }

    @Test
    public void testGetCapacity() {
        assertEquals(3, cache.getCapacity());
    }

    @Test
    public void testPutAndGetSimple() {
        assertNull(cache.put(1, "one"));
        assertNull(cache.put(2, "two"));
        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals(2, cache.size());
    }

    @Test
    public void testPutUpdatesExistingValueAndMovesToHead() {
        cache.put(1, "one");
        cache.put(2, "two");
        // access 1 to make it most recent
        cache.get(1);
        // now put 1 again with new value, should stay most recent
        cache.put(1, "ONE");
        assertEquals("ONE", cache.get(1));
        // order should be 1,2
        assertEquals("{1=ONE, 2=two}", cache.toString());
    }

    @Test
    public void testLRUOrderAfterGets() {
        cache.put(1, "a");
        cache.put(2, "b");
        cache.put(3, "c"); // order: 3,2,1
        assertEquals("{3=c, 2=b, 1=a}", cache.toString());

        // get 2 -> should become most recent: 2,3,1
        cache.get(2);
        assertEquals("{2=b, 3=c, 1=a}", cache.toString());

        // get 1 -> order: 1,2,3
        cache.get(1);
        assertEquals("{1=a, 2=b, 3=c}", cache.toString());
    }

    @Test
    public void testEvictionWhenCapacityExceeded() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three"); // cache full, order 3,2,1
        assertEquals(3, cache.size());

        // Adding fourth entry should evict the least recently used (key 1)
        cache.put(4, "four");
        assertEquals(3, cache.size());
        assertFalse(cache.containsKey(1));
        assertTrue(cache.containsKey(4));

        // Verify order: newest first
        assertEquals("{4=four, 3=three, 2=two}", cache.toString());
    }

    @Test
    public void testEvictionRespectsRecentAccess() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three"); // order 3,2,1

        // Access key 1, making it most recent: 1,3,2
        cache.get(1);
        assertEquals("{1=one, 3=three, 2=two}", cache.toString());

        // Add another entry, should evict LRU which is key 2
        cache.put(4, "four");
        assertFalse(cache.containsKey(2));
        assertEquals("{4=four, 1=one, 3=three}", cache.toString());
    }

    @Test
    public void testRemoveExistingKey() {
        cache.put(1, "one");
        cache.put(2, "two");
        assertEquals("one", cache.remove(1));
        assertNull(cache.get(1));
        assertEquals(1, cache.size());
        assertEquals("{2=two}", cache.toString());
    }

    @Test
    public void testRemoveNonExistingKey() {
        cache.put(1, "one");
        assertNull(cache.remove(99));
        assertEquals(1, cache.size());
    }

    @Test
    public void testClear() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals("{}", cache.toString());
    }

    @Test
    public void testContainsKeyAndValueIncludingNulls() {
        cache.put(null, null);
        cache.put(1, null);
        cache.put(null, "valueForNullKey");
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsKey(1));
        assertTrue(cache.containsValue(null));
        assertTrue(cache.containsValue("valueForNullKey"));
        assertFalse(cache.containsValue("missing"));
    }

    @Test
    public void testEntrySetKeySetValuesOrder() {
        cache.put(1, "a");
        cache.put(2, "b");
        cache.put(3, "c"); // order 3,2,1

        // entrySet order
        List<Map.Entry<Integer, String>> entries = new ArrayList<>(cache.entrySet());
        assertEquals(Integer.valueOf(3), entries.get(0).getKey());
        assertEquals("c", entries.get(0).getValue());
        assertEquals(Integer.valueOf(2), entries.get(1).getKey());
        assertEquals("b", entries.get(1).getValue());

        // keySet order
        List<Integer> keys = new ArrayList<>(cache.keySet());
        assertEquals(Arrays.asList(3, 2, 1), keys);

        // values order
        List<String> values = new ArrayList<>(cache.values());
        assertEquals(Arrays.asList("c", "b", "a"), values);
    }

    @Test
    public void testEqualsAndHashCode() {
        cache.put(1, "one");
        cache.put(2, "two");

        Map<Integer, String> other = new HashMap<>();
        other.put(1, "one");
        other.put(2, "two");

        assertTrue(cache.equals(other));
        assertEquals(other.hashCode(), cache.hashCode());

        // Different mapping should not be equal
        other.put(3, "three");
        assertFalse(cache.equals(other));
    }

    @Test
    public void testPutAll() {
        Map<Integer, String> source = new LinkedHashMap<>();
        source.put(10, "ten");
        source.put(20, "twenty");
        source.put(30, "thirty");

        cache.putAll(source);
        assertEquals(3, cache.size());
        assertEquals("{30=thirty, 20=twenty, 10=ten}", cache.toString());
    }

    @Test
    public void testToStringSelfReference() {
        // key is the cache itself
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, cache); // self reference as value
        // also insert cache as key
        cache.put(cache, "selfKey");

        String representation = cache.toString();

        // The self‑reference should be replaced by the placeholder
        assertTrue(representation.contains("3=(this Collection)"));
        assertTrue(representation.contains("(this Collection)=selfKey"));
    }

    @Test
    public void testHashCodeConsistencyAfterMutations() {
        cache.put(1, "a");
        cache.put(2, "b");
        int hash1 = cache.hashCode();

        cache.put(3, "c");
        int hash2 = cache.hashCode();
        assertNotEquals(hash1, hash2);

        cache.remove(2);
        int hash3 = cache.hashCode();
        assertNotEquals(hash2, hash3);
    }

    @Test
    public void testIsEmptyAndSize() {
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        cache.put(1, "one");
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());
    }

    @Test
    public void testGetReturnsNullForMissingKey() {
        assertNull(cache.get("missing"));
    }

    @Test
    public void testConcurrentAccessDoesNotThrow() throws InterruptedException {
        // Simple sanity check: multiple threads performing gets/puts should not throw.
        final LockingLRUCacheStrategy<Integer, Integer> concurrentCache = new LockingLRUCacheStrategy<>(5);
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                concurrentCache.put(i, i);
            }
        });

        Thread reader = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                concurrentCache.get(i);
            }
        });

        writer.start();
        reader.start();
        writer.join();
        reader.join();

        // After execution, size must not exceed capacity
        assertTrue(concurrentCache.size() <= 5);
    }
}
