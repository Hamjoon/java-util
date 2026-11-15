package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_subList_Test {

    // instance created with the no‑arg constructor
    private List<Integer> list;

    // instance that wraps an existing list
    private List<Integer> wrappedList;

    @BeforeEach
    void setupBeforeEach() {
        // Stand‑alone ConcurrentList
        list = new ConcurrentList<>();
        // Wrapper ConcurrentList (backed by a regular ArrayList)
        List<Integer> backing = new ArrayList<>(Arrays.asList(10, 20, 30, 40, 50));
        wrappedList = new ConcurrentList<>(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        list = null;
        wrappedList = null;
    }

    /**
     * Supplies every test case defined in the JSON payload.
     *
     * <p>Both groups – the regular “UnsupportedOperationException” group and the
     * “edge cases” group – are flattened into a single stream of arguments.</p>
     *
     * @return a stream of {@link Arguments} each containing {@code fromIndex},
     *         {@code toIndex} and the case description.
     */
    private static Stream<Arguments> provideUnsupportedSubListCases() {
        return Stream.of(// ---- Group: subList UnsupportedOperationException ----
        Arguments.of(0, 5, "Calling subList with a normal range (0,5) should throw UnsupportedOperationException because the method is not implemented."), Arguments.of(3, 2, "Calling subList with fromIndex greater than toIndex (3,2) should still throw UnsupportedOperationException."), Arguments.of(-1, 4, "Calling subList with a negative fromIndex (-1,4) should throw UnsupportedOperationException."), Arguments.of(0, 1000, "Calling subList with toIndex far beyond the list size (0,1000) should throw UnsupportedOperationException."), // ---- Group: subList UnsupportedOperationException - edge cases ----
        Arguments.of(0, 0, "Calling subList with an empty range (0,0) should still throw UnsupportedOperationException."), Arguments.of(5, 5, "Calling subList where fromIndex equals toIndex at the list's size boundary (5,5) should throw UnsupportedOperationException."), Arguments.of(-5, -1, "Both fromIndex and toIndex are negative; the method must still throw UnsupportedOperationException."), Arguments.of(2, 2, "Calling subList with a zero‑length range in the middle of the list (2,2) should throw UnsupportedOperationException."), Arguments.of(2147483646, 2147483647, "Using near‑max int values for indices should still result in UnsupportedOperationException."), Arguments.of(2147483647, 2147483647, "Both indices are Integer.MAX_VALUE; the method must throw UnsupportedOperationException."), Arguments.of(-2147483648, -2147483647, "Using Integer.MIN_VALUE and the next value tests extreme negative indices; the method should still throw UnsupportedOperationException."), Arguments.of(0, -1, "Valid fromIndex with a negative toIndex should result in UnsupportedOperationException."));
    }

    @Test
    void testSubListUnsupportedStandalone() {
        // Populate the list with a known sequence
        list.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // subList is declared as unsupported – it must throw
        assertThrows(UnsupportedOperationException.class, () -> list.subList(1, 4), "subList should throw UnsupportedOperationException for a standalone ConcurrentList");
    }

    @Test
    void testSubListUnsupportedWrapped() {
        // wrappedList already contains 10,20,30,40,50 from the @BeforeEach setup
        assertThrows(UnsupportedOperationException.class, () -> wrappedList.subList(0, 2), "subList should throw UnsupportedOperationException for a wrapped ConcurrentList");
    }

    @Test
    void testSubListUnsupportedWithInvalidIndices() {
        // Even with out‑of‑range indices the method should still throw the same
        // UnsupportedOperationException (the implementation does not examine the
        // indices before throwing).
        assertThrows(UnsupportedOperationException.class, () -> list.subList(-1, 100), "subList should throw UnsupportedOperationException regardless of index values");
    }

    /**
     * All combinations from the supplied JSON are exercised here.
     *
     * @param fromIndex   the start index passed to {@code subList}
     * @param toIndex     the end index passed to {@code subList}
     * @param description a human‑readable description used in the assertion message
     */
    @ParameterizedTest(name = "{index}: subList({0},{1}) → UnsupportedOperationException – {2}")
    @MethodSource("provideUnsupportedSubListCases")
    void testSubListUnsupportedForAllScenarios(int fromIndex, int toIndex, String description) {
        // Stand‑alone list
        assertThrows(UnsupportedOperationException.class, () -> list.subList(fromIndex, toIndex), description);
        // Wrapped list
        assertThrows(UnsupportedOperationException.class, () -> wrappedList.subList(fromIndex, toIndex), description);
    }
}
