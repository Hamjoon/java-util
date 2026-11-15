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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_set_Test {

    /**
     * fields used for the wrapper‑mode tests **
     */
    @Mock
    private List<String> // injected by Mockito
    mockBackingList;

    // ConcurrentList that wraps the mock
    private ConcurrentList<String> wrappedList;

    // real list, no mocking needed
    private ConcurrentList<Integer> standaloneList;

    // integer list for generic tests
    private ConcurrentList<Integer> standaloneIntList;

    // string list for string‑specific tests
    private ConcurrentList<String> standaloneStrList;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (required when using @Mock without a runner)
        MockitoAnnotations.openMocks(this);
        // Wrapper mode – a ConcurrentList that delegates to a mocked List
        wrappedList = new ConcurrentList<>(mockBackingList);
        // Stand‑alone mode – fresh ConcurrentLists backed by real ArrayLists
        standaloneIntList = new ConcurrentList<>();
        standaloneStrList = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Release references – helps the garbage collector and isolates tests
        mockBackingList = null;
        wrappedList = null;
        standaloneIntList = null;
        standaloneStrList = null;
    }

    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(Arguments.of(1, "newValue", "oldValue", "Replace the element at a valid middle index. The method should return the previous element ('oldValue') and the underlying list should now contain 'newValue' at that position."), Arguments.of(0, null, "firstValue", "Replace the first element with null. The method should return the original first element and allow null to be stored in the list."), Arguments.of(2, "last", "third", "Replace the element at the last valid index (size‑1). The method should return the previous last element."));
    }

    private static Stream<Arguments> invalidIndexProvider() {
        return Stream.of(Arguments.of(-1, "alpha", IndexOutOfBoundsException.class, "Attempt to set an element at a negative index; the method must throw IndexOutOfBoundsException."), Arguments.of(3, "beta", IndexOutOfBoundsException.class, "Attempt to set an element at index equal to the current list size (size = 3). This is out‑of‑range and should raise IndexOutOfBoundsException."), Arguments.of(5, "gamma", IndexOutOfBoundsException.class, "Attempt to set an element at an index greater than the list size; the method must throw IndexOutOfBoundsException."));
    }

    @Test
    void set_ReplacesElement_Standalone() {
        // Arrange – populate the list with known values
        standaloneIntList.add(10);
        standaloneIntList.add(20);
        standaloneIntList.add(30);
        // Act – replace the element at index 1
        Integer previous = standaloneIntList.set(1, 99);
        // Assert – the returned value is the element that was replaced
        assertEquals(20, previous, "set should return the previous element");
        // The list now contains the new value at the same index
        assertEquals(99, standaloneIntList.get(1), "element at index 1 should be updated");
        // The size must stay unchanged
        assertEquals(3, standaloneIntList.size(), "size should remain unchanged after set");
    }

    @Test
    void set_ThrowsIndexOutOfBounds_Standalone() {
        // Arrange – empty list
        // Act & Assert – illegal index should propagate the same exception as a normal List
        assertThrows(IndexOutOfBoundsException.class, () -> standaloneIntList.set(0, 1), "set on an empty list should throw IndexOutOfBoundsException");
    }

    @Test
    void set_AllowsNullElement_Standalone() {
        // Arrange
        standaloneIntList.add(null);
        standaloneIntList.add(1);
        // Act
        Object previous = standaloneIntList.set(0, "notNull");
        // Assert
        assertNull(previous, "previous value should be null");
        assertEquals("notNull", standaloneIntList.get(0), "new value should be stored");
    }

    @Test
    void set_DelegatesToBackingList_ReturnsPrevious() {
        // Arrange – define mock behaviour
        when(mockBackingList.set(1, "B")).thenReturn("A");
        // Act
        String returned = wrappedList.set(1, "B");
        // Assert – the result matches the mock's return value
        assertEquals("A", returned, "ConcurrentList.set should return the value from the backing list");
        // Verify that the backing list received exactly one call with the correct arguments
        verify(mockBackingList, times(1)).set(1, "B");
        verifyNoMoreInteractions(mockBackingList);
    }

    @Test
    void set_DelegatesAndPropagatesException() {
        // Arrange – make the mock throw an exception for an illegal index
        when(mockBackingList.set(5, "X")).thenThrow(new IndexOutOfBoundsException("bad index"));
        // Act & Assert – the same exception type and message should be observed
        IndexOutOfBoundsException ex = assertThrows(IndexOutOfBoundsException.class, () -> wrappedList.set(5, "X"), "ConcurrentList.set should propagate exceptions from the backing list");
        assertEquals("bad index", ex.getMessage());
        // Verify interaction
        verify(mockBackingList, times(1)).set(5, "X");
        verifyNoMoreInteractions(mockBackingList);
    }

    @Test
    void set_AllowsNullElement_Wrapper() {
        // Arrange – mock returns a previous non‑null value
        when(mockBackingList.set(0, null)).thenReturn("prev");
        // Act
        String previous = wrappedList.set(0, null);
        // Assert
        assertEquals("prev", previous);
        verify(mockBackingList, times(1)).set(0, null);
        verifyNoMoreInteractions(mockBackingList);
    }

    /**
     * Normal operation – replace existing elements **
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalOperationProvider")
    void set_NormalOperation(int index, String newElement, String expectedPrevious, String description) {
        // Initialise list as described in the JSON cases:
        // ["firstValue", "oldValue", "third"]
        standaloneStrList.add("firstValue");
        standaloneStrList.add("oldValue");
        standaloneStrList.add("third");
        String previous = standaloneStrList.set(index, newElement);
        assertEquals(expectedPrevious, previous, description);
        assertEquals(newElement, standaloneStrList.get(index), "list should contain the new value at index " + index);
        assertEquals(3, standaloneStrList.size(), "size must remain unchanged");
    }

    /**
     * Boundary conditions – invalid indices **
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidIndexProvider")
    void set_InvalidIndices(int index, String element, Class<? extends Throwable> expectedException, String description) {
        // List with three elements for index checks
        standaloneStrList.add("a");
        standaloneStrList.add("b");
        standaloneStrList.add("c");
        assertThrows(expectedException, () -> standaloneStrList.set(index, element), description);
    }

    /**
     * Empty list handling **
     */
    @Test
    void set_OnEmptyList_ThrowsIndexOutOfBounds() {
        // The list is empty by construction
        assertThrows(IndexOutOfBoundsException.class, () -> standaloneStrList.set(0, "first"), "Calling set on an empty ConcurrentList (size = 0) should throw IndexOutOfBoundsException regardless of the index value.");
    }

    /**
     * No‑op replacement (same value) **
     */
    @Test
    void set_NoOpReplacement_ReturnsSameValue() {
        // List where index 1 already holds "second"
        standaloneStrList.add("first");
        standaloneStrList.add("second");
        standaloneStrList.add("third");
        String previous = standaloneStrList.set(1, "second");
        assertEquals("second", previous, "When the new element is identical to the current element, set should return the previous element (which is the same).");
        assertEquals("second", standaloneStrList.get(1), "Element at index 1 must remain unchanged.");
        assertEquals(3, standaloneStrList.size(), "Size must stay the same after a no‑op set.");
    }

    /**
     * Null handling – existing null element **
     */
    @Test
    void set_NullOnExistingNull_ReturnsNull() {
        // List where index 2 already contains null
        standaloneStrList.add("x");
        standaloneStrList.add("y");
        standaloneStrList.add(null);
        String previous = standaloneStrList.set(2, null);
        assertNull(previous, "If the list already contains null at the specified index, setting null again should return null.");
        assertNull(standaloneStrList.get(2), "The element should remain null after the operation.");
        assertEquals(3, standaloneStrList.size(), "Size must stay unchanged.");
    }

    /**
     * Size invariance after set **
     */
    @Test
    void set_SizeInvarianceAfterReplace() {
        // Initialise list as described: ["head","mid","tail"]
        standaloneStrList.add("head");
        standaloneStrList.add("mid");
        standaloneStrList.add("tail");
        String previous = standaloneStrList.set(0, "newHead");
        assertEquals("head", previous, "Replacing the first element must return the previous element.");
        assertEquals("newHead", standaloneStrList.get(0), "New head should be stored.");
        assertEquals(3, standaloneStrList.size(), "List size must remain 3 after set.");
    }
}
