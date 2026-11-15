package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Iterator;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_clear_Test {

    /**
     * Fields used for the “real” list tests **
     */
    private ConcurrentList<Integer> realList;

    /**
     * Fields used for the wrapper‑with‑mock tests **
     */
    @Mock
    private List<Integer> mockedBackingList;

    private ConcurrentList<Integer> listWithMock;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the real list (stand‑alone mode)
        realList = new ConcurrentList<>();
        // Initialise the list that wraps a mocked backing list.
        // MockitoExtension already creates the mock, we just need to wrap it.
        listWithMock = new ConcurrentList<>(mockedBackingList);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid interaction bleed‑over between tests.
        // This is safe even when no interactions have occurred.
        reset(mockedBackingList);
    }

    @Test
    void clearEmptiesStandaloneList() {
        // GIVEN: a list with several elements
        realList.add(1);
        realList.add(2);
        realList.add(3);
        assertFalse(realList.isEmpty(), "Pre‑condition: list should not be empty");
        assertEquals(3, realList.size(), "Pre‑condition: size should be 3");
        // WHEN: clear() is invoked
        realList.clear();
        // THEN: the list is empty and size is zero
        assertTrue(realList.isEmpty(), "List should be empty after clear()");
        assertEquals(0, realList.size(), "Size should be 0 after clear()");
    }

    @Test
    void clearOnAlreadyEmptyStandaloneListDoesNotThrow() {
        // GIVEN: an empty list
        assertTrue(realList.isEmpty(), "Pre‑condition: list is empty");
        // WHEN / THEN: clear() should complete without exception and leave the list empty
        assertDoesNotThrow(() -> realList.clear(), "clear() on an empty list should not throw");
        assertTrue(realList.isEmpty(), "List should remain empty after clear()");
    }

    @Test
    void clearDelegatesToBackingListWhenWrapped() {
        // GIVEN: a wrapper around a mocked backing list
        // (no need to pre‑populate the mock – we only care about the clear() call)
        // WHEN: clear() is called on the wrapper
        listWithMock.clear();
        // THEN: the mock's clear() method must have been invoked exactly once
        verify(mockedBackingList, times(1)).clear();
        // No other interactions should have happened
        verifyNoMoreInteractions(mockedBackingList);
    }

    @Test
    void clearDoesNotSwallowExceptionsFromBackingList() {
        // GIVEN: a mocked list that throws a RuntimeException when clear() is called
        doThrow(new IllegalStateException("boom")).when(mockedBackingList).clear();
        ConcurrentList<Integer> faultyWrapper = new ConcurrentList<>(mockedBackingList);
        // WHEN / THEN: the exception propagates out of ConcurrentList.clear()
        IllegalStateException ex = assertThrows(IllegalStateException.class, faultyWrapper::clear, "Exception from backing list should propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void iteratorReflectsEmptyStateAfterClearEvenIfObtainedBefore() {
        // GIVEN: a populated list and an iterator captured before clear()
        realList.add(10);
        realList.add(20);
        // snapshot iterator
        Iterator<Integer> it = realList.iterator();
        // Sanity‑check that iterator initially has elements
        assertTrue(it.hasNext(), "Iterator should have elements before clear()");
        // WHEN: clear() is invoked
        realList.clear();
        // THEN: the previously obtained iterator reports no remaining elements
        assertFalse(it.hasNext(), "Iterator obtained before clear() must report no elements after clear()");
        // Also verify the list itself is empty
        assertTrue(realList.isEmpty(), "List should be empty after clear()");
    }

    @Test
    void clearPropagatesExceptionFromCustomBackingList() {
        // GIVEN: a mocked list that throws IllegalStateException on clear()
        doThrow(new IllegalStateException("custom failure")).when(mockedBackingList).clear();
        ConcurrentList<Integer> wrapper = new ConcurrentList<>(mockedBackingList);
        // WHEN / THEN: the same exception propagates out of ConcurrentList.clear()
        IllegalStateException ex = assertThrows(IllegalStateException.class, wrapper::clear, "Exception from custom backing list should propagate");
        assertEquals("custom failure", ex.getMessage());
    }
}
