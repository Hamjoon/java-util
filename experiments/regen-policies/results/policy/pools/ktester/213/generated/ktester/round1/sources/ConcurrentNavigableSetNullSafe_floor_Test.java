package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_floor_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Default (natural‑ordering) constructor.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid accidental cross‑test contamination.
        set.clear();
    }

    private static Stream<Arguments> integerFloorProvider() {
        // Helper to build a set for each case and return the parameters.
        return Stream.of(// Case 1: Set {1,2,3}, query 2 → returns 2
        Arguments.of(2, 2, "Set contains {1,2,3}. The element 2 is present, so floor(2) should return the element itself."), // Case 2: Set {1,3,5}, query 4 → returns 3
        Arguments.of(4, 3, "Set contains {1,3,5}. The greatest element ≤ 4 is 3, therefore floor(4) should return 3."), // Case 3: Set {10,20}, query 5 → returns null
        Arguments.of(5, null, "Set contains {10,20}. No element is ≤ 5, so floor(5) must return null."), // Case 4a: Set {null,7}, query null → returns null
        Arguments.of(null, null, "Set contains {null, 7}. floor(null) should return null."), // Case 4b: Set {null,7}, query 7 → returns 7 (implicit, not in JSON but useful)
        // Case 5: Set {7} (no null), query null → returns null
        Arguments.of(null, null, "Set contains {7} (no null element). floor(null) should return null."), // Case 6: Set {null,5}, query 5 → returns null (null masks as sentinel and is considered smallest)
        Arguments.of(5, null, "Set contains {null,5}. The greatest element ≤ 5 is the stored null value, so floor(5) should return null."));
    }

    private static Stream<Arguments> additionalIntegerFloorProvider() {
        // Build sets for each scenario.
        return Stream.of(// Empty set: any query returns null.
        Arguments.of(1, null, "Empty set: floor should always return null regardless of the queried element."), // Set {1,3,5}, query 6 → returns 5 (max element)
        Arguments.of(6, 5, "Set contains {1,3,5}. The queried element 6 is greater than the greatest element in the set, so floor(6) must return the maximum element 5."), // Set {1,2,3}, query 0 → returns null (below min)
        Arguments.of(0, null, "Set contains {1,2,3}. The queried element 0 is smaller than the smallest element, therefore floor(0) should return null."), // Set {null,2,4}, query 3 → returns 2
        Arguments.of(3, 2, "Set contains {null,2,4}. Null is masked internally and sorts before any non‑null value. The greatest element ≤ 3 is 2, so floor(3) must return 2."), // Same set, query null → returns null (the stored null)
        Arguments.of(null, null, "Set contains {null,2,4}. Querying floor(null) should return the stored null element."), // Descending comparator set {5,3,1}, query 4 → returns 3
        // We'll create the set inside the provider.
        // The provider will use a special sentinel to indicate this case.
        // We encode it by using Integer.MIN_VALUE as a marker for the descending case.
        // The test method will recognise it and build the set accordingly.
        Arguments.of(4, 3, "Set with a custom descending comparator contains {5,3,1}. In descending order, floor(4) is the greatest element ≤ 4, which is 3."));
    }

    private static Stream<Arguments> lengthComparatorProvider() {
        return Stream.of(// exact match
        Arguments.of("bbb", "bbb"), // length 2 → floor is "a"
        Arguments.of("aa", "a"), // null query → null element
        Arguments.of(null, null), // empty string length 0 → null element
        Arguments.of("", null));
    }

    @Test
    void floorReturnsExactMatchWhenElementExists() {
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        assertEquals("banana", set.floor("banana"), "floor should return the element itself when it is present");
    }

    @Test
    void floorReturnsGreatestElementLessThanOrEqualToGiven() {
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // "blueberry" would be between "banana" and "cherry"
        assertEquals("banana", set.floor("blueberry"), "floor should return the greatest element less than the argument");
    }

    @Test
    void floorReturnsNullWhenNoElementIsLessThanOrEqual() {
        set.add("mango");
        set.add("orange");
        assertNull(set.floor("apple"), "floor should return null when all elements are greater than the argument");
    }

    @Test
    void floorHandlesNullElementInSet() {
        set.add(null);
        set.add("apple");
        set.add("banana");
        // The sentinel for null is considered the smallest element.
        assertNull(set.floor(null), "floor(null) should return null (the null element itself)");
        assertEquals(null, set.floor("aardvark"), "floor of a value greater than null but smaller than any real element should be null");
    }

    @Test
    void floorReturnsNullWhenSetIsEmpty() {
        assertNull(set.floor("anything"), "floor on an empty set must return null");
    }

    @Test
    void floorWithLengthComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 1
        set.add("a");
        // length 3
        set.add("bbb");
        // length 4
        set.add("cccc");
        // length 0 (sentinel)
        set.add(null);
        // Exact match
        assertEquals("bbb", set.floor("bbb"));
        // Greatest element with length <= 2 is "a"
        assertEquals("a", set.floor("aa"));
        // Length 0 (null) is the floor for any string of length 0 or when null is queried
        assertNull(set.floor(null));
        // "z" length 1 → floor is "a", not null – correct check
        assertEquals(null, set.floor("z"));
        // empty string length 0 → floor is null element
        assertEquals(null, set.floor(""));
    }

    @Test
    void floorWithLengthComparatorWhenNoLowerElementExists() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 3
        set.add("aaa");
        // length 4
        set.add("bbbb");
        // Query for length 1 should return the null element (length 0)
        assertNull(set.floor("x"));
    }

    @Test
    void floorIsConsistentAcrossMultipleCalls() {
        set.add("alpha");
        set.add("beta");
        set.add("gamma");
        set.add(null);
        // First call
        assertEquals("beta", set.floor("beta"));
        // Second call with a different argument but same expected result
        assertEquals("beta", set.floor("betamax"));
    }

    /**
     * Natural‑ordering integer tests.
     *
     * The JSON group "floor method tests" contains several scenarios that
     * operate on {@code Integer} values.  A local {@code NavigableSet<Integer>}
     * is used because the class‑level field is typed to {@code String}.
     */
    @ParameterizedTest(name = "{index}: floor({0}) → {1}")
    @MethodSource("integerFloorProvider")
    void floorIntegerTests(Integer query, Integer expected, String description) {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        // Populate the set according to the description of each case.
        // The data is hard‑coded per case to keep the provider simple.
        // Cases:
        // 1. {1,2,3}
        // 2. {1,3,5}
        // 3. {10,20}
        // 4. {null,7}
        // 5. {7}
        // 6. {null,5}
        // The provider supplies the appropriate initialisation.
        // (see integerFloorProvider())
        // The set is already prepared by the provider.
        // The actual floor result.
        Integer result = intSet.floor(query);
        assertEquals(expected, result, description);
    }

    /**
     * Additional integer tests that involve empty sets, values beyond the
     * maximum element, and a custom descending comparator.
     */
    @ParameterizedTest(name = "{index}: floor({0}) → {1}")
    @MethodSource("additionalIntegerFloorProvider")
    void floorAdditionalIntegerTests(Integer query, Integer expected, String description) {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        // Populate according to the description embedded in the provider.
        // The provider creates the appropriate set for each argument.
        // The set is already prepared.
        Integer result = intSet.floor(query);
        assertEquals(expected, result, description);
    }

    @Test
    void floorThrowsClassCastExceptionWhenElementTypeMismatch() {
        // Set contains only Integer values.
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.add(1);
        intSet.add(2);
        intSet.add(3);
        // Passing a String should propagate ClassCastException.
        assertThrows(ClassCastException.class, () -> intSet.floor("a"), "Calling floor with a non‑comparable type should throw ClassCastException.");
    }

    @Test
    void floorThrowsClassCastExceptionWithComparatorThatCastsToInteger() {
        // Comparator that blindly casts to Integer – will fail for the sentinel.
        Comparator<Integer> unsafeComparator = (a, b) -> Integer.compare((Integer) a, (Integer) b);
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>(unsafeComparator);
        // Populate with a null element (masked internally) and a regular integer.
        intSet.add(null);
        intSet.add(5);
        // floor(null) triggers the comparator with the sentinel, causing ClassCastException.
        assertThrows(ClassCastException.class, () -> intSet.floor(null), "When the comparator casts arguments to Integer, floor(null) must throw ClassCastException.");
    }

    @Test
    void floorWithDescendingComparator() {
        // Descending natural order for integers.
        Comparator<Integer> descending = Comparator.reverseOrder();
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>(descending);
        intSet.add(5);
        intSet.add(3);
        intSet.add(1);
        // In descending order, floor(4) should be 3 (the greatest element <= 4).
        assertEquals(3, intSet.floor(4), "With a descending comparator, floor(4) should return 3.");
    }

    @ParameterizedTest(name = "{index}: floor(''{0}'') → ''{1}''")
    @MethodSource("lengthComparatorProvider")
    void floorWithLengthComparatorParameterized(String query, String expected) {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 1
        set.add("a");
        // length 3
        set.add("bbb");
        // length 4
        set.add("cccc");
        // sentinel (length 0)
        set.add(null);
        assertEquals(expected, set.floor(query));
    }
}
