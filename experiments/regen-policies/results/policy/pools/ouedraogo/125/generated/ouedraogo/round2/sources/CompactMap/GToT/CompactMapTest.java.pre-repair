package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class CompactMapTest {

    private CompactMap<String, Integer> emptyMap;

    @Before
    public void setUp() {
        emptyMap = new CompactMap<>();
    }

    /*--------------------  Constructors  --------------------*/

    @Test
    public void testEmptyConstructor() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructorWithMap() {
        Map<String, Integer> src = new HashMap<>();
        src.put("alpha", 10);
        src.put("beta", 20);
        CompactMap<String, Integer> map = new CompactMap<>(src);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(10), map.get("alpha"));
        assertEquals(Integer.valueOf(20), map.get("beta"));
    }

    /*--------------------  Size / Empty  --------------------*/

    @Test
    public void testSizeTransitionsWithCompactSize() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .compactSize(3)   // small size to trigger transition
                .build();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("k1", 1);
        assertEquals(1, map.size());

        map.put("k2", 2);
        assertEquals(2, map.size());

        map.put("k3", 3);
        assertEquals(3, map.size());

        // Adding fourth entry forces transition to backing map
        map.put("k4", 4);
        assertEquals(4, map.size());
    }

    /*--------------------  containsKey / containsValue  --------------------*/

    @Test
    public void testContainsKeyAndValue() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("A", 100);
        assertTrue(map.containsKey("A"));
        assertFalse(map.containsKey("B"));
        assertTrue(map.containsValue(100));
        assertFalse(map.containsValue(200));
    }

    /*--------------------  get  --------------------*/

    @Test
    public void testGetExistingAndMissingKeys() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("present", 42);
        map.put("nullValue", null);

        assertEquals(Integer.valueOf(42), map.get("present"));
        assertNull(map.get("missing"));
        assertNull(map.get("nullValue"));
    }

    /*--------------------  put (overwrite)  --------------------*/

    @Test
    public void testPutOverwriteReturnsOldValue() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("key", 1);
        Integer old = map.put("key", 2);
        assertEquals(Integer.valueOf(1), old);
        assertEquals(Integer.valueOf(2), map.get("key"));
    }

    /*--------------------  remove  --------------------*/

    @Test
    public void testRemoveExistingKey() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 10);
        map.put("b", 20);
        Integer removed = map.remove("a");
        assertEquals(Integer.valueOf(10), removed);
        assertNull(map.get("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testRemoveNonExistingKeyReturnsNull() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 1);
        assertNull(map.remove("y"));
        assertEquals(1, map.size());
    }

    /*--------------------  clear  --------------------*/

    @Test
    public void testClearEmptiesMap() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /*--------------------  hashCode / equals  --------------------*/

    @Test
    public void testEqualsAndHashCodeConsistency() {
        Map<String, Integer> std = new HashMap<>();
        std.put("a", 1);
        std.put("b", 2);
        CompactMap<String, Integer> cm = new CompactMap<>(std);
        assertEquals(std, cm);
        assertEquals(std.hashCode(), cm.hashCode());
    }

    @Test
    public void testEqualsWithDifferentMapTypes() {
        CompactMap<String, Integer> map1 = new CompactMap<>();
        map1.put("x", 5);

        CompactMap<String, Integer> map2 = CompactMap.builder()
                .mapType(LinkedHashMap.class)
                .build();
        map2.put("x", 5);

        assertTrue(map1.equals(map2));
        assertTrue(map2.equals(map1));
    }

    /*--------------------  toString  --------------------*/

    @Test
    public void testToStringFormat() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("first", 1);
        map.put("second", 2);
        String s = map.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("first=1"));
        assertTrue(s.contains("second=2"));
    }

    /*--------------------  Builder Options  --------------------*/

    @Test
    public void testCaseInsensitiveKeys() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .caseSensitive(false)
                .build();

        map.put("Key", 123);
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsKey("KEY"));
        assertEquals(Integer.valueOf(123), map.get("kEy"));
    }

    @Test
    public void testSortedOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .sortedOrder()
                .compactSize(2)   // force array -> map after 3 entries
                .build();

        map.put("b", 2);
        map.put("c", 3);
        map.put("a", 1);

        Iterator<String> it = map.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
    }

    @Test
    public void testReverseOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .reverseOrder()
                .compactSize(2)
                .build();

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Iterator<String> it = map.keySet().iterator();
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
    }

    @Test
    public void testInsertionOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .insertionOrder()
                .compactSize(2)
                .build();

        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        Iterator<String> it = map.keySet().iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
    }

    @Test
    public void testSourceMapInitialisation() {
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("a", 10);
        src.put("b", 20);

        CompactMap<String, Integer> map = CompactMap.builder()
                .sourceMap(src)
                .insertionOrder()
                .build();

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(10), map.get("a"));
        assertEquals(Integer.valueOf(20), map.get("b"));
    }

    @Test
    public void testBuilderCustomMapTypeLinkedHashMapPreservesInsertionOrder() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .mapType(LinkedHashMap.class)
                .insertionOrder()
                .compactSize(2)
                .build();

        for (int i = 1; i <= 5; i++) {
            map.put("k" + i, i);
        }

        Iterator<String> it = map.keySet().iterator();
        for (int i = 1; i <= 5; i++) {
            assertEquals("k" + i, it.next());
        }
    }

    @Test
    public void testBuilderCustomMapTypeTreeMapSortedOrder() {
        CompactMap<Integer, String> map = CompactMap.builder()
                .mapType(TreeMap.class)
                .sortedOrder()
                .compactSize(2)
                .build();

        map.put(5, "five");
        map.put(2, "two");
        map.put(8, "eight");

        Iterator<Integer> it = map.keySet().iterator();
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(5), it.next());
        assertEquals(Integer.valueOf(8), it.next());
    }

    /*--------------------  Deprecated Operations  --------------------*/

    @Test(expected = UnsupportedOperationException.class)
    public void testMinusUnsupportedOperation() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.minus("anything");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlusUnsupportedOperation() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.plus("anything");
    }

    /*--------------------  Static newMap / validateAndFinalizeOptions  --------------------*/

    @Test(expected = IllegalArgumentException.class)
    public void testValidateAndFinalizeOptionsCompactSizeTooSmall() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(CompactMap.COMPACT_SIZE, 1);   // invalid, must be >= 2
        CompactMap.validateAndFinalizeOptions(opts);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateAndFinalizeOptionsUnsupportedMapType() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(CompactMap.MAP_TYPE, IdentityHashMap.class); // not supported
        CompactMap.validateAndFinalizeOptions(opts);
    }

    @Test
    public void testNewMapWithValidOptions() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(CompactMap.COMPACT_SIZE, 5);
        opts.put(CompactMap.CASE_SENSITIVE, true);
        CompactMap<String, Integer> map = CompactMap.newMap(opts);
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    /*--------------------  Iterator Behaviour  --------------------*/

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorDetectsConcurrentModification() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        map.put("c", 3);   // structural change after iterator creation
        it.next();        // should throw CME
    }

    @Test
    public void testIteratorRemoveWorks() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            if ("a".equals(k)) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetContainsAndRemove() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 100);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();

        assertTrue(map.entrySet().contains(entry));

        boolean removed = map.entrySet().remove(entry);
        assertTrue(removed);
        assertFalse(map.containsKey("x"));
    }

    @Test
    public void testKeySetRemoveReturnsCorrectFlag() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        boolean changed = map.keySet().remove("one");
        assertTrue(changed);
        assertFalse(map.containsKey("one"));
        // removing non‑existent key should return false
        assertFalse(map.keySet().remove("missing"));
    }

    @Test
    public void testValuesCollectionSizeAndClear() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Collection<Integer> values = map.values();
        assertEquals(2, values.size());

        map.clear();
        assertEquals(0, values.size());
    }

    /*--------------------  Single‑Value Optimization (default key "id")  --------------------*/

    @Test
    public void testSingleValueKeyOptimization() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("id", 999);
        assertEquals(Integer.valueOf(999), map.get("id"));
        assertEquals(1, map.size());

        // Overwrite using the same single‑value key
        Integer old = map.put("id", 555);
        assertEquals(Integer.valueOf(999), old);
        assertEquals(Integer.valueOf(555), map.get("id"));
        assertEquals(1, map.size());
    }

    @Test
    public void testPutNullValueWithSingleValueKey() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("id", null);
        assertTrue(map.containsKey("id"));
        assertNull(map.get("id"));
        assertEquals(1, map.size());
    }

    /*--------------------  Builder mapType Validation  --------------------*/

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderMapTypeValidationRejectsWeakHashMap() {
        CompactMap.builder()
                .mapType(WeakHashMap.class)   // not allowed
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderMapTypeValidationRejectsIdentityHashMap() {
        CompactMap.builder()
                .mapType(IdentityHashMap.class)   // not allowed
                .build();
    }

    /*--------------------  Builder mapType Null Argument  --------------------*/

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderMapTypeNullArgument() {
        CompactMap.builder()
                .mapType(null);
    }
}
