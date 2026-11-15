package com.cedarsoftware.util.cache;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class LockingLRUCacheStrategyTest {

    private LockingLRUCacheStrategy<Integer, String> cache;

    @Before
    public void setUp() {
        cache = new LockingLRUCacheStrategy<>(3);
    }

    /*** 1. Constructor ***/
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeCapacity() {
        new LockingLRUCacheStrategy<>(-1);
    }

    /*** 2. put() – basic insertion ***/
    @Test
    public void testPutAndGetBasic() {
        assertNull(cache.put(1, "one"));
        assertEquals("one", cache.get(1));
        assertEquals(1, cache.size());
    }

    /*** 3. put() – overwrite existing key returns new value ***/
    @Test
    public void testPutOverwriteReturnsNewValue() {
        cache.put(2, "two");
        String returned = cache.put(2, "TWO");
        assertEquals("TWO", returned);
        assertEquals("TWO", cache.get(2));
        assertEquals(1, cache.size());
    }

    /*** 4. Capacity eviction (LRU order) ***/
    @Test
    public void testCapacityEviction() {
        cache.put(1, "one"); // LRU
        cache.put(2, "two");
        cache.put(3, "three"); // cache full (capacity = 3)
        // access 1 to make it most recent
        assertEquals("one", cache.get(1));
        // add fourth entry, should evict the least recently used (which is 2)
        cache.put(4, "four");
        assertFalse(cache.containsKey(2));
        assertTrue(cache.containsKey(1));
        assertTrue(cache.containsKey(3));
        assertTrue(cache.containsKey(4));
        assertEquals(3, cache.size());
    }

    /*** 5. put() – null key and null value ***/
    @Test
    public void testPutNullKeyAndValue() {
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
    }

    /*** 6. containsValue(null) after explicit null value ***/
    @Test
    public void testContainsValueNull() {
        cache.put(10, null);
        assertTrue(cache.containsValue(null));
    }

    /*** 7. remove() ***/
    @Test
    public void testRemove() {
        cache.put(5, "five");
        String removed = cache.remove(5);
        assertEquals("five", removed);
        assertNull(cache.get(5));
        assertEquals(0, cache.size());
    }

    /*** 8. clear() ***/
    @Test
    public void testClear() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    /*** 9. isEmpty() and size() ***/
    @Test
    public void testIsEmptyAndSize() {
        assertTrue(cache.isEmpty());
        cache.put(1, "one");
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());
    }

    /*** 10. containsKey() ***/
    @Test
    public void testContainsKey() {
        cache.put(7, "seven");
        assertTrue(cache.containsKey(7));
        assertFalse(cache.containsKey(8));
    }

    /*** 11. putAll() basic ***/
    @Test
    public void testPutAll() {
        Map<Integer, String> map = new HashMap<>();
        map.put(11, "eleven");
        map.put(12, "twelve");
        cache.putAll(map);
        assertEquals("eleven", cache.get(11));
        assertEquals("twelve", cache.get(12));
        assertEquals(2, cache.size());
    }

    /*** 12. putAll(null) throws NPE ***/
    @Test(expected = NullPointerException.class)
    public void testPutAllNullMap() {
        cache.putAll(null);
    }

    /*** 13. entrySet order (most‑recent first) ***/
    @Test
    public void testEntrySetOrder() {
        cache.put(1, "one");   // LRU
        cache.put(2, "two");
        cache.get(1);          // makes 1 most recent
        cache.put(3, "three"); // now order: 3 (most recent), 1, 2
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        Map.Entry<Integer, String> first = it.next();
        Map.Entry<Integer, String> second = it.next();
        Map.Entry<Integer, String> third = it.next();

        assertEquals(Integer.valueOf(3), first.getKey());
        assertEquals(Integer.valueOf(1), second.getKey());
        assertEquals(Integer.valueOf(2), third.getKey());
    }

    /*** 14. keySet order ***/
    @Test
    public void testKeySetOrder() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.get(1); // 1 becomes most recent
        cache.put(3, "three");
        List<Integer> keys = new ArrayList<>(cache.keySet());
        assertEquals(Arrays.asList(3, 1, 2), keys);
    }

    /*** 15. values order ***/
    @Test
    public void testValuesOrder() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.get(1); // 1 most recent
        cache.put(3, "three");
        List<String> values = new ArrayList<>(cache.values());
        assertEquals(Arrays.asList("three", "one", "two"), values);
    }

    /*** 16. equals() with another map ***/
    @Test
    public void testEqualsWithSameEntries() {
        cache.put(1, "one");
        cache.put(2, "two");

        Map<Integer, String> other = new LinkedHashMap<>();
        other.put(1, "one");
        other.put(2, "two");

        assertTrue(cache.equals(other));
        assertTrue(other.equals(cache)); // Map's equals should also be true
    }

    /*** 17. equals() with different object ***/
    @Test
    public void testEqualsWithNonMap() {
        assertFalse(cache.equals("not a map"));
    }

    /*** 18. hashCode consistency with equals ***/
    @Test
    public void testHashCodeConsistency() {
        cache.put(1, "one");
        cache.put(2, "two");

        Map<Integer, String> other = new LinkedHashMap<>();
        other.put(1, "one");
        other.put(2, "two");

        assertEquals(other.hashCode(), cache.hashCode());
    }

    /*** 19. toString formatting – self‑reference placeholder ***/
    @Test
    public void testToStringSelfReference() {
        // Use the cache itself as a key and as a value in separate entries
        cache.put(1, "value1");
        cache.put(null, "nullKey");
        cache.put(cache, "cacheValue");
        cache.put(2, cache); // value is the cache itself

        String s = cache.toString();

        // Expected fragments
        assertTrue(s.contains("(this Collection)=cacheValue") || s.contains("null=cacheValue"));
        assertTrue(s.contains("2=(this Collection)"));
        // Ensure surrounding braces
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }

    /*** 20. toString basic format ***/
    @Test
    public void testToStringBasic() {
        cache.put(1, "one");
        cache.put(2, "two");
        String s = cache.toString();
        // Order should be most‑recent first (2 then 1)
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("2=two"));
        assertTrue(s.contains("1=one"));
        // The comma/space separator should appear between entries
        assertTrue(s.contains(", "));
    }
}
