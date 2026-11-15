package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Objects;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactSet_toArray_2_Test {

    /**
     * Set used in the majority of the tests – insertion order guarantees deterministic ordering.
     */
    private CompactSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Build a CompactSet with insertion ordering so that the iteration order is predictable.
        set = CompactSet.<String>builder().insertionOrder().build();
        // Populate the set in a known order.
        set.add("apple");
        set.add("banana");
        set.add("cherry");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid accidental cross‑test contamination.
        set.clear();
    }

    /**
     * Helper to avoid accidental {@code null} elements sneaking into the expected arrays.
     */
    private static void assertArrayEquals(Object[] expected, Object[] actual, String message) {
        // Use JUnit's built‑in assertion after a null‑safety check.
        assertEquals(expected.length, actual.length, message + " – array lengths differ");
        for (int i = 0; i < expected.length; i++) {
            assertTrue(Objects.equals(expected[i], actual[i]), message + " – element at index " + i + " differs (expected: " + expected[i] + ", actual: " + actual[i] + ")");
        }
    }

    @Test
    @DisplayName("toArray(T[]) returns the supplied array when it is exactly the right size")
    void testToArrayExactSize() {
        String[] supplied = new String[3];
        String[] result = set.toArray(supplied);
        // The same array instance must be returned.
        assertSame(supplied, result, "Expected the same array instance to be returned");
        // Verify the contents and order.
        assertArrayEquals(new String[] { "apple", "banana", "cherry" }, result, "Array contents do not match the set's insertion order");
    }

    @Test
    @DisplayName("toArray(T[]) returns the supplied larger array and null‑terminates the remainder")
    void testToArrayLargerArray() {
        String[] supplied = new String[5];
        String[] result = set.toArray(supplied);
        // Same instance should be returned.
        assertSame(supplied, result, "Expected the same array instance to be returned");
        // First three positions contain the set elements.
        assertArrayEquals(new String[] { "apple", "banana", "cherry", null, null }, result, "Trailing elements should be null when the supplied array is larger than needed");
    }

    @Test
    @DisplayName("toArray(T[]) returns a new array when the supplied one is too small")
    void testToArraySmallerArray() {
        // smaller than the set size (3)
        String[] supplied = new String[2];
        String[] result = set.toArray(supplied);
        // A new array must be created.
        assertNotSame(supplied, result, "Expected a new array instance because the supplied one was too small");
        assertEquals(3, result.length, "Resulting array should have the exact size of the set");
        // Verify contents.
        assertArrayEquals(new String[] { "apple", "banana", "cherry" }, result, "Array contents do not match the set's insertion order");
    }

    @Test
    @DisplayName("toArray(T[]) works with a super‑type array (Object[])")
    void testToArraySuperTypeArray() {
        Object[] supplied = new Object[3];
        Object[] result = set.toArray(supplied);
        assertSame(supplied, result, "Expected the same Object[] instance to be returned");
        assertArrayEquals(new Object[] { "apple", "banana", "cherry" }, result, "Array contents do not match the set's insertion order");
    }

    @Test
    @DisplayName("toArray(T[]) on an empty set returns an empty array")
    void testToArrayEmptySet() {
        CompactSet<Integer> empty = new CompactSet<>();
        Integer[] supplied = new Integer[0];
        Integer[] result = empty.toArray(supplied);
        // According to the Collection contract, the same zero‑length array may be returned.
        assertSame(supplied, result, "Expected the same zero‑length array to be returned for an empty set");
        assertEquals(0, result.length, "Resulting array should be empty");
    }

    @Test
    @DisplayName("toArray(T[]) does not expose internal references (modifying the returned array does not affect the set)")
    void testReturnedArrayIsIndependent() {
        String[] supplied = new String[3];
        String[] result = set.toArray(supplied);
        // Mutate the returned array.
        result[0] = "modified";
        // The set should still contain the original element.
        assertTrue(set.contains("apple"), "Modifying the returned array must not affect the set contents");
        assertFalse(set.contains("modified"), "Set must not contain the mutated value");
    }

    @Test
    @DisplayName("Empty CompactSet with an empty String[] returns the same empty array")
    void testEmptySetWithEmptyArray() {
        CompactSet<String> empty = new CompactSet<>();
        String[] supplied = new String[0];
        String[] result = empty.toArray(supplied);
        assertSame(supplied, result, "Empty set should return the supplied empty array instance");
        assertEquals(0, result.length, "Resulting array should be empty");
    }

    @Test
    @DisplayName("Empty CompactSet with a larger String[] returns the same array filled with nulls")
    void testEmptySetWithLargerArray() {
        CompactSet<String> empty = new CompactSet<>();
        String[] supplied = new String[5];
        String[] result = empty.toArray(supplied);
        assertSame(supplied, result, "Empty set should return the supplied array instance");
        // All positions must be null according to the contract (first element set to null, rest unchanged)
        assertArrayEquals(new String[] { null, null, null, null, null }, result, "All elements must be null for an empty set with a larger array");
    }

    @Test
    @DisplayName("Empty CompactSet with null argument throws NullPointerException")
    void testEmptySetWithNullArray() {
        CompactSet<String> empty = new CompactSet<>();
        assertThrows(NullPointerException.class, () -> empty.toArray((String[]) null), "Passing null to toArray must throw NullPointerException");
    }

    @Test
    @DisplayName("Non‑empty CompactSet with exact‑size array returns the same array")
    void testNonEmptyExactSizeArray() {
        CompactSet<String> smallSet = CompactSet.<String>builder().insertionOrder().build();
        smallSet.add("alpha");
        smallSet.add("beta");
        String[] supplied = new String[2];
        String[] result = smallSet.toArray(supplied);
        assertSame(supplied, result, "When array size matches set size, the same instance must be returned");
        assertArrayEquals(new String[] { "alpha", "beta" }, result, "Array contents must reflect insertion order");
    }

    @Test
    @DisplayName("Non‑empty CompactSet with larger array returns same array with null termination")
    void testNonEmptyLargerArray() {
        CompactSet<String> smallSet = CompactSet.<String>builder().insertionOrder().build();
        smallSet.add("alpha");
        smallSet.add("beta");
        String[] supplied = new String[5];
        String[] result = smallSet.toArray(supplied);
        assertSame(supplied, result, "When supplied array is larger, the same instance must be returned");
        assertArrayEquals(new String[] { "alpha", "beta", null, null, null }, result, "Elements after the last must be null");
    }

    @Test
    @DisplayName("Non‑empty CompactSet with smaller array returns a new correctly sized array")
    void testNonEmptySmallerArray() {
        CompactSet<String> smallSet = CompactSet.<String>builder().insertionOrder().build();
        smallSet.add("alpha");
        smallSet.add("beta");
        // too small
        String[] supplied = new String[1];
        String[] result = smallSet.toArray(supplied);
        assertNotSame(supplied, result, "A new array must be created when supplied array is too small");
        assertEquals(2, result.length, "New array length must equal set size");
        assertArrayEquals(new String[] { "alpha", "beta" }, result, "New array must contain the set elements in insertion order");
    }

    @Test
    @DisplayName("Type mismatch between set elements and array component type throws ArrayStoreException")
    void testTypeMismatchArrayStoreException() {
        CompactSet<Integer> intSet = CompactSet.<Integer>builder().insertionOrder().build();
        intSet.add(1);
        intSet.add(2);
        String[] supplied = new String[2];
        assertThrows(ArrayStoreException.class, () -> intSet.toArray(supplied), "Supplying an array of an incompatible component type must throw ArrayStoreException");
    }
}
