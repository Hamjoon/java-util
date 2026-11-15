package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.cedarsoftware.util.DeepEquals.deepEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class ConcurrentList_remove_Test {

    // the list under test
    private List<Integer> list;

    // optional backing list for wrapper‑mode tests
    private List<Integer> backingList;

    @BeforeEach
    void setupBeforeEach() {
        // By default we test the “stand‑alone” constructor.
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps the GC and isolates tests.
        list = null;
        backingList = null;
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for valid index removals (middle, first, last)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> validIndexProvider() {
        return Stream.of(// middle element
        Arguments.of(Arrays.asList("a", "b", "c"), 1, "b", Arrays.asList("a", "c")), // first element
        Arguments.of(Arrays.asList("a", "b", "c"), 0, "a", Arrays.asList("b", "c")), // last element
        Arguments.of(Arrays.asList("a", "b", "c"), 2, "c", Arrays.asList("a", "b")));
    }

    @Test
    void removeAtMiddleIndex_returnsRemovedElement_andShiftsElements() {
        list.addAll(Arrays.asList(0, 1, 2, 3, 4));
        // remove element at index 2 (value 2)
        int removed = list.remove(2);
        assertEquals(2, removed, "remove(int) should return the element that was at the supplied index");
        // expected list after removal
        List<Integer> expected = Arrays.asList(0, 1, 3, 4);
        assertTrue(deepEquals(expected, list), "List contents after removal are incorrect");
    }

    @Test
    void removeAtFirstAndLastIndex_behavesCorrectly() {
        list.addAll(Arrays.asList(10, 20, 30, 40));
        // first index
        assertEquals(10, list.remove(0), "First element removal returned wrong value");
        List<Integer> afterFirst = Arrays.asList(20, 30, 40);
        assertTrue(deepEquals(afterFirst, list), "List after removing first element is incorrect");
        // last index (now index 2)
        assertEquals(40, list.remove(2), "Last element removal returned wrong value");
        List<Integer> afterLast = Arrays.asList(20, 30);
        assertTrue(deepEquals(afterLast, list), "List after removing last element is incorrect");
    }

    @Test
    void removeFromEmptyList_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(0), "Removing from an empty list should throw IndexOutOfBoundsException");
    }

    @Test
    void removeWithNegativeIndex_throwsIndexOutOfBoundsException() {
        list.addAll(Arrays.asList("a", "b", "c"));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1), "Negative index should cause IndexOutOfBoundsException");
    }

    @Test
    void removeWithIndexEqualToSize_throwsIndexOutOfBoundsException() {
        list.addAll(Arrays.asList("a", "b", "c"));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(3), "Index equal to size should cause IndexOutOfBoundsException");
    }

    @Test
    void removeUsingWrapperMode_reflectsChangeInBackingList() {
        // Create a mutable backing list and wrap it.
        backingList = new ArrayList<>(Arrays.asList(100, 200, 300, 400));
        list = new ConcurrentList<>(backingList);
        // Remove the element at index 1 (value 200)
        int removed = list.remove(1);
        assertEquals(200, removed, "Removed element should be 200");
        // Verify both the wrapper and the original list have the same state.
        List<Integer> expected = Arrays.asList(100, 300, 400);
        assertTrue(deepEquals(expected, list), "Wrapper list state is incorrect after removal");
        assertTrue(deepEquals(expected, backingList), "Backing list was not updated correctly after removal");
    }

    @ParameterizedTest(name = "{index}: remove index {1} from {0} → returns {2}")
    @MethodSource("validIndexProvider")
    void removeValidIndex_returnsRemovedElement_andShiftsElements(List<String> initial, int index, String expectedRemoved, List<String> expectedAfter) {
        list.addAll(initial);
        String removed = list.remove(index);
        assertEquals(expectedRemoved, removed, "remove(int) should return the element that was at the supplied index");
        assertTrue(deepEquals(expectedAfter, list), "List contents after removal are incorrect");
    }

    /* -------------------------------------------------------------
     *  Single‑element list removal
     * ------------------------------------------------------------- */
    @Test
    void removeSingleElement_returnsElement_andLeavesListEmpty() {
        list.addAll(Collections.singletonList("x"));
        String removed = list.remove(0);
        assertEquals("x", removed, "Removing the sole element should return it");
        assertTrue(list.isEmpty(), "List should be empty after removing its only element");
    }

    /* -------------------------------------------------------------
     *  Removal of a null element
     * ------------------------------------------------------------- */
    @Test
    void removeNullElement_returnsNull_andShiftsElements() {
        list.addAll(Arrays.asList("a", null, "c"));
        Object removed = list.remove(1);
        assertNull(removed, "Removing a null element should return null");
        List<String> expected = Arrays.asList("a", "c");
        assertTrue(deepEquals(expected, list), "List after null removal is incorrect");
    }

    /* -------------------------------------------------------------
     *  Removal when duplicate elements are present
     * ------------------------------------------------------------- */
    @Test
    void removeDuplicateElement_returnsCorrectInstance_andShifts() {
        list.addAll(Arrays.asList("a", "b", "b", "c"));
        // second "b"
        String removed = list.remove(2);
        assertEquals("b", removed, "Removed element should be the second 'b'");
        List<String> expected = Arrays.asList("a", "b", "c");
        assertTrue(deepEquals(expected, list), "List after removing duplicate element is incorrect");
    }

    /* -------------------------------------------------------------
     *  Consecutive removals (stateful behavior)
     * ------------------------------------------------------------- */
    @Test
    void removeAfterPreviousRemoval_behavesCorrectly() {
        list.addAll(Arrays.asList("a", "b", "c"));
        // first removal
        assertEquals("a", list.remove(0), "First removal should return 'a'");
        // second removal on the now‑shortened list
        assertEquals("b", list.remove(0), "Second removal should return 'b'");
        List<String> expected = Collections.singletonList("c");
        assertTrue(deepEquals(expected, list), "List after two removals is incorrect");
    }

    /* -------------------------------------------------------------
     *  Unmodifiable underlying list – expect UnsupportedOperationException
     * ------------------------------------------------------------- */
    @Test
    void removeFromUnmodifiableBackingList_propagatesUnsupportedOperationException() {
        List<String> unmodifiable = Collections.unmodifiableList(Arrays.asList("a", "b"));
        list = new ConcurrentList<>(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> list.remove(0), "Removal from an unmodifiable underlying list should throw UnsupportedOperationException");
    }

    /* -------------------------------------------------------------
     *  Internal list set to null via reflection – expect NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void removeWhenInternalListIsNull_throwsNullPointerException() throws Exception {
        // Create a normal instance first
        list = new ConcurrentList<>();
        list.addAll(Arrays.asList("x", "y"));
        // Use reflection to nullify the private 'list' field
        Field field = ConcurrentList.class.getDeclaredField("list");
        field.setAccessible(true);
        field.set(list, null);
        // Now invoking remove should hit a NPE inside the lambda
        assertThrows(NullPointerException.class, () -> list.remove(0), "Calling remove when internal list is null should throw NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Existing integer‑based tests (kept unchanged)
     * ------------------------------------------------------------- */
    @Test
    void removeAtMiddleIndex_returnsRemovedElement_andShiftsElements_int() {
        List<Integer> intList = new ConcurrentList<>();
        intList.addAll(Arrays.asList(0, 1, 2, 3, 4));
        int removed = intList.remove(2);
        assertEquals(2, removed, "remove(int) should return the element that was at the supplied index");
        List<Integer> expected = Arrays.asList(0, 1, 3, 4);
        assertTrue(deepEquals(expected, intList), "List contents after removal are incorrect");
    }

    @Test
    void removeAtFirstAndLastIndex_behavesCorrectly_int() {
        List<Integer> intList = new ConcurrentList<>();
        intList.addAll(Arrays.asList(10, 20, 30, 40));
        assertEquals(10, intList.remove(0), "First element removal returned wrong value");
        List<Integer> afterFirst = Arrays.asList(20, 30, 40);
        assertTrue(deepEquals(afterFirst, intList), "List after removing first element is incorrect");
        assertEquals(40, intList.remove(2), "Last element removal returned wrong value");
        List<Integer> afterLast = Arrays.asList(20, 30);
        assertTrue(deepEquals(afterLast, intList), "List after removing last element is incorrect");
    }
}
