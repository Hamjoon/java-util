package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

class CaseInsensitiveSet_toArray_2_Test {

    private CaseInsensitiveSet<String> set;

    /**
     * Data holder for a single test case.
     */
    private static class ToArrayCase {

        final String description;

        // the array instance to pass
        final Object suppliedArray;

        // either an array instance or an exception class
        final Object expectedResult;

        ToArrayCase(String description, Object suppliedArray, Object expectedResult) {
            this.description = description;
            this.suppliedArray = suppliedArray;
            this.expectedResult = expectedResult;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test case.
        set = new CaseInsensitiveSet<>();
        set.add("Apple");
        set.add("banana");
        set.add("Cherry");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to aid GC and avoid cross‑test contamination.
        set.clear();
    }

    /**
     * Provides the stream of test cases for the parameterized test.
     */
    private static Stream<ToArrayCase> toArrayCases() {
        // Helper to create a fresh set with possible duplicate‑case entries for one case.
        CaseInsensitiveSet<String> duplicateSet = new CaseInsensitiveSet<>();
        duplicateSet.add("Alpha");
        // duplicate, ignored
        duplicateSet.add("alpha");
        duplicateSet.add("BETA");
        // duplicate, ignored
        duplicateSet.add("beta");
        return Stream.of(// ---- Empty set scenarios ----
        new ToArrayCase("Empty set, supplied array length 0 -> empty array", new String[0], new String[0]), new ToArrayCase("Empty set, supplied larger array -> same array with first element null", new String[5], // expectation: same instance, first element null, rest unchanged (null)
        new String[] { null, null, null, null, null }), // ---- Non‑empty set scenarios ----
        new ToArrayCase("Non‑empty set, supplied array length 0 -> new array of size 3", new String[0], new String[] { "alpha", "beta", "gamma" }), new ToArrayCase("Non‑empty set, supplied array exact size 3 -> same array filled", new String[3], new String[] { "alpha", "beta", "gamma" }), new ToArrayCase("Non‑empty set, supplied array larger (5) -> same array with null sentinel", new String[5], new String[] { "alpha", "beta", "gamma", null, null }), new ToArrayCase("Supplied Object[3] (supertype) -> same array filled with Strings", new Object[3], new Object[] { "alpha", "beta", "gamma" }), new ToArrayCase("Supplied null array -> NullPointerException", null, NullPointerException.class), new ToArrayCase("Supplied Integer[3] (incompatible) -> ArrayStoreException", new Integer[3], ArrayStoreException.class), // ---- Additional scenarios ----
        new ToArrayCase("Supplied String[2] (smaller) -> new String[3] allocated", new String[2], new String[] { "alpha", "beta", "gamma" }), new ToArrayCase("Supplied Object[5] (larger, supertype) -> same array with nulls", new Object[5], new Object[] { "alpha", "beta", "gamma", null, null }), new ToArrayCase("Supplied Object[0] (zero length, supertype) -> new Object[3]", new Object[0], new Object[] { "alpha", "beta", "gamma" }), new ToArrayCase("Set with case‑insensitive duplicates, supplied String[0] -> new array size 2", new String[0], // Expected array content based on insertion order of distinct elements
        new String[] { "Alpha", "BETA" }), new ToArrayCase("Supplied CharSequence[3] (interface supertype) -> same array filled", new CharSequence[3], new CharSequence[] { "alpha", "beta", "gamma" }), new ToArrayCase("Supplied Serializable[3] (another supertype) -> same array filled", new java.io.Serializable[3], new java.io.Serializable[] { "alpha", "beta", "gamma" }), new ToArrayCase("Returned array modification does not affect set", new String[3], // special marker handled below
        "mutation-check"), new ToArrayCase("Empty set, supplied Object[0] -> same empty array instance", new Object[0], // special marker
        "same-instance"));
    }

    @Test
    @DisplayName("toArray(T[]) returns the supplied array when it has exact size")
    void testToArrayExactSize() {
        // GIVEN
        String[] supplied = new String[3];
        // WHEN
        String[] result = set.toArray(supplied);
        // THEN
        // The returned array should be the same instance that was supplied.
        assertSame(supplied, result, "Returned array should be the supplied array when it fits exactly");
        // All elements must be present, order is not guaranteed.
        assertTrue(Arrays.asList(result).containsAll(set), "Result array must contain all elements of the set");
        assertEquals(set.size(), result.length, "Array length must equal set size");
    }

    @Test
    @DisplayName("toArray(T[]) returns a larger supplied array with a trailing null")
    void testToArrayLargerArray() {
        // GIVEN
        // larger than set size (3)
        String[] supplied = new String[5];
        // WHEN
        String[] result = set.toArray(supplied);
        // THEN
        assertSame(supplied, result, "Returned array should be the supplied array when it is larger");
        // Elements must be present in the first part of the array.
        assertTrue(Arrays.asList(result).subList(0, set.size()).containsAll(set), "First part of the array must contain all set elements");
        // The element immediately after the last valid entry must be null.
        assertNull(result[set.size()], "Element after the last set element must be null");
    }

    @Test
    @DisplayName("toArray(T[]) returns a new array when supplied array is too small")
    void testToArraySmallerArray() {
        // GIVEN
        // smaller than set size
        String[] supplied = new String[1];
        // WHEN
        String[] result = set.toArray(supplied);
        // THEN
        // The returned array must NOT be the supplied one because it was too small.
        assertNotSame(supplied, result, "Returned array should be a new array when supplied array is too small");
        // Length must match the set size.
        assertEquals(set.size(), result.length, "Result array length must equal set size");
        // All elements must be present.
        assertTrue(Arrays.asList(result).containsAll(set), "Result array must contain all elements of the set");
    }

    @Test
    @DisplayName("toArray(T[]) throws NullPointerException when supplied array is null")
    void testToArrayNullArray() {
        // GIVEN
        String[] supplied = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> set.toArray(supplied), "Calling toArray with a null array should throw NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("toArrayCases")
    void testToArrayParameterized(ToArrayCase tc) {
        // For the special cases that need a custom set or extra verification,
        // we adjust the fixture accordingly.
        if (tc.description.contains("case‑insensitive duplicates")) {
            // Re‑create the set with duplicates.
            set.clear();
            set.add("Alpha");
            // duplicate, ignored
            set.add("alpha");
            set.add("BETA");
            // duplicate, ignored
            set.add("beta");
        }
        // -----------------------------------------------------------------
        // Execute and verify according to the expected result type.
        // -----------------------------------------------------------------
        if (tc.expectedResult instanceof Class) {
            // Expect an exception.
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) tc.expectedResult;
            assertThrows(exClass, () -> set.toArray((Object[]) tc.suppliedArray), tc.description);
            return;
        }
        // Normal array result expected.
        Object[] supplied = (Object[]) tc.suppliedArray;
        Object[] result = set.toArray(supplied);
        // Special markers handling
        if ("mutation-check".equals(tc.expectedResult)) {
            // Verify that mutating the returned array does not affect the set.
            assertNotNull(result);
            assertEquals(3, result.length);
            // Mutate
            result[0] = "changed";
            // The set must still contain the original elements.
            assertTrue(set.contains("alpha"));
            assertFalse(set.contains("changed"));
            return;
        }
        if ("same-instance".equals(tc.expectedResult)) {
            // For empty set with zero‑length array, the contract says the same array is returned.
            assertSame(supplied, result, "Empty set with zero‑length array should return the supplied array unchanged");
            assertEquals(0, result.length);
            return;
        }
        // From here on, expectedResult is an array with the exact contents we anticipate.
        Object[] expectedArray = (Object[]) tc.expectedResult;
        // Verify whether the method returned the original array or allocated a new one.
        if (supplied.length >= set.size() && supplied.length != 0) {
            // When the supplied array is large enough, the spec says it should be returned.
            assertSame(supplied, result, "When supplied array is large enough, the same instance must be returned");
        } else {
            // Otherwise a new array must be created.
            assertNotSame(supplied, result, "When supplied array is too small, a new array must be allocated");
        }
        // Length checks
        assertEquals(expectedArray.length, result.length, "Result array length must match expected length");
        // Content checks – order is defined by the set's iterator (which is deterministic for the
        // underlying map implementation). We compare using List equality to avoid ordering issues
        // when the contract does not guarantee order.
        assertEquals(Arrays.asList(expectedArray), Arrays.asList(result), "Array contents must match expected values for case: " + tc.description);
    }
}
