package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_listIterator_2_Test {

    /**
     * Instance created with the no‑arg constructor (stand‑alone mode).
     */
    private List<Integer> standaloneList;

    /**
     * Instance created by wrapping an existing list (wrapper mode).
     */
    private List<Integer> wrappedList;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh list for each test to guarantee isolation.
        standaloneList = new ConcurrentList<>();
        // Wrapper mode – use a pre‑populated backing list.
        List<Integer> backing = new ArrayList<>();
        backing.add(10);
        backing.add(20);
        wrappedList = new ConcurrentList<>(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required but keeps the pattern consistent.
        standaloneList = null;
        wrappedList = null;
    }

    /**
     * Provides the exhaustive set of index values described in the JSON test data.
     * Each {@link Arguments} contains the index to test and a short description
     * used as the assertion message.
     */
    private static Stream<Arguments> indexProvider() {
        return Stream.of(// Group: listIterator(int) throws UnsupportedOperationException
        Arguments.of(-1, "negative index should throw UnsupportedOperationException"), Arguments.of(0, "index 0 (start of list) should throw UnsupportedOperationException"), Arguments.of(5, "positive index within typical bounds should throw UnsupportedOperationException"), Arguments.of(Integer.MAX_VALUE, "Integer.MAX_VALUE should still throw UnsupportedOperationException"), // Group: additional indices
        Arguments.of(Integer.MIN_VALUE, "Integer.MIN_VALUE should still throw UnsupportedOperationException"), Arguments.of(1, "index 1 (list may contain elements) should throw UnsupportedOperationException"), Arguments.of(2, "index 2 (typical list size) should throw UnsupportedOperationException"), Arguments.of(3, "index equal to size after adding three elements should throw UnsupportedOperationException"), Arguments.of(4, "index greater than size (size+1) should throw UnsupportedOperationException"), Arguments.of(1000, "large positive index far beyond realistic size should throw UnsupportedOperationException"));
    }

    @Test
    @DisplayName("listIterator(int) throws UnsupportedOperationException in stand‑alone mode")
    void testListIteratorUnsupportedStandalone() {
        // Given an empty ConcurrentList created via the no‑arg constructor
        // When listIterator is invoked with any valid index (e.g., 0)
        // Then an UnsupportedOperationException is thrown
        assertThrows(UnsupportedOperationException.class, () -> standaloneList.listIterator(0), "listIterator(int) should throw UnsupportedOperationException");
    }

    @Test
    @DisplayName("listIterator(int) throws UnsupportedOperationException in wrapper mode")
    void testListIteratorUnsupportedWrapper() {
        // Given a ConcurrentList that wraps an existing list containing two elements
        // When listIterator is invoked with a valid index (e.g., 1)
        // Then an UnsupportedOperationException is thrown
        assertThrows(UnsupportedOperationException.class, () -> wrappedList.listIterator(1), "listIterator(int) should throw UnsupportedOperationException even when wrapping another list");
    }

    @Test
    @DisplayName("listIterator(int) throws UnsupportedOperationException for out‑of‑range index")
    void testListIteratorUnsupportedOutOfRange() {
        // Even if the index is outside the current size, the same exception type is expected.
        assertThrows(UnsupportedOperationException.class, () -> standaloneList.listIterator(5), "listIterator(int) should throw UnsupportedOperationException for out‑of‑range index");
    }

    @Test
    @DisplayName("listIterator(int) throws UnsupportedOperationException in stand‑alone mode (explicit test)")
    void testListIteratorUnsupportedStandaloneExplicit() {
        assertThrows(UnsupportedOperationException.class, () -> standaloneList.listIterator(0), "listIterator(int) should throw UnsupportedOperationException");
    }

    @Test
    @DisplayName("listIterator(int) throws UnsupportedOperationException in wrapper mode (explicit test)")
    void testListIteratorUnsupportedWrapperExplicit() {
        assertThrows(UnsupportedOperationException.class, () -> wrappedList.listIterator(1), "listIterator(int) should throw UnsupportedOperationException even when wrapping another list");
    }
}
