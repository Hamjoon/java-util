package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_toArray_2_Test {

    private ConcurrentSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh set for each test.
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC; not strictly required.
        set = null;
    }

    @Test
    void toArray_resizesWhenProvidedArrayIsTooSmall() {
        // Arrange
        set.addAll(Arrays.asList("x", "y", "z"));
        // Provide an array smaller than the set size (length 1)
        String[] supplied = new String[1];
        // Act
        String[] result = set.toArray(supplied);
        // Assert
        assertNotSame(supplied, result, "Method should allocate a new array when supplied one is too small");
        assertEquals(3, result.length, "Result array length must match set size");
        List<String> asList = Arrays.asList(result);
        assertTrue(asList.containsAll(Arrays.asList("x", "y", "z")), "All elements must be present");
    }

    @Test
    void toArray_handlesNullSentinelAndTrailingNull() {
        // Arrange
        set.addAll(Arrays.asList("a", null, "b"));
        // Provide a larger array (size 5) to test trailing null insertion
        String[] supplied = new String[5];
        Arrays.fill(supplied, "placeholder");
        // Act
        String[] result = set.toArray(supplied);
        // Assert
        assertSame(supplied, result, "When supplied array is large enough, it should be returned");
        assertEquals(5, result.length, "Returned array should keep original length");
        // Elements 0..2 should be the set contents (order is unspecified)
        List<String> content = Arrays.asList(result).subList(0, 3);
        assertTrue(content.containsAll(Arrays.asList("a", null, "b")), "Array should contain the set elements, including null");
        // Position after the last element must be null per Set contract
        assertNull(result[3], "Element immediately after the last set element must be null");
        // The remaining slot(s) should stay untouched (could be null or placeholder); we only assert they are not part of the set
        assertEquals("placeholder", result[4], "Extra slots beyond the trailing null should retain original values");
    }

    @Test
    void toArray_withExactSizeArrayReturnsSameInstance() {
        // Arrange
        set.addAll(Arrays.asList("one", "two"));
        String[] exact = new String[2];
        // Act
        String[] result = set.toArray(exact);
        // Assert
        assertSame(exact, result, "When supplied array length equals set size, the same array should be returned");
        List<String> asList = Arrays.asList(result);
        assertTrue(asList.containsAll(Arrays.asList("one", "two")), "Array must contain all set elements");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toArray_exactSizeArrayReturnsSameInstance() {
        // Arrange
        set.addAll(Arrays.asList("first", "second"));
        String[] supplied = new String[2];
        // Act
        String[] result = set.toArray(supplied);
        // Assert
        assertSame(supplied, result, "When supplied array length equals set size, the same array should be returned");
        assertArrayEquals(new String[] { "first", "second" }, result, "Array must contain exactly the set elements in any order");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toArray_smallerArrayResize() {
        // Arrange
        set.addAll(Arrays.asList("first", "second"));
        // smaller than set size (2)
        String[] supplied = new String[1];
        // Act
        String[] result = set.toArray(supplied);
        // Assert
        assertNotSame(supplied, result, "A new array should be allocated when the supplied one is too small");
        assertEquals(2, result.length, "Result array length must match the set size");
        List<String> asList = Arrays.asList(result);
        assertTrue(asList.containsAll(Arrays.asList("first", "second")), "All elements must be present in the resulting array");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toArray_largerArrayTrailingNull() {
        // Arrange
        set.addAll(Arrays.asList("first", "second"));
        String[] supplied = new String[5];
        Arrays.fill(supplied, "placeholder");
        // Act
        String[] result = set.toArray(supplied);
        // Assert
        assertSame(supplied, result, "When supplied array is large enough, it should be returned");
        assertEquals(5, result.length, "Returned array should keep original length");
        // Elements 0..1 are the set contents (order unspecified)
        List<String> content = Arrays.asList(result).subList(0, 2);
        assertTrue(content.containsAll(Arrays.asList("first", "second")), "Array should contain the set elements");
        // Position after the last element must be null per Set contract
        assertNull(result[2], "Element immediately after the last set element must be null");
        // Remaining slots should retain their original values (placeholders)
        assertEquals("placeholder", result[3], "Extra slots beyond the trailing null should retain original values");
        assertEquals("placeholder", result[4], "Extra slots beyond the trailing null should retain original values");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toArray_emptySetZeroArray() {
        // Arrange: set is empty
        String[] supplied = new String[0];
        // Act
        String[] result = set.toArray(supplied);
        // Assert
        assertSame(supplied, result, "Empty set with zero‑length array should return the same array");
        assertEquals(0, result.length, "Resulting array length must be zero");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toArray_emptySetNonZeroArray() {
        // Arrange: set is empty
        String[] supplied = new String[3];
        // All elements are null by default; we just verify behaviour
        // Act
        String[] result = set.toArray(supplied);
        // Assert
        assertSame(supplied, result, "Empty set with larger array should return the same array");
        assertEquals(3, result.length, "Array length must stay unchanged");
        assertNull(result[0], "First extra slot must be set to null");
        assertNull(result[1], "Remaining slots should stay null");
        assertNull(result[2], "Remaining slots should stay null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toArray_setWithOnlyNullElement() {
        // Arrange
        // internal sentinel used
        set.add(null);
        // too small
        Object[] supplied = new Object[0];
        // Act
        Object[] result = set.toArray(supplied);
        // Assert
        assertNotSame(supplied, result, "A new array should be allocated when supplied array is too small");
        assertEquals(1, result.length, "Result array size must match the single null element");
        assertNull(result[0], "The stored null sentinel must be converted back to null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toArray_incompatibleArrayType_throwsClassCastException() {
        // Arrange
        set.add("string");
        // incompatible component type
        Integer[] supplied = new Integer[1];
        // Act & Assert
        assertThrows(ClassCastException.class, () -> set.toArray(supplied), "Casting a String element to Integer should throw ClassCastException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toArray_supertypeArrayWorks() {
        // Arrange
        set.addAll(Arrays.asList("first", "second"));
        Object[] supplied = new Object[2];
        // Act
        Object[] result = set.toArray(supplied);
        // Assert
        assertSame(supplied, result, "When supplied array is of a supertype and correct size, it should be returned");
        assertArrayEquals(new Object[] { "first", "second" }, result, "Array should contain the set elements");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toArray_nullArrayArgument_throwsNullPointerException() {
        // Arrange
        set.add("anything");
        // Act & Assert
        assertThrows(NullPointerException.class, () -> set.toArray(null), "Passing a null array reference must result in NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toArray_emptySetIncompatibleArray_noException() {
        // Arrange: set is empty
        // incompatible type but no elements to cast
        Integer[] supplied = new Integer[1];
        // Act
        Integer[] result = set.toArray(supplied);
        // Assert
        assertSame(supplied, result, "When set is empty, the supplied array should be returned unchanged");
        assertEquals(1, result.length, "Array length must stay unchanged");
        assertNull(result[0], "First slot must be set to null");
    }
}
