package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Array;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_toArray_2_Test {

    /**
     * Instance used by the tests.
     */
    private ConcurrentSet<String> stringSet;

    /**
     * Create a fresh set before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        stringSet = new ConcurrentSet<>();
    }

    /**
     * Clean reference after each test – not strictly required but keeps the pattern tidy.
     */
    @AfterEach
    void teardownAfterEach() {
        stringSet = null;
    }

    /**
     * Verify that when the supplied array is larger than the set size,
     * the method returns the same array instance, fills the first {@code size}
     * positions with the set elements (order is unspecified) and writes a
     * {@code null} marker at the first unused slot.
     */
    @Test
    void testToArrayArrayLargerThanSet() {
        // Arrange – add three elements, one of them null
        stringSet.addAll(Arrays.asList("alpha", null, "beta"));
        assertEquals(3, stringSet.size());
        // An array larger than the set (5 slots).  Pre‑populate with a sentinel value
        // to ensure the method really overwrites only the required positions.
        String[] supplied = new String[5];
        Arrays.fill(supplied, "UNUSED");
        // Act
        String[] result = stringSet.toArray(supplied);
        // Assert – the same array instance must be returned
        assertSame(supplied, result, "Method should return the supplied array when it is large enough");
        // The element immediately after the last real entry must be null
        assertNull(result[3], "The first slot after the set contents must be set to null");
        // The remaining slot (index 4) should stay untouched (still \"UNUSED\")
        assertEquals("UNUSED", result[4], "Slots beyond the first null marker must remain unchanged");
        // The three populated slots must contain exactly the three elements of the set,
        // order is not defined, so we check as a collection.
        List<String> populated = Arrays.asList(result[0], result[1], result[2]);
        assertTrue(populated.containsAll(Arrays.asList("alpha", "beta", null)), "Array must contain the three set elements (including null)");
    }

    /**
     * Verify that when the supplied array is smaller than the set size,
     * a new array of the correct size is allocated and filled with the set
     * elements.  The returned array must not be the same instance as the
     * supplied one.
     */
    @Test
    void testToArrayArraySmallerThanSet() {
        // Arrange – three distinct elements (including null)
        stringSet.addAll(Arrays.asList("x", null, "y"));
        assertEquals(3, stringSet.size());
        // A too‑small array (size 1)
        String[] supplied = new String[1];
        supplied[0] = "placeholder";
        // Act
        String[] result = stringSet.toArray(supplied);
        // Assert – a new array must be created
        assertNotSame(supplied, result, "Method should allocate a new array when supplied one is too small");
        assertEquals(3, result.length, "New array length must match the set size");
        // Verify contents (order not guaranteed)
        List<String> resultList = Arrays.asList(result);
        assertTrue(resultList.containsAll(Arrays.asList("x", "y", null)), "Result array must contain all set elements (including null)");
    }

    /**
     * Verify the behaviour when the supplied array length exactly matches the set size.
     * No trailing {@code null} element should be added.
     */
    @Test
    void testToArrayArrayExactSize() {
        // Arrange
        stringSet.addAll(Arrays.asList("one", "two"));
        assertEquals(2, stringSet.size());
        String[] exact = new String[2];
        exact[0] = "first";
        exact[1] = "second";
        // Act
        String[] result = stringSet.toArray(exact);
        // Assert – same instance, no extra null slot
        assertSame(exact, result, "When array length equals set size the same array should be returned");
        // No extra slot, so we only check that the two positions hold the set elements
        List<String> resultList = Arrays.asList(result);
        assertTrue(resultList.containsAll(Arrays.asList("one", "two")), "Result array must contain exactly the set elements");
    }

    /**
     * Verify that the generic version works for a different element type
     * (Integer) and that the returned array type matches the supplied array's
     * component type.
     */
    @Test
    void testToArrayIntegerSet() {
        ConcurrentSet<Integer> intSet = new ConcurrentSet<>();
        intSet.addAll(Arrays.asList(1, 2, 3));
        // let the method allocate the right size
        Integer[] supplied = new Integer[0];
        Integer[] result = intSet.toArray(supplied);
        assertEquals(3, result.length);
        // Order of a ConcurrentHashMap‑backed set is not guaranteed, so we verify as a collection
        List<Integer> list = Arrays.asList(result);
        assertTrue(list.containsAll(Arrays.asList(1, 2, 3)), "Array should contain the three integers (order not important)");
    }

    /**
     * Verify that when the set is empty, the method returns the supplied array
     * (if it is large enough) and writes a {@code null} marker at index 0.
     */
    @Test
    void testToArrayEmptySet() {
        // Empty set, supplied array larger than needed
        String[] supplied = new String[2];
        supplied[0] = "a";
        supplied[1] = "b";
        String[] result = stringSet.toArray(supplied);
        assertSame(supplied, result);
        assertNull(result[0], "First element must be null when the set is empty");
        // The rest of the array should stay untouched
        assertEquals("b", result[1]);
    }

    /**
     * Group: Array length equals set size (no resizing, no extra slot)
     */
    @Test
    void testArrayLengthEqualsSetSize_NoResize() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", "b"));
        // length == set size
        String[] supplied = new String[] { "x", "y" };
        String[] result = set.toArray(supplied);
        assertSame(supplied, result, "Should return the supplied array when sizes match");
        List<String> contents = Arrays.asList(result);
        assertTrue(contents.containsAll(Arrays.asList("a", "b")), "Array must contain exactly the set elements");
    }

    /**
     * Group: Array length greater than set size (trailing null must be set)
     */
    @Test
    void testArrayLengthGreaterThanSetSize_TrailingNull() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2));
        Integer[] supplied = new Integer[5];
        // sentinel to see what stays unchanged
        Arrays.fill(supplied, 99);
        Integer[] result = set.toArray(supplied);
        assertSame(supplied, result, "Should return the supplied array when it is large enough");
        // Verify first two positions hold the set elements (order not guaranteed)
        List<Integer> firstTwo = Arrays.asList(result[0], result[1]);
        assertTrue(firstTwo.containsAll(Arrays.asList(1, 2)));
        // Position at index == size must be null
        assertNull(result[2], "Trailing null marker must be placed at index equal to set size");
        // Remaining positions should stay as they were (99)
        assertEquals(99, result[3]);
        assertEquals(99, result[4]);
    }

    /**
     * Group: Array length smaller than set size (new array allocation)
     */
    @Test
    void testArrayLengthSmallerThanSetSize_NewAllocation() {
        ConcurrentSet<Long> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(10L, 20L));
        // too small
        Long[] supplied = new Long[1];
        Long[] result = set.toArray(supplied);
        assertNotSame(supplied, result, "A new array must be allocated when supplied one is too small");
        assertEquals(2, result.length);
        List<Long> list = Arrays.asList(result);
        assertTrue(list.containsAll(Arrays.asList(10L, 20L)));
    }

    /**
     * Group: Set contains a null element represented by NullSentinel
     */
    @Test
    void testSetContainsNullSentinel_ExactSize() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add("value");
        // stored internally as NullSentinel
        set.add(null);
        Object[] supplied = new Object[2];
        supplied[0] = "placeholder1";
        supplied[1] = "placeholder2";
        Object[] result = set.toArray(supplied);
        assertSame(supplied, result);
        // The array must contain the real null, not the sentinel object
        List<Object> list = Arrays.asList(result);
        assertTrue(list.containsAll(Arrays.asList("value", null)));
    }

    /**
     * Group: Set contains null sentinel and supplied array is larger (null sentinel + trailing null)
     */
    @Test
    void testSetContainsNullSentinel_LargerArray() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add("first");
        set.add(null);
        String[] supplied = new String[4];
        Arrays.fill(supplied, "UNCHANGED");
        String[] result = set.toArray(supplied);
        assertSame(supplied, result);
        // Verify the two real elements are present (order not guaranteed)
        List<String> firstTwo = Arrays.asList(result[0], result[1]);
        assertTrue(firstTwo.containsAll(Arrays.asList("first", null)));
        // Index 2 (size) must be null (trailing marker)
        assertNull(result[2], "Trailing null marker must be set at index equal to set size");
        // Remaining slot stays unchanged (null because we filled with nulls)
        assertNull(result[3]);
    }

    /**
     * Group: Empty set with zero-length array
     */
    @Test
    void testEmptySet_ZeroLengthArray() {
        Object[] supplied = new Object[0];
        Object[] result = stringSet.toArray(supplied);
        assertSame(supplied, result, "Empty set should return the supplied zero‑length array");
        assertEquals(0, result.length);
    }

    /**
     * Group: Empty set with larger array (trailing null)
     */
    @Test
    void testEmptySet_LargerArray_TrailingNull() {
        String[] supplied = new String[3];
        Arrays.fill(supplied, "X");
        String[] result = stringSet.toArray(supplied);
        assertSame(supplied, result);
        // Index 0 must be null (size == 0)
        assertNull(result[0], "First element must be null for empty set with larger array");
        // The rest of the array should stay unchanged
        assertEquals("X", result[1]);
        assertEquals("X", result[2]);
    }

    /**
     * Group: Non‑empty set with zero‑length array (new allocation)
     */
    @Test
    void testNonEmptySet_ZeroLengthArray_NewAllocation() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(5, 10));
        Integer[] supplied = new Integer[0];
        Integer[] result = set.toArray(supplied);
        assertNotSame(supplied, result);
        assertEquals(2, result.length);
        List<Integer> list = Arrays.asList(result);
        assertTrue(list.containsAll(Arrays.asList(5, 10)));
    }

    /**
     * Group: Type widening – Integer set to Number array
     */
    @Test
    void testTypeWidening_IntegerToNumberArray() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(7, 14));
        Number[] supplied = new Number[2];
        Number[] result = set.toArray(supplied);
        assertSame(supplied, result);
        List<Number> list = Arrays.asList(result);
        assertTrue(list.containsAll(Arrays.asList(7, 14)));
    }

    /**
     * Group: Type incompatibility – Integer set to String array (exception)
     */
    @Test
    void testTypeIncompatibility_IntegerToStringArray() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2));
        String[] supplied = new String[2];
        assertThrows(ClassCastException.class, () -> set.toArray(supplied), "Storing Integer into a String[] must throw ClassCastException");
    }

    /**
     * Group: Null array argument (NullPointerException)
     */
    @Test
    void testNullArrayArgument() {
        assertThrows(NullPointerException.class, () -> stringSet.toArray(null), "Passing null as the destination array must throw NullPointerException");
    }

    /**
     * Group: Primitive array with stored null element (ArrayStoreException)
     *
     * The generic method cannot be invoked with a primitive array in a type‑safe way,
     * so we use a raw ConcurrentSet reference to bypass compile‑time checks.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void testPrimitiveArrayWithNullElement() {
        // raw type to allow passing a primitive array
        ConcurrentSet rawSet = new ConcurrentSet();
        // stored as NullSentinel internally
        rawSet.add(null);
        int[] primitiveArray = new int[2];
        assertThrows(ArrayStoreException.class, () -> rawSet.toArray(primitiveArray), "Attempting to store a null into a primitive array must throw ArrayStoreException");
    }
}
