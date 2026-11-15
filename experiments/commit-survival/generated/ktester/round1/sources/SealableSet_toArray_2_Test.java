package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableSet_toArray_2_Test {

    private SealableSet<Integer> set;

    private volatile boolean sealed = false;

    private Supplier<Boolean> sealedSupplier = () -> sealed;

    /* --------------------------------------------------------------------- *
     *  Common fixtures used by the original integer‑based tests.
     *  They are left untouched to preserve the provided test‑class framework.
     * --------------------------------------------------------------------- */
    private SealableSet<Integer> intSet;

    @BeforeEach
    void setupBeforeEach() {
        intSet = new SealableSet<>(sealedSupplier);
        intSet.add(10);
        intSet.add(20);
        intSet.add(null);
    }

    /* --------------------------------------------------------------------- *
     *  Helper to create a SealableSet with arbitrary elements.
     * --------------------------------------------------------------------- */
    private <E> SealableSet<E> createSet(E... elements) {
        SealableSet<E> set = new SealableSet<>(sealedSupplier);
        for (E e : elements) {
            set.add(e);
        }
        return set;
    }

    private static boolean contains(Object[] array, Object value) {
        for (Object o : array) {
            if (value == null ? o == null : value.equals(o)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsFirstTwo(Object[] array, Object v1, Object v2) {
        // Order is not guaranteed, so we just check that both appear in the first two positions
        boolean first = (array[0] == null ? v1 == null : array[0].equals(v1)) || (array[0] == null ? v2 == null : array[0].equals(v2));
        boolean second = (array[1] == null ? v1 == null : array[1].equals(v1)) || (array[1] == null ? v2 == null : array[1].equals(v2));
        return first && second;
    }

    @Test
    void testToArrayExactSize() {
        Integer[] supplied = new Integer[3];
        Integer[] result = intSet.toArray(supplied);
        // The supplied array should be returned
        assertSame(supplied, result, "Should return the supplied array when it fits exactly");
        // Verify that all expected elements are present (order is not guaranteed)
        boolean found10 = false, found20 = false, foundNull = false;
        for (Integer i : result) {
            if (i == null) {
                foundNull = true;
            } else if (i == 10) {
                found10 = true;
            } else if (i == 20) {
                found20 = true;
            }
        }
        assertTrue(found10, "Array should contain 10");
        assertTrue(found20, "Array should contain 20");
        assertTrue(foundNull, "Array should contain null");
    }

    @Test
    void testToArrayLargerArray() {
        Integer[] supplied = new Integer[5];
        // Fill with sentinel values to ensure they are not overwritten incorrectly
        for (int i = 0; i < supplied.length; i++) {
            supplied[i] = -1;
        }
        Integer[] result = intSet.toArray(supplied);
        assertSame(supplied, result, "Should return the supplied array when it is larger than needed");
        // First three positions must contain the set elements (order not guaranteed)
        boolean found10 = false, found20 = false, foundNull = false;
        for (int i = 0; i < 3; i++) {
            Integer v = result[i];
            if (v == null) {
                foundNull = true;
            } else if (v == 10) {
                found10 = true;
            } else if (v == 20) {
                found20 = true;
            }
        }
        assertTrue(found10, "First three slots should contain 10");
        assertTrue(found20, "First three slots should contain 20");
        assertTrue(foundNull, "First three slots should contain null");
        // The element immediately after the last should be set to null per Set.toArray contract
        assertNull(result[3], "Element after the last should be null");
        // The remaining slot should retain its original sentinel value (-1)
        assertEquals(-1, result[4].intValue(), "Trailing elements beyond the null terminator should be untouched");
    }

    @Test
    void testToArraySmallerArray() {
        Integer[] supplied = new Integer[0];
        Integer[] result = intSet.toArray(supplied);
        // A new array should be allocated because the supplied one is too small
        assertNotSame(supplied, result, "Should allocate a new array when supplied one is too small");
        assertEquals(3, result.length, "Resulting array should have size equal to the set size");
        // Verify contents
        boolean found10 = false, found20 = false, foundNull = false;
        for (Integer i : result) {
            if (i == null) {
                foundNull = true;
            } else if (i == 10) {
                found10 = true;
            } else if (i == 20) {
                found20 = true;
            }
        }
        assertTrue(found10, "Result should contain 10");
        assertTrue(found20, "Result should contain 20");
        assertTrue(foundNull, "Result should contain null");
    }

    @Test
    void testToArraySupertype() {
        Number[] supplied = new Number[0];
        Number[] result = intSet.toArray(supplied);
        assertEquals(3, result.length);
        boolean found10 = false, found20 = false, foundNull = false;
        for (Number n : result) {
            if (n == null) {
                foundNull = true;
            } else if (n.intValue() == 10) {
                found10 = true;
            } else if (n.intValue() == 20) {
                found20 = true;
            }
        }
        assertTrue(found10, "Supertype array should contain 10");
        assertTrue(found20, "Supertype array should contain 20");
        assertTrue(foundNull, "Supertype array should contain null");
    }

    @Test
    void testStringArrayEmptySupplied() {
        // Underlying set contains "alpha" and "beta"
        SealableSet<String> set = createSet("alpha", "beta");
        String[] supplied = new String[0];
        String[] result = set.toArray(supplied);
        // A new array must be allocated (size == set.size())
        assertNotSame(supplied, result, "A new array should be allocated for an empty supplied array");
        assertEquals(2, result.length, "Resulting array length must match set size");
        // Verify contents (order not guaranteed)
        assertTrue(contains(result, "alpha"));
        assertTrue(contains(result, "beta"));
    }

    @Test
    void testStringArrayLargerSupplied() {
        SealableSet<String> set = createSet("alpha", "beta");
        String[] supplied = new String[5];
        // Fill with sentinel values
        for (int i = 0; i < supplied.length; i++) {
            supplied[i] = "sentinel";
        }
        String[] result = set.toArray(supplied);
        assertSame(supplied, result, "Supplied larger array should be returned");
        // First two slots must contain the elements (order not guaranteed)
        assertTrue(containsFirstTwo(result, "alpha", "beta"));
        // The element after the last filled index must be null
        assertNull(result[2], "Element after the last filled slot must be null");
        // Remaining slots must retain the sentinel values
        assertEquals("sentinel", result[3]);
        assertEquals("sentinel", result[4]);
    }

    @Test
    void testStringArraySupertypeObject() {
        SealableSet<String> set = createSet("alpha", "beta");
        Object[] supplied = new Object[0];
        Object[] result = set.toArray(supplied);
        assertEquals(2, result.length);
        assertTrue(contains(result, "alpha"));
        assertTrue(contains(result, "beta"));
    }

    @Test
    void testStringArrayNullSupplied() {
        SealableSet<String> set = createSet("alpha", "beta");
        assertThrows(NullPointerException.class, () -> set.toArray((String[]) null), "Passing a null array must propagate NullPointerException");
    }

    @Test
    void testStringArrayIncompatibleComponentType() {
        SealableSet<String> set = createSet("alpha", "beta");
        Integer[] supplied = new Integer[0];
        assertThrows(ArrayStoreException.class, () -> set.toArray(supplied), "Supplying an array with an incompatible component type must throw ArrayStoreException");
    }

    @Test
    void testStringArrayExactSizeSmallerSupplied() {
        SealableSet<String> set = createSet("alpha", "beta");
        // length 1 < set size 2
        String[] supplied = new String[] { "placeholder" };
        String[] result = set.toArray(supplied);
        // A new array must be allocated because supplied array is too small
        assertNotSame(supplied, result, "A new array should be allocated when supplied array is too small");
        assertEquals(2, result.length);
        assertTrue(contains(result, "alpha"));
        assertTrue(contains(result, "beta"));
    }

    @Test
    void testExactSizeStringArray() {
        SealableSet<String> set = createSet("alpha", "beta");
        String[] supplied = new String[2];
        String[] result = set.toArray(supplied);
        assertSame(supplied, result, "When supplied array length exactly matches set size, it should be returned");
        assertTrue(containsFirstTwo(result, "alpha", "beta"));
        // No trailing null should be added
        assertNull(result[2], "There should be no element at index 2 (array length is 2)");
    }

    @Test
    void testExactSizeObjectArraySupertype() {
        SealableSet<String> set = createSet("alpha", "beta");
        Object[] supplied = new Object[2];
        Object[] result = set.toArray(supplied);
        assertSame(supplied, result, "Exact‑size supertype array must be returned");
        assertTrue(containsFirstTwo(result, "alpha", "beta"));
    }

    @Test
    void testEmptySetWithEmptyArray() {
        // no elements
        SealableSet<String> emptySet = createSet();
        String[] supplied = new String[0];
        String[] result = emptySet.toArray(supplied);
        // According to Set.toArray contract, the same empty array may be returned
        assertSame(supplied, result, "Empty set with empty supplied array should return the same instance");
    }

    @Test
    void testEmptySetWithLargerArray() {
        SealableSet<String> emptySet = createSet();
        String[] supplied = new String[5];
        // Fill with sentinel values
        for (int i = 0; i < supplied.length; i++) {
            supplied[i] = "sentinel";
        }
        String[] result = emptySet.toArray(supplied);
        assertSame(supplied, result, "Empty set with larger supplied array should return the same array");
        // Per contract, first element must be set to null
        assertNull(result[0], "First element must be null for empty set");
        // Remaining slots untouched
        assertEquals("sentinel", result[1]);
        assertEquals("sentinel", result[2]);
        assertEquals("sentinel", result[3]);
        assertEquals("sentinel", result[4]);
    }

    @Test
    void testEmptySetSupertypeArray() {
        SealableSet<String> emptySet = createSet();
        Object[] supplied = new Object[0];
        Object[] result = emptySet.toArray(supplied);
        assertSame(supplied, result, "Empty set with empty supertype array should return the same instance");
    }

    @Test
    void testSetWithSingleNullElementAndLargerObjectArray() {
        // set size 1 containing a null
        SealableSet<Object> set = createSet((Object) null);
        Object[] supplied = new Object[3];
        // Fill with sentinel values
        for (int i = 0; i < supplied.length; i++) {
            supplied[i] = "sentinel";
        }
        Object[] result = set.toArray(supplied);
        assertSame(supplied, result, "Supplied array should be returned when it is larger than needed");
        // Index 0 must contain the null element from the set
        assertNull(result[0], "First slot must contain the null element from the set");
        // Index 1 must be the trailing null per contract
        assertNull(result[1], "Trailing null after the last element must be set");
        // Remaining slots untouched
        assertEquals("sentinel", result[2]);
    }
}
