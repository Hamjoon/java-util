package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_subList_Test {

    /**
     * Instance created with the no‑arg constructor.
     */
    private ConcurrentList<Integer> emptyList;

    /**
     * Instance created by wrapping an existing mutable list.
     */
    private ConcurrentList<Integer> wrappedList;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh list for each test to guarantee isolation.
        emptyList = new ConcurrentList<>();
        // Initialise a list that wraps an existing ArrayList with some data.
        List<Integer> backing = new ArrayList<>();
        backing.add(10);
        backing.add(20);
        backing.add(30);
        wrappedList = new ConcurrentList<>(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly drop references – helps the GC in long test suites.
        emptyList = null;
        wrappedList = null;
    }

    private static Stream<Arguments> mainCasesProvider() {
        return Stream.of(// fromIndex == toIndex (empty sub‑list)
        Arguments.of(0, 0), // typical valid range
        Arguments.of(0, 5), // fromIndex > toIndex (invalid range)
        Arguments.of(3, 2));
    }

    private static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(// negative fromIndex
        Arguments.of(-1, 0), // negative toIndex
        Arguments.of(0, -1), // both negative
        Arguments.of(-5, -1), // equal to typical list size
        Arguments.of(5, 5), // toIndex beyond size
        Arguments.of(4, 6), // both beyond size
        Arguments.of(6, 8), // large but within int range
        Arguments.of(2147483646, 2147483647), // both at Integer.MAX_VALUE
        Arguments.of(2147483647, 2147483647), // from > to at max values
        Arguments.of(2147483647, 2147483646), // extremely large range
        Arguments.of(0, 2147483647));
    }

    private static Stream<Arguments> extremeValuesProvider() {
        return Stream.of(// both at Integer.MIN_VALUE
        Arguments.of(-2147483648, -2147483648), // min value to zero
        Arguments.of(-2147483648, 0), // zero to min value
        Arguments.of(0, -2147483648), // max to min (reversed)
        Arguments.of(2147483647, -2147483648), // full extreme range
        Arguments.of(-2147483648, 2147483647));
    }

    @Test
    void subList_onEmptyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> emptyList.subList(0, 0), "subList on an empty ConcurrentList should throw UnsupportedOperationException");
    }

    @Test
    void subList_onWrappedList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> wrappedList.subList(1, 2), "subList on a wrapped ConcurrentList should throw UnsupportedOperationException");
    }

    @Test
    void subList_withInvalidIndices_throwsUnsupportedOperationException() {
        // Even when the indices are out of bounds, the method is expected to fail
        // with the same UnsupportedOperationException rather than IndexOutOfBoundsException.
        assertThrows(UnsupportedOperationException.class, () -> wrappedList.subList(-1, 5), "subList with invalid indices should still throw UnsupportedOperationException");
    }

    /**
     * Main group: typical valid/invalid ranges.
     */
    @ParameterizedTest(name = "subList({0}, {1}) → UnsupportedOperationException")
    @MethodSource("mainCasesProvider")
    void subList_mainCases_throwsUnsupportedOperationException(int fromIndex, int toIndex) {
        assertThrows(UnsupportedOperationException.class, () -> wrappedList.subList(fromIndex, toIndex));
    }

    /**
     * Edge‑case group: negative indices, equal indices at size, out‑of‑range values, etc.
     */
    @ParameterizedTest(name = "subList({0}, {1}) → UnsupportedOperationException")
    @MethodSource("edgeCasesProvider")
    void subList_edgeCases_throwsUnsupportedOperationException(int fromIndex, int toIndex) {
        assertThrows(UnsupportedOperationException.class, () -> wrappedList.subList(fromIndex, toIndex));
    }

    /**
     * Extreme integer values group: Integer.MIN_VALUE / Integer.MAX_VALUE.
     */
    @ParameterizedTest(name = "subList({0}, {1}) → UnsupportedOperationException")
    @MethodSource("extremeValuesProvider")
    void subList_extremeValues_throwsUnsupportedOperationException(int fromIndex, int toIndex) {
        assertThrows(UnsupportedOperationException.class, () -> wrappedList.subList(fromIndex, toIndex));
    }
}
