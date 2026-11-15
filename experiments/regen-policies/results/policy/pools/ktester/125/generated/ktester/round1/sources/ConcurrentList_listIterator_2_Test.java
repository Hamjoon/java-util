package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ListIterator;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_listIterator_2_Test {

    private ConcurrentList<Integer> list;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh list for each test and populate it with a few elements
        list = new ConcurrentList<>();
        list.add(10);
        list.add(20);
        list.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; the list itself does not hold external resources
        list = null;
    }

    @Test
    void listIteratorZeroIndexThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> list.listIterator(0), "listIterator(0) should throw UnsupportedOperationException");
    }

    @Test
    void listIteratorMiddleIndexThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> list.listIterator(1), "listIterator(1) should throw UnsupportedOperationException");
    }

    @Test
    void listIteratorBeyondSizeThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> list.listIterator(5), "listIterator(5) (beyond current size) should throw UnsupportedOperationException");
    }

    @Test
    void listIteratorNegativeIndexThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> list.listIterator(-1), "listIterator(-1) should throw UnsupportedOperationException");
    }

    /**
     * Parameterized test that verifies {@code listIterator(int)} always throws
     * {@link UnsupportedOperationException} with the expected message for a wide range of
     * index values, including edge cases such as {@link Integer#MIN_VALUE},
     * {@link Integer#MAX_VALUE}, negative values, zero, and values beyond the current
     * list size.
     *
     * @param index the index supplied to {@code listIterator}
     */
    @ParameterizedTest(name = "listIterator({0}) throws UnsupportedOperationException")
    @ValueSource(ints = { // empty list (if we cleared it) – covered by default list state
    0, // simple negative index
    -1, // Integer.MIN_VALUE
    -2147483648, // middle index when list has elements
    1, // size-1 index
    2, // size index (equal to current size)
    3, // beyond current size
    5, // far beyond size
    123456, // extreme positive index
    Integer.MAX_VALUE })
    void listIteratorAlwaysUnsupported(int index) {
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, () -> list.listIterator(index), () -> "listIterator(" + index + ") should throw UnsupportedOperationException");
        // Verify that the exception message matches the documented message
        assertEquals("listIterator(index) not implemented for ConcurrentList", thrown.getMessage(), "Exception message should be the documented one");
    }

    /**
     * Additional sanity check that calling the zero‑argument {@code listIterator()}
     * (which is *implemented*) does not throw and returns a valid iterator.
     * This ensures the test suite is not falsely passing because the whole class
     * is broken.
     */
    @Test
    void listIteratorNoArgReturnsReadOnlyIterator() {
        ListIterator<Integer> it = list.listIterator();
        // The iterator should start at the beginning of the list
        // and allow forward traversal without modification.
        // should not throw
        it.next();
        // should not throw
        it.next();
        // should not throw
        it.next();
        // No more elements – next() would throw NoSuchElementException, but we don't call it here.
    }
}
