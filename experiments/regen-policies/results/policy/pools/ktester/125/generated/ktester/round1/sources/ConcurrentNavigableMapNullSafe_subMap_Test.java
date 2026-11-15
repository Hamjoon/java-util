package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_subMap_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /* --------------------------------------------------------------
     *  Non‑Comparable key exception
     * -------------------------------------------------------------- */
    private static class NonComparable {

        final String id;

        NonComparable(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "NC{" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // A map with a mixture of numeric string keys and a null key.
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);
        map.put("4", 4);
        map.put("5", 5);
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination
        map.clear();
    }

    /* --------------------------------------------------------------
     *  Normal range with non‑null keys
     * -------------------------------------------------------------- */
    private static Stream<Arguments> normalRangeProvider() {
        return Stream.of(// fromKey = "1", inclusive; toKey = "5", exclusive → keys 1‑4
        Arguments.of("1", true, "5", false, List.of("1", "2", "3", "4")), // fromKey = "2", exclusive; toKey = "4", inclusive → keys 3‑4
        Arguments.of("2", false, "4", true, List.of("3", "4")));
    }

    /* --------------------------------------------------------------
     *  Custom comparator (reverse order)
     * -------------------------------------------------------------- */
    private static ConcurrentNavigableMapNullSafe<String, Integer> createReverseOrderMap() {
        // Use reflection to invoke the package‑private constructor that accepts an internal map and comparator
        ConcurrentSkipListMap<String, Integer> internal = new ConcurrentSkipListMap<>(Collections.reverseOrder());
        internal.put("1", 1);
        internal.put("2", 2);
        internal.put("3", 3);
        internal.put("4", 4);
        internal.put("5", 5);
        // null is considered the *smallest* key in the descending view
        internal.put(null, 99);
        try {
            var ctor = ConcurrentNavigableMapNullSafe.class.getDeclaredConstructor(ConcurrentNavigableMap.class, Comparator.class);
            ctor.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentNavigableMapNullSafe<String, Integer> map = (ConcurrentNavigableMapNullSafe<String, Integer>) ctor.newInstance(internal, Collections.reverseOrder());
            return map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void subMap_inclusiveFrom_exclusiveTo_excludesUpperBound() {
        // WHEN: we request a sub‑map from "a" (inclusive) to "c" (exclusive)
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", true, "c", false);
        // THEN: the sub‑map should contain "a" and "b" only
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertFalse(sub.containsKey("c"));
        assertFalse(sub.containsKey(null));
        // AND: the values are the same as in the original map
        assertEquals(1, sub.get("a"));
        assertEquals(2, sub.get("b"));
    }

    @Test
    void subMap_nullFromKey_resultsInEmptyMap() {
        // WHEN: we request a sub‑map starting at the null key (which is considered last)
        //       and ending at a regular key "c". According to the contract null is ordered
        //       after any non‑null key, therefore the range is empty.
        ConcurrentNavigableMap<String, Integer> sub = map.subMap(null, true, "c", false);
        // THEN: the returned map should be empty
        assertTrue(sub.isEmpty());
    }

    @Test
    void subMap_modificationsAreReflectedInOriginalMap() {
        // WHEN: we obtain a sub‑map that includes "b" and "c"
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("b", true, null, true);
        // THEN: the sub‑map initially contains the expected entries
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("b"));
        assertTrue(sub.containsKey("c"));
        assertFalse(sub.containsKey("a"));
        assertTrue(sub.containsKey(null));
        // AND WHEN: we modify the sub‑map
        sub.put("b", 20);
        sub.remove("c");
        // "d" is within the range (between "b" and null)
        sub.put("d", 40);
        // THEN: the original map reflects those changes
        assertEquals(20, map.get("b"));
        assertNull(map.get("c"));
        assertEquals(40, map.get("d"));
        // The size of the original map has been updated accordingly
        // a, b, d, null, plus the original "a"
        assertEquals(5, map.size());
    }

    @Test
    void subMap_exclusiveFrom_andInclusiveTo_behaviour() {
        // WHEN: we request a sub‑map from "a" (exclusive) to "c" (inclusive)
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", false, "c", true);
        // THEN: "a" must be excluded, "c" included
        assertFalse(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertTrue(sub.containsKey("c"));
        assertFalse(sub.containsKey(null));
        // only "b" and "c"
        assertEquals(2, sub.size());
    }

    @ParameterizedTest
    @MethodSource("normalRangeProvider")
    void subMap_normalRange(String fromKey, boolean fromInc, String toKey, boolean toInc, List<String> expectedKeys) {
        ConcurrentNavigableMap<String, Integer> sub = map.subMap(fromKey, fromInc, toKey, toInc);
        // verify size and content
        assertEquals(expectedKeys.size(), sub.size(), "Sub‑map size does not match expected number of entries");
        for (String k : expectedKeys) {
            assertTrue(sub.containsKey(k), "Expected key missing: " + k);
            assertEquals(map.get(k), sub.get(k), "Value mismatch for key: " + k);
        }
        // ensure null key is never present in these ranges (null is greatest)
        assertFalse(sub.containsKey(null));
    }

    /* --------------------------------------------------------------
     *  Null‑key handling (null is the greatest key)
     * -------------------------------------------------------------- */
    @Test
    void subMap_nullFromKey_inclusiveToNonNull_excludesAll() {
        // null (greatest) → any non‑null upper bound yields an empty range
        ConcurrentNavigableMap<String, Integer> sub = map.subMap(null, true, "3", true);
        assertTrue(sub.isEmpty(), "Sub‑map should be empty when null is the lower bound and a non‑null upper bound");
    }

    @Test
    void subMap_nonNullFromKey_inclusiveToNull_excludesNullWhenUpperExclusive() {
        // Upper bound null exclusive → null should be omitted
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("2", true, null, false);
        // Expected keys: 2,3,4,5 (null excluded)
        List<String> expected = List.of("2", "3", "4", "5");
        assertEquals(expected.size(), sub.size());
        expected.forEach(k -> assertTrue(sub.containsKey(k)));
        assertFalse(sub.containsKey(null));
    }

    @Test
    void subMap_nullFromKey_inclusiveToNull_inclusiveReturnsWholeMap() {
        // Both bounds null inclusive → whole map view
        ConcurrentNavigableMap<String, Integer> sub = map.subMap(null, true, null, true);
        assertEquals(map.size(), sub.size());
        assertTrue(sub.containsKey(null));
        assertTrue(sub.containsKey("1"));
        assertTrue(sub.containsKey("5"));
    }

    /* --------------------------------------------------------------
     *  Invalid range handling
     * -------------------------------------------------------------- */
    @Test
    void subMap_fromKeyGreaterThanToKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> map.subMap("5", true, "2", true), "Expected IllegalArgumentException when fromKey > toKey");
    }

    @Test
    void subMap_sameKeyBothExclusive_returnsEmptyMap() {
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("3", false, "3", false);
        assertTrue(sub.isEmpty(), "Sub‑map should be empty when bounds are equal and both exclusive");
    }

    @Test
    void subMap_sameKeyBothInclusive_containsSingleEntry() {
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("3", true, "3", true);
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey("3"));
        assertEquals(3, sub.get("3"));
    }

    /* --------------------------------------------------------------
     *  View consistency between original and sub‑map
     * -------------------------------------------------------------- */
    @Test
    void subMap_viewIsLive_modifyThroughSubMap_reflectedInOriginal() {
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("2", true, "4", true);
        // initial assertions
        assertEquals(3, sub.size());
        assertTrue(sub.containsKey("2"));
        assertTrue(sub.containsKey("3"));
        assertTrue(sub.containsKey("4"));
        // modify through sub‑map
        sub.put("3", 30);
        sub.remove("2");
        // "6" is greater than upper bound → should NOT be added
        sub.put("6", 60);
        // verify original map reflects changes
        assertNull(map.get("2"));
        assertEquals(30, map.get("3"));
        // unchanged
        assertEquals(5, map.get("5"));
        // not present
        assertNull(map.get("6"));
        // size check
        // original 6 entries minus removed "2"
        assertEquals(5, map.size());
    }

    /* --------------------------------------------------------------
     *  Null‑key exclusive lower bound
     * -------------------------------------------------------------- */
    @Test
    void subMap_nullFromKey_exclusive_excludesNullKey() {
        // exclusive null lower bound → start after null (i.e., first non‑null key)
        ConcurrentNavigableMap<String, Integer> sub = map.subMap(null, false, "3", true);
        // Expected keys: 1,2,3 (null excluded)
        List<String> expected = List.of("1", "2", "3");
        assertEquals(expected.size(), sub.size());
        expected.forEach(k -> assertTrue(sub.containsKey(k)));
        assertFalse(sub.containsKey(null));
    }

    /* --------------------------------------------------------------
     *  Empty map scenarios
     * -------------------------------------------------------------- */
    @Test
    void subMap_onEmptyMap_returnsEmptyView() {
        ConcurrentNavigableMapNullSafe<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<String, Integer> sub = empty.subMap("1", true, "5", false);
        assertTrue(sub.isEmpty(), "Sub‑map of an empty map must be empty");
        assertEquals(0, sub.size());
    }

    @Test
    void subMap_fullRangeOnEmptyMap_returnsEmptyView() {
        ConcurrentNavigableMapNullSafe<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<String, Integer> sub = empty.subMap(null, true, null, true);
        assertTrue(sub.isEmpty());
        assertEquals(0, sub.size());
    }

    @Test
    void subMap_reverseOrder_normalRange() {
        ConcurrentNavigableMapNullSafe<String, Integer> revMap = createReverseOrderMap();
        // In reverse order, "5" is the lowest element, "2" is higher.
        ConcurrentNavigableMap<String, Integer> sub = revMap.subMap("5", true, "2", false);
        // Expected keys: 5,4,3 (2 is exclusive)
        List<String> expected = List.of("5", "4", "3");
        assertEquals(expected.size(), sub.size());
        expected.forEach(k -> assertTrue(sub.containsKey(k)));
        assertFalse(sub.containsKey("2"));
    }

    @Test
    void subMap_reverseOrder_nullBounds() {
        ConcurrentNavigableMapNullSafe<String, Integer> revMap = createReverseOrderMap();
        // null is the *smallest* key in the descending view, so a range from null (inclusive) to "3" (inclusive)
        // should contain null,5,4,3
        ConcurrentNavigableMap<String, Integer> sub = revMap.subMap(null, true, "3", true);
        List<String> expected = List.of(null, "5", "4", "3");
        assertEquals(expected.size(), sub.size());
        expected.forEach(k -> assertTrue(sub.containsKey(k)));
    }

    /* --------------------------------------------------------------
     *  Type mismatch exception
     * -------------------------------------------------------------- */
    @Test
    void subMap_typeMismatch_throwsClassCastException() {
        // map with String keys; provide an Integer as toKey
        assertThrows(ClassCastException.class, () -> map.subMap("a", true, 10, true), "Expected ClassCastException when keys are of incompatible types");
    }

    /* --------------------------------------------------------------
     *  Returned map type verification
     * -------------------------------------------------------------- */
    @Test
    void subMap_returnsInstanceOfConcurrentNavigableMapNullSafe() {
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("1", true, "4", false);
        assertTrue(sub instanceof ConcurrentNavigableMapNullSafe, "subMap should return a ConcurrentNavigableMapNullSafe view");
    }

    /* --------------------------------------------------------------
     *  Null key removal propagation
     * -------------------------------------------------------------- */
    @Test
    void subMap_removeNullKey_reflectedInOriginal() {
        ConcurrentNavigableMap<String, Integer> sub = map.subMap(null, true, "5", false);
        assertTrue(sub.containsKey(null), "Sub‑map should initially contain the null key");
        sub.remove(null);
        assertFalse(sub.containsKey(null));
        assertFalse(map.containsKey(null), "Removal via sub‑map must affect the original map");
        // both sizes should be equal after removal
        assertEquals(map.size(), sub.size());
    }

    @Test
    void subMap_nonComparableKey_throwsClassCastException() {
        // Build a map with natural ordering (no comparator) but insert a non‑comparable key via reflection.
        ConcurrentNavigableMapNullSafe<Object, Integer> ncMap = new ConcurrentNavigableMapNullSafe<>();
        // Insert a non‑comparable key directly into the internal map (bypassing type safety)
        try {
            var internalField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
            internalField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentMap<Object, Object> internal = (ConcurrentMap<Object, Object>) internalField.get(ncMap);
            internal.put(new NonComparable("x"), 1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Attempt to create a sub‑map using the non‑comparable key as a bound
        assertThrows(ClassCastException.class, () -> ncMap.subMap(new NonComparable("x"), true, new NonComparable("y"), true), "Expected ClassCastException when using non‑Comparable keys without a comparator");
    }
}
