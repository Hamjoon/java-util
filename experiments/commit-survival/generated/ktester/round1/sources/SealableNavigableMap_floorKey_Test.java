package com.cedarsoftware.util;

import java.util.NavigableMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_floorKey_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start each test with the map un‑sealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // use the same concrete backing map that the production tests use
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        // the underlying map permits a null entry
        backingMap.put(null, null);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test tidy
        backingMap.clear();
        sealed = false;
    }

    /**
     * Provides numeric test data matching the first JSON group.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>the key to query (Integer, may be {@code null})</li>
     *   <li>the expected result (Integer or {@code null})</li>
     *   <li>a description (used only for readability)</li>
     * </ul>
     */
    private static Stream<Arguments> numericFloorKeyCases() {
        return Stream.of(Arguments.of(null, null, "Passing null should throw NullPointerException as per NavigableMap contract."), Arguments.of(5, null, "Key 5 is less than the smallest key (10) → null"), Arguments.of(10, 10, "Key 10 exists → returns 10"), Arguments.of(15, 10, "Key 15 between 10 and 20 → returns 10"), Arguments.of(20, 20, "Key 20 exists → returns 20"), Arguments.of(25, 20, "Key 25 greater than max (20) → returns 20"));
    }

    @Test
    void floorKey_returnsExactKeyWhenPresent() {
        assertEquals("one", sealableMap.floorKey("one"));
        assertEquals("two", sealableMap.floorKey("two"));
        assertEquals("three", sealableMap.floorKey("three"));
    }

    @Test
    void floorKey_returnsGreatestKeyLessThanOrEqualToGivenKey() {
        // "two-and-half" is lexicographically after "two" but before "three"
        assertEquals("two", sealableMap.floorKey("two-and-half"));
        // "zero" is before the first key ("one")
        assertNull(sealableMap.floorKey("zero"));
    }

    @Test
    void floorKey_handlesNullKey() {
        // The backing map contains a null key; floorKey(null) should return null
        assertNull(sealableMap.floorKey(null));
    }

    @Test
    void floorKey_behaviourWhenMapIsEmpty() {
        // Create an empty SealableNavigableMap
        NavigableMap<String, Integer> emptyBacking = new ConcurrentNavigableMapNullSafe<>();
        SealableNavigableMap<String, Integer> emptyMap = new SealableNavigableMap<>(emptyBacking, sealedSupplier);
        assertNull(emptyMap.floorKey("any"));
        assertNull(emptyMap.floorKey(null));
    }

    @Test
    void floorKey_isUnaffectedBySealedState() {
        // Verify read‑only operation works even when the map is sealed
        sealed = true;
        assertEquals("two", sealableMap.floorKey("two-and-half"));
        assertNull(sealableMap.floorKey("zero"));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("numericFloorKeyCases")
    void floorKey_numericScenarios(Integer queryKey, Integer expected, String description) {
        // Build a backing map with natural ordering: {10= "a", 20= "b"}
        NavigableMap<Integer, String> intMap = new TreeMap<>();
        intMap.put(10, "a");
        intMap.put(20, "b");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(intMap, () -> false);
        if (queryKey == null) {
            // The contract of TreeMap.floorKey(null) throws NPE
            assertThrows(NullPointerException.class, () -> map.floorKey(null), "Expected NullPointerException for null key");
        } else {
            assertEquals(expected, map.floorKey(queryKey), "floorKey(" + queryKey + ") should return " + expected);
        }
    }

    @Test
    void floorKey_emptyMapAlwaysReturnsNull() {
        NavigableMap<Integer, String> empty = new TreeMap<>();
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(empty, () -> false);
        assertNull(map.floorKey(5), "Empty map should return null for any key");
    }

    @Test
    void floorKey_singleElementMap_behaviour() {
        NavigableMap<Integer, String> single = new TreeMap<>();
        single.put(10, "x");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(single, () -> false);
        assertNull(map.floorKey(5), "Key less than sole entry should return null");
        assertEquals(10, map.floorKey(10), "Exact match should return the key itself");
        assertEquals(10, map.floorKey(15), "Key greater than sole entry should return that entry");
    }

    @Test
    void floorKey_reverseOrderComparator() {
        // descending order
        Comparator<Integer> reverse = Comparator.reverseOrder();
        NavigableMap<Integer, String> revMap = new TreeMap<>(reverse);
        revMap.put(20, "high");
        revMap.put(10, "low");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(revMap, () -> false);
        // In descending order, 20 is the *lowest* element.
        assertEquals(20, map.floorKey(15), "With reverse comparator, floorKey(15) should return 20");
    }

    @Test
    void floorKey_caseInsensitiveStringComparator() {
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;
        NavigableMap<String, Integer> ciMap = new TreeMap<>(caseInsensitive);
        ciMap.put("A", 1);
        ciMap.put("B", 2);
        ciMap.put("C", 3);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(ciMap, () -> false);
        assertEquals("B", map.floorKey("b"), "Case‑insensitive floorKey should ignore case");
    }

    @Test
    void floorKey_typeMismatchThrowsClassCastException() {
        NavigableMap<Integer, String> intMap = new TreeMap<>();
        intMap.put(1, "one");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(intMap, () -> false);
        assertThrows(ClassCastException.class, () -> map.floorKey("string"), "Calling floorKey with a non‑comparable type should throw ClassCastException");
    }

    @Test
    void floorKey_comparatorRejectsNull_throwsNullPointerException() {
        Comparator<Integer> nullRejecting = (o1, o2) -> {
            if (o1 == null || o2 == null) {
                throw new NullPointerException("Null keys not allowed");
            }
            return Integer.compare(o1, o2);
        };
        NavigableMap<Integer, String> mapWithNullReject = new TreeMap<>(nullRejecting);
        mapWithNullReject.put(10, "ten");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(mapWithNullReject, () -> false);
        assertThrows(NullPointerException.class, () -> map.floorKey(null), "Custom comparator should propagate NullPointerException for null key");
    }

    @Test
    void floorKey_comparatorThrowsIllegalArgumentException() {
        Comparator<Integer> illegalFor13 = (o1, o2) -> {
            if (Integer.valueOf(13).equals(o1) || Integer.valueOf(13).equals(o2)) {
                throw new IllegalArgumentException("Key 13 is illegal");
            }
            return Integer.compare(o1, o2);
        };
        NavigableMap<Integer, String> mapWithIllegal = new TreeMap<>(illegalFor13);
        mapWithIllegal.put(10, "ten");
        mapWithIllegal.put(20, "twenty");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(mapWithIllegal, () -> false);
        // The exception should be thrown when the comparator is invoked for key 13
        assertThrows(IllegalArgumentException.class, () -> map.floorKey(13), "Comparator should propagate IllegalArgumentException for key 13");
    }
}
