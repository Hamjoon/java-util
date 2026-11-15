package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_headMap_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    /* -------------------------------------------------------------
       Common test fixture
       ------------------------------------------------------------- */
    @BeforeEach
    void setupBeforeEach() {
        sealed = false;
        sealedSupplier = () -> sealed;
        // a simple mutable backing map – the implementation used by the library
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        backingMap.put("four", 4);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    /**
     * Helper to build a fresh SealableNavigableMap<Integer,String> with the supplied entries.
     */
    private SealableNavigableMap<Integer, String> createIntMap(Map<Integer, String> entries, Supplier<Boolean> sealSup, Comparator<Integer> comparator) {
        NavigableMap<Integer, String> backing;
        if (comparator == null) {
            backing = new ConcurrentNavigableMapNullSafe<>();
        } else {
            // TreeMap respects the supplied comparator; we then wrap it with the null‑safe implementation.
            backing = new ConcurrentNavigableMapNullSafe<>(new TreeMap<>(comparator));
        }
        backing.putAll(entries);
        return new SealableNavigableMap<>(backing, sealSup);
    }

    /**
     * ----------------------------------------------------------------
     *        1. Normal behaviour – parameterised tests covering the JSON "Normal behavior"
     *        ----------------------------------------------------------------
     */
    private static Stream<Arguments> normalBehaviorProvider() {
        return Stream.of(// toKey = 5 → keys {1,3,4}
        Arguments.of(5, Map.of(1, "One", 3, "Three", 4, "Four")), // toKey = 1 → empty
        Arguments.of(1, Map.of()), // toKey = 10 → all entries
        Arguments.of(10, Map.of(1, "One", 3, "Three", 4, "Four", 5, "Five", 7, "Seven")), // toKey = 0 → empty
        Arguments.of(0, Map.of()));
    }

    /* -------------------------------------------------------------
       headMap(K) – exclusive upper bound
       ------------------------------------------------------------- */
    @Test
    void headMap_exclusiveRangeReflectsChangesWhenUnsealed() {
        // headMap up to "three" (exclusive) should contain "one" and "two"
        SortedMap<String, Integer> head = sealableMap.headMap("three");
        assertEquals(2, head.size());
        assertTrue(head.containsKey("one"));
        assertTrue(head.containsKey("two"));
        assertFalse(head.containsKey("three"));
        // Adding a key that falls inside the range should be visible in the view
        sealableMap.put("two-and-half", 25);
        assertTrue(head.containsKey("two-and-half"));
        assertEquals(3, head.size());
        // Adding a key outside the range must NOT appear in the view
        sealableMap.put("four", 40);
        assertFalse(head.containsKey("four"));
        assertEquals(3, head.size());
    }

    @Test
    void headMap_mutationsAllowedWhenUnsealed() {
        SortedMap<String, Integer> head = sealableMap.headMap("three");
        // mutate through the view – within range
        head.put("alpha", 0);
        assertEquals(Integer.valueOf(0), head.get("alpha"));
        assertEquals(Integer.valueOf(0), sealableMap.get("alpha"));
        assertTrue(sealableMap.containsKey("alpha"));
    }

    @Test
    void headMap_mutationsThrowWhenSealed() {
        // make the map immutable
        sealed = true;
        SortedMap<String, Integer> head = sealableMap.headMap("three");
        assertThrows(UnsupportedOperationException.class, () -> head.put("beta", 99));
        assertThrows(UnsupportedOperationException.class, () -> head.remove("one"));
        assertThrows(UnsupportedOperationException.class, head::clear);
    }

    @Test
    void headMap_putOutsideRangeThrowsIllegalArgumentException() {
        SortedMap<String, Integer> head = sealableMap.headMap("three");
        // "three" is the exclusive upper bound – putting it (or anything larger) is illegal
        assertThrows(IllegalArgumentException.class, () -> head.put("three", 33));
        assertThrows(IllegalArgumentException.class, () -> head.put("zulu", 99));
    }

    @Test
    void headMap_reflectsDynamicSealState() {
        SortedMap<String, Integer> head = sealableMap.headMap("three");
        // initially unsealed – mutation succeeds
        head.put("gamma", 7);
        assertEquals(Integer.valueOf(7), sealableMap.get("gamma"));
        // seal the map – subsequent mutation must fail
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> head.put("delta", 8));
    }

    @ParameterizedTest(name = "headMap({0}) should contain {1}")
    @MethodSource("normalBehaviorProvider")
    void headMap_normalBehavior(Integer toKey, Map<Integer, String> expected) {
        // arrange
        Map<Integer, String> source = new LinkedHashMap<>();
        source.put(1, "One");
        source.put(3, "Three");
        source.put(4, "Four");
        source.put(5, "Five");
        source.put(7, "Seven");
        Supplier<Boolean> sealSup = () -> false;
        SealableNavigableMap<Integer, String> map = createIntMap(source, sealSup, null);
        // act
        SortedMap<Integer, String> view = map.headMap(toKey);
        // assert
        assertEquals(expected.size(), view.size(), "Unexpected size for headMap(" + toKey + ")");
        assertEquals(expected, view);
    }

    /**
     * ----------------------------------------------------------------
     *        2. Empty map – any toKey must yield an empty view
     *        ----------------------------------------------------------------
     */
    @Test
    void headMap_emptyBackingMapReturnsEmptyView() {
        Supplier<Boolean> sealSup = () -> false;
        SealableNavigableMap<Integer, String> emptyMap = createIntMap(Collections.emptyMap(), sealSup, null);
        SortedMap<Integer, String> view = emptyMap.headMap(5);
        assertTrue(view.isEmpty(), "View of an empty backing map should be empty");
    }

    /**
     * ----------------------------------------------------------------
     *        3. Null toKey – should throw NPE when natural ordering is used
     *        ----------------------------------------------------------------
     */
    @Test
    void headMap_nullToKeyThrowsNPE() {
        Supplier<Boolean> sealSup = () -> false;
        SealableNavigableMap<Integer, String> map = createIntMap(Map.of(1, "One"), sealSup, null);
        assertThrows(NullPointerException.class, () -> map.headMap(null));
    }

    /**
     * ----------------------------------------------------------------
     *        4. Incompatible key type – ClassCastException
     *        ----------------------------------------------------------------
     */
    @Test
    void headMap_incompatibleKeyTypeThrowsClassCastException() {
        Supplier<Boolean> sealSup = () -> false;
        SealableNavigableMap<Integer, String> map = createIntMap(Map.of(1, "One", 2, "Two"), sealSup, null);
        // Passing a String where an Integer is expected
        assertThrows(ClassCastException.class, () -> map.headMap((Integer) (Object) "string"));
    }

    /**
     * ----------------------------------------------------------------
     *        5. Exclusive boundary at existing key (toKey == 4)
     *        ----------------------------------------------------------------
     */
    @Test
    void headMap_exclusiveBoundaryAtExistingKey() {
        Supplier<Boolean> sealSup = () -> false;
        SealableNavigableMap<Integer, String> map = createIntMap(Map.of(1, "One", 3, "Three", 4, "Four", 5, "Five"), sealSup, null);
        SortedMap<Integer, String> view = map.headMap(4);
        assertEquals(Map.of(1, "One", 3, "Three"), view);
    }

    /**
     * ----------------------------------------------------------------
     *        6. Negative toKey with negative keys
     *        ----------------------------------------------------------------
     */
    @Test
    void headMap_negativeToKeyReturnsCorrectSubset() {
        Supplier<Boolean> sealSup = () -> false;
        SealableNavigableMap<Integer, String> map = createIntMap(Map.of(-5, "MinusFive", -3, "MinusThree", -1, "MinusOne", 2, "Two"), sealSup, null);
        SortedMap<Integer, String> view = map.headMap(-2);
        assertEquals(Map.of(-5, "MinusFive", -3, "MinusThree"), view);
    }

    /**
     * ----------------------------------------------------------------
     *        7. Concurrent modification after view creation – view should detect CME
     *        ----------------------------------------------------------------
     */
    @Test
    void headMap_concurrentModificationDetected() {
        Supplier<Boolean> sealSup = () -> false;
        SealableNavigableMap<Integer, String> map = createIntMap(Map.of(1, "One", 2, "Two", 3, "Three"), sealSup, null);
        SortedMap<Integer, String> view = map.headMap(3);
        // modify the underlying map after the view has been obtained
        map.put(4, "Four");
        // Any subsequent operation on the view should now throw CME
        assertThrows(ConcurrentModificationException.class, view::size);
        assertThrows(ConcurrentModificationException.class, () -> view.get(1));
    }

    /**
     * ----------------------------------------------------------------
     *        8. Sealed map – modifications through the view must fail
     *        ----------------------------------------------------------------
     */
    @Test
    void headMap_sealedMapDisallowsModificationsViaView() {
        // start unsealed, obtain view, then seal the map
        Supplier<Boolean> sealSup = () -> sealed;
        SealableNavigableMap<Integer, String> map = createIntMap(Map.of(1, "One", 2, "Two", 3, "Three"), sealSup, null);
        SortedMap<Integer, String> view = map.headMap(3);
        // mutation while unsealed succeeds
        view.put(0, "Zero");
        assertEquals("Zero", map.get(0));
        // now seal the map
        sealed = true;
        // further mutations must throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> view.put(4, "Four"));
        assertThrows(UnsupportedOperationException.class, () -> view.remove(1));
        assertThrows(UnsupportedOperationException.class, view::clear);
    }

    /**
     * ----------------------------------------------------------------
     *        9. Comparator that permits null keys – headMap(null) should NOT throw NPE
     *        ----------------------------------------------------------------
     */
    @Test
    void headMap_customComparatorAllowsNullKey() {
        // Comparator that treats null as greater than any non‑null integer
        Comparator<Integer> nullFriendly = (a, b) -> {
            if (a == null && b == null)
                return 0;
            // null is considered larger
            if (a == null)
                return 1;
            if (b == null)
                return -1;
            return Integer.compare(a, b);
        };
        Supplier<Boolean> sealSup = () -> false;
        SealableNavigableMap<Integer, String> map = createIntMap(Map.of(1, "One", 2, "Two", 3, "Three"), sealSup, nullFriendly);
        // headMap(null) should return all entries because every non‑null key is < null according to the comparator
        SortedMap<Integer, String> view = map.headMap(null);
        assertEquals(3, view.size());
        assertTrue(view.containsKey(1));
        assertTrue(view.containsKey(2));
        assertTrue(view.containsKey(3));
    }
}
