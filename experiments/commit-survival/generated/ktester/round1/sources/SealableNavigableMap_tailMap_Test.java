package com.cedarsoftware.util;

import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_tailMap_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        sealed = false;
        sealedSupplier = () -> sealed;
        // Populate the backing map with a predictable ordering, including a null key
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put(null, null);
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    /**
     * Creates a {@link SealableNavigableMap} wrapping the supplied {@link NavigableMap}
     * and using the current {@code sealed} flag.
     */
    private <K, V> SealableNavigableMap<K, V> createSealableMap(NavigableMap<K, V> map) {
        return new SealableNavigableMap<>(map, () -> sealed);
    }

    private static Stream<Arguments> basicInclusionProvider() {
        // Map: {1="a", 2="b", 3="c"}
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "a");
        base.put(2, "b");
        base.put(3, "c");
        return Stream.of(Arguments.of(base, 2, Map.of(2, "b", 3, "c")), Arguments.of(base, 2.5, Map.of(3, "c")), // null key case is handled separately because TreeMap does not allow null keys
        Arguments.of(base, 1, Map.of(1, "a", 2, "b", 3, "c")), Arguments.of(base, 3, Map.of(3, "c")), Arguments.of(base, 4, Collections.emptyMap()));
    }

    private static Stream<Arguments> singleEntryProvider() {
        NavigableMap<Integer, String> single = new TreeMap<>();
        single.put(5, "z");
        return Stream.of(Arguments.of(single, 0, Map.of(5, "z")), Arguments.of(single, 5, Map.of(5, "z")), Arguments.of(single, 10, Collections.emptyMap()));
    }

    @Test
    void tailMapIncludesFromKeyAndExcludesLowerKeys() {
        NavigableMap<String, Integer> tail = sealableMap.tailMap("two");
        assertTrue(tail.containsKey("two"), "Tail map must contain the fromKey");
        assertTrue(tail.containsKey("three"), "Tail map must contain keys after the fromKey");
        assertFalse(tail.containsKey("one"), "Tail map must not contain keys before the fromKey");
        assertFalse(tail.containsKey(null), "Tail map must not contain keys before the fromKey");
    }

    @Test
    void tailMapDefaultIsInclusive() {
        // The public tailMap(K) delegates to tailMap(K, true)
        NavigableMap<String, Integer> tail = sealableMap.tailMap("two");
        assertTrue(tail.containsKey("two"), "Default tailMap should be inclusive of the fromKey");
    }

    @Test
    void tailMapReflectsChangesWhenUnsealed() {
        NavigableMap<String, Integer> tail = sealableMap.tailMap("two");
        tail.put("four", 4);
        assertEquals(Integer.valueOf(4), sealableMap.get("four"), "Changes made through the tailMap view must be reflected in the underlying map");
        assertTrue(tail.containsKey("four"), "The tail view must also see the newly added entry");
    }

    @Test
    void tailMapIsImmutableWhenSealed() {
        // switch the map to sealed (read‑only) mode
        sealed = true;
        NavigableMap<String, Integer> tail = sealableMap.tailMap("one");
        assertThrows(UnsupportedOperationException.class, () -> tail.put("zero", 0), "Mutating a tailMap view while the map is sealed must throw");
    }

    @Test
    void tailMapViewHonorsSealingAfterCreation() {
        // Create the view while the map is unsealed
        NavigableMap<String, Integer> tail = sealableMap.tailMap("one");
        // Now seal the underlying map
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> tail.put("zero", 0), "A previously obtained tailMap view must become immutable once the map is sealed");
    }

    @Test
    void tailMapHandlesNullFromKey() {
        // The backing map contains a null key; tailMap(null) should start at that key
        NavigableMap<String, Integer> tail = sealableMap.tailMap(null);
        assertTrue(tail.containsKey(null), "Tail map starting at null must contain the null key");
        assertTrue(tail.containsKey("one"), "Tail map must contain keys after null");
        assertTrue(tail.containsKey("two"));
        assertTrue(tail.containsKey("three"));
    }

    @Test
    void tailMapDoesNotIncludeKeysBeforeFromKeyWhenNullKeyPresent() {
        // Verify that a non‑null fromKey excludes the null key
        NavigableMap<String, Integer> tail = sealableMap.tailMap("one");
        assertFalse(tail.containsKey(null), "Tail map starting at a non‑null key must not contain the null key");
    }

    @ParameterizedTest(name = "tailMap({1}) on {0} returns {2}")
    @MethodSource("basicInclusionProvider")
    void tailMapBasicInclusion(NavigableMap<Integer, String> base, Number fromKeyNumber, Map<Integer, String> expected) {
        // Convert Number to appropriate key type (Integer)
        Integer fromKey = fromKeyNumber.intValue();
        SealableNavigableMap<Integer, String> map = createSealableMap(base);
        SortedMap<Integer, String> tail = map.tailMap(fromKey);
        assertEquals(expected, tail, () -> "Expected tailMap view to contain " + expected + " but was " + tail);
    }

    @Test
    void tailMapNullKeyThrowsNPE() {
        // TreeMap does not permit null keys – the contract of NavigableMap.tailMap
        // specifies a NullPointerException.
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "a");
        SealableNavigableMap<Integer, String> map = createSealableMap(base);
        assertThrows(NullPointerException.class, () -> map.tailMap(null), "Calling tailMap(null) on a map that disallows null keys must throw NPE");
    }

    @Test
    void tailMapOnEmptyMapReturnsEmptyView() {
        NavigableMap<Integer, String> empty = new TreeMap<>();
        SealableNavigableMap<Integer, String> map = createSealableMap(empty);
        SortedMap<Integer, String> tail = map.tailMap(0);
        assertTrue(tail.isEmpty(), "Tail map of an empty map must be empty");
    }

    @ParameterizedTest(name = "single‑entry tailMap({1}) → {2}")
    @MethodSource("singleEntryProvider")
    void tailMapSingleEntry(NavigableMap<Integer, String> base, Integer fromKey, Map<Integer, String> expected) {
        SealableNavigableMap<Integer, String> map = createSealableMap(base);
        SortedMap<Integer, String> tail = map.tailMap(fromKey);
        assertEquals(expected, tail);
    }

    @Test
    void tailMapWithDescendingComparator() {
        Comparator<Integer> descending = Comparator.reverseOrder();
        NavigableMap<Integer, String> descendingMap = new TreeMap<>(descending);
        descendingMap.put(1, "a");
        descendingMap.put(2, "b");
        descendingMap.put(3, "c");
        SealableNavigableMap<Integer, String> map = createSealableMap(descendingMap);
        // According to the descending order, keys >= 2 are 2 and 1
        SortedMap<Integer, String> tail = map.tailMap(2);
        Map<Integer, String> expected = new LinkedHashMap<>();
        expected.put(2, "b");
        expected.put(1, "a");
        assertEquals(expected, tail);
    }

    @Test
    void tailMapViewReflectsAdditions() {
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "a");
        base.put(2, "b");
        base.put(3, "c");
        SealableNavigableMap<Integer, String> map = createSealableMap(base);
        SortedMap<Integer, String> tail = map.tailMap(2);
        // add via view
        tail.put(4, "d");
        // Underlying map must now contain the new entry
        assertEquals("d", map.get(4));
        // The view must also contain it because 4 >= 2 in natural order
        assertTrue(tail.containsKey(4));
    }

    @Test
    void tailMapWithIncompatibleKeyTypeThrowsClassCastException() {
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "a");
        base.put(2, "b");
        SealableNavigableMap<Integer, String> map = createSealableMap(base);
        assertThrows(ClassCastException.class, () -> map.tailMap("stringKey"), "Supplying a key of the wrong type must raise ClassCastException");
    }

    @Test
    void tailMapIncludesEntriesWithNullValues() {
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, null);
        base.put(2, "b");
        base.put(3, "c");
        SealableNavigableMap<Integer, String> map = createSealableMap(base);
        SortedMap<Integer, String> tail = map.tailMap(1);
        Map<Integer, String> expected = Map.of(1, null, 2, "b", 3, "c");
        assertEquals(expected, tail);
    }

    @Test
    void tailMapPutWhenSealedThrowsUnsupportedOperationException() {
        sealed = true;
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "a");
        base.put(2, "b");
        SealableNavigableMap<Integer, String> map = createSealableMap(base);
        SortedMap<Integer, String> tail = map.tailMap(2);
        assertThrows(UnsupportedOperationException.class, () -> tail.put(3, "c"), "Mutating a tailMap view of a sealed map must throw");
    }

    @Test
    void tailMapRemoveWhenSealedThrowsUnsupportedOperationException() {
        sealed = true;
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "a");
        base.put(2, "b");
        SealableNavigableMap<Integer, String> map = createSealableMap(base);
        SortedMap<Integer, String> tail = map.tailMap(1);
        assertThrows(UnsupportedOperationException.class, () -> tail.remove(1), "Removing from a tailMap view of a sealed map must throw");
    }

    @Test
    void tailMapWithNonComparableKeyThrowsClassCastException() {
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "a");
        base.put(2, "b");
        SealableNavigableMap<Integer, String> map = createSealableMap(base);
        Object nonComparable = new Object();
        assertThrows(ClassCastException.class, () -> map.tailMap(nonComparable), "Providing a non‑Comparable key to a map without a Comparator must throw");
    }

    @Test
    void tailMapWithMutuallyIncomparableKeyThrowsClassCastException() {
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "a");
        base.put(2, "b");
        SealableNavigableMap<Integer, String> map = createSealableMap(base);
        Long longKey = 5L;
        assertThrows(ClassCastException.class, () -> map.tailMap(longKey), "Supplying a Long key to a map of Integer keys must raise ClassCastException");
    }

    @Test
    void tailMapWithExceptionThrowingComparatorPropagatesException() {
        Comparator<Integer> exploding = (a, b) -> {
            throw new RuntimeException("Comparator exploded");
        };
        NavigableMap<Integer, String> base = new TreeMap<>(exploding);
        // Populate via putAll to avoid comparator use during insertion
        base.putAll(Map.of(1, "a", 2, "b"));
        SealableNavigableMap<Integer, String> map = createSealableMap(base);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> map.tailMap(1), "The exception from the comparator must propagate");
        assertEquals("Comparator exploded", ex.getMessage());
    }
}
