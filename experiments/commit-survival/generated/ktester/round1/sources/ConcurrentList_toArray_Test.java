package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_toArray_Test {

    private ConcurrentList<Integer> list;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh list for every test to guarantee isolation.
        list = new ConcurrentList<>();
    }

    private static Stream<Arguments> provideListsForToArray() {
        return Stream.of(// non‑null elements
        Arguments.of("Multiple non‑null elements", Arrays.asList(1, 2, 3), new Object[] { 1, 2, 3 }), // null values
        Arguments.of("List containing null values", Arrays.asList(null, 4, null), new Object[] { null, 4, null }), // mixed null and non‑null
        Arguments.of("Mixed null and non‑null elements", Arrays.asList(5, null, 6, null, 7), new Object[] { 5, null, 6, null, 7 }), // single element
        Arguments.of("Single element list", Collections.singletonList(42), new Object[] { 42 }));
    }

    /**
     * -----------------------------------------------------------------
     *  Test that an empty ConcurrentList returns an empty Object[].
     *  -----------------------------------------------------------------
     */
    @Test
    void toArray_EmptyList_ReturnsEmptyArray() {
        Object[] result = list.toArray();
        assertNotNull(result, "Resulting array must not be null");
        assertEquals(0, result.length, "Array length should be 0 for an empty list");
    }

    /**
     * -----------------------------------------------------------------
     *  Test that a list with elements returns an array containing those
     *  elements in the correct order.
     *  -----------------------------------------------------------------
     */
    @Test
    void toArray_PopulatedList_ReturnsArrayWithSameElements() {
        List<Integer> expected = Arrays.asList(10, 20, 30, 40);
        // populate the ConcurrentList
        expected.forEach(list::add);
        Object[] array = list.toArray();
        // Verify size
        assertEquals(expected.size(), array.length, "Array length must match list size");
        // Verify content and order
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), array[i], "Element at index " + i + " differs");
        }
    }

    /**
     * -----------------------------------------------------------------
     *  Verify that the returned array is a *copy* – mutating it must not
     *  affect the underlying ConcurrentList.
     *  -----------------------------------------------------------------
     */
    @Test
    void toArray_ReturnedArrayIsIndependentFromList() {
        list.addAll(Arrays.asList(1, 2, 3));
        Object[] snapshot = list.toArray();
        // Change the snapshot
        snapshot[0] = 99;
        // The list must still contain the original values
        assertEquals(1, list.get(0), "List element should remain unchanged after mutating the returned array");
        assertEquals(3, list.size(), "List size must stay the same");
    }

    /**
     * -----------------------------------------------------------------
     *  When a user supplies their own backing List, ConcurrentList should
     *  delegate the toArray() call to that List.  We verify the delegation
     *  using a Mockito spy.
     *  -----------------------------------------------------------------
     */
    @Test
    void toArray_DelegatesToUnderlyingList_whenWrappedListProvided() {
        // Create a real list and wrap it with a Mockito spy so we can verify interactions.
        List<Integer> backing = new ArrayList<>(Arrays.asList(5, 6, 7));
        @SuppressWarnings("unchecked")
        List<Integer> spyBacking = spy(backing);
        // Construct a ConcurrentList that wraps the spy.
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(spyBacking);
        // Call the method under test.
        Object[] result = wrapped.toArray();
        // Verify that the spy's toArray() method was invoked exactly once.
        verify(spyBacking, times(1)).toArray();
        // Also assert that the result matches the backing list's contents.
        assertArrayEquals(new Object[] { 5, 6, 7 }, result, "Returned array must reflect the contents of the wrapped list");
    }

    /**
     * -----------------------------------------------------------------
     *  Parameterized tests for various content scenarios (nulls, mixed,
     *  single element, etc.).
     *  -----------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideListsForToArray")
    void toArray_VariousContents_ReturnsCorrectArray(String description, List<Integer> input, Object[] expected) {
        // Populate the list
        input.forEach(list::add);
        // Capture the snapshot
        Object[] result = list.toArray();
        // Verify length and content
        assertArrayEquals(expected, result, description);
    }

    /**
     * -----------------------------------------------------------------
     *  Snapshot semantics – modifications after toArray must not affect the
     *  previously returned array.
     *  -----------------------------------------------------------------
     */
    @Test
    void toArray_SnapshotIsImmutableAfterSubsequentModifications() {
        list.addAll(Arrays.asList(100, 200, 300));
        Object[] snapshot = list.toArray();
        // Mutate the underlying list after the snapshot was taken
        list.add(400);
        list.remove(Integer.valueOf(200));
        // The snapshot must stay unchanged
        assertArrayEquals(new Object[] { 100, 200, 300 }, snapshot, "Array snapshot must not reflect later modifications");
    }

    /**
     * -----------------------------------------------------------------
     *  The length of the returned array must equal the size of the list at
     *  the moment toArray() is invoked, regardless of internal capacity.
     *  -----------------------------------------------------------------
     */
    @Test
    void toArray_LengthMatchesSizeAtCallTime() {
        // Populate with 7 elements
        for (int i = 0; i < 7; i++) {
            list.add(i);
        }
        // Capture size before calling toArray
        int expectedSize = list.size();
        Object[] result = list.toArray();
        assertEquals(expectedSize, result.length, "Returned array length must equal list size at call time");
    }

    /**
     * -----------------------------------------------------------------
     *  Concurrent modification test – while another thread attempts to add
     *  elements, toArray() should still return a consistent snapshot.
     *  -----------------------------------------------------------------
     */
    @Test
    void toArray_ConcurrentModificationDuringCall_StillReturnsConsistentSnapshot() throws Exception {
        // The list starts empty.
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        Thread modifier = new Thread(() -> {
            try {
                // Wait until the main thread signals that toArray is about to run
                startLatch.await();
                // Perform a modification that could interleave with toArray
                list.add(999);
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        modifier.start();
        // Signal the modifier thread that we are about to call toArray
        startLatch.countDown();
        // Call toArray – due to the read lock inside ConcurrentList, this should
        // capture the state before the modifier thread's addition becomes visible.
        Object[] snapshot = list.toArray();
        // Wait for the modifier thread to finish
        doneLatch.await();
        // The snapshot must be empty because the list was empty when the read lock was taken.
        assertEquals(0, snapshot.length, "Snapshot should be empty despite concurrent addition");
        // Verify that the list now contains the element added by the other thread.
        assertEquals(1, list.size());
        assertEquals(999, list.get(0));
    }

    /**
     * -----------------------------------------------------------------
     *  Stress test with 1 000 elements – verifies correct allocation and order.
     *  -----------------------------------------------------------------
     */
    @Test
    void toArray_LargeNumberOfElements_ReturnsFullArrayInOrder() {
        int count = 1000;
        for (int i = 0; i < count; i++) {
            list.add(i);
        }
        Object[] result = list.toArray();
        assertEquals(count, result.length, "Array length must match the number of inserted elements");
        for (int i = 0; i < count; i++) {
            assertEquals(i, result[i], "Element at index " + i + " is out of order");
        }
    }

    /**
     * -----------------------------------------------------------------
     *  After clearing the list, toArray() must return an empty array.
     *  -----------------------------------------------------------------
     */
    @Test
    void toArray_AfterClear_ReturnsEmptyArray() {
        list.addAll(Arrays.asList(1, 2, 3));
        list.clear();
        Object[] result = list.toArray();
        assertNotNull(result, "Result must not be null");
        assertEquals(0, result.length, "Array must be empty after clear()");
    }
}
