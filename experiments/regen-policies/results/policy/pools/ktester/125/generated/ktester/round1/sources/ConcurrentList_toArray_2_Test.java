package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_toArray_2_Test {

    /**
     * Mocked backing list used by the {@link ConcurrentList} under test.
     */
    @Mock
    private List<Integer> mockBackingList;

    /**
     * The {@code ConcurrentList} instance that wraps {@code mockBackingList}.
     */
    private ConcurrentList<Integer> concurrentList;

    @BeforeEach
    void setupBeforeEach() {
        // The MockitoExtension creates the mock for us; just wrap it.
        concurrentList = new ConcurrentList<>(mockBackingList);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions so each test starts with a clean slate.
        reset(mockBackingList);
    }

    /**
     * Helper to create a ConcurrentList wrapping a mutable ArrayList with given elements.
     */
    private static <E> ConcurrentList<E> createConcurrentList(E... elements) {
        List<E> backing = new ArrayList<>(Arrays.asList(elements));
        return new ConcurrentList<>(backing);
    }

    private static Stream<Arguments> emptyListScenarios() {
        return Stream.of(Arguments.of(new String[0], "zero‑length array"), Arguments.of(new String[5], "larger array"));
    }

    @Test
    void toArrayReturnsArrayFromBackingList() {
        Integer[] expected = new Integer[] { 1, 2, 3 };
        // Configure the mock to return the expected array when its toArray(T[]) is called.
        when(mockBackingList.toArray(any(Integer[].class))).thenReturn(expected);
        Integer[] result = concurrentList.toArray(new Integer[0]);
        // Verify delegation.
        verify(mockBackingList, times(1)).toArray(any(Integer[].class));
        // The result should be exactly the array supplied by the mock.
        assertArrayEquals(expected, result, "toArray should return the array produced by the backing list");
    }

    @Test
    void toArrayUsesSuppliedArrayWhenLargeEnough() {
        // Backing list contains two elements.
        Integer[] backingArray = new Integer[] { 10, 20 };
        when(mockBackingList.toArray(any(Integer[].class))).thenReturn(backingArray);
        // Provide an array that is larger than the list size.
        Integer[] supplied = new Integer[5];
        Integer[] result = concurrentList.toArray(supplied);
        verify(mockBackingList, times(1)).toArray(any(Integer[].class));
        // The contract of List.toArray(T[]) is that the returned array is the same
        // instance if it is large enough; our mock returns a new array, so we assert
        // content equality.
        assertArrayEquals(backingArray, result, "Result array should contain the elements from the backing list");
    }

    @Test
    void toArrayPropagatesNullPointerExceptionFromBackingList() {
        // Simulate the backing list throwing NPE when a null array is passed.
        when(mockBackingList.toArray(null)).thenThrow(new NullPointerException("array is null"));
        NullPointerException ex = assertThrows(NullPointerException.class, () -> concurrentList.toArray(null), "toArray should propagate NullPointerException from the backing list");
        assertEquals("array is null", ex.getMessage());
        verify(mockBackingList, times(1)).toArray(null);
    }

    @Test
    void toArrayWorksWithEmptyList() {
        // Empty backing list returns the supplied array (per List contract).
        Integer[] emptyResult = new Integer[0];
        when(mockBackingList.toArray(any(Integer[].class))).thenReturn(emptyResult);
        Integer[] result = concurrentList.toArray(new Integer[0]);
        verify(mockBackingList, times(1)).toArray(any(Integer[].class));
        assertArrayEquals(emptyResult, result, "Empty list should produce an empty array");
    }

    @ParameterizedTest(name = "{index} => suppliedArray={0}")
    @MethodSource("emptyListScenarios")
    void emptyListScenarios(Object suppliedArray, String description) {
        // GIVEN an empty ConcurrentList
        ConcurrentList<String> list = createConcurrentList();
        // WHEN toArray is invoked
        String[] result = list.toArray((String[]) suppliedArray);
        // THEN the same array instance must be returned (per List contract)
        assertSame(suppliedArray, result, "Empty list with supplied array should return the same array instance");
        // Additional contract checks
        if (result.length > 0) {
            assertNull(result[0], "First element must be null when list is empty and array is larger");
        }
    }

    @Test
    void nonEmptyArraySizeEqual() {
        ConcurrentList<String> list = createConcurrentList("alpha", "beta");
        // empty but correct size
        String[] supplied = new String[2];
        String[] result = list.toArray(supplied);
        // Same instance should be returned
        assertSame(supplied, result, "When supplied array size equals list size, the same array should be returned");
        assertArrayEquals(new String[] { "alpha", "beta" }, result, "Array must contain the list elements in order");
    }

    @Test
    void nonEmptyArrayLargerThanList() {
        ConcurrentList<String> list = createConcurrentList("alpha", "beta");
        // larger than needed
        String[] supplied = new String[5];
        String[] result = list.toArray(supplied);
        assertSame(supplied, result, "When supplied array is larger, the same array should be returned");
        assertEquals("alpha", result[0]);
        assertEquals("beta", result[1]);
        assertNull(result[2], "Element after the last list element must be set to null");
    }

    @Test
    void nonEmptyArraySmallerThanList() {
        ConcurrentList<String> list = createConcurrentList("alpha", "beta");
        // too small
        String[] supplied = new String[0];
        String[] result = list.toArray(supplied);
        // A new array must be allocated
        assertNotSame(supplied, result, "A new array should be allocated when supplied array is too small");
        assertArrayEquals(new String[] { "alpha", "beta" }, result, "Returned array must contain the list elements");
    }

    @Test
    void arrayOfSupertype() {
        ConcurrentList<String> list = createConcurrentList("alpha", "beta");
        Object[] supplied = new Object[2];
        Object[] result = list.toArray(supplied);
        assertSame(supplied, result, "Supertype array of sufficient size should be returned unchanged");
        assertEquals("alpha", result[0]);
        assertEquals("beta", result[1]);
    }

    @Test
    void incompatibleArrayComponentType() {
        ConcurrentList<String> list = createConcurrentList("alpha", "beta");
        Integer[] supplied = new Integer[2];
        assertThrows(ArrayStoreException.class, () -> list.toArray(supplied), "Supplying an array of an incompatible component type must throw ArrayStoreException");
    }

    @Test
    void nullArrayArgument() {
        ConcurrentList<String> list = createConcurrentList("alpha", "beta");
        assertThrows(NullPointerException.class, () -> list.toArray(null), "Passing null as the destination array must result in NullPointerException");
    }

    @Test
    void modificationSafetyReturnedArrayIsIndependent() {
        ConcurrentList<String> list = createConcurrentList("alpha", "beta");
        // forces allocation of a new array
        String[] supplied = new String[0];
        String[] result = list.toArray(supplied);
        // Verify a new array was allocated
        assertNotSame(supplied, result, "A new array should be allocated for a zero‑length supplied array");
        // Mutate the returned array
        result[0] = "changed";
        // The list must remain unchanged
        assertEquals("alpha", list.get(0), "Modifying the returned array must not affect the list contents");
    }
}
