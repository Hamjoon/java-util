package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_add_2_Test {

    // instance created with a real backing list
    private ConcurrentList<Integer> listUnderTest;

    @Mock
    private List<Integer> // mock used for wrapper‑mode tests
    mockBackingList;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh, empty ConcurrentList for the “stand‑alone” tests
        listUnderTest = new ConcurrentList<>();
        // Mockito will initialise mockBackingList (annotated with @Mock) automatically
    }

    /**
     * Provides the data for {@link #testAdd_NormalInsertion}.
     */
    private static Stream<Arguments> normalInsertionProvider() {
        return Stream.of(// Insert into an empty list at index 0 (beginning)
        arguments(Collections.emptyList(), 0, "first", Collections.singletonList("first")), // Insert into a non‑empty list at a middle position
        arguments(Arrays.asList("first", "second"), 1, "third", Arrays.asList("first", "third", "second")), // Insert at index equal to current size (append)
        arguments(Arrays.asList("first", "second", "third"), 3, "fourth", Arrays.asList("first", "second", "third", "fourth")), // Insert a null element at the beginning of an empty list
        arguments(Collections.emptyList(), 0, null, Collections.singletonList(null)));
    }

    /**
     * Provides the data for {@link #testAdd_InvalidIndexThrows}.
     */
    private static Stream<Arguments> invalidIndexProvider() {
        // size = 3
        List<String> base = Arrays.asList("a", "b", "c");
        return Stream.of(arguments(base, -1, "neg"), arguments(base, 5, "outOfRange"), arguments(base, -5, "any"), arguments(base, 100, "any"));
    }

    @Test
    void testAddAtVariousPositions_Standalone() {
        // start with a known list state
        listUnderTest.addAll(Arrays.asList(1, 3, 4));
        // add at the beginning
        listUnderTest.add(0, 0);
        assertEquals(Arrays.asList(0, 1, 3, 4), listUnderTest, "Adding at index 0 should prepend the element");
        // add in the middle
        listUnderTest.add(2, 2);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), listUnderTest, "Adding at a middle index should insert the element at that position");
        // add at the end (size == 5, valid index = 5)
        listUnderTest.add(5, 5);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), listUnderTest, "Adding at index equal to size should append the element");
    }

    @Test
    void testAddAtVariousPositions_WrapperMode() {
        // Build a ConcurrentList that wraps the Mockito mock
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(mockBackingList);
        // Define behaviour of the mock for successful addition – we don't need a real list,
        // just verify that the correct delegate method is called.
        when(mockBackingList.add(anyInt(), anyInt())).thenReturn(null);
        // Perform the operation under test
        wrapped.add(0, 10);
        wrapped.add(1, 20);
        wrapped.add(2, 30);
        // Verify that the underlying list received the exact calls in order
        InOrder inOrder = inOrder(mockBackingList);
        inOrder.verify(mockBackingList).add(0, 10);
        inOrder.verify(mockBackingList).add(1, 20);
        inOrder.verify(mockBackingList).add(2, 30);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void testAddWithInvalidIndex_ThrowsException() {
        // Populate the list with a few elements
        listUnderTest.addAll(Arrays.asList(1, 2, 3));
        // Negative index
        assertThrows(IndexOutOfBoundsException.class, () -> listUnderTest.add(-1, 99), "Negative index should throw IndexOutOfBoundsException");
        // Index greater than size
        assertThrows(IndexOutOfBoundsException.class, () -> listUnderTest.add(4, 99), "Index greater than size should throw IndexOutOfBoundsException");
    }

    /**
     * Normal insertion scenarios (including insertion of {@code null}).
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>initial list content (may be empty)</li>
     *   <li>index at which to insert</li>
     *   <li>element to insert (can be {@code null})</li>
     *   <li>expected list after insertion</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] Insert {2} at {1} into {0}")
    @MethodSource("normalInsertionProvider")
    void testAdd_NormalInsertion(List<String> initial, int index, String element, List<String> expected) {
        // Use a fresh ConcurrentList for each case
        ConcurrentList<String> list = new ConcurrentList<>();
        // populate with the supplied initial state
        list.addAll(initial);
        // operation under test
        list.add(index, element);
        assertEquals(expected, list, "List state after insertion does not match expectation");
        assertEquals(expected.size(), list.size(), "Size after insertion should match expected size");
    }

    /**
     * Scenarios where the supplied index is invalid and an {@link IndexOutOfBoundsException}
     * must be thrown.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>initial list content (size is 3 for all cases, matching the JSON description)</li>
     *   <li>invalid index</li>
     *   <li>element to attempt to insert (value is irrelevant for the exception)</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] Invalid index {1} on list {0}")
    @MethodSource("invalidIndexProvider")
    void testAdd_InvalidIndexThrows(List<String> initial, int index, String element) {
        ConcurrentList<String> list = new ConcurrentList<>();
        list.addAll(initial);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(index, element), "Adding at invalid index should throw IndexOutOfBoundsException");
    }
}
