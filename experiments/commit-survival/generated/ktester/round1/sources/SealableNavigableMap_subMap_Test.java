package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_subMap_Test {

    private static final String ONE = "one";

    private static final String TWO = "two";

    private static final String THREE = "three";

    private static final String FOUR = "four";

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start each test un‑sealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // a simple mutable backing map
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put(ONE, 1);
        backingMap.put(TWO, 2);
        backingMap.put(THREE, 3);
        // the original test suite allowed a null entry
        backingMap.put(null, null);
        // create the SealableNavigableMap that wraps the backing map
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    /**
     * Helper to create a SealableNavigableMap backed by a simple TreeMap with integer keys.
     */
    private SealableNavigableMap<Integer, Integer> createIntegerMap(int... keys) {
        NavigableMap<Integer, Integer> base = new TreeMap<>();
        for (int k : keys) {
            base.put(k, k);
        }
        return new SealableNavigableMap<>(base, () -> false);
    }

    /**
     * Parameter source for normal‑range tests.
     */
    static Stream<Arguments> normalRangeProvider() {
        return Stream.of(Arguments.of(1, 5, List.of(1, 2, 3, 4)), Arguments.of(2, 2, List.of()), Arguments.of(3, 7, List.of(3, 4, 5, 6)));
    }

    /**
     * The {@code subMap(K, K)} overload is defined as:
     *   subMap(fromKey, true, toKey, false)
     * i.e. the lower bound is inclusive, the upper bound exclusive.
     */
    @Test
    void testSubMapInclusiveExclusiveBehaviour() {
        // subMap from "one" (inclusive) to "three" (exclusive) should contain "one" and "two"
        NavigableMap<String, Integer> sub = sealableMap.subMap(ONE, THREE);
        assertEquals(2, sub.size(), "Sub‑map should contain exactly two entries");
        assertTrue(sub.containsKey(ONE), "Sub‑map must contain the lower bound key");
        assertTrue(sub.containsKey(TWO), "Sub‑map must contain a key between the bounds");
        assertFalse(sub.containsKey(THREE), "Upper bound is exclusive – should not contain \"three\"");
    }

    @Test
    void testSubMapReflectsChangesFromParentAndViceVersa() {
        // inclusive/exclusive
        NavigableMap<String, Integer> sub = sealableMap.subMap(ONE, THREE);
        // change through parent map – should be visible in sub‑map
        sealableMap.put("one-and-a-half", 15);
        assertTrue(sub.containsKey("one-and-a-half"));
        // change through sub‑map – should be reflected in the parent
        sub.put("two-and-a-half", 25);
        assertTrue(sealableMap.containsKey("two-and-a-half"));
        assertEquals(Integer.valueOf(25), sealableMap.get("two-and-a-half"));
    }

    @Test
    void testSubMapMutationsWhenSealedThrowUnsupportedOperationException() {
        NavigableMap<String, Integer> sub = sealableMap.subMap(ONE, THREE);
        // make the map sealed
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> sub.put("newKey", 99), "Putting into a sub‑map while sealed must fail");
        assertThrows(UnsupportedOperationException.class, () -> sub.remove(ONE), "Removing from a sub‑map while sealed must fail");
    }

    @Test
    void testSubMapPutOutsideRangeThrowsIllegalArgumentExceptionWhenUnsealed() {
        NavigableMap<String, Integer> sub = sealableMap.subMap(ONE, THREE);
        // map is unsealed (default). Trying to put a key outside the defined range should fail.
        assertThrows(IllegalArgumentException.class, () -> sub.put("zero", 0), "Key below lower bound should be rejected");
        assertThrows(IllegalArgumentException.class, () -> sub.put("three", 3), "Key equal to exclusive upper bound should be rejected");
    }

    @Test
    void testSubMapWithNullKeys() {
        // The underlying implementation permits a null key; verify subMap handles it correctly.
        // Create a sub‑map that starts at null (inclusive) and ends at "one" (exclusive).
        NavigableMap<String, Integer> sub = sealableMap.subMap(null, ONE);
        assertTrue(sub.containsKey(null), "Sub‑map should contain the null key");
        assertFalse(sub.containsKey(ONE), "Upper bound is exclusive – should not contain \"one\"");
        // Mutate through sub‑map and verify parent sees the change.
        sub.put("nullKey2", 42);
        assertTrue(sealableMap.containsKey("nullKey2"));
    }

    @ParameterizedTest(name = "subMap({0},{1}) should contain {2}")
    @MethodSource("normalRangeProvider")
    void testValidSubMapRanges(int from, int to, List<Integer> expectedKeys) {
        SealableNavigableMap<Integer, Integer> map = createIntegerMap(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        SortedMap<Integer, Integer> sub = map.subMap(from, to);
        assertEquals(expectedKeys.size(), sub.size(), "Size mismatch");
        assertEquals(expectedKeys, new ArrayList<>(sub.keySet()), "Key set mismatch");
    }

    @Test
    void testSubMapOnEmptyUnderlyingMap() {
        SealableNavigableMap<Integer, Integer> emptyMap = createIntegerMap();
        SortedMap<Integer, Integer> sub = emptyMap.subMap(10, 20);
        assertTrue(sub.isEmpty(), "Sub‑map of empty map must be empty");
    }

    @Test
    void testSubMapNullKeyThrowsNPE() {
        SealableNavigableMap<Integer, Integer> map = createIntegerMap(1, 2, 3);
        assertThrows(NullPointerException.class, () -> map.subMap(null, 5), "subMap(null,5) should throw NPE");
        assertThrows(NullPointerException.class, () -> map.subMap(1, null), "subMap(1,null) should throw NPE");
        assertThrows(NullPointerException.class, () -> map.subMap(null, null), "subMap(null,null) should throw NPE");
    }

    @Test
    void testFromKeyGreaterThanToKeyThrowsIllegalArgumentException() {
        SealableNavigableMap<Integer, Integer> map = createIntegerMap(1, 2, 3, 4, 5);
        assertThrows(IllegalArgumentException.class, () -> map.subMap(8, 3), "fromKey > toKey must throw IllegalArgumentException");
    }

    @Test
    void testInclusiveExclusiveFlagsVerification() {
        SealableNavigableMap<Integer, Integer> map = createIntegerMap(1, 2, 3, 4, 5, 6, 7, 8, 9);
        SortedMap<Integer, Integer> sub = map.subMap(5, 9);
        assertEquals(List.of(5, 6, 7, 8), new ArrayList<>(sub.keySet()), "Inclusive lower, exclusive upper");
        SortedMap<Integer, Integer> subSingle = map.subMap(5, 6);
        assertEquals(List.of(5), new ArrayList<>(subSingle.keySet()), "Single‑element range");
    }

    @Test
    void testNonExistentLowerBoundStartsAtFirstKey() {
        SealableNavigableMap<Integer, Integer> map = createIntegerMap(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        SortedMap<Integer, Integer> sub = map.subMap(0, 3);
        assertEquals(List.of(1, 2), new ArrayList<>(sub.keySet()), "Lower bound below first key should start at first key");
    }

    @Test
    void testRangeBeyondUpperLimit() {
        SealableNavigableMap<Integer, Integer> map = createIntegerMap(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        SortedMap<Integer, Integer> sub1 = map.subMap(9, 15);
        assertEquals(List.of(9, 10), new ArrayList<>(sub1.keySet()), "Upper bound beyond max key");
        SortedMap<Integer, Integer> sub2 = map.subMap(11, 20);
        assertTrue(sub2.isEmpty(), "Both bounds beyond max key must yield empty sub‑map");
    }

    @Test
    void testSubMapReflectsUnderlyingModifications() {
        SealableNavigableMap<Integer, Integer> map = createIntegerMap(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        SortedMap<Integer, Integer> sub = map.subMap(3, 7);
        assertEquals(List.of(3, 4, 5, 6), new ArrayList<>(sub.keySet()));
        // modify parent
        map.put(5, 99);
        assertEquals(99, sub.get(5));
        map.remove(4);
        assertFalse(sub.containsKey(4));
        assertEquals(List.of(3, 5, 6), new ArrayList<>(sub.keySet()));
    }

    @Test
    void testSubMapAfterOriginalMapCleared() {
        SealableNavigableMap<Integer, Integer> map = createIntegerMap(1, 2, 3, 4, 5);
        SortedMap<Integer, Integer> sub = map.subMap(2, 5);
        assertFalse(sub.isEmpty());
        map.clear();
        assertTrue(sub.isEmpty(), "Sub‑map must reflect cleared parent map");
    }

    @Test
    void testCustomComparatorReverseOrder() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        NavigableMap<Integer, Integer> base = new TreeMap<>(reverse);
        for (int i = 1; i <= 5; i++) {
            base.put(i, i);
        }
        SealableNavigableMap<Integer, Integer> map = new SealableNavigableMap<>(base, () -> false);
        // according to reverse order, 4 > 2
        SortedMap<Integer, Integer> sub = map.subMap(4, 2);
        // Expected keys: 4,3 (2 is exclusive)
        assertEquals(List.of(4, 3), new ArrayList<>(sub.keySet()), "Reverse order sub‑map");
    }

    @Test
    void testClassCastExceptionOnIncompatibleKeyType() {
        SealableNavigableMap<Integer, Integer> map = createIntegerMap(1, 2, 3);
        assertThrows(ClassCastException.class, () -> map.subMap((Integer) (Object) "string", 5), "Incompatible fromKey type should throw CCE");
        assertThrows(ClassCastException.class, () -> map.subMap(1, (Integer) (Object) "string"), "Incompatible toKey type should throw CCE");
    }

    @Test
    void testComparatorThrowsRuntimeExceptionDuringComparison() {
        Comparator<Integer> badComparator = (a, b) -> {
            if (a == 5 || b == 5)
                throw new RuntimeException("Bad key 5");
            return Integer.compare(a, b);
        };
        NavigableMap<Integer, Integer> base = new TreeMap<>(badComparator);
        for (int i = 1; i <= 10; i++) base.put(i, i);
        SealableNavigableMap<Integer, Integer> map = new SealableNavigableMap<>(base, () -> false);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> map.subMap(5, 10), "Comparator exception should propagate");
        assertEquals("Bad key 5", ex.getMessage());
    }

    @Test
    void testSealedMapSubMapThrowsIllegalStateException() {
        // Build a sealed map – the supplier returns true
        Supplier<Boolean> alwaysSealed = () -> true;
        NavigableMap<Integer, Integer> base = new TreeMap<>();
        for (int i = 1; i <= 5; i++) base.put(i, i);
        SealableNavigableMap<Integer, Integer> sealedMap = new SealableNavigableMap<>(base, alwaysSealed);
        assertThrows(IllegalStateException.class, () -> sealedMap.subMap(2, 6), "Requesting subMap on a sealed map must throw IllegalStateException");
    }
}
