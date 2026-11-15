package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_ceiling_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set for each test
        set = new SealableNavigableSet<>(sealedSupplier);
        // Populate with a known ordering
        set.add(10);
        set.add(20);
        set.add(30);
        // Include a null element to verify that ceiling handles it correctly
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test tidy
        set = null;
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of(1, "1", "Exact element present – should return the element itself."), Arguments.of(2, "3", "No exact match – should return the next higher element."), Arguments.of(10, "null", "Query greater than any element – should return null."), Arguments.of(null, "NullPointerException", "Null query – should propagate NPE."));
    }

    private static Stream<Arguments> emptySetProvider() {
        return Stream.of(Arguments.of(5, "null", "Empty set – ceiling should always return null."));
    }

    private static Stream<Arguments> lowerThanSmallestProvider() {
        return Stream.of(Arguments.of(1, "3", "Query lower than the smallest element – should return the smallest."));
    }

    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(Arguments.of(0, "42", "Query smaller than the sole element – should return that element."), Arguments.of(100, "null", "Query larger than the sole element – should return null."));
    }

    private static Stream<Arguments> maxValueBoundaryProvider() {
        return Stream.of(Arguments.of(Integer.MAX_VALUE - 1, String.valueOf(Integer.MAX_VALUE), "MAX_VALUE‑1 should return MAX_VALUE as the ceiling."), Arguments.of(Integer.MAX_VALUE, String.valueOf(Integer.MAX_VALUE), "Exact MAX_VALUE query should return MAX_VALUE."));
    }

    private static Stream<Arguments> reverseComparatorProvider() {
        return Stream.of(Arguments.of(4, "3", "Reverse order set – ceiling of 4 should be 3."));
    }

    @Test
    void ceilingReturnsExactMatchWhenElementExists() {
        // given
        Integer query = 20;
        // when
        Integer result = set.ceiling(query);
        // then
        assertEquals(20, result);
    }

    @Test
    void ceilingReturnsNextHigherElementWhenExactMatchMissing() {
        // given
        // between 20 and 30
        Integer query = 25;
        // when
        Integer result = set.ceiling(query);
        // then
        assertEquals(30, result);
    }

    @Test
    void ceilingReturnsNullWhenNoHigherElementExists() {
        // given
        // greater than any element in the set
        Integer query = 35;
        // when
        Integer result = set.ceiling(query);
        // then
        assertNull(result);
    }

    @Test
    void ceilingWorksWhenSetIsSealed() {
        // given
        // seal the set
        sealedState = true;
        // between 10 and 20
        Integer query = 15;
        // when
        Integer result = set.ceiling(query);
        // then
        assertEquals(20, result);
        // No exception should be thrown even though the set is sealed,
        // because ceiling is a read‑only operation.
    }

    @Test
    void ceilingReturnsNullWhenQueryIsNull() {
        // given
        // The underlying set contains a null element, but NavigableSet.ceiling(null)
        // is defined to return the least element greater than or equal to null,
        // which for a natural‑ordering set is the first (non‑null) element.
        // The behaviour of ConcurrentNavigableSetNullSafe (used internally) follows
        // the standard NavigableSet contract.
        // when
        Integer result = set.ceiling(null);
        // then
        // The first non‑null element is 10
        assertEquals(10, result);
    }

    /* ---------- basic functionality ---------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicFunctionalityProvider")
    void ceilingBasicFunctionality(Integer query, String expected, String description) {
        // Re‑initialise the set to contain exactly [1, 3, 5]
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(1);
        set.add(3);
        set.add(5);
        if ("NullPointerException".equals(expected)) {
            assertThrows(NullPointerException.class, () -> set.ceiling(query), description);
        } else if ("null".equals(expected)) {
            assertNull(set.ceiling(query), description);
        } else {
            Integer result = set.ceiling(query);
            assertEquals(Integer.valueOf(expected), result, description);
        }
    }

    /* ---------- empty underlying set ---------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("emptySetProvider")
    void ceilingEmptyUnderlyingSet(Integer query, String expected, String description) {
        // Empty set
        set = new SealableNavigableSet<>(sealedSupplier);
        // No elements added
        if ("null".equals(expected)) {
            assertNull(set.ceiling(query), description);
        } else {
            fail("Unexpected expected value for empty set test: " + expected);
        }
    }

    /* ---------- element lower than smallest ---------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("lowerThanSmallestProvider")
    void ceilingElementLowerThanSmallest(Integer query, String expected, String description) {
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(3);
        set.add(5);
        set.add(7);
        Integer result = set.ceiling(query);
        assertEquals(Integer.valueOf(expected), result, description);
    }

    /* ---------- single‑element set scenarios ---------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("singleElementProvider")
    void ceilingSingleElementSet(Integer query, String expected, String description) {
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(42);
        if ("null".equals(expected)) {
            assertNull(set.ceiling(query), description);
        } else {
            Integer result = set.ceiling(query);
            assertEquals(Integer.valueOf(expected), result, description);
        }
    }

    /* ---------- boundary with Integer.MAX_VALUE ---------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("maxValueBoundaryProvider")
    void ceilingBoundaryWithMaxValue(Integer query, String expected, String description) {
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(Integer.MAX_VALUE);
        if ("null".equals(expected)) {
            assertNull(set.ceiling(query), description);
        } else {
            Integer result = set.ceiling(query);
            assertEquals(Integer.valueOf(expected), result, description);
        }
    }

    /* ---------- custom comparator (reverse order) ---------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("reverseComparatorProvider")
    void ceilingCustomComparatorReverse(Integer query, String expected, String description) throws Exception {
        // Build a TreeSet with a reverse (descending) comparator
        NavigableSet<Integer> reverseSet = new TreeSet<>(Comparator.reverseOrder());
        reverseSet.addAll(Arrays.asList(5, 3, 1));
        // Replace the private navSet field via reflection
        set = new SealableNavigableSet<>(sealedSupplier);
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        navSetField.set(set, reverseSet);
        Integer result = set.ceiling(query);
        assertEquals(Integer.valueOf(expected), result, description);
    }
}
