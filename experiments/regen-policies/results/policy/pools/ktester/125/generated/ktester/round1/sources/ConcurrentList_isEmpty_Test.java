package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_isEmpty_Test {

    /**
     * A list created with the no‑arg constructor (stand‑alone mode).
     */
    private ConcurrentList<Integer> standaloneList;

    /**
     * A list created by wrapping an existing list (wrapper mode).
     */
    private ConcurrentList<Integer> wrappedList;

    /**
     * The backing list used for the wrapper mode.
     */
    private List<Integer> backingList;

    /**
     * Helper subclass that counts how many times {@code readOperation} is invoked.
     */
    private static class CountingConcurrentList<E> extends ConcurrentList<E> {

        private int readCount = 0;

        CountingConcurrentList(List<E> backing) {
            super(backing);
        }

        @Override
        protected <T> T readOperation(Supplier<T> operation) {
            readCount++;
            return super.readOperation(operation);
        }

        int getReadCount() {
            return readCount;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh standalone list for each test.
        standaloneList = new ConcurrentList<>();
        // Initialise a fresh backing list and wrap it.
        backingList = new ArrayList<>();
        wrappedList = new ConcurrentList<>(backingList);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required but keeps the pattern clean.
        standaloneList = null;
        wrappedList = null;
        backingList = null;
    }

    /**
     * Test data provider for the simple true/false cases (empty vs non‑empty).
     */
    private static Stream<Arguments> emptyVsNonEmptyProvider() {
        return Stream.of(Arguments.of(new ConcurrentList<>(new ArrayList<>()), true, "Underlying list empty → isEmpty() should return true"), Arguments.of(new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), false, "Underlying list non‑empty → isEmpty() should return false"));
    }

    @Test
    void isEmptyOnNewStandaloneList() {
        assertTrue(standaloneList.isEmpty(), "A newly created standalone ConcurrentList should be empty");
    }

    @Test
    void isEmptyAfterAddingElementStandalone() {
        standaloneList.add(42);
        assertFalse(standaloneList.isEmpty(), "List should not be empty after adding an element");
    }

    @Test
    void isEmptyAfterClearingStandalone() {
        standaloneList.add(1);
        standaloneList.add(2);
        assertFalse(standaloneList.isEmpty(), "List should not be empty before clear");
        standaloneList.clear();
        assertTrue(standaloneList.isEmpty(), "List should be empty after clear");
    }

    @Test
    void isEmptyOnNewWrappedList() {
        assertTrue(wrappedList.isEmpty(), "A newly wrapped ConcurrentList should be empty");
    }

    @Test
    void isEmptyAfterAddingElementWrapped() {
        wrappedList.add(7);
        assertFalse(wrappedList.isEmpty(), "Wrapped list should not be empty after adding an element");
        // also verify that the backing list reflects the change
        assertEquals(1, backingList.size(), "Backing list size should match the wrapped list");
        assertEquals(7, backingList.get(0));
    }

    @Test
    void isEmptyAfterRemovingAllWrapped() {
        wrappedList.addAll(List.of(1, 2, 3));
        assertFalse(wrappedList.isEmpty(), "Wrapped list should not be empty after bulk add");
        wrappedList.clear();
        assertTrue(wrappedList.isEmpty(), "Wrapped list should be empty after clear");
        assertTrue(backingList.isEmpty(), "Backing list should also be empty after clear");
    }

    @Test
    void isEmptyWhenBackingListIsModifiedDirectly() {
        // Directly modify the backing list to simulate external changes.
        backingList.add(99);
        // The wrapper should see the change because it references the same list.
        assertFalse(wrappedList.isEmpty(), "Wrapped list should reflect external modifications to the backing list");
    }

    @Test
    void isEmptyWithNullElementsAllowed() {
        // Underlying ArrayList permits nulls, so test that null does not affect emptiness logic.
        wrappedList.add(null);
        assertFalse(wrappedList.isEmpty(), "List containing a null element should not be considered empty");
        wrappedList.clear();
        assertTrue(wrappedList.isEmpty(), "List should be empty after clearing null element");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("emptyVsNonEmptyProvider")
    void isEmptyDelegatesToUnderlyingList(ConcurrentList<?> list, boolean expected, String description) {
        assertEquals(expected, list.isEmpty(), "isEmpty() must reflect the state of the wrapped list");
    }

    @Test
    @DisplayName("isEmpty() must invoke readOperation exactly once")
    void isEmptyInvokesReadOperationOnce() {
        // Arrange: a counting wrapper around an empty backing list
        CountingConcurrentList<Integer> countingList = new CountingConcurrentList<>(new ArrayList<>());
        // Act
        boolean result = countingList.isEmpty();
        // Assert
        assertTrue(result, "Empty list should report true");
        assertEquals(1, countingList.getReadCount(), "readOperation should be called exactly once by isEmpty()");
    }

    @Test
    @DisplayName("Exception from underlying list's isEmpty() propagates")
    void isEmptyPropagatesExceptionFromUnderlyingList() {
        // Arrange: a list whose isEmpty() throws IllegalStateException
        List<String> faultyList = new AbstractList<>() {

            @Override
            public String get(int index) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                throw new IllegalStateException("underlying failure");
            }
        };
        ConcurrentList<String> concurrentList = new ConcurrentList<>(faultyList);
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, concurrentList::isEmpty, "isEmpty() must not swallow exceptions from the wrapped list");
        assertEquals("underlying failure", ex.getMessage());
    }

    @Test
    @DisplayName("Null backing list results in NullPointerException")
    void isEmptyWithNullBackingListThrowsNPE() {
        // Arrange: construct with null (the constructor itself does not throw)
        ConcurrentList<Integer> list = new ConcurrentList<>(null);
        // Act & Assert
        assertThrows(NullPointerException.class, list::isEmpty, "Calling isEmpty() on a ConcurrentList with a null backing list should throw NPE");
    }

    @Test
    @DisplayName("Exception thrown by overridden readOperation propagates")
    void isEmptyPropagatesReadOperationException() {
        // Arrange: subclass that forces readOperation to throw
        ConcurrentList<Integer> brokenList = new ConcurrentList<>(new ArrayList<>()) {

            @Override
            protected <T> T readOperation(Supplier<T> operation) {
                throw new IllegalStateException("readOperation failure");
            }
        };
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, brokenList::isEmpty, "isEmpty() must propagate exceptions thrown by readOperation");
        assertEquals("readOperation failure", ex.getMessage());
    }

    @Test
    @DisplayName("Error thrown by underlying list's isEmpty() propagates unchanged")
    void isEmptyPropagatesErrorFromUnderlyingList() {
        // Arrange: a list whose isEmpty() throws an AssertionError (subclass of Error)
        List<String> errorList = new AbstractList<>() {

            @Override
            public String get(int index) {
                return null;
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                throw new AssertionError("boom");
            }
        };
        ConcurrentList<String> concurrentList = new ConcurrentList<>(errorList);
        // Act & Assert
        AssertionError err = assertThrows(AssertionError.class, concurrentList::isEmpty, "isEmpty() must let Errors propagate without wrapping");
        assertEquals("boom", err.getMessage());
    }
}
