package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

class SealableNavigableSet_lower_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    private SealableNavigableSet<Integer> intSet;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test and populate it with a known ordering.
        // The underlying set is a ConcurrentNavigableSetNullSafe which allows null values.
        set = new SealableNavigableSet<>(sealedSupplier);
        // explicit null element – lower(null) should return null
        set.add(null);
        set.add(10);
        set.add(20);
        set.add(30);
        // Default integer set used by a few tests – populated with a known ordering.
        intSet = new SealableNavigableSet<>(sealedSupplier);
        intSet.add(2);
        intSet.add(3);
        intSet.add(4);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no test leaks state to the next one.
        set = null;
        sealedState = false;
        intSet = null;
        sealedState = false;
    }

    /* -----------------------------------------------------------
     *  Helper factories
     * ----------------------------------------------------------- */
    private <T> SealableNavigableSet<T> createSet(Collection<T> elements) {
        SealableNavigableSet<T> set = new SealableNavigableSet<>(sealedSupplier);
        elements.forEach(set::add);
        return set;
    }

    private <T> SealableNavigableSet<T> createSetWithComparator(Comparator<? super T> cmp, Collection<T> elements) {
        SealableNavigableSet<T> set = new SealableNavigableSet<>(cmp, sealedSupplier);
        elements.forEach(set::add);
        return set;
    }

    /* -----------------------------------------------------------
     *  Integer specific cases (from the JSON spec)
     * ----------------------------------------------------------- */
    static Stream<Arguments> integerLowerProvider() {
        return Stream.of(// empty set case
        Arguments.of(Collections.emptyList(), 10, null, "Empty set returns null for any element"), // single element set cases
        Arguments.of(List.of(5), 5, null, "Single element set – lower(5) == null"), Arguments.of(List.of(5), 6, 5, "Single element set – lower(6) == 5"), // normal predecessor cases
        Arguments.of(List.of(1, 2, 3), 2, 1, "Set [1,2,3] – lower(2) == 1"), Arguments.of(List.of(1, 2, 3), 4, 3, "Set [1,2,3] – lower(4) == 3"), // edge cases with min/max values
        Arguments.of(List.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE), Integer.MAX_VALUE, 0, "Set [MIN,0,MAX] – lower(MAX) == 0"), // negative numbers
        Arguments.of(List.of(-10, -5, 0, 5), -5, -10, "Set [-10,-5,0,5] – lower(-5) == -10"));
    }

    /* -----------------------------------------------------------
     *  String specific cases (including case‑insensitive comparator)
     * ----------------------------------------------------------- */
    static Stream<Arguments> stringLowerProvider() {
        return Stream.of(// natural order
        Arguments.of(List.of("a", "c", "d"), "b", "a", "Natural order – lower('b') == 'a'"), Arguments.of(List.of("a", "c", "d"), "a", null, "Natural order – lower('a') == null"), // case‑insensitive comparator
        Arguments.of(List.of("Apple", "banana", "Cherry"), "banana", "Apple", "CASE_INSENSITIVE_ORDER – lower('banana') == 'Apple'"));
    }

    /* -----------------------------------------------------------
     *  Double specific cases
     * ----------------------------------------------------------- */
    static Stream<Arguments> doubleLowerProvider() {
        return Stream.of(Arguments.of(List.of(1.1, 2.2, 3.3), 2.5, 2.2, "Set [1.1,2.2,3.3] – lower(2.5) == 2.2"));
    }

    @Test
    void lowerReturnsImmediatePredecessor() {
        // 20's predecessor in the set is 10
        assertEquals(10, set.lower(20));
        // 30's predecessor is 20
        assertEquals(20, set.lower(30));
        // 10 is the smallest non‑null element, so lower(10) should be null
        assertNull(set.lower(10));
    }

    @Test
    void lowerReturnsNullWhenNoElementIsLower() {
        // Values lower than the smallest element (10) – expect null
        assertNull(set.lower(5));
        // The set contains a null element; lower(null) is defined to be null
        assertNull(set.lower(null));
    }

    @Test
    void lowerWorksWhenSetIsSealed() {
        // Seal the set – read‑only operations must still work
        sealedState = true;
        // lower should still return the correct predecessor without throwing
        assertEquals(10, set.lower(20));
        assertEquals(20, set.lower(30));
        assertNull(set.lower(10));
    }

    @Test
    void lowerWithComparatorAndCustomOrdering() {
        // Create a set with a reverse order comparator to ensure lower respects the comparator
        SealableNavigableSet<Integer> reverseSet = new SealableNavigableSet<>(Comparator.reverseOrder(), sealedSupplier);
        reverseSet.add(10);
        reverseSet.add(20);
        reverseSet.add(30);
        // In reverse order, the "lower" element is actually the next higher value in natural order
        // For element 20, the lower element (according to reverse order) is 30
        assertEquals(30, reverseSet.lower(20));
        // Clean up
        reverseSet = null;
    }

    /* -----------------------------------------------------------
     *  Null argument tests
     * ----------------------------------------------------------- */
    @Test
    @DisplayName("lower(null) should throw NullPointerException")
    void lower_NullArgument_ThrowsNPE() {
        // The underlying NavigableSet.lower contract specifies NPE for null.
        assertThrows(NullPointerException.class, () -> intSet.lower(null));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("integerLowerProvider")
    void lower_IntegerScenarios(List<Integer> elements, Integer query, Integer expected, String _desc) {
        SealableNavigableSet<Integer> set = createSet(elements);
        assertEquals(expected, set.lower(query));
    }

    /* -----------------------------------------------------------
     *  Integer cases that expect ClassCastException
     * ----------------------------------------------------------- */
    @Test
    @DisplayName("lower(String) on Integer set should throw ClassCastException")
    void lower_WrongType_StringOnIntegerSet() {
        assertThrows(ClassCastException.class, () -> intSet.lower("string"));
    }

    @Test
    @DisplayName("lower(Double) on Integer set should throw ClassCastException")
    void lower_WrongType_DoubleOnIntegerSet() {
        assertThrows(ClassCastException.class, () -> intSet.lower(1.5));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("stringLowerProvider")
    void lower_StringScenarios(List<String> elements, String query, String expected, String _desc) {
        SealableNavigableSet<String> set;
        if (elements.contains("Apple")) {
            // case‑insensitive test
            set = createSetWithComparator(String.CASE_INSENSITIVE_ORDER, elements);
        } else {
            set = createSet(elements);
        }
        assertEquals(expected, set.lower(query));
    }

    @Test
    @DisplayName("lower(Object) on String set with unrelated type should throw ClassCastException")
    void lower_WrongType_ObjectOnStringSet() {
        SealableNavigableSet<String> stringSet = createSet(List.of("a", "b"));
        assertThrows(ClassCastException.class, () -> stringSet.lower(new Object()));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("doubleLowerProvider")
    void lower_DoubleScenarios(List<Double> elements, Double query, Double expected, String _desc) {
        SealableNavigableSet<Double> set = createSet(elements);
        assertEquals(expected, set.lower(query));
    }

    /* -----------------------------------------------------------
     *  Reverse order comparator test (already covered in framework,
     *  but retained here for completeness)
     * ----------------------------------------------------------- */
    @Test
    @DisplayName("lower respects reverse order comparator")
    void lower_ReverseOrderComparator() {
        SealableNavigableSet<Integer> reverseSet = new SealableNavigableSet<>(Comparator.reverseOrder(), sealedSupplier);
        reverseSet.add(10);
        reverseSet.add(20);
        reverseSet.add(30);
        // In reverse order, the element lower than 20 is 30.
        assertEquals(30, reverseSet.lower(20));
    }

    /* -----------------------------------------------------------
     *  Sealed‑state behaviour – read‑only operations must still work
     * ----------------------------------------------------------- */
    @Test
    @DisplayName("lower works correctly when the set is sealed")
    void lower_WhenSealed_ShouldReturnPredecessor() {
        // make the set read‑only
        sealedState = true;
        // Re‑use the intSet populated in @BeforeEach ([2,3,4])
        assertEquals(2, intSet.lower(3));
        assertNull(intSet.lower(2));
    }
}
