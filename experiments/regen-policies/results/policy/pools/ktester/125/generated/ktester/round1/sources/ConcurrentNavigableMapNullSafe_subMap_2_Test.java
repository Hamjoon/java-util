package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_subMap_2_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test and populate it with a predictable ordering.
        // The natural ordering of String puts "a" < "b" < "c".  Null keys are considered *last*.
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null is treated as the greatest key
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference – not strictly required but keeps the pattern consistent.
        map = null;
    }

    /**
     * Helper to build a map populated with integer keys 1‑5 and a null key.
     */
    private static ConcurrentNavigableMapNullSafe<Integer, Integer> buildIntegerMap(boolean includeNull) {
        ConcurrentNavigableMapNullSafe<Integer, Integer> m = new ConcurrentNavigableMapNullSafe<>();
        for (int i = 1; i <= 5; i++) {
            m.put(i, i * 10);
        }
        if (includeNull) {
            m.put(null, 999);
        }
        return m;
    }

    private static Stream<Arguments> validRangeProvider() {
        return Stream.of(Arguments.of(1, 5, Set.of(1, 2, 3, 4)), Arguments.of(3, 3, Set.of()), Arguments.of(2, 4, Set.of(2, 3)));
    }

    /**
     * Verify that a normal sub‑map (fromKey inclusive, toKey exclusive) contains the expected entries.
     */
    @Test
    void subMap_normalRange_containsExpectedEntries() {
        // from "a" (inclusive) to "c" (exclusive) should give {"a", "b"}
        NavigableMap<String, Integer> sub = map.subMap("a", "c");
        assertEquals(2, sub.size(), "Sub‑map should contain exactly two entries");
        assertTrue(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertFalse(sub.containsKey("c"));
        assertEquals(1, sub.get("a"));
        assertEquals(2, sub.get("b"));
    }

    /**
     * When the {@code fromKey} is {@code null}, it is considered the greatest key,
     * therefore the resulting sub‑map must be empty (nothing is greater than null).
     */
    @Test
    void subMap_fromKeyIsNull_returnsEmptyMap() {
        NavigableMap<String, Integer> sub = map.subMap(null, "c");
        assertTrue(sub.isEmpty(), "Sub‑map with null as fromKey should be empty");
    }

    /**
     * When the {@code toKey} is {@code null}, the sub‑map should contain all
     * entries whose keys are less than the null sentinel (i.e., all non‑null keys).
     */
    @Test
    void subMap_toKeyIsNull_includesAllNonNullKeys() {
        NavigableMap<String, Integer> sub = map.subMap("a", null);
        // Expected keys: a, b, c (null is excluded because toInclusive = false)
        assertEquals(3, sub.size());
        assertTrue(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertTrue(sub.containsKey("c"));
        assertFalse(sub.containsKey(null));
    }

    /**
     * If {@code fromKey} is greater than {@code toKey} according to the map's ordering,
     * the resulting sub‑map must be empty.
     */
    @Test
    void subMap_fromKeyGreaterThanToKey_returnsEmptyMap() {
        NavigableMap<String, Integer> sub = map.subMap("c", "a");
        assertTrue(sub.isEmpty(), "Sub‑map where fromKey > toKey should be empty");
    }

    /**
     * Changes made to the view returned by {@code subMap} must be reflected in the original map
     * and vice‑versa.
     */
    @Test
    void subMap_modificationsAreReflectedInOriginalMap() {
        // contains a and b
        NavigableMap<String, Integer> sub = map.subMap("a", "c");
        // Update an existing entry via the sub‑map
        sub.put("a", 10);
        assertEquals(10, map.get("a"));
        // Add a new entry that falls inside the sub‑map range
        sub.put("b", 20);
        assertEquals(20, map.get("b"));
        // Remove an entry via the sub‑map
        sub.remove("a");
        assertNull(map.get("a"));
        // b and c (null) remain
        assertEquals(2, map.size());
    }

    /**
     * Verify that the {@code subMap} view respects the inclusive/exclusive contract:
     * fromKey is inclusive, toKey is exclusive.
     */
    @Test
    void subMap_inclusiveExclusiveBehavior() {
        // fromKey "b" inclusive, toKey "c" exclusive → should contain only "b"
        SortedMap<String, Integer> sub = map.subMap("b", "c");
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey("b"));
        assertFalse(sub.containsKey("c"));
    }

    @Test
    void subMap_validRange_standardCase() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = buildIntegerMap(false);
        NavigableMap<Integer, Integer> sub = intMap.subMap(1, 5);
        // Expected keys: 1,2,3,4
        assertEquals(Set.of(1, 2, 3, 4), sub.keySet());
        assertEquals(4, sub.size());
    }

    @Test
    void subMap_validRange_equalBoundsProducesEmptyMap() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = buildIntegerMap(false);
        NavigableMap<Integer, Integer> sub = intMap.subMap(3, 3);
        assertTrue(sub.isEmpty(), "Equal from/to keys must yield an empty sub‑map");
    }

    @Test
    void subMap_validRange_lowerInclusiveUpperExclusive() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = buildIntegerMap(false);
        NavigableMap<Integer, Integer> sub = intMap.subMap(2, 4);
        // Expected keys: 2,3
        assertEquals(Set.of(2, 3), sub.keySet());
    }

    @Test
    void subMap_nullFromKeyAcceptedAndIncludesNullKey() {
        // According to the JSON spec for this group, null is allowed as lower bound.
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = buildIntegerMap(true);
        NavigableMap<Integer, Integer> sub = intMap.subMap(null, 2);
        // Expected: null key (inclusive) and key 1 (since 2 is exclusive)
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(null));
        assertTrue(sub.containsKey(1));
        assertFalse(sub.containsKey(2));
    }

    @Test
    void subMap_nullToKeyThrowsIllegalArgumentException() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = buildIntegerMap(true);
        assertThrows(IllegalArgumentException.class, () -> intMap.subMap(1, null), "A null toKey should be rejected with IllegalArgumentException");
    }

    @Test
    void subMap_reversedRangeThrowsIllegalArgumentException() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = buildIntegerMap(false);
        assertThrows(IllegalArgumentException.class, () -> intMap.subMap(5, 1), "fromKey > toKey must cause IllegalArgumentException (like ConcurrentSkipListMap)");
    }

    @Test
    void subMap_onEmptyMapReturnsEmptyView() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        NavigableMap<Integer, Integer> sub = empty.subMap(1, 5);
        assertTrue(sub.isEmpty(), "Sub‑map of an empty map must be empty");
    }

    @Test
    void subMap_nonComparableKeysWithoutComparatorThrowsClassCastException() {
        // Use a simple Object that does not implement Comparable
        Object a = new Object();
        Object b = new Object();
        ConcurrentNavigableMapNullSafe<Object, String> objMap = new ConcurrentNavigableMapNullSafe<>();
        objMap.put(a, "A");
        objMap.put(b, "B");
        assertThrows(ClassCastException.class, () -> objMap.subMap(a, b), "Non‑Comparable keys without a Comparator must cause ClassCastException");
    }

    @Test
    void subMap_customComparatorReverseOrder_validDescendingRange() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, Integer> revMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        for (int i = 1; i <= 5; i++) {
            revMap.put(i, i);
        }
        // In descending order, 4 is "lower" than 2, so subMap(4,2) should contain 4 and 3.
        NavigableMap<Integer, Integer> sub = revMap.subMap(4, 2);
        assertEquals(Set.of(4, 3), sub.keySet());
    }

    @Test
    void subMap_customComparatorReverseOrder_invalidRangeYieldsEmptyMap() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, Integer> revMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        for (int i = 1; i <= 5; i++) {
            revMap.put(i, i);
        }
        // fromKey 2 is "greater" than toKey 4 in descending order → empty interval.
        NavigableMap<Integer, Integer> sub = revMap.subMap(2, 4);
        assertTrue(sub.isEmpty(), "Invalid descending interval must produce an empty sub‑map");
    }

    @Test
    void subMap_customComparatorRejectsNullKey_throwsNullPointerException() {
        Comparator<Integer> noNulls = (k1, k2) -> {
            // Explicit NPE when either argument is null
            Objects.requireNonNull(k1);
            Objects.requireNonNull(k2);
            return Integer.compare(k1, k2);
        };
        ConcurrentNavigableMapNullSafe<Integer, Integer> mapWithComparator = new ConcurrentNavigableMapNullSafe<>(noNulls);
        mapWithComparator.put(1, 10);
        mapWithComparator.put(2, 20);
        assertThrows(NullPointerException.class, () -> mapWithComparator.subMap(null, 2), "Comparator that cannot handle null must propagate NullPointerException");
    }

    @Test
    void subMap_customComparatorTypeMismatch_throwsClassCastException() {
        Comparator<Integer> intComparator = Integer::compareTo;
        ConcurrentNavigableMapNullSafe<Integer, Integer> mapWithComparator = new ConcurrentNavigableMapNullSafe<>(intComparator);
        mapWithComparator.put(1, 10);
        mapWithComparator.put(2, 20);
        // Upper bound is a String; comparator will attempt to cast and fail.
        assertThrows(ClassCastException.class, () -> mapWithComparator.subMap(1, (Integer) (Object) "2"), "Mismatched key type should result in ClassCastException");
    }

    @Test
    void subMap_comparatorThrowsIllegalArgumentException_propagated() {
        Comparator<Integer> validator = (k1, k2) -> {
            if (k1 < 0) {
                throw new IllegalArgumentException("Negative keys not allowed");
            }
            return Integer.compare(k1, k2);
        };
        ConcurrentNavigableMapNullSafe<Integer, Integer> mapWithValidator = new ConcurrentNavigableMapNullSafe<>(validator);
        mapWithValidator.put(1, 10);
        mapWithValidator.put(5, 50);
        assertThrows(IllegalArgumentException.class, () -> mapWithValidator.subMap(-1, 5), "Comparator's IllegalArgumentException must be propagated");
    }

    @Test
    void subMap_naturalOrderingMixedKeyTypes_throwsClassCastException() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        // Upper bound is a String – should cause ClassCastException during comparison.
        assertThrows(ClassCastException.class, () -> intMap.subMap(1, (Integer) (Object) "2"), "Mixed key types under natural ordering must throw ClassCastException");
    }

    @ParameterizedTest(name = "subMap({0}, {1}) → expected keys {2}")
    @MethodSource("validRangeProvider")
    void subMap_validRanges_parameterized(Integer from, Integer to, Set<Integer> expected) {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = buildIntegerMap(false);
        NavigableMap<Integer, Integer> sub = intMap.subMap(from, to);
        assertEquals(expected, sub.keySet());
    }
}
