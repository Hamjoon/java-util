package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_subList_Test {

    /**
     * Supplies all (fromIndex, toIndex, description) tuples defined in the JSON test cases.
     */
    private static Stream<Arguments> subListIndexProvider() {
        return Stream.of(// Group: subList throws UnsupportedOperationException
        Arguments.of(0, 0, "empty range on empty list"), Arguments.of(0, 1, "single‑element range (0,1)"), Arguments.of(5, 10, "indices beyond current size (5,10)"), Arguments.of(-1, 2, "negative fromIndex (-1,2)"), Arguments.of(3, 2, "fromIndex > toIndex (3,2)"), // Group: subList throws UnsupportedOperationException - additional edge cases
        Arguments.of(2, 2, "equal non‑zero indices (2,2)"), Arguments.of(3, 3, "fromIndex equal to toIndex at list end (3,3)"), Arguments.of(0, 5, "range exceeds list size (0,5)"), Arguments.of(-5, -1, "both indices negative (-5,-1)"), Arguments.of(-3, 2, "negative fromIndex with positive toIndex (-3,2)"), Arguments.of(2, -2, "positive fromIndex with negative toIndex (2,-2)"), Arguments.of(2147483646, 2147483647, "near Integer.MAX_VALUE"), Arguments.of(2147483647, 2147483647, "both indices = Integer.MAX_VALUE"), Arguments.of(10, 5, "fromIndex > toIndex (10,5)"), Arguments.of(0, -1, "valid fromIndex but negative toIndex (0,-1)"), // Group: subList throws UnsupportedOperationException - extreme integer values
        Arguments.of(-2147483648, -2147483648, "both indices = Integer.MIN_VALUE"), Arguments.of(-2147483648, 0, "fromIndex = Integer.MIN_VALUE, toIndex = 0"), Arguments.of(0, -2147483648, "fromIndex = 0, toIndex = Integer.MIN_VALUE"), Arguments.of(2147483647, -2147483648, "fromIndex = Integer.MAX_VALUE, toIndex = Integer.MIN_VALUE"));
    }

    @Test
    void subList_shouldThrowUnsupportedOperationException_whenCalledOnStandaloneList() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2, 3, 4, 5));
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> list.subList(1, 4), "subList should throw UnsupportedOperationException");
        assertEquals("subList not implemented for ConcurrentList", ex.getMessage(), "Exception message should match the implementation");
    }

    @Test
    void subList_shouldThrowUnsupportedOperationException_whenCalledOnWrappedList() {
        List<Integer> backing = Arrays.asList(10, 20, 30, 40, 50);
        List<Integer> list = new ConcurrentList<>(backing);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> list.subList(0, 2), "subList should throw UnsupportedOperationException even when wrapping another list");
        assertEquals("subList not implemented for ConcurrentList", ex.getMessage(), "Exception message should be consistent for wrapped lists");
    }

    @Test
    void subList_shouldThrowUnsupportedOperationException_forInvalidRange() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2, 3));
        // Even with an invalid range, the same UnsupportedOperationException is expected
        assertThrows(UnsupportedOperationException.class, () -> list.subList(-1, 10), "subList should still throw UnsupportedOperationException for out‑of‑bounds indices");
    }

    /**
     * Executes {@link ConcurrentList#subList(int, int)} for every combination supplied by
     * {@link #subListIndexProvider()} and asserts that an {@link UnsupportedOperationException}
     * with the expected message is always thrown.
     *
     * The list is populated with a few elements (1‑5) to ensure that the method is exercised
     * on a non‑empty backing collection; however, the content is irrelevant because the
     * implementation never reaches the range‑checking logic.
     *
     * @param fromIndex   the start index passed to {@code subList}
     * @param toIndex     the end index passed to {@code subList}
     * @param description a human‑readable description of the test case (used for readability)
     */
    @ParameterizedTest(name = "[{index}] subList({0}, {1}) – {2}")
    @MethodSource("subListIndexProvider")
    void subList_throwsUnsupportedOperationException_forAllIndexCombinations(int fromIndex, int toIndex, String description) {
        // Arrange: a modest list; the exact size does not matter for this test.
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // Act & Assert: the method must always throw the same exception with the same message.
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> list.subList(fromIndex, toIndex), () -> "Expected UnsupportedOperationException for subList(" + fromIndex + ", " + toIndex + "): " + description);
        assertEquals("subList not implemented for ConcurrentList", ex.getMessage(), "Exception message must be consistent across all index values");
    }

    /**
     * The same exhaustive matrix, but this time the {@code ConcurrentList} wraps an existing
     * {@link java.util.ArrayList}.  This verifies that the behaviour is identical in *wrapper mode*.
     */
    @ParameterizedTest(name = "[Wrapped] subList({0}, {1}) – {2}")
    @MethodSource("subListIndexProvider")
    void subList_throwsUnsupportedOperationException_whenWrappedList(int fromIndex, int toIndex, String description) {
        // Arrange: an existing mutable list that will be wrapped.
        List<Integer> backing = new java.util.ArrayList<>(Arrays.asList(10, 20, 30, 40, 50));
        List<Integer> list = new ConcurrentList<>(backing);
        // Act & Assert
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> list.subList(fromIndex, toIndex), () -> "Expected UnsupportedOperationException for wrapped subList(" + fromIndex + ", " + toIndex + "): " + description);
        assertEquals("subList not implemented for ConcurrentList", ex.getMessage(), "Exception message must be consistent for wrapped lists as well");
    }
}
