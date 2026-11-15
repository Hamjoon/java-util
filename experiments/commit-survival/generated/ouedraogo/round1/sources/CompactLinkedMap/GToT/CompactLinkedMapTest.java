package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class CompactLinkedMapTest {

    /** Helper subclass to expose protected members of CompactLinkedMap */
    private static class TestableCompactLinkedMap<K, V> extends CompactLinkedMap<K, V> {
        public TestableCompactLinkedMap() { super(); }
        public TestableCompactLinkedMap(Map<K, V> m) { super(m); }
        public int getCompactSize() { return super.compactSize(); }
    }

    private CompactLinkedMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactLinkedMap<>();
    }

    /* ----------- Basic sanity tests (one per public method) ----------- */

    @Test
    public void testDefaultConstructor_isEmpty() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructorWithMap_copiesEntries() {
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        CompactLinkedMap<String, Integer> m = new CompactLinkedMap<>(src);
        assertEquals(2, m.size());
        assertEquals(Integer.valueOf(1), m.get("a"));
        assertEquals(Integer.valueOf(2), m.get("b"));
    }

    @Test
    public void testPutAndGet() {
        map.put("x", 10);
        assertEquals(Integer.valueOf(10), map.get("x"));
    }

    @Test
    public void testPutOverwrite_returnsOldValue() {
        map.put("k", 1);
        Integer old = map.put("k", 2);
        assertEquals(Integer.valueOf(1), old);
        assertEquals(Integer.valueOf(2), map.get("k"));
    }

    @Test
    public void testRemove_existingAndMissing() {
        map.put("p", 5);
        assertEquals(Integer.valueOf(5), map.remove("p"));
        assertNull(map.remove("p")); // removing again returns null
    }

    @Test
    public void testContainsKeyAndValue() {
        map.put("z", 99);
        assertTrue(map.containsKey("z"));
        assertTrue(map.containsValue(99));
        assertFalse(map.containsKey("missing"));
        assertFalse(map.containsValue(0));
    }

    @Test
    public void testIsEmptyAndSize() {
        assertTrue(map.isEmpty());
        map.put("a", 1);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> src = new HashMap<>();
        src.put("c", 3);
        src.put("d", 4);
        map.putAll(src);
        assertEquals(Integer.valueOf(3), map.get("c"));
        assertEquals(Integer.valueOf(4), map.get("d"));
    }

    @Test
    public void testClear() {
        map.put("x", 1);
        map.put("y", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testKeySetValuesEntrySet() {
        map.put("k1", 10);
        map.put("k2", 20);
        assertEquals(2, map.keySet().size());
        assertEquals(2, map.values().size());
        assertEquals(2, map.entrySet().size());
    }

    @Test
    public void testMinus_removesKey() {
        map.put("a", 1);
        map.put("b", 2);
        Map<String, Integer> result = map.minus("a");
        assertFalse(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        // original map must stay unchanged
        assertTrue(map.containsKey("a"));
    }

    @Test
    public void testPlus_addsEntry() {
        map.put("a", 1);
        Map<String, Integer> result = map.plus("b");
        // The exact semantics of plus(Object) in CompactMap add a key with a null value.
        assertTrue(result.containsKey("b"));
        assertTrue(map.containsKey("a"));
        assertFalse(map.containsKey("b"));
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactLinkedMap<String, Integer> m1 = new CompactLinkedMap<>();
        CompactLinkedMap<String, Integer> m2 = new CompactLinkedMap<>();
        m1.put("x", 1);
        m2.put("x", 1);
        assertTrue(m1.equals(m2));
        assertEquals(m1.hashCode(), m2.hashCode());
        assertFalse(m1.equals("not a map"));
    }

    @Test
    public void testToString_containsEntries() {
        map.put("a", 1);
        String s = map.toString();
        assertTrue(s.contains("a"));
        assertTrue(s.contains("1"));
    }

    /* -------------------- Edge‑case & Exception Tests -------------------- */

    @Test(expected = NullPointerException.class)
    public void testConstructor_nullMap_NPE() {
        new CompactLinkedMap<>(null);
    }

    @Test
    public void testPut_nullKeyAndValue() {
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));
    }

    @Test(expected = NullPointerException.class)
    public void testPutAll_nullArgument_NPE() {
        map.putAll(null);
    }

    @Test
    public void testOrderPreservedWhenSizeExceedsCompactSize() {
        TestableCompactLinkedMap<Integer, String> m = new TestableCompactLinkedMap<>();
        int threshold = m.getCompactSize();

        // Fill up to the compact size
        for (int i = 0; i < threshold; i++) {
            m.put(i, "v" + i);
        }
        // One more entry forces the internal delegate to become a LinkedHashMap
        m.put(threshold, "last");

        // Verify that iteration order matches insertion order
        List<Integer> iterated = new ArrayList<>();
        for (Map.Entry<Integer, String> e : m.entrySet()) {
            iterated.add(e.getKey());
        }
        for (int i = 0; i <= threshold; i++) {
            assertEquals(Integer.valueOf(i), iterated.get(i));
        }
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModificationDuringIteration() {
        map.put(1, "a");
        map.put(2, "b");
        Iterator<Map.Entry<Integer, String>> it = ((CompactLinkedMap<Integer, String>) map).entrySet().iterator();
        map.put(3, "c"); // structural change
        it.next(); // should trigger CME
    }

    @Test
    public void testLargeNumberOfEntries() {
        CompactLinkedMap<Integer, Integer> big = new CompactLinkedMap<>();
        int count = 10_000;
        for (int i = 0; i < count; i++) {
            big.put(i, i);
        }
        assertEquals(count, big.size());
        assertEquals(Integer.valueOf(0), big.get(0));
        assertEquals(Integer.valueOf(count - 1), big.get(count - 1));
    }

    @Test
    public void testMinus_withNullKey() {
        map.put("a", 1);
        Map<String, Integer> result = map.minus(null);
        // Removing a null key that does not exist should leave map unchanged
        assertEquals(1, result.size());
        assertTrue(result.containsKey("a"));
    }

    @Test
    public void testPlus_withExistingKey() {
        map.put("x", 1);
        Map<String, Integer> result = map.plus("x"); // adds same key again (value will be null)
        // result should contain "x" (it already did) and still be a map
        assertTrue(result.containsKey("x"));
        // Original map unchanged
        assertEquals(Integer.valueOf(1), map.get("x"));
    }

    @Test
    public void testContainsKey_nullWhenNotPresent() {
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testContainsValue_nullWhenNotPresent() {
        assertFalse(map.containsValue(null));
    }
}
