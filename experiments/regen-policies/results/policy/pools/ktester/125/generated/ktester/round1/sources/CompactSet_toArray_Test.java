package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

// Mockito not used but kept as per template
@ExtendWith(MockitoExtension.class)
class CompactSet_toArray_Test {

    static Stream<Arguments> basicBehaviourProvider() {
        return Stream.of(Arguments.of("Empty set returns empty array", (Consumer<CompactSet<Object>>) s -> {
        }, /* no elements */
        new Object[] {}), Arguments.of("Single null element", (Consumer<CompactSet<Object>>) s -> s.add(null), new Object[] { null }), Arguments.of("Null among elements", (Consumer<CompactSet<Object>>) s -> {
            s.add("x");
            s.add(null);
            s.add("y");
        }, new Object[] { "x", null, "y" }));
    }

    /**
     * Verify that an empty CompactSet returns an empty Object[] from toArray().
     */
    @Test
    void toArray_EmptySet_ReturnsEmptyArray() {
        CompactSet<String> set = new CompactSet<>();
        Object[] arr = set.toArray();
        assertNotNull(arr, "Returned array should not be null");
        assertEquals(0, arr.length, "Array length should be 0 for an empty set");
    }

    /**
     * Verify that toArray() returns the elements in the iteration order of the set.
     * The default builder creates an insertion‑ordered set.
     */
    @Test
    void toArray_InsertionOrder_ReturnsElementsInOrder() {
        CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
        set.add("first");
        set.add("second");
        set.add("third");
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[] { "first", "second", "third" }, arr, "toArray() should preserve insertion order");
    }

    /**
     * Verify the generic toArray(T[]) overload when the supplied array is larger than the set.
     * The element immediately after the last populated slot must be set to {@code null}.
     */
    @Test
    void toArray_Generic_LargerArray_FillsAndNullTerminator() {
        CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
        set.add("alpha");
        set.add("beta");
        set.add("gamma");
        // larger than the collection
        String[] supplied = new String[5];
        String[] result = set.toArray(supplied);
        // The same array instance should be returned
        assertSame(supplied, result, "When the supplied array is large enough, it should be returned");
        // Verify contents and null terminator
        assertEquals("alpha", result[0]);
        assertEquals("beta", result[1]);
        assertEquals("gamma", result[2]);
        assertNull(result[3], "The element after the last should be null");
        // The remaining slot (index 4) is unspecified by the contract; we simply ensure no exception occurs
    }

    /**
     * Verify the generic toArray(T[]) overload when the supplied array is exactly the right size.
     */
    @Test
    void toArray_Generic_ExactSizeArray_FillsWithoutNull() {
        CompactSet<Integer> set = CompactSet.<Integer>builder().insertionOrder().build();
        set.add(10);
        set.add(20);
        set.add(30);
        Integer[] supplied = new Integer[3];
        Integer[] result = set.toArray(supplied);
        // The same array instance should be returned
        assertSame(supplied, result, "Exact‑size array should be returned");
        assertArrayEquals(new Integer[] { 10, 20, 30 }, result, "Array should contain all elements in insertion order");
    }

    /**
     * Verify the generic toArray(T[]) overload when the supplied array is smaller than the set.
     * A new array of the proper size must be allocated and returned.
     */
    @Test
    void toArray_Generic_SmallerArray_ReturnsNewArray() {
        CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
        set.add("one");
        set.add("two");
        set.add("three");
        // smaller than collection size
        String[] supplied = new String[2];
        String[] result = set.toArray(supplied);
        // A new array must be created; it should NOT be the same instance as the supplied one
        assertNotSame(supplied, result, "A new array should be allocated when the supplied one is too small");
        assertArrayEquals(new String[] { "one", "two", "three" }, result, "Returned array must contain all elements in correct order");
    }

    /**
     * Verify that toArray() works correctly with a CompactSet built with a custom configuration
     * (case‑insensitive, sorted order). The resulting array should be sorted according to the
     * set's ordering semantics.
     */
    @Test
    void toArray_SortedCaseInsensitive_ReturnsSortedLowerCaseElements() {
        CompactSet<String> set = CompactSet.<String>builder().caseSensitive(false).sortedOrder().compactSize(50).build();
        set.add("Zebra");
        set.add("apple");
        set.add("Monkey");
        Object[] arr = set.toArray();
        // Because the set is case‑insensitive and sorted, the natural order of lower‑cased strings is expected
        Object[] expected = { "apple", "Monkey", "Zebra" };
        assertArrayEquals(expected, arr, "Sorted, case‑insensitive set should produce a sorted array (original case preserved)");
    }

    /**
     * When the CompactSet contains only a null element, toArray() should return an
     * Object[] containing a single null.
     */
    @Test
    void toArray_SingleNullElement_ReturnsArrayWithNull() {
        CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
        set.add(null);
        Object[] arr = set.toArray();
        assertEquals(1, arr.length, "Array length should be 1");
        assertNull(arr[0], "The sole element must be null");
    }

    /**
     * Verify that a null entry among other elements is preserved in the result.
     */
    @Test
    void toArray_NullAmongElements_PreservesNullPosition() {
        CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
        set.add("x");
        set.add(null);
        set.add("y");
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[] { "x", null, "y" }, arr, "Array should preserve null entry in its original position");
    }

    /**
     * Add duplicate elements and ensure they appear only once, preserving the order
     * of first insertion.
     */
    @Test
    void toArray_DuplicateElements_IgnoredAfterFirstInsertion() {
        CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
        set.add("dup");
        // duplicate
        set.add("dup");
        set.add("unique");
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[] { "dup", "unique" }, arr, "Duplicates must be stored only once, preserving first insertion order");
    }

    /**
     * Insert 1000 distinct strings and verify that toArray() returns all of them
     * in insertion order.
     */
    @Test
    void toArray_LargeSet_ReturnsAllElementsInOrder() {
        CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
        int size = 1000;
        for (int i = 0; i < size; i++) {
            set.add(String.valueOf(i));
        }
        Object[] arr = set.toArray();
        assertEquals(size, arr.length, "Array length must match number of inserted elements");
        for (int i = 0; i < size; i++) {
            assertEquals(String.valueOf(i), arr[i], "Element at index " + i + " should match insertion order");
        }
    }

    /**
     * After removing an element, toArray() must reflect the removal.
     */
    @Test
    void toArray_AfterRemoval_ReflectsCurrentState() {
        CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
        set.add("first");
        set.add("second");
        set.add("third");
        set.remove("second");
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[] { "first", "third" }, arr, "Array should contain only the remaining elements in original order");
    }

    /**
     * After invoking {@code clear()}, the set should be empty and toArray()
     * must return an empty array.
     */
    @Test
    void toArray_AfterClear_ReturnsEmptyArray() {
        CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
        set.add("a");
        set.add("b");
        set.add("c");
        set.clear();
        Object[] arr = set.toArray();
        assertEquals(0, arr.length, "Array must be empty after clear()");
    }

    /**
     * Ensure that the array returned by {@code toArray()} is a copy, not a live view.
     */
    @Test
    void toArray_ReturnedArrayIsIndependentFromSet() {
        CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
        set.add("alpha");
        set.add("beta");
        Object[] first = set.toArray();
        // Mutate the returned array
        first[0] = "changed";
        // Subsequent call must produce the original contents
        Object[] second = set.toArray();
        assertArrayEquals(new Object[] { "alpha", "beta" }, second, "Modifying the previously returned array must not affect the set");
    }

    /**
     * Verify that heterogeneous element types are preserved in the resulting array.
     */
    @Test
    void toArray_MixedElementTypes_PreservedInOrder() {
        CompactSet<Object> set = CompactSet.<Object>builder().insertionOrder().build();
        set.add(1);
        set.add("two");
        set.add(3);
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[] { 1, "two", 3 }, arr, "Array must preserve mixed types and insertion order");
    }

    /**
     * Adding {@code null} twice should result in a single {@code null} entry.
     */
    @Test
    void toArray_NullDuplicate_IgnoredAfterFirstInsertion() {
        CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
        set.add(null);
        // duplicate null
        set.add(null);
        set.add("val");
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[] { null, "val" }, arr, "Duplicate nulls must be ignored, preserving a single null entry");
    }

    /**
     * Simulate a concurrent modification while {@code toArray()} is executing.
     * The expected outcome is a {@link ConcurrentModificationException}.
     */
    @Test
    void toArray_ConcurrentModification_ThrowsCME() throws Exception {
        // Build a relatively large set to increase the window where modification can happen
        CompactSet<Integer> set = CompactSet.<Integer>builder().insertionOrder().build();
        for (int i = 0; i < 10_000; i++) {
            set.add(i);
        }
        // Latch to make sure the modifier thread runs while toArray() is in progress
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch modifyLatch = new CountDownLatch(1);
        Callable<Object[]> task = () -> {
            // signal that we are about to call toArray()
            startLatch.countDown();
            // Wait a moment to give the other thread a chance to modify
            Thread.sleep(1);
            // may throw CME
            return set.toArray();
        };
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Object[]> future = executor.submit(task);
        // Wait until the toArray thread has started
        startLatch.await();
        // Perform modification
        set.add(-1);
        modifyLatch.countDown();
        // Expect CME
        try {
            // will rethrow exception if any
            future.get();
            fail("Expected ConcurrentModificationException was not thrown");
        } catch (ExecutionException ee) {
            assertTrue(ee.getCause() instanceof ConcurrentModificationException, "Expected ConcurrentModificationException, got: " + ee.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Using reflection, set the internal {@code map} field to {@code null}
     * and verify that {@code toArray()} throws {@link NullPointerException}.
     */
    @Test
    void toArray_InternalMapNull_ThrowsNPE() throws Exception {
        CompactSet<String> set = new CompactSet<>();
        // Force the private 'map' field to null via reflection
        Field mapField = CompactSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, null);
        assertThrows(NullPointerException.class, set::toArray, "Calling toArray() when internal map is null should throw NullPointerException");
    }

    /**
     * Parameterized test covering several basic scenarios:
     * <ul>
     *   <li>empty set</li>
     *   <li>single null element</li>
     *   <li>mix of null and non‑null</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicBehaviourProvider")
    void toArray_BasicBehaviour(String description, Consumer<CompactSet<Object>> setup, Object[] expected) {
        CompactSet<Object> set = CompactSet.<Object>builder().insertionOrder().build();
        setup.accept(set);
        Object[] actual = set.toArray();
        assertArrayEquals(expected, actual, description);
    }
}
