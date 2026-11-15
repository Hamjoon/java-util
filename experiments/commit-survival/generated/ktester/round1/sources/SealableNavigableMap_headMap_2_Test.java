package com.cedarsoftware.util;

import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_headMap_2_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    /**
     * Helper comparator that can compare any {@link Number} by its double value.
     * This allows us to mix Integer and Double keys in the same map, which is
     * required for the 2.5 test cases.
     */
    private static final Comparator<Number> NUMBER_COMPARATOR = Comparator.comparingDouble(Number::doubleValue);

    @BeforeEach
    void setupBeforeEach() {
        sealed = false;
        sealedSupplier = () -> sealed;
        // Use a simple, deterministic backing map (no null keys to avoid NPEs in ordering)
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        backingMap.put("c", 3);
        backingMap.put("d", 4);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    /**
     * Creates a fresh {@link SealableNavigableMap} whose backing map contains
     * the integer keys 1, 2 and 3 (as {@link Integer}) and uses the number
     * comparator defined above.
     */
    private static SealableNavigableMap<Number, Integer> createIntegerKeyMap(Supplier<Boolean> sealedSupplier) {
        NavigableMap<Number, Integer> backing = new TreeMap<>(NUMBER_COMPARATOR);
        backing.put(1, 1);
        backing.put(2, 2);
        backing.put(3, 3);
        return new SealableNavigableMap<>(backing, sealedSupplier);
    }

    static Stream<Arguments> inclusiveTrueCases() {
        return Stream.of(// toKey = 2 (existing key), inclusive = true => [1,2]
        Arguments.of(2, true, List.of(1, 2)), // toKey = 2.5 (between 2 and 3), inclusive = true => [1,2]
        Arguments.of(2.5, true, List.of(1, 2)), // toKey = 0 (smaller than smallest), inclusive = true => []
        Arguments.of(0, true, List.of()), // toKey = 5 (greater than greatest), inclusive = true => [1,2,3]
        Arguments.of(5, true, List.of(1, 2, 3)));
    }

    static Stream<Arguments> inclusiveFalseCases() {
        return Stream.of(// toKey = 2 (existing key), inclusive = false => [1]
        Arguments.of(2, false, List.of(1)), // toKey = 2.5 (between 2 and 3), inclusive = false => [1,2]
        Arguments.of(2.5, false, List.of(1, 2)), // toKey = 0 (smaller than smallest), inclusive = false => []
        Arguments.of(0, false, List.of()), // toKey = 5 (greater than greatest), inclusive = false => [1,2,3]
        Arguments.of(5, false, List.of(1, 2, 3)));
    }

    @Test
    void headMapExclusive_excludesToKey() {
        SortedMap<String, Integer> head = sealableMap.headMap("c", false);
        assertEquals(2, head.size(), "Exclusive headMap should contain two entries");
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertFalse(head.containsKey("c"));
        assertFalse(head.containsKey("d"));
    }

    @Test
    void headMapInclusive_includesToKey() {
        SortedMap<String, Integer> head = sealableMap.headMap("c", true);
        assertEquals(3, head.size(), "Inclusive headMap should contain three entries");
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertTrue(head.containsKey("c"));
        assertFalse(head.containsKey("d"));
    }

    @Test
    void headMapMutationsWhenUnsealedAreReflectedInOriginalMap() {
        SortedMap<String, Integer> head = sealableMap.headMap("c", false);
        assertFalse(sealedSupplier.get(), "Map should start unsealed");
        // Adding a key that is within the view range
        head.put("b2", 22);
        assertEquals(Integer.valueOf(22), sealableMap.get("b2"));
        assertTrue(sealableMap.keySet().contains("b2"));
        // Removing a key that is within the view range
        head.remove("a");
        assertNull(sealableMap.get("a"));
        assertFalse(sealableMap.keySet().contains("a"));
    }

    @Test
    void headMapMutationsWhenSealedThrowUnsupportedOperationException() {
        SortedMap<String, Integer> head = sealableMap.headMap("c", true);
        // seal the map
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> head.put("x", 99));
        assertThrows(UnsupportedOperationException.class, () -> head.remove("b"));
        assertThrows(UnsupportedOperationException.class, head::clear);
    }

    @Test
    void headMapDoesNotAllowOutOfRangeInsertions() {
        SortedMap<String, Integer> head = sealableMap.headMap("c", false);
        // "d" is outside the exclusive range; should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> head.put("d", 4));
        // "c" is also outside the exclusive range; should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> head.put("c", 3));
    }

    @ParameterizedTest(name = "inclusive=true : toKey={0} → expected={2}")
    @MethodSource("inclusiveTrueCases")
    void headMap_InclusiveTrue_VariousKeys(Object toKeyObj, boolean inclusive, List<Integer> expected) {
        // Arrange
        Supplier<Boolean> sealedSupplier = () -> false;
        SealableNavigableMap<Number, Integer> map = createIntegerKeyMap(sealedSupplier);
        // safe cast – test data supplies Number
        Number toKey = (Number) toKeyObj;
        // Act
        SortedMap<Number, Integer> head = map.headMap(toKey, inclusive);
        // Assert
        List<Integer> actual = new ArrayList<>();
        for (Number k : head.keySet()) {
            // keys are 1,2,3 – we compare their int values
            actual.add(k.intValue());
        }
        assertEquals(expected, actual, () -> "headMap(toKey=" + toKey + ", inclusive=true) returned wrong key set");
    }

    @ParameterizedTest(name = "inclusive=false : toKey={0} → expected={2}")
    @MethodSource("inclusiveFalseCases")
    void headMap_InclusiveFalse_VariousKeys(Object toKeyObj, boolean inclusive, List<Integer> expected) {
        // Arrange
        Supplier<Boolean> sealedSupplier = () -> false;
        SealableNavigableMap<Number, Integer> map = createIntegerKeyMap(sealedSupplier);
        Number toKey = (Number) toKeyObj;
        // Act
        SortedMap<Number, Integer> head = map.headMap(toKey, inclusive);
        // Assert
        List<Integer> actual = new ArrayList<>();
        for (Number k : head.keySet()) {
            actual.add(k.intValue());
        }
        assertEquals(expected, actual, () -> "headMap(toKey=" + toKey + ", inclusive=false) returned wrong key set");
    }

    @Test
    void headMap_NullKey_ThrowsNullPointerException() {
        Supplier<Boolean> sealedSupplier = () -> false;
        // Backing map that does NOT allow null keys (TreeMap throws NPE on null)
        NavigableMap<Integer, Integer> backing = new TreeMap<>();
        backing.put(1, 1);
        backing.put(2, 2);
        SealableNavigableMap<Integer, Integer> map = new SealableNavigableMap<>(backing, sealedSupplier);
        assertThrows(NullPointerException.class, () -> map.headMap(null, true));
    }

    @Test
    void headMap_IncompatibleKeyType_ThrowsClassCastException() {
        Supplier<Boolean> sealedSupplier = () -> false;
        // Integer‑keyed map
        NavigableMap<Integer, Integer> backing = new TreeMap<>();
        backing.put(1, 1);
        backing.put(2, 2);
        SealableNavigableMap<Integer, Integer> map = new SealableNavigableMap<>(backing, sealedSupplier);
        // Passing a String where an Integer is expected
        assertThrows(ClassCastException.class, () -> map.headMap("string", false));
    }
}
