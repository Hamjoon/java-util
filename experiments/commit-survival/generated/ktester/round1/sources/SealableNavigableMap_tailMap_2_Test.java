package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_tailMap_2_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start each test un‑sealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // a deterministic backing map (ConcurrentNavigableMapNullSafe is the default implementation)
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        backingMap.put("c", 3);
        backingMap.put("d", 4);
        backingMap.put("e", 5);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    /**
     * Data provider for basic functionality (inclusive true / false).
     */
    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// inclusive → includes 1
        Arguments.of(1, true, List.of(1, 2, 3, 4, 5)), // exclusive → skips 1
        Arguments.of(1, false, List.of(2, 3, 4, 5)));
    }

    @Test
    void tailMapInclusiveTrue_containsFromKey() {
        // inclusive = true → the returned view must contain the fromKey ("c")
        NavigableMap<String, Integer> tail = sealableMap.tailMap("c", true);
        assertTrue(tail.containsKey("c"), "Inclusive tailMap should contain the fromKey");
        assertEquals(3, tail.size(), "Tail map from 'c' inclusive should have three entries (c,d,e)");
        assertEquals(Integer.valueOf(3), tail.get("c"));
        assertEquals(Integer.valueOf(4), tail.get("d"));
        assertEquals(Integer.valueOf(5), tail.get("e"));
    }

    @Test
    void tailMapInclusiveFalse_excludesFromKey() {
        // inclusive = false → the returned view must *not* contain the fromKey ("c")
        NavigableMap<String, Integer> tail = sealableMap.tailMap("c", false);
        assertFalse(tail.containsKey("c"), "Exclusive tailMap must not contain the fromKey");
        assertEquals(2, tail.size(), "Tail map from 'c' exclusive should have two entries (d,e)");
        assertNull(tail.get("c"));
        assertEquals(Integer.valueOf(4), tail.get("d"));
        assertEquals(Integer.valueOf(5), tail.get("e"));
    }

    @Test
    void tailMapReflectsChangesInBackingMap() {
        NavigableMap<String, Integer> tail = sealableMap.tailMap("b", true);
        assertEquals(4, tail.size());
        // Add a new entry that belongs to the tail range
        sealableMap.put("bb", 22);
        assertTrue(tail.containsKey("bb"));
        assertEquals(Integer.valueOf(22), tail.get("bb"));
        // Remove an entry via the tail view and ensure the backing map sees it
        tail.remove("c");
        assertFalse(sealableMap.containsKey("c"));
        assertEquals(3, tail.size());
    }

    @Test
    void tailMapIsImmutableWhenSealed() {
        // Obtain a view before sealing
        NavigableMap<String, Integer> tail = sealableMap.tailMap("b", true);
        // now the map is sealed
        sealed = true;
        // Mutating the view must throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> tail.put("z", 26));
        assertThrows(UnsupportedOperationException.class, () -> tail.remove("b"));
        assertThrows(UnsupportedOperationException.class, tail::clear);
    }

    @Test
    void tailMapAllowsMutationsWhenUnsealed() {
        // Ensure the map is unsealed
        assertFalse(sealedSupplier.get());
        NavigableMap<String, Integer> tail = sealableMap.tailMap("b", true);
        tail.put("z", 26);
        assertEquals(Integer.valueOf(26), sealableMap.get("z"));
        assertTrue(tail.containsKey("z"));
    }

    @Test
    void tailMapThrowsWhenFromKeyIsNullAndComparatorDoesNotAllowNulls() {
        // The default comparator for String does not allow null keys.
        // Passing null should result in a NullPointerException (as per NavigableMap contract).
        assertThrows(NullPointerException.class, () -> sealableMap.tailMap(null, true));
    }

    @ParameterizedTest(name = "tailMap({0}, inclusive={1}) returns keys {2}")
    @MethodSource("basicFunctionalityProvider")
    void tailMapBasicFunctionality_IntegerKeys(int fromKey, boolean inclusive, List<Integer> expectedKeys) {
        // Set up an Integer‑keyed map with deterministic values.
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "one");
        base.put(2, "two");
        base.put(3, "three");
        base.put(4, "four");
        base.put(5, "five");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(base, () -> false);
        NavigableMap<Integer, String> tail = map.tailMap(fromKey, inclusive);
        // Verify size and content.
        assertEquals(expectedKeys.size(), tail.size(), "Tail map size should match expected number of entries");
        for (Integer k : expectedKeys) {
            assertTrue(tail.containsKey(k), "Tail map should contain key " + k);
        }
    }

    @Test
    void tailMapEmptyUnderlyingMap_returnsEmptyView() {
        NavigableMap<Integer, String> emptyBase = new TreeMap<>();
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(emptyBase, () -> false);
        NavigableMap<Integer, String> tail = map.tailMap(5, true);
        assertTrue(tail.isEmpty(), "Tail map of an empty backing map must be empty");
        assertEquals(0, tail.size());
    }

    @Test
    void tailMapFromKeyGreaterThanMaxKey_returnsEmptyView() {
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(10, "ten");
        base.put(20, "twenty");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(base, () -> false);
        // inclusive true
        NavigableMap<Integer, String> tailInc = map.tailMap(100, true);
        assertTrue(tailInc.isEmpty(), "Tail map with fromKey > max (inclusive) should be empty");
        // inclusive false
        NavigableMap<Integer, String> tailExc = map.tailMap(100, false);
        assertTrue(tailExc.isEmpty(), "Tail map with fromKey > max (exclusive) should be empty");
    }

    @Test
    void tailMapFromKeyEqualsMinKeyInclusiveFalse_excludesMinKey() {
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "one");
        base.put(2, "two");
        base.put(3, "three");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(base, () -> false);
        NavigableMap<Integer, String> tail = map.tailMap(1, false);
        assertFalse(tail.containsKey(1), "Exclusive tailMap must not contain the minimum key");
        assertEquals(List.of(2, 3), new ArrayList<>(tail.keySet()), "Tail map should contain only keys greater than the minimum");
    }

    @Test
    void tailMapSealStatePropagation_sameSupplierInstance() throws Exception {
        // Use the String‑keyed map created in @BeforeEach.
        NavigableMap<String, Integer> tail = sealableMap.tailMap("b", true);
        // Access the private sealedSupplier field via reflection.
        Field supplierField = SealableNavigableMap.class.getDeclaredField("sealedSupplier");
        supplierField.setAccessible(true);
        Supplier<?> returnedSupplier = (Supplier<?>) supplierField.get(tail);
        assertSame(sealedSupplier, returnedSupplier, "The tailMap view must retain the original sealedSupplier instance");
    }

    @Test
    void tailMapComparatorConsistency_sameComparator() {
        // TreeMap uses natural ordering for Integer.
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "one");
        base.put(2, "two");
        base.put(3, "three");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(base, () -> false);
        NavigableMap<Integer, String> tail = map.tailMap(2, true);
        // The comparator of the tail view should be the same as the original map's comparator.
        assertSame(base.comparator(), tail.comparator(), "Comparator of the tailMap should be identical to the original map's comparator");
    }

    @Test
    void tailMapIncompatibleKeyType_throwsClassCastException() {
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "one");
        base.put(2, "two");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(base, () -> false);
        // Passing a String where Integer is expected should cause ClassCastException.
        assertThrows(ClassCastException.class, () -> map.tailMap("stringKey", true));
        assertThrows(ClassCastException.class, () -> map.tailMap("stringKey", false));
    }

    @Test
    void tailMapComparatorRejectsKey_throwsIllegalArgumentException() {
        // Comparator that rejects negative keys by throwing IllegalArgumentException.
        Comparator<Integer> rejectNegatives = (a, b) -> {
            if (a < 0 || b < 0) {
                throw new IllegalArgumentException("Negative keys are not allowed");
            }
            return Integer.compare(a, b);
        };
        NavigableMap<Integer, String> base = new TreeMap<>(rejectNegatives);
        base.put(1, "one");
        base.put(2, "two");
        base.put(3, "three");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(base, () -> false);
        // The comparator is invoked during tailMap creation; negative fromKey triggers exception.
        assertThrows(IllegalArgumentException.class, () -> map.tailMap(-5, true));
        assertThrows(IllegalArgumentException.class, () -> map.tailMap(-10, false));
    }

    @Test
    void tailMapNullKeyWhenComparatorAllowsNulls_returnsEmptyOrThrowsAccordingToMap() {
        // TreeMap with a comparator that permits null (custom comparator).
        Comparator<String> nullFriendly = Comparator.nullsFirst(String::compareTo);
        NavigableMap<String, Integer> base = new TreeMap<>(nullFriendly);
        base.put("a", 1);
        base.put("b", 2);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(base, () -> false);
        // Null key is allowed; tailMap(null, true) should return the whole map.
        NavigableMap<String, Integer> tail = map.tailMap(null, true);
        assertEquals(2, tail.size(), "Tail map with null fromKey should contain all entries when comparator permits nulls");
    }
}
