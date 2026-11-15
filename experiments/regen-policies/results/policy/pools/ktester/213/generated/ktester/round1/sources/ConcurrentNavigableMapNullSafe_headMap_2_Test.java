package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_headMap_2_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate with a mix of normal and null keys
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put(null, 100);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; the map itself does not hold external resources
        map = null;
    }

    @Test
    void headMap_exclusiveNonNullKey() {
        // headMap should be exclusive of the supplied key
        NavigableMap<String, Integer> head = map.headMap("b");
        // Only keys strictly less than "b" (according to natural ordering) should be present
        assertEquals(1, head.size(), "Expected only one entry in the headMap");
        assertTrue(head.containsKey("a"));
        assertFalse(head.containsKey("b"));
        assertFalse(head.containsKey("c"));
        assertFalse(head.containsKey(null));
        // Verify that the view reflects the original map's values
        assertEquals(1, head.get("a"));
    }

    @Test
    void headMap_exclusiveKeyAtFirstElement() {
        // When the toKey is the smallest key, the resulting headMap should be empty
        NavigableMap<String, Integer> head = map.headMap("a");
        assertTrue(head.isEmpty(), "headMap of the smallest key should be empty");
    }

    @Test
    void headMap_nullKeyIsConsideredLast() {
        // Null is treated as the greatest key; headMap(null) should contain all non‑null entries
        NavigableMap<String, Integer> head = map.headMap(null);
        assertEquals(3, head.size(), "headMap(null) should contain all non‑null keys");
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertTrue(head.containsKey("c"));
        assertFalse(head.containsKey(null));
        // Ensure values are correct
        assertEquals(1, head.get("a"));
        assertEquals(2, head.get("b"));
        assertEquals(3, head.get("c"));
    }

    @Test
    void headMap_viewIsLive_ModificationsReflectInOriginal() {
        // Obtain a view that includes keys less than "c"
        NavigableMap<String, Integer> head = map.headMap("c");
        // Add a new entry via the view; it should appear in the original map
        head.put("b2", 20);
        assertEquals(20, map.get("b2"));
        assertTrue(head.containsKey("b2"));
        // Remove an existing entry via the view; it should disappear from the original map
        head.remove("a");
        assertNull(map.get("a"));
        assertFalse(head.containsKey("a"));
    }

    @Test
    void headMap_immutableWhenOriginalIsModified() {
        // Obtain a view before mutating the original map
        NavigableMap<String, Integer> head = map.headMap("c");
        // Add a key that is still within the view's range
        map.put("b0", 0);
        assertTrue(head.containsKey("b0"));
        assertEquals(0, head.get("b0"));
        // Add a key that is outside the view's range; it must not appear in the view
        map.put("d", 4);
        assertFalse(head.containsKey("d"));
    }

    /**
     * Parameterized test covering the three basic behaviours:
     * <ul>
     *   <li>toKey exists in the map</li>
     *   <li>toKey lies between existing keys</li>
     *   <li>toKey greater than the greatest key</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] toKey={0} expectsKeys={1}")
    @CsvSource({ // toKey exists – exclusive, so key 2 is omitted
    "2, a", // toKey does not exist – includes keys < 2.5 (i.e., 1 and 2)
    "2.5, a|b", // toKey greater than any key – whole map (excluding null)
    "5, a|b|c" })
    void headMap_basicBehavior(String toKeyStr, String expectedKeysCsv) {
        // Build a map with Integer keys for this test
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        // Convert the string representation of toKey to the proper type
        Number toKeyNumber = toKeyStr.contains(".") ? Double.valueOf(toKeyStr) : Integer.valueOf(toKeyStr);
        // The map expects Integer keys, so we cast to Integer when possible
        Integer toKey = (toKeyNumber instanceof Integer) ? (Integer) toKeyNumber : null;
        // For the 2.5 case we cannot cast to Integer; headMap will treat it as a Double
        // which should cause a ClassCastException – however the JSON expects a view.
        // To stay faithful to the contract we use Double as the key type for that case.
        ConcurrentNavigableMap<Integer, String> view;
        if (toKey != null) {
            view = intMap.headMap(toKey);
        } else {
            // Use a map with Double keys for the 2.5 scenario
            ConcurrentNavigableMapNullSafe<Double, String> doubleMap = new ConcurrentNavigableMapNullSafe<>();
            doubleMap.put(1.0, "one");
            doubleMap.put(2.0, "two");
            doubleMap.put(3.0, "three");
            view = doubleMap.headMap(Double.valueOf(toKeyStr));
        }
        // Build expected key set
        Set<Integer> expected = new LinkedHashSet<>();
        if (!expectedKeysCsv.isEmpty()) {
            for (String k : expectedKeysCsv.split("\\|")) {
                expected.add(Integer.valueOf(k));
            }
        }
        // Verify size and content
        assertEquals(expected.size(), view.size(), "headMap size does not match expectation for toKey=" + toKeyStr);
        assertEquals(expected, view.keySet(), "headMap keys do not match expectation for toKey=" + toKeyStr);
    }

    @Test
    void headMap_toKeySmallerThanSmallest_returnsEmpty() {
        // Map with Integer keys 1,2,3
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        NavigableMap<Integer, String> head = intMap.headMap(-1);
        assertTrue(head.isEmpty(), "headMap with toKey smaller than any entry should be empty");
    }

    @Test
    void headMap_onEmptyMap_returnsEmptyView() {
        ConcurrentNavigableMapNullSafe<Integer, String> emptyMap = new ConcurrentNavigableMapNullSafe<>();
        NavigableMap<Integer, String> head = emptyMap.headMap(10);
        assertTrue(head.isEmpty(), "headMap on an empty map must be empty regardless of toKey");
    }

    @Test
    void headMap_viewMutability_removeFromViewReflectsInOriginal() {
        // Map with Integer keys
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        NavigableMap<Integer, String> view = intMap.headMap(3);
        // Remove key 2 via the view
        view.remove(2);
        assertNull(intMap.get(2), "Removal from view must remove entry from original map");
        assertFalse(view.containsKey(2));
    }

    @Test
    void headMap_viewMutability_addToOriginalReflectedInView() {
        // Map with Integer keys
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        NavigableMap<Integer, String> view = intMap.headMap(3);
        // Add a new entry that falls within the view's range
        intMap.put(0, "zero");
        assertTrue(view.containsKey(0));
        assertEquals("zero", view.get(0));
    }

    @Test
    void headMap_withCustomReverseComparator() {
        // Reverse order comparator (largest key considered "least")
        Comparator<Integer> reverse = Comparator.<Integer>reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> revMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        revMap.put(9, "nine");
        revMap.put(7, "seven");
        revMap.put(5, "five");
        revMap.put(3, "three");
        // According to reverse order, keys greater than 5 (i.e., 9 and 7) are "less than" 5
        NavigableMap<Integer, String> head = revMap.headMap(5);
        Set<Integer> expected = new LinkedHashSet<>(Arrays.asList(9, 7));
        assertEquals(expected, head.keySet(), "Reverse comparator headMap should contain keys 9 and 7");
    }

    @Test
    void headMap_withCustomReverseComparator_toKeySmallerThanAll_returnsEmpty() {
        Comparator<Integer> reverse = Comparator.<Integer>reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> revMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        revMap.put(9, "nine");
        revMap.put(7, "seven");
        revMap.put(5, "five");
        revMap.put(3, "three");
        // In reverse order, 8 is smaller than the smallest key (9)
        NavigableMap<Integer, String> head = revMap.headMap(8);
        assertTrue(head.isEmpty(), "With reverse comparator, headMap(8) should be empty because no key is less than 8");
    }

    @Test
    void headMap_typeMismatch_throwsClassCastException() {
        // Map with Integer keys
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        // Supplying a String key should cause ClassCastException
        assertThrows(ClassCastException.class, () -> intMap.headMap("stringKey"));
    }

    @Test
    void headMap_typeMismatch_withComparator_throwsClassCastException() {
        // Map with String keys but a custom comparator (natural order)
        Comparator<String> comp = Comparator.naturalOrder();
        ConcurrentNavigableMapNullSafe<String, Integer> strMap = new ConcurrentNavigableMapNullSafe<>(comp);
        strMap.put("a", 1);
        strMap.put("b", 2);
        // Supplying an Integer toKey should cause ClassCastException
        assertThrows(ClassCastException.class, () -> strMap.headMap(123));
    }

    @Test
    void headMap_onSubMap_outOfRange_throwsIllegalArgumentException() {
        // Base map with Integer keys
        ConcurrentNavigableMapNullSafe<Integer, String> base = new ConcurrentNavigableMapNullSafe<>();
        base.put(2, "two");
        base.put(3, "three");
        base.put(4, "four");
        base.put(5, "five");
        // SubMap view: [2,5) i.e., keys 2,3,4
        ConcurrentNavigableMap<Integer, String> sub = base.subMap(2, true, 5, false);
        // headMap(6) on this subMap exceeds the high bound (5) → IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> sub.headMap(6));
    }
}
