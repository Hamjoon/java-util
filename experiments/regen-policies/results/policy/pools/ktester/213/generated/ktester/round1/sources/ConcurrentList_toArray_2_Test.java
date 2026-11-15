package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_toArray_2_Test {

    /**
     * The list under test.
     */
    private ConcurrentList<Integer> list;

    /**
     * Empty‑list scenarios.
     */
    @Nested
    class EmptyListScenarios {

        @BeforeEach
        void makeListEmpty() {
            list.clear();
        }

        @Test
        void toArray_zeroLengthArray_returnsSameArray() {
            String[] supplied = new String[0];
            String[] result = list.toArray(supplied);
            assertSame(supplied, result, "Empty list with zero‑length array should return the supplied array");
            assertEquals(0, result.length);
        }

        @Test
        void toArray_largerArrayAllNulls() {
            String[] supplied = new String[5];
            String[] result = list.toArray(supplied);
            assertSame(supplied, result, "Empty list with larger array should return the supplied array");
            for (int i = 0; i < supplied.length; i++) {
                assertNull(supplied[i], "All positions must be null for an empty list");
            }
        }
    }

    /**
     * Populate a fresh list before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        list = new ConcurrentList<>();
        // deterministic content used by most test cases
        list.add("a");
        list.add("b");
        list.add("c");
    }

    @AfterEach
    void teardownAfterEach() {
        // clear the list to avoid side‑effects between tests
        list.clear();
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// supplied array length 0 → new array allocated
        Arguments.of("Supplied array length 0 – allocate new array of correct runtime type", new String[0], new String[] { "a", "b", "c" }, String[].class), // supplied array larger than list size
        Arguments.of("Supplied array larger than list – fill and null‑terminate", new String[5], new String[] { "a", "b", "c", null, null }, String[].class), // supplied array exactly the list size
        Arguments.of("Supplied array exactly the list size – reuse supplied array", new String[3], new String[] { "a", "b", "c" }, String[].class), // supplied array smaller than list size
        Arguments.of("Supplied array smaller than list – allocate new array", new String[2], new String[] { "a", "b", "c" }, String[].class), // supplied array of a supertype (Object)
        Arguments.of("Supplied array of supertype Object – store elements", new Object[3], new Object[] { "a", "b", "c" }, Object[].class), // supplied array of an interface type (CharSequence)
        Arguments.of("Supplied array of interface type CharSequence – store elements", new CharSequence[3], new CharSequence[] { "a", "b", "c" }, CharSequence[].class));
    }

    @Test
    void toArray_exactSizeArray_returnsSameArrayWithElements() {
        Integer[] supplied = new Integer[3];
        Integer[] result = list.toArray(supplied);
        // The supplied array should be returned because it is large enough
        assertSame(supplied, result, "Should return the supplied array when it fits exactly");
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result, "Array contents must match the list");
    }

    @Test
    void toArray_largerArray_returnsSuppliedArrayWithTrailingNull() {
        Integer[] supplied = new Integer[5];
        Integer[] result = list.toArray(supplied);
        // The supplied array should be returned
        assertSame(supplied, result, "Should return the supplied array when it is larger than needed");
        // Verify the first three entries and the trailing null
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
        assertNull(result[3], "First element after the list size must be null");
        assertNull(result[4], "Remaining slots must be null");
    }

    @Test
    void toArray_smallerArray_returnsNewArrayWithElements() {
        // smaller than list size
        Integer[] supplied = new Integer[2];
        Integer[] result = list.toArray(supplied);
        // The returned array must be a new instance (different from supplied)
        assertNotSame(supplied, result, "Should allocate a new array when supplied one is too small");
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result, "New array must contain all list elements");
        // The new array length must equal list size
        assertEquals(3, result.length);
    }

    @Test
    void toArray_differentComponentType_returnsArrayOfRequestedType() {
        Number[] supplied = new Number[3];
        Number[] result = list.toArray(supplied);
        // The supplied array is the right size, so it should be returned
        assertSame(supplied, result, "Should return the supplied array when it fits");
        assertArrayEquals(new Number[] { 1, 2, 3 }, result, "Array elements must be correctly up‑cast");
    }

    @Test
    void toArray_emptyList_returnsEmptyArray() {
        // make the list empty
        list.clear();
        Integer[] supplied = new Integer[0];
        Integer[] result = list.toArray(supplied);
        // For an empty list the supplied array (size 0) should be returned unchanged
        assertSame(supplied, result, "Empty list should return the supplied zero‑length array");
        assertEquals(0, result.length);
    }

    @Test
    void toArray_modificationsAfterCallDoNotAffectReturnedArray() {
        Integer[] supplied = new Integer[3];
        Integer[] snapshot = list.toArray(supplied);
        // mutate the list after taking the snapshot
        list.add(4);
        list.remove(Integer.valueOf(1));
        // The snapshot must stay unchanged
        assertArrayEquals(new Integer[] { 1, 2, 3 }, snapshot, "Array returned by toArray must be a snapshot of the list at call time");
    }

    /**
     * Basic functionality – various sized supplied arrays.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicFunctionalityProvider")
    void toArray_basicFunctionality(String description, String[] supplied, String[] expected, Class<?> expectedArrayClass) {
        String[] result = list.toArray(supplied);
        // verify the returned array instance when size matches exactly
        if (supplied.length == list.size()) {
            assertSame(supplied, result, "When supplied array length equals list size the same instance must be returned");
        }
        // verify the returned array instance when supplied array is larger
        if (supplied.length > list.size()) {
            assertSame(supplied, result, "When supplied array is larger the same instance must be returned");
        }
        // verify the returned array instance when supplied array is smaller
        if (supplied.length < list.size()) {
            assertNotSame(supplied, result, "When supplied array is too small a new array must be allocated");
        }
        assertArrayEquals(expected, result, description);
        assertEquals(expectedArrayClass, result.getClass(), "Result array runtime type must match the supplied component type");
    }

    /**
     * Null array argument – must throw NullPointerException.
     */
    @Test
    void toArray_nullArray_throwsNPE() {
        assertThrows(NullPointerException.class, () -> list.toArray((String[]) null), "Passing a null array must result in NullPointerException per Collection contract");
    }

    /**
     * Incompatible component type – should throw ArrayStoreException.
     */
    @Test
    void toArray_incompatibleComponentType_throwsArrayStoreException() {
        Integer[] wrongTypeArray = new Integer[3];
        assertThrows(ArrayStoreException.class, () -> list.toArray(wrongTypeArray), "Supplying an array whose component type is not assignable from the element type must throw ArrayStoreException");
    }

    /**
     * List containing a null element – the null must be preserved in the result array.
     */
    @Test
    void toArray_listWithNullElement_preservesNull() {
        list.clear();
        list.add("a");
        list.add(null);
        list.add("c");
        String[] supplied = new String[3];
        String[] result = list.toArray(supplied);
        assertSame(supplied, result, "Array of exact size should be reused");
        assertArrayEquals(new String[] { "a", null, "c" }, result, "Null element must be copied to the result array");
    }
}
