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
class ConcurrentList_remove_2_Test {

    /**
     * Instance used for the majority of tests (stand‑alone mode).
     */
    private ConcurrentList<Integer> list;

    /**
     * Backing list used for wrapper‑mode tests.
     */
    private List<Integer> backingList;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh list for each test to guarantee isolation.
        list = new ConcurrentList<>();
        // will be created only when needed.
        backingList = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse.
        list = null;
        backingList = null;
    }

    /**
     * Provides arguments for successful removal scenarios.
     *
     * Each argument consists of:
     * <ul>
     *   <li>Initial list contents (as an {@code Object[]} array)</li>
     *   <li>Element to remove</li>
     *   <li>Expected list contents after removal</li>
     *   <li>Human‑readable description (used only for documentation)</li>
     * </ul>
     */
    private static Stream<Arguments> removalSuccessProvider() {
        return Stream.of(// 1. Single element ["alpha"]; remove "alpha"
        Arguments.of(new Object[] { "alpha" }, "alpha", new Object[] {}, "ConcurrentList initialized with a single element [\"alpha\"]. Removing \"alpha\" should succeed and return true."), // 2. Elements [1, 42, 3]; remove 42
        Arguments.of(new Object[] { 1, 42, 3 }, 42, new Object[] { 1, 3 }, "ConcurrentList initialized with elements [1, 42, 3]. Removing existing element 42 should return true and leave the list as [1, 3]."), // 3. Elements [null, \"x\", \"y\"]; remove null
        Arguments.of(new Object[] { null, "x", "y" }, null, new Object[] { "x", "y" }, "ConcurrentList initialized with elements [null, \"x\", \"y\"]. Removing null should succeed and return true."), // 4. Duplicate elements [\"dup\", \"a\", \"dup\", \"b\"]; remove \"dup\" (first occurrence)
        Arguments.of(new Object[] { "dup", "a", "dup", "b" }, "dup", new Object[] { "a", "dup", "b" }, "ConcurrentList initialized with duplicate elements [\"dup\", \"a\", \"dup\", \"b\"]. Removing \"dup\" should remove the first occurrence, return true, and result in [\"a\", \"dup\", \"b\"]."));
    }

    @Test
    void removeExistingElement_returnsTrue_andRemovesElement() {
        list.add(10);
        list.add(20);
        list.add(30);
        assertTrue(list.remove(Integer.valueOf(20)), "remove(Object) should return true when the element exists");
        assertEquals(2, list.size(), "Size should decrease after successful removal");
        assertFalse(list.contains(20), "List must no longer contain the removed element");
    }

    @Test
    void removeNonExistingElement_returnsFalse_andLeavesListUnchanged() {
        list.add(1);
        list.add(2);
        list.add(3);
        assertFalse(list.remove(Integer.valueOf(99)), "remove(Object) should return false when the element does not exist");
        assertEquals(3, list.size(), "Size must stay the same when removal fails");
        assertTrue(list.contains(1) && list.contains(2) && list.contains(3), "Original elements must remain in the list");
    }

    @Test
    void removeNullElement_whenListAllowsNull_returnsTrue() {
        // Underlying ArrayList permits null values, so ConcurrentList should as well.
        list.add(null);
        list.add(5);
        assertTrue(list.remove(null), "remove(null) should succeed when a null element is present");
        assertEquals(1, list.size(), "Size should reflect the removal of the null element");
        assertFalse(list.contains(null), "List must no longer contain null");
    }

    @Test
    void removeNullElement_whenNullNotPresent_returnsFalse() {
        list.add(1);
        list.add(2);
        assertFalse(list.remove(null), "remove(null) should return false when no null element is present");
        assertEquals(2, list.size(), "Size must remain unchanged");
    }

    @Test
    void removeFromWrappedList_reflectsChangeInBackingList() {
        // Prepare a backing list with known content.
        backingList = new ArrayList<>();
        backingList.add(100);
        backingList.add(200);
        backingList.add(300);
        // Wrap the backing list with ConcurrentList.
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backingList);
        // Verify initial state.
        assertEquals(3, wrapped.size());
        assertTrue(wrapped.contains(200));
        // Perform removal via the wrapper.
        assertTrue(wrapped.remove(Integer.valueOf(200)), "remove(Object) should succeed on the wrapped list when element exists");
        // Both the wrapper and the original backing list must reflect the change.
        assertEquals(2, wrapped.size());
        assertEquals(2, backingList.size(), "Backing list size must also be reduced");
        assertFalse(wrapped.contains(200));
        assertFalse(backingList.contains(200));
    }

    @Test
    void removeFromEmptyList_returnsFalse() {
        assertTrue(list.isEmpty(), "List should start empty");
        assertFalse(list.remove(Integer.valueOf(1)), "remove(Object) on an empty list must return false");
        assertEquals(0, list.size(), "Size must stay zero");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("removalSuccessProvider")
    void removeSuccessCases(Object[] initialElements, Object elementToRemove, Object[] expectedElementsAfterRemoval, String description) {
        // Use a raw ConcurrentList<Object> to hold heterogeneous values.
        @SuppressWarnings("unchecked")
        ConcurrentList<Object> cl = new ConcurrentList<>();
        // Populate the list with the supplied initial elements.
        for (Object e : initialElements) {
            cl.add(e);
        }
        // Perform the removal.
        boolean result = cl.remove(elementToRemove);
        // Verify the contract.
        assertTrue(result, "remove(Object) should return true for existing element");
        assertEquals(expectedElementsAfterRemoval.length, cl.size(), "Size after removal must match expected size");
        // Verify remaining contents (order matters).
        List<Object> actual = new ArrayList<>();
        for (Object o : cl) {
            actual.add(o);
        }
        assertEquals(Arrays.asList(expectedElementsAfterRemoval), actual, "List contents after removal do not match expected");
    }

    @Test
    void removeFromUnmodifiableBackingList_throwsUnsupportedOperationException() {
        // Create a mutable list, then wrap it with Collections.unmodifiableList.
        List<String> mutable = new ArrayList<>(Arrays.asList("any", "other"));
        List<String> unmodifiable = Collections.unmodifiableList(mutable);
        // Wrap the unmodifiable list with ConcurrentList.
        ConcurrentList<String> wrapped = new ConcurrentList<>(unmodifiable);
        // Attempting to remove should propagate the exception from the delegate.
        assertThrows(UnsupportedOperationException.class, () -> wrapped.remove("any"), "remove(Object) on an unmodifiable underlying list must throw UnsupportedOperationException");
    }
}
