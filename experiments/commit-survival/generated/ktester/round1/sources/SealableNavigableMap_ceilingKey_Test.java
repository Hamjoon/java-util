package com.cedarsoftware.util;

import java.util.NavigableMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.TreeMap;
import java.util.Comparator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_ceilingKey_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start each test un‑sealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // a simple mutable backing map (the concrete class used in the original tests)
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        // allow a null entry – the underlying map implementation permits it
        backingMap.put(null, null);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    // ---------- Normal operation with existing integer keys ----------
    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(arguments(2, 2, "Exact match"), arguments(0, 1, "Smaller than smallest"), arguments(2.5, 3, "Between two keys"), arguments(5, null, "Greater than greatest"));
    }

    // ---------- Single element map ----------
    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(arguments(5, 5, "Exact match"), arguments(3, 5, "Less than the only key"), arguments(7, null, "Greater than the only key"));
    }

    // ---------- Negative and zero keys ----------
    private static Stream<Arguments> negativeZeroProvider() {
        return Stream.of(arguments(-7, -5, "Between -10 and -5"), arguments(-10, -10, "Exact smallest"), arguments(-11, -10, "Smaller than any key"), arguments(6, null, "Larger than greatest"));
    }

    // ---------- Custom comparator (reverse order) ----------
    private static Stream<Arguments> reverseComparatorProvider() {
        return Stream.of(arguments(2, 2, "Exact match in reverse order"), arguments(2.5, 3, "Between keys in reverse order"), arguments(0, 1, "Lowest key >= 0"), arguments(4, null, "No key >= 4"));
    }

    // ---------- Comparator by string length ----------
    private static Stream<Arguments> lengthComparatorProvider() {
        return Stream.of(arguments("dd", "bb", "Length 2 matches 'bb'"), arguments("eeee", null, "Length 4 exceeds any key length"), arguments("", "a", "Empty string matches smallest key"));
    }

    // ---------- Boundary integer values ----------
    private static Stream<Arguments> boundaryIntegerProvider() {
        return Stream.of(arguments(2147483645, 2147483646, "Just below MAX-1"), arguments(2147483647, 2147483647, "Exact MAX"), arguments(2147483648L, null, "Beyond Integer.MAX_VALUE"));
    }

    @Test
    void ceilingKey_returnsExactMatchWhenPresent() {
        assertEquals("one", sealableMap.ceilingKey("one"));
        assertEquals("two", sealableMap.ceilingKey("two"));
        assertEquals("three", sealableMap.ceilingKey("three"));
    }

    @Test
    void ceilingKey_returnsNextHigherKeyWhenExactKeyMissing() {
        // "one-and-a-half" lies between "one" and "two"
        assertEquals("two", sealableMap.ceilingKey("one-and-a-half"));
        // "zero" is before the first (non‑null) key, so the first key should be returned
        assertEquals("one", sealableMap.ceilingKey("zero"));
        // a key greater than any existing key yields null
        assertNull(sealableMap.ceilingKey("zzz"));
    }

    @Test
    void ceilingKey_handlesNullKey() {
        // The backing map contains a null key; ceilingKey(null) should return null
        assertNull(sealableMap.ceilingKey(null));
    }

    @Test
    void ceilingKey_worksWhenMapIsSealed() {
        // switch to sealed (read‑only) mode
        sealed = true;
        // read‑only operations must still function
        assertEquals("two", sealableMap.ceilingKey("one-and-a-half"));
        assertEquals("one", sealableMap.ceilingKey("zero"));
        assertNull(sealableMap.ceilingKey("zzz"));
    }

    @ParameterizedTest(name = "[{index}] key={0} → expected={2} ({3})")
    @MethodSource("normalOperationProvider")
    void ceilingKey_normalOperation(Number keyNumber, Integer expected) {
        // Build a fresh integer‑key map for this group
        NavigableMap<Integer, String> map = new TreeMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        map.put(4, "d");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, () -> false);
        // TreeMap expects a non‑null key; null case is handled separately
        Integer key = keyNumber == null ? null : keyNumber.intValue();
        assertEquals(expected, sut.ceilingKey(key));
    }

    // ---------- Edge cases ----------
    @Test
    void ceilingKey_nullKeyOnTreeMapThrowsNPE() {
        NavigableMap<Integer, String> map = new TreeMap<>();
        map.put(1, "a");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, () -> false);
        assertThrows(NullPointerException.class, () -> sut.ceilingKey(null));
    }

    @Test
    void ceilingKey_wrongKeyTypeThrowsClassCastException() {
        NavigableMap<Integer, String> map = new TreeMap<>();
        map.put(1, "a");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, () -> false);
        assertThrows(ClassCastException.class, () -> sut.ceilingKey("string"));
    }

    @Test
    void ceilingKey_emptyMapReturnsNull() {
        NavigableMap<Integer, String> empty = new TreeMap<>();
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(empty, () -> false);
        assertNull(sut.ceilingKey(10));
    }

    @ParameterizedTest(name = "[{index}] key={0} → expected={2}")
    @MethodSource("singleElementProvider")
    void ceilingKey_singleElementMap(Integer key, Integer expected) {
        NavigableMap<Integer, String> map = new TreeMap<>();
        map.put(5, "five");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, () -> false);
        assertEquals(expected, sut.ceilingKey(key));
    }

    @ParameterizedTest(name = "[{index}] key={0} → expected={2}")
    @MethodSource("negativeZeroProvider")
    void ceilingKey_negativeAndZeroKeys(Integer key, Integer expected) {
        NavigableMap<Integer, String> map = new TreeMap<>();
        map.put(-10, "n10");
        map.put(-5, "n5");
        map.put(0, "z");
        map.put(5, "p5");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, () -> false);
        assertEquals(expected, sut.ceilingKey(key));
    }

    @ParameterizedTest(name = "[{index}] key={0} → expected={2}")
    @MethodSource("reverseComparatorProvider")
    void ceilingKey_reverseComparator(Number keyNumber, Integer expected) {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        NavigableMap<Integer, String> map = new TreeMap<>(reverse);
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, () -> false);
        Integer key = keyNumber.intValue();
        assertEquals(expected, sut.ceilingKey(key));
    }

    @ParameterizedTest(name = "[{index}] key=\"{0}\" → expected=\"{1}\"")
    @MethodSource("lengthComparatorProvider")
    void ceilingKey_stringLengthComparator(String key, String expected) {
        Comparator<String> lengthComp = Comparator.comparingInt(String::length);
        NavigableMap<String, Integer> map = new TreeMap<>(lengthComp);
        map.put("a", 1);
        map.put("bb", 2);
        map.put("ccc", 3);
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(map, () -> false);
        assertEquals(expected, sut.ceilingKey(key));
    }

    @ParameterizedTest(name = "[{index}] key={0} → expected={2}")
    @MethodSource("boundaryIntegerProvider")
    void ceilingKey_boundaryIntegerValues(Number keyNumber, Integer expected) {
        NavigableMap<Integer, String> map = new TreeMap<>();
        map.put(2147483646, "a");
        map.put(2147483647, "b");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, () -> false);
        // Long key will cause ClassCastException, handled separately
        Integer key = (keyNumber instanceof Long) ? null : keyNumber.intValue();
        if (keyNumber instanceof Long) {
            // Expect ClassCastException for a Long key (handled in separate test)
            assertThrows(ClassCastException.class, () -> sut.ceilingKey(keyNumber));
        } else {
            assertEquals(expected, sut.ceilingKey(key));
        }
    }

    // ---------- Type mismatch with Long key ----------
    @Test
    void ceilingKey_longKeyThrowsClassCastException() {
        NavigableMap<Integer, String> map = new TreeMap<>();
        map.put(1, "a");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, () -> false);
        assertThrows(ClassCastException.class, () -> sut.ceilingKey(2L));
    }

    // ---------- Comparator that throws IllegalArgumentException ----------
    @Test
    void ceilingKey_comparatorThrowsIllegalArgumentException() {
        Comparator<Integer> badComparator = (a, b) -> {
            if (a < 0 || b < 0) {
                throw new IllegalArgumentException("Negative keys not allowed");
            }
            return Integer.compare(a, b);
        };
        NavigableMap<Integer, String> map = new TreeMap<>(badComparator);
        map.put(1, "a");
        map.put(2, "b");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, () -> false);
        assertThrows(IllegalArgumentException.class, () -> sut.ceilingKey(-1));
    }

    // ---------- Non‑Comparable key ----------
    @Test
    void ceilingKey_nonComparableKeyThrowsClassCastException() {
        NavigableMap<Integer, String> map = new TreeMap<>();
        map.put(1, "a");
        SealableNavigableMap<Integer, String> sut = new SealableNavigableMap<>(map, () -> false);
        Object nonComparable = new Object();
        assertThrows(ClassCastException.class, () -> sut.ceilingKey(nonComparable));
    }
}
