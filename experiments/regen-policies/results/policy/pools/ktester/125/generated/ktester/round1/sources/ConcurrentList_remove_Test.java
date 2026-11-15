package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_remove_Test {

    /**
     * The list under test – created fresh for each test method.
     */
    private ConcurrentList<Integer> list;

    /**
     * A backing list used for the “wrapper mode” tests.
     */
    private List<Integer> backingList;

    class ThrowingArrayList<E> extends ArrayList<E> {

        @Override
        public E remove(int index) {
            throw new IllegalStateException("forced exception");
        }
    }

    class ThrowingArrayList<E> extends ArrayList<E> {

        @Override
        public E remove(int index) {
            throw new IllegalStateException("forced exception");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stand‑alone mode list for the majority of tests.
        list = new ConcurrentList<>();
        // Backing list for wrapper‑mode tests – created but not used unless a test needs it.
        backingList = new ArrayList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required.
        list = null;
        backingList = null;
    }

    private static Stream<Arguments> validRemovalProvider() {
        return Stream.of(Arguments.of(1, "B", List.of("A", "C")), Arguments.of(0, "A", List.of("B", "C")), Arguments.of(2, "C", List.of("A", "B")));
    }

    private static Stream<Arguments> invalidRemovalProvider() {
        return Stream.of(Arguments.of(-1), // equal to size
        Arguments.of(3), // far beyond size
        Arguments.of(100));
    }

    @Test
    void removeAtMiddleIndex_returnsElementAndShrinksList() {
        list.addAll(List.of(0, 1, 2, 3, 4));
        // remove element at index 2 (value 2)
        Integer removed = list.remove(2);
        assertEquals(2, removed, "remove(int) should return the element at the given index");
        assertIterableEquals(List.of(0, 1, 3, 4), list, "List contents should reflect the removal");
        assertEquals(4, list.size(), "Size should be decremented after removal");
    }

    @Test
    void removeFirstAndLastIndex_behaveCorrectly() {
        list.addAll(List.of(10, 20, 30, 40));
        // first element
        assertEquals(10, list.remove(0), "First element should be removed");
        assertIterableEquals(List.of(20, 30, 40), list);
        // last element (now index 2)
        assertEquals(40, list.remove(2), "Last element should be removed");
        assertIterableEquals(List.of(20, 30), list);
    }

    @Test
    void removeFromEmptyList_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(0), "Removing from an empty list should throw IndexOutOfBoundsException");
    }

    @Test
    void removeWithNegativeIndex_throwsIndexOutOfBoundsException() {
        list.addAll(List.of(1, 2, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1), "Negative index should cause IndexOutOfBoundsException");
    }

    @Test
    void removeWithIndexEqualToSize_throwsIndexOutOfBoundsException() {
        list.addAll(List.of(5, 6, 7));
        assertThrows(IndexOutOfBoundsException.class, // valid indices are 0‑2
        () -> list.remove(3), "Index equal to size should cause IndexOutOfBoundsException");
    }

    @Test
    void removeNullElement_supportedWhenBackingListAllowsNulls() {
        // Use wrapper mode with a backing list that permits nulls.
        backingList.add(null);
        backingList.add(1);
        list = new ConcurrentList<>(backingList);
        assertEquals(null, list.remove(0), "Removing a null element should return null");
        assertIterableEquals(List.of(1), list, "List should contain the remaining non‑null element");
    }

    @Test
    void removeInWrapperMode_reflectsChangeInBackingList() {
        // Prepare a backing list with known content.
        backingList.addAll(List.of(100, 200, 300));
        list = new ConcurrentList<>(backingList);
        // Perform removal via the ConcurrentList façade.
        // removes 200
        Integer removed = list.remove(1);
        assertEquals(200, removed, "Returned element should be the removed value");
        // Both the wrapper and the original backing list must now be in sync.
        assertIterableEquals(List.of(100, 300), list, "Wrapper view should be updated");
        assertIterableEquals(List.of(100, 300), backingList, "Backing list should reflect the removal");
    }

    /**
     * Valid‑index removal scenarios – list ["A","B","C"]
     */
    @ParameterizedTest(name = "remove({0}) → expected ''{1}''; resulting list {2}")
    @MethodSource("validRemovalProvider")
    void remove_validIndices(int index, String expectedElement, List<String> expectedList) {
        // use a fresh ConcurrentList<String> for each invocation
        ConcurrentList<String> stringList = new ConcurrentList<>();
        stringList.addAll(List.of("A", "B", "C"));
        String removed = stringList.remove(index);
        assertEquals(expectedElement, removed, () -> "remove(" + index + ") should return element '" + expectedElement + "'");
        assertIterableEquals(expectedList, stringList, () -> "After removal the list should be " + expectedList);
    }

    /**
     * Invalid‑index removal scenarios – all must throw IndexOutOfBoundsException
     */
    @ParameterizedTest(name = "remove({0}) → throws IndexOutOfBoundsException")
    @MethodSource("invalidRemovalProvider")
    void remove_invalidIndices(int index) {
        ConcurrentList<String> stringList = new ConcurrentList<>();
        stringList.addAll(List.of("A", "B", "C"));
        assertThrows(IndexOutOfBoundsException.class, () -> stringList.remove(index), () -> "remove(" + index + ") should throw IndexOutOfBoundsException");
    }

    /**
     * Single‑element list removal
     */
    @Test
    void remove_singleElementList() {
        ConcurrentList<String> single = new ConcurrentList<>();
        single.add("X");
        String removed = single.remove(0);
        assertEquals("X", removed, "Removing the only element should return it");
        assertTrue(single.isEmpty(), "List should be empty after removal");
    }

    /**
     * List containing a null element at the requested index
     */
    @Test
    void remove_listContainingNulls() {
        ConcurrentList<String> listWithNull = new ConcurrentList<>();
        listWithNull.addAll(List.of("A", null, "C"));
        String removed = listWithNull.remove(1);
        assertNull(removed, "Removing a null element should return null");
        assertIterableEquals(List.of("A", "C"), listWithNull, "List should contain the remaining elements after null removal");
    }

    /**
     * Single‑thread sanity check – ensures the method returns the correct element
     */
    @Test
    void remove_concurrentSafety_singleThreadSanityCheck() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.addAll(List.of("first", "second"));
        String removed = cl.remove(0);
        assertEquals("first", removed, "Method should return the element at index 0");
        assertIterableEquals(List.of("second"), cl);
    }

    /**
     * Removing from an unmodifiable backing list must propagate UnsupportedOperationException
     */
    @Test
    void remove_unsupportedOperation_unmodifiableBackingList() {
        List<String> unmodifiable = Collections.unmodifiableList(new ArrayList<>(List.of("only")));
        ConcurrentList<String> wrapper = new ConcurrentList<>(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> wrapper.remove(0), "remove on an unmodifiable backing list should throw UnsupportedOperationException");
    }

    /**
     * Underlying list throws a custom runtime exception – it must propagate unchanged
     */
    @Test
    void remove_underlyingListThrowsCustomException() {
        // A list that deliberately throws IllegalStateException on remove(int)
        class ThrowingArrayList<E> extends ArrayList<E> {

            @Override
            public E remove(int index) {
                throw new IllegalStateException("forced exception");
            }
        }
        List<String> throwingList = new ThrowingArrayList<>();
        throwingList.addAll(List.of("a", "b"));
        ConcurrentList<String> wrapper = new ConcurrentList<>(throwingList);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> wrapper.remove(0), "Exception from the underlying list should propagate");
        assertEquals("forced exception", ex.getMessage());
    }
}
