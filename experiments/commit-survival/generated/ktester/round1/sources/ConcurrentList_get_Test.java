package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_get_Test {

    /**
     * List created with the no‑arg constructor – the “real” implementation.
     */
    private ConcurrentList<Integer> realList;

    /**
     * List that wraps a mocked underlying List – used to verify delegation.
     */
    private ConcurrentList<Integer> wrapperList;

    /**
     * Mocked underlying List used by {@code wrapperList}.
     */
    @Mock
    private List<Integer> mockedUnderlyingList;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the real list for normal behaviour tests.
        realList = new ConcurrentList<>();
        // Initialise the wrapper list that delegates to a mocked List.
        // MockitoExtension already creates the mock instance for us.
        wrapperList = new ConcurrentList<>(mockedUnderlyingList);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC and avoid accidental reuse.
        realList = null;
        wrapperList = null;
    }

    @Test
    void getReturnsCorrectElement() {
        // given
        realList.add(10);
        realList.add(20);
        realList.add(30);
        // when
        int value = realList.get(1);
        // then
        assertEquals(20, value, "get(1) should return the element at index 1");
    }

    @Test
    void getOnEmptyListThrowsIndexOutOfBoundsException() {
        // given – an empty list (realList is already empty)
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> realList.get(0), "Calling get(0) on an empty list should throw IndexOutOfBoundsException");
    }

    @Test
    void getWithNegativeIndexThrowsIndexOutOfBoundsException() {
        // given
        realList.add(1);
        realList.add(2);
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> realList.get(-1), "Calling get with a negative index should throw IndexOutOfBoundsException");
    }

    @Test
    void getDelegatesToUnderlyingListWhenWrapped() {
        // given
        when(mockedUnderlyingList.get(0)).thenReturn(99);
        when(mockedUnderlyingList.get(1)).thenReturn(100);
        // when
        int first = wrapperList.get(0);
        int second = wrapperList.get(1);
        // then
        assertEquals(99, first, "Wrapper should return the value supplied by the mocked list for index 0");
        assertEquals(100, second, "Wrapper should return the value supplied by the mocked list for index 1");
        // Verify that the underlying list's get method was invoked exactly once per call.
        verify(mockedUnderlyingList, times(1)).get(0);
        verify(mockedUnderlyingList, times(1)).get(1);
        verifyNoMoreInteractions(mockedUnderlyingList);
    }

    @Test
    void getPropagatesUnderlyingListException() {
        // given – mock throws an exception for a specific index
        when(mockedUnderlyingList.get(5)).thenThrow(new IndexOutOfBoundsException("mocked OOB"));
        // when / then
        IndexOutOfBoundsException ex = assertThrows(IndexOutOfBoundsException.class, () -> wrapperList.get(5), "Exception from the underlying list should be propagated");
        assertEquals("mocked OOB", ex.getMessage());
    }

    /**
     * Retrieve the last element (high‑end boundary).
     */
    @Test
    void getReturnsLastElement() {
        // given
        realList.add(100);
        realList.add(200);
        // size == 3, last index = 2
        realList.add(300);
        // when
        int last = realList.get(realList.size() - 1);
        // then
        assertEquals(300, last, "get(size-1) should return the last element");
    }

    /**
     * Index equal to size → out‑of‑range.
     */
    @Test
    void getWithIndexEqualToSizeThrowsIndexOutOfBoundsException() {
        // given
        realList.add(1);
        realList.add(2);
        // valid indices are 0..size-1
        int outOfRange = realList.size();
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> realList.get(outOfRange), "Calling get(size) should throw IndexOutOfBoundsException");
    }

    /**
     * Underlying list throws ConcurrentModificationException – should propagate.
     */
    @Test
    void getPropagatesConcurrentModificationException() {
        // given
        when(mockedUnderlyingList.get(0)).thenThrow(new ConcurrentModificationException("concurrent change"));
        // when / then
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, () -> wrapperList.get(0), "ConcurrentModificationException from the underlying list must propagate");
        assertEquals("concurrent change", ex.getMessage());
    }

    /**
     * List contains a single null element – get should return null.
     */
    @Test
    void getReturnsNullWhenElementIsNull() {
        // given
        realList.add(null);
        // when
        Integer result = realList.get(0);
        // then
        assertNull(result, "get(0) should return null when the stored element is null");
    }

    /**
     * Retrieve a middle element from a list of size >= 4.
     */
    @Test
    void getMiddleElement() {
        // given
        realList.add(10);
        realList.add(20);
        realList.add(30);
        realList.add(40);
        // index 2 is the third element (middle for size 4)
        // when
        int middle = realList.get(2);
        // then
        assertEquals(30, middle, "get(2) should return the middle element");
    }

    /**
     * Index far beyond the list size should still throw IndexOutOfBoundsException.
     */
    @Test
    void getWithFarBeyondSizeThrowsIndexOutOfBoundsException() {
        // given
        realList.add(1);
        realList.add(2);
        int farIndex = realList.size() + 5;
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> realList.get(farIndex), "Index far beyond size must result in IndexOutOfBoundsException");
    }

    /**
     * Integer.MIN_VALUE as index – expect IndexOutOfBoundsException.
     */
    @Test
    void getWithIntegerMinValueThrowsIndexOutOfBoundsException() {
        // given
        // any element to avoid empty‑list shortcut
        realList.add(1);
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> realList.get(Integer.MIN_VALUE), "Using Integer.MIN_VALUE as index should throw IndexOutOfBoundsException");
    }

    /**
     * Integer.MAX_VALUE as index – expect IndexOutOfBoundsException.
     */
    @Test
    void getWithIntegerMaxValueThrowsIndexOutOfBoundsException() {
        // given
        realList.add(1);
        realList.add(2);
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> realList.get(Integer.MAX_VALUE), "Using Integer.MAX_VALUE as index should throw IndexOutOfBoundsException");
    }

    /**
     * Manipulate the private `list` field to be null via reflection – NullPointerException expected.
     */
    @Test
    void getWhenUnderlyingListIsNullThrowsNullPointerException() throws Exception {
        // given
        ConcurrentList<Integer> listWithNullField = new ConcurrentList<>();
        // Use reflection to set the private field `list` to null.
        Field field = ConcurrentList.class.getDeclaredField("list");
        field.setAccessible(true);
        field.set(listWithNullField, null);
        // when / then
        assertThrows(NullPointerException.class, () -> listWithNullField.get(0), "If the internal list reference is null, get(0) should throw NullPointerException");
    }
}
