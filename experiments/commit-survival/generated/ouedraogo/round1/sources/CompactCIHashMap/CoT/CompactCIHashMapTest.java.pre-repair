package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

public class CompactCIHashMapTest {

    /** -----------------------------------------------------------------
     *  LIST OF PUBLIC METHODS (including inherited from CompactMap)
     *  -----------------------------------------------------------------
     *  Constructors
     *      CompactCIHashMap()
     *      CompactCIHashMap(Map<K,V> other)
     *
     *  Map / CompactMap public API
     *      int size()
     *      boolean isEmpty()
     *      boolean containsKey(Object key)
     *      boolean containsValue(Object value)
     *      V get(Object key)
     *      V put(K key, V value)
     *      V remove(Object key)
     *      void putAll(Map<? extends K, ? extends V> m)
     *      void clear()
     *      int hashCode()
     *      boolean equals(Object o)
     *      String toString()
     *      Set<K> keySet()
     *      Collection<V> values()
     *      Set<Map.Entry<K,V>> entrySet()
     *      Map<K,V> minus(Object removeMe)
     *      Map<K,V> plus(Object right)
     * ----------------------------------------------------------------- */

    private CompactCIHashMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactCIHashMap<>();
    }

    /** --------------------------- BASIC FUNCTIONALITY --------------------------- */

    @Test
    public void testConstructorEmpty() {
        CompactCIHashMap<String, Integer> empty = new CompactCIHashMap<>();
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
    }

    @Test
    public void testConstructorWithMap() {
        Map<String, Integer> src = new HashMap<>();
        src.put("One", 1);
        src.put("Two", 2);
        CompactCIHashMap<String, Integer> fromMap = new CompactCIHashMap<>(src);
        assertEquals(2, fromMap.size());
        assertTrue(fromMap.containsKey("One"));
        assertTrue(fromMap.containsKey("Two"));
    }

    @Test
    public void testPutAndGet_CaseInsensitive() {
        map.put("Key", 10);
        assertEquals(Integer.valueOf(10), map.get("Key"));
        // different case should retrieve same value
        assertEquals(Integer.valueOf(10), map.get("kEy"));
        // containsKey must be case‑insensitive
        assertTrue(map.containsKey("KEY"));
    }

    @Test
    public void testPutOverwriteDifferentCase() {
        map.put("Key", 10);
        map.put("kEy", 20);   // should overwrite the previous entry
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(20), map.get("KEY"));
    }

    @Test
    public void testKeySetRetainsOriginalCase() {
        map.put("FirstKey", 1);
        map.put("secondkey", 2);
        map.put("FIRSTKEY", 3); // overwrite first entry with different case
        Set<String> keys = map.keySet();
        assertEquals(2, keys.size());
        // The key that survived should keep the case of the *first* insertion
        assertTrue(keys.contains("FirstKey"));
        assertTrue(keys.contains("secondkey"));
        assertFalse(keys.contains("FIRSTKEY"));
    }

    @Test
    public void testContainsKey_NonStringKey() {
        CompactCIHashMap<Integer, String> intKeyMap = new CompactCIHashMap<>();
        intKeyMap.put(100, "hundred");
        assertTrue(intKeyMap.containsKey(100));
        assertFalse(intKeyMap.containsKey(101));
    }

    @Test
    public void testRemove_CaseInsensitive() {
        map.put("RemoveMe", 99);
        Integer removed = map.remove("removeme");
        assertEquals(Integer.valueOf(99), removed);
        assertFalse(map.containsKey("REMOVEme"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testPutAll_MixedCases() {
        Map<String, Integer> src = new HashMap<>();
        src.put("Alpha", 1);
        src.put("beta", 2);
        src.put("ALPHA", 3); // should overwrite Alpha
        map.putAll(src);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("alpha"));
        assertEquals(Integer.valueOf(2), map.get("BETA"));
    }

    @Test
    public void testClear() {
        map.put("A", 1);
        map.put("B", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testEqualsAndHashCode() {
        map.put("One", 1);
        map.put("Two", 2);
        CompactCIHashMap<String, Integer> other = new CompactCIHashMap<>();
        other.put("ONE", 1);
        other.put("two", 2);
        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("X", 10);
        map.put("Y", 20);
        String s = map.toString();
        assertTrue(s.contains("X=10"));
        assertTrue(s.contains("Y=20"));
    }

    @Test
    public void testValuesCollection() {
        map.put("A", 5);
        map.put("B", 6);
        Collection<Integer> vals = map.values();
        assertEquals(2, vals.size());
        assertTrue(vals.containsAll(Arrays.asList(5, 6)));
    }

    @Test
    public void testEntrySetIteration() {
        map.put("One", 1);
        map.put("Two", 2);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        Map<String, Integer> seen = new HashMap<>();
        for (Map.Entry<String, Integer> e : entries) {
            seen.put(e.getKey(), e.getValue());
        }
        assertEquals(2, seen.size());
        assertEquals(Integer.valueOf(1), seen.get("One"));
        assertEquals(Integer.valueOf(2), seen.get("Two"));
    }

    /** --------------------------- EDGE CASES & EXCEPTIONS --------------------------- */

    @Test
    public void testNullKeyBehaviour() {
        map.put(null, 42);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(42), map.get(null));
        map.remove(null);
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testNullValueBehaviour() {
        map.put("NullVal", null);
        assertTrue(map.containsKey("nullval"));
        assertNull(map.get("NULLVAL"));
        assertTrue(map.containsValue(null));
    }

    @Test
    public void testMinusRemovesKeyCaseInsensitively() {
        map.put("Keep", 1);
        map.put("DeleteMe", 2);
        Map<String, Integer> reduced = map.minus("deleteme");
        assertEquals(1, reduced.size());
        assertTrue(reduced.containsKey("Keep"));
        assertFalse(reduced.containsKey("DeleteMe"));
        // original map must stay unchanged
        assertEquals(2, map.size());
    }

    @Test
    public void testPlusAddsEntriesFromAnotherMap() {
        map.put("A", 1);
        Map<String, Integer> extra = new HashMap<>();
        extra.put("B", 2);
        extra.put("a", 3); // same key (different case) should overwrite "A"
        Map<String, Integer> merged = map.plus(extra);
        // merged map should have 2 entries, "A" overwritten by "a"
        assertEquals(2, merged.size());
        assertEquals(Integer.valueOf(3), merged.get("A"));
        assertEquals(Integer.valueOf(2), merged.get("B"));
        // original map unchanged
        assertEquals(Integer.valueOf(1), map.get("A"));
        assertFalse(map.containsKey("B"));
    }

    @Test
    public void testLargeNumberOfEntriesTriggersDelegateSwitch() {
        // CompactMap switches to a HashMap after compactSize() elements.
        // We don't know the exact compactSize, but inserting many entries
        // should not break normal operation.
        int many = 200; // far beyond any reasonable compactSize
        for (int i = 0; i < many; i++) {
            map.put("key" + i, i);
        }
        assertEquals(many, map.size());
        // Spot‑check a few entries
        assertEquals(Integer.valueOf(0), map.get("KEY0"));
        assertEquals(Integer.valueOf(199), map.get("KeY199"));
    }

    /** --------------------------- PROTECTED METHOD VERIFICATION --------------------------- */
    @Test
    public void testIsCaseInsensitiveProtectedMethod() throws Exception {
        Method m = CompactCIHashMap.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean result = (Boolean) m.invoke(map);
        assertTrue(result);
    }

    @Test
    public void testUseCopyIteratorProtectedMethod() throws Exception {
        Method m = CompactCIHashMap.class.getDeclaredMethod("useCopyIterator");
        m.setAccessible(true);
        boolean result = (Boolean) m.invoke(map);
        assertFalse(result);
    }

    @Test
    public void testGetNewMapCreatesCaseInsensitiveMap() throws Exception {
        Method m = CompactCIHashMap.class.getDeclaredMethod("getNewMap");
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Integer> newMap = (Map<String, Integer>) m.invoke(map);
        // Verify that the returned map behaves case‑insensitively
        newMap.put("TestKey", 5);
        assertEquals(Integer.valueOf(5), newMap.get("testkey"));
    }
}
