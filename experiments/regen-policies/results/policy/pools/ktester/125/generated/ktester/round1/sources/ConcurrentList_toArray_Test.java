package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_toArray_Test {

    private ConcurrentList<Integer> list;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh list for each test (stand‑alone mode)
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        list = null;
    }

    /**
     * Supplies test data for {@link #toArray_BasicScenarios_ReturnsExpectedArray}.
     *
     * @return a stream of arguments: (list of elements, expected array, description)
     */
    private static Stream<Arguments> basicToArrayProvider() {
        return Stream.of(// Case: three integers
        arguments(List.of(1, 2, 3), new Object[] { 1, 2, 3 }, "A list containing three integers. toArray() should return an Object[] with the same elements in the same order."), // Case: two strings
        arguments(List.of("a", "b"), new Object[] { "a", "b" }, "A list containing two strings. toArray() should return an Object[] with those strings preserving order."));
    }

    @Test
    void toArray_EmptyList_ReturnsEmptyArray() {
        Object[] array = list.toArray();
        assertNotNull(array, "Returned array should not be null");
        assertEquals(0, array.length, "Array length should be zero for an empty list");
    }

    @Test
    void toArray_ListWithElements_ReturnsCorrectArray() {
        list.add(10);
        list.add(20);
        list.add(30);
        Object[] array = list.toArray();
        assertArrayEquals(new Object[] { 10, 20, 30 }, array, "toArray should return an array containing the list elements in order");
    }

    @Test
    void toArray_SnapshotSemantics_ModifyingListAfterCallDoesNotAffectReturnedArray() {
        list.add(1);
        list.add(2);
        list.add(3);
        Object[] snapshot = list.toArray();
        // Mutate the list after the snapshot was taken
        list.add(4);
        list.remove(Integer.valueOf(1));
        // The snapshot must remain unchanged
        assertArrayEquals(new Object[] { 1, 2, 3 }, snapshot, "Array returned by toArray must represent a snapshot of the list at call time");
    }

    @Test
    void toArray_WrapperMode_ReflectsUnderlyingListContents() {
        // Prepare a backing list with known contents
        List<Integer> backing = new ArrayList<>();
        backing.add(100);
        backing.add(200);
        backing.add(300);
        // Wrap the existing list
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backing);
        // The array should reflect the current state of the backing list
        Object[] array = wrapped.toArray();
        assertArrayEquals(new Object[] { 100, 200, 300 }, array, "Wrapped ConcurrentList should expose the elements of the backing list");
        // Change the backing list and verify that toArray reflects the change
        backing.add(400);
        Object[] updatedArray = wrapped.toArray();
        assertArrayEquals(new Object[] { 100, 200, 300, 400 }, updatedArray, "toArray on a wrapped list must reflect modifications made directly to the backing list");
    }

    @Test
    void toArray_ListContainsNullElements_HandlesNullsCorrectly() {
        list.add(null);
        list.add(42);
        list.add(null);
        Object[] array = list.toArray();
        assertArrayEquals(new Object[] { null, 42, null }, array, "toArray must preserve null elements in the returned array");
    }

    /**
     * Parameterized test that covers the basic scenarios described in the JSON test
     * specification (group: {@code toArray_basic}). It verifies that {@code toArray()}
     * returns an {@code Object[]} containing exactly the elements that were added,
     * preserving order and type.
     *
     * @param elements the elements to add to a freshly created {@code ConcurrentList}
     * @param expected the expected array content after calling {@code toArray()}
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicToArrayProvider")
    void toArray_BasicScenarios_ReturnsExpectedArray(List<?> elements, Object[] expected, String description) {
        // Use a raw ConcurrentList to accommodate both Integer and String elements.
        @SuppressWarnings("unchecked")
        ConcurrentList<Object> genericList = new ConcurrentList<>();
        // Populate the list with the supplied elements.
        for (Object e : elements) {
            genericList.add(e);
        }
        // Invoke the method under test.
        Object[] actual = genericList.toArray();
        // Verify that the returned array matches the expectation.
        assertArrayEquals(expected, actual, description);
    }
}
