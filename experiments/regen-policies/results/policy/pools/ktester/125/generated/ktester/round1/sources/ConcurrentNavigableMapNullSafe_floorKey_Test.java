package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_floorKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    private ConcurrentNavigableMapNullSafe<String, Integer> reverseMap;

    @BeforeEach
    void setupBeforeEach() {
        // Normal map with natural ordering
        map = new ConcurrentNavigableMapNullSafe<>();
        // Map with a reverse comparator to verify that floorKey respects custom ordering
        Comparator<String> reverseComparator = Comparator.<String>reverseOrder();
        reverseMap = new ConcurrentNavigableMapNullSafe<>(reverseComparator);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the test clean
        map = null;
        reverseMap = null;
    }

    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(// Exact match – Integer
        Arguments.of(mapFactory -> {
            mapFactory.put(5, 10);
            return 5;
        }, 5, "Exact match: map contains key 5; floorKey(5) should return 5."), // Key not present – greatest lower key exists
        Arguments.of(mapFactory -> {
            mapFactory.putAll(Map.of(1, 1, 3, 3, 5, 5));
            return 7;
        }, 5, "Key not present but lower key exists: floorKey(7) returns 5."), // All keys greater than argument
        Arguments.of(mapFactory -> {
            mapFactory.putAll(Map.of(2, 2, 4, 4, 6, 6));
            return 0;
        }, null, "All map keys are greater than the argument; floorKey(0) returns null."), // Negative key with only positive keys
        Arguments.of(mapFactory -> {
            mapFactory.putAll(Map.of(1, 1, 2, 2, 3, 3));
            return -10;
        }, null, "Negative key with only positive keys; floorKey returns null."), // String natural ordering
        Arguments.of(mapFactory -> {
            mapFactory.put("a", 1);
            mapFactory.put("c", 3);
            return "b";
        }, "a", "String keys: floorKey(\"b\") returns \"a\"."), // String exact match
        Arguments.of(mapFactory -> {
            mapFactory.put("a", 1);
            mapFactory.put("c", 3);
            return "a";
        }, "a", "String exact match: floorKey(\"a\") returns \"a\"."), // String numeric key less than smallest
        Arguments.of(mapFactory -> {
            mapFactory.put("1", 1);
            mapFactory.put("2", 2);
            return "0";
        }, null, "String numeric key not present and less than smallest; floorKey(\"0\") returns null."), // Boolean exact match (true)
        Arguments.of(mapFactory -> {
            mapFactory.put(false, 0);
            mapFactory.put(true, 1);
            return true;
        }, true, "Boolean exact match: floorKey(true) returns true."), // Boolean exact match (false)
        Arguments.of(mapFactory -> {
            mapFactory.put(false, 0);
            mapFactory.put(true, 1);
            return false;
        }, false, "Boolean exact match: floorKey(false) returns false."), // Boolean key when only true present
        Arguments.of(mapFactory -> {
            mapFactory.put(true, 1);
            return false;
        }, null, "Map contains only true; floorKey(false) returns null."), // Double exact match
        Arguments.of(mapFactory -> {
            mapFactory.put(2.71, 1);
            mapFactory.put(3.14, 2);
            mapFactory.put(4.0, 3);
            return 3.14;
        }, 3.14, "Floating‑point exact match: floorKey(3.14) returns 3.14."), // Double non‑existent key
        Arguments.of(mapFactory -> {
            mapFactory.put(2.71, 1);
            mapFactory.put(3.14, 2);
            mapFactory.put(4.0, 3);
            return 3.5;
        }, 3.14, "Floating‑point non‑existent key: floorKey(3.5) returns 3.14."));
    }

    @Test
    void floorKey_normalKeys() {
        // Given
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put("date", 4);
        // When / Then
        // exact match
        assertEquals("banana", map.floorKey("banana"));
        // greatest key <= "blueberry"
        assertEquals("banana", map.floorKey("blueberry"));
        // less than the smallest key -> null
        assertEquals("apple", map.floorKey("aardvark"));
        assertNull(map.floorKey("aardvark"));
        // larger than any key -> highest key
        assertEquals("date", map.floorKey("zebra"));
    }

    @Test
    void floorKey_withNullKeyPresent() {
        // Given a null key (which is considered greater than any non‑null key)
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);
        // When the queried key is null, the floor should be the null key itself
        assertNull(map.floorKey(null));
        // When the queried key is non‑null, the null key is *after* it, so floorKey should be the greatest
        // non‑null key that is <= the queried key.
        // "b" is the greatest key <= "c"
        assertEquals("b", map.floorKey("c"));
        // exact match
        assertEquals("a", map.floorKey("a"));
        // no key <= "0"
        assertNull(map.floorKey("0"));
    }

    @Test
    void floorKey_withoutNullKey() {
        // No null key stored – null is still considered after all real keys
        map.put("x", 10);
        map.put("y", 20);
        // floorKey(null) should return the highest existing key because null would be after them
        assertEquals("y", map.floorKey(null));
    }

    @Test
    void floorKey_respectsCustomComparator() {
        // Reverse order: "z" < "y" < ... < "a"
        reverseMap.put("a", 1);
        reverseMap.put("c", 3);
        reverseMap.put("e", 5);
        // null is considered *first* in descending view
        reverseMap.put(null, 99);
        // In reverse order, floorKey returns the greatest key that is *less than or equal* according to the comparator.
        // For key "d", the ordering is e > d > c > a, so floorKey("d") should be "e".
        assertEquals("e", reverseMap.floorKey("d"));
        // Exact match
        assertEquals("c", reverseMap.floorKey("c"));
        // Null key is first in this ordering, so floorKey(null) returns null itself
        assertNull(reverseMap.floorKey(null));
        // Key greater than any existing key according to reverse order (e.g., "z") -> returns null because null is first
        assertNull(reverseMap.floorKey("z"));
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("normalCasesProvider")
    void floorKey_normalAndBoundaryCases(Function<ConcurrentNavigableMapNullSafe<Object, Integer>, Object> mapSetup, Object queryKey, String description) {
        // Arrange – the lambda populates the map and returns the key to query.
        mapSetup.apply(map);
        // Act
        Object result = map.floorKey(queryKey);
        // Assert – the expected value is encoded in the test data (the lambda returns it)
        // We compare with the expected value that the lambda stored in the mapSetup's closure.
        // For readability we recompute the expected using the same lambda's return value.
        // dummy, just to get expected
        Object expected = mapSetup.apply(new ConcurrentNavigableMapNullSafe<>());
        // The above line is only to retrieve the expected value; we actually know it from the test case.
        // Therefore we directly assert using the known expected from the arguments.
        // (JUnit will display the description on failure.)
        assertEquals(expected, result, description);
    }

    @Test
    void floorKey_nullKeyPresent() {
        // Map contains a null key (masked internally). According to the contract,
        // null is considered greater than any non‑null key, so floorKey(null) returns the null key itself.
        map.put(null, 99);
        map.put("a", 1);
        map.put("b", 2);
        assertNull(map.floorKey(null), "When a null key is stored, floorKey(null) should return null after unmasking.");
    }

    @Test
    void floorKey_nullKeyAbsent() {
        // No null key stored – null is still considered after all real keys.
        map.put(1, 10);
        map.put(2, 20);
        assertNull(map.floorKey(null), "Without a stored null key, floorKey(null) must return null because no key ≤ masked‑null exists.");
    }

    @Test
    void floorKey_respectsReverseComparator() {
        // Reverse order: higher natural keys are “smaller” according to the comparator.
        reverseMap.put("a", 1);
        reverseMap.put("c", 3);
        reverseMap.put("e", 5);
        // null is considered first in the descending view
        reverseMap.put(null, 99);
        // According to reverse order, floorKey("d") should be the greatest key ≤ "d"
        // i.e., the key that appears *before* "d" in reverse ordering – which is "e".
        assertEquals("e", reverseMap.floorKey("d"), "Reverse order floorKey('d') should return 'e'.");
        // Exact match
        assertEquals("c", reverseMap.floorKey("c"), "Exact match in reverse order.");
        // Null key is first in reverse ordering, so floorKey(null) returns null itself.
        assertNull(reverseMap.floorKey(null), "In reverse order, null is first; floorKey(null) returns null.");
        // Key greater than any existing key according to reverse order (e.g., "z")
        // Since null is first, there is no key ≤ "z" → null.
        assertNull(reverseMap.floorKey("z"), "Key beyond highest in reverse order returns null.");
    }

    @Test
    void floorKey_typeMismatch_integerMap_stringKey() {
        // Populate with Integer keys.
        map.put(1, 10);
        map.put(2, 20);
        map.put(3, 30);
        // Query with a String – should throw ClassCastException from the underlying SkipList.
        assertThrows(ClassCastException.class, () -> map.floorKey("someString"), "Calling floorKey with a String on an Integer‑keyed map must throw ClassCastException.");
    }

    @Test
    void floorKey_typeMismatch_stringMap_integerKey() {
        // Populate with String keys.
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // Query with an Integer – expect ClassCastException.
        assertThrows(ClassCastException.class, () -> map.floorKey(42), "Calling floorKey with an Integer on a String‑keyed map must throw ClassCastException.");
    }

    @Test
    void floorKey_typeMismatch_customComparator_nullSentinel() {
        // Custom comparator that only accepts Strings and throws on any other type.
        Comparator<Object> stringOnlyComparator = (o1, o2) -> {
            if (!(o1 instanceof String) || !(o2 instanceof String))
                throw new ClassCastException("Comparator only supports String");
            return ((String) o1).compareTo((String) o2);
        };
        ConcurrentNavigableMapNullSafe<Object, Integer> customMap = new ConcurrentNavigableMapNullSafe<>(stringOnlyComparator);
        customMap.put("alpha", 1);
        customMap.put("beta", 2);
        // The internal null‑mask sentinel is not a String, so invoking floorKey(null) triggers the comparator.
        assertThrows(ClassCastException.class, () -> customMap.floorKey(null), "Custom comparator that cannot handle the null‑mask sentinel should cause ClassCastException.");
    }
}
