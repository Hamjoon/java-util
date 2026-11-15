package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_toArray_2_Test {

    private ConcurrentSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh set for each test ensures independence.
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set in case a test mutates it in an unexpected way.
        set.clear();
    }

    private static Stream<Arguments> smallArrayProvider() {
        return Stream.of(// case: normal elements, array too small
        Arguments.of(List.of("one", "two"), new String[1], // expect new array
        false, List.of("one", "two")), // case: set contains null, array too small
        Arguments.of(List.of("alpha", null), new String[1], false, List.of("alpha", null)));
    }

    @Test
    void toArrayCreatesNewArrayWhenSuppliedArrayIsTooSmall() {
        // given
        set.addAll(Arrays.asList("x", "y"));
        // smaller than the set size (2)
        String[] smallArray = new String[1];
        // when
        String[] result = set.toArray(smallArray);
        // then
        assertNotSame(smallArray, result, "A new array should be allocated when the supplied array is too small");
        assertEquals(2, result.length, "Result array length must match the set size");
        assertTrue(Arrays.asList(result).containsAll(Arrays.asList("x", "y")), "Result array must contain all set elements");
    }

    @Test
    void toArrayReturnsSuppliedArrayWhenItIsExactlyTheRightSize() {
        // given
        set.addAll(Arrays.asList("a", "b", "c"));
        String[] exactArray = new String[3];
        // when
        String[] result = set.toArray(exactArray);
        // then
        assertSame(exactArray, result, "When the supplied array length equals the set size, the same array should be returned");
        assertEquals(3, result.length);
        assertTrue(Arrays.asList(result).containsAll(Arrays.asList("a", "b", "c")));
    }

    @Test
    void toArraySetsTrailingNullWhenSuppliedArrayIsLargerThanSet() {
        // given
        set.addAll(Arrays.asList("one", "two"));
        String[] largerArray = new String[5];
        // pre‑fill with a sentinel value to ensure the method overwrites only the required slot
        Arrays.fill(largerArray, "placeholder");
        // when
        String[] result = set.toArray(largerArray);
        // then
        assertSame(largerArray, result, "The original array should be returned when it is large enough");
        assertEquals(5, result.length);
        // The first two positions contain the set elements (order is unspecified)
        assertTrue(Arrays.asList(result[0], result[1]).containsAll(Arrays.asList("one", "two")));
        // The element immediately after the last set element must be null
        assertNull(result[2], "The element after the last set entry must be null");
        // The remaining positions should stay untouched (they may be null or the original placeholder)
        // We only assert they are not overwritten with set data.
        assertTrue(result[3] == null || "placeholder".equals(result[3]));
        assertTrue(result[4] == null || "placeholder".equals(result[4]));
    }

    @Test
    void toArrayHandlesNullElementCorrectly() {
        // given
        set.addAll(Arrays.asList("alpha", null, "beta"));
        // supply an array of size 0 to force allocation of a correctly‑sized array
        String[] emptyArray = new String[0];
        // when
        String[] result = set.toArray(emptyArray);
        // then
        assertEquals(3, result.length, "Result array must have the same size as the set");
        assertTrue(Arrays.asList(result).containsAll(Arrays.asList("alpha", "beta")), "Result must contain the non‑null elements");
        assertTrue(Arrays.asList(result).contains(null), "Result must contain the null element");
    }

    @ParameterizedTest(name = "{index}: set={0}, suppliedArrayLength={1}")
    @MethodSource("smallArrayProvider")
    void toArrayAllocatesNewArrayWhenSuppliedTooSmall(List<Object> elements, String[] supplied, boolean expectSame, List<Object> expectedContents) {
        // Populate a generic ConcurrentSet<Object> because we may have nulls.
        ConcurrentSet<Object> genericSet = new ConcurrentSet<>();
        genericSet.addAll(elements);
        String[] result = genericSet.toArray(supplied);
        if (expectSame) {
            assertSame(supplied, result, "Expected the same array instance");
        } else {
            assertNotSame(supplied, result, "A new array should be allocated");
        }
        assertEquals(elements.size(), result.length, "Result length must match set size");
        assertTrue(Arrays.asList(result).containsAll(expectedContents), "Result array must contain all expected elements");
    }

    @Test
    void toArrayEmptySetWithZeroLengthArrayReturnsSameArray() {
        // given: empty set, zero‑length array
        String[] zeroArray = new String[0];
        // when
        String[] result = set.toArray(zeroArray);
        // then
        assertSame(zeroArray, result, "When set is empty and array length is 0, the same array should be returned");
        assertEquals(0, result.length);
    }

    @Test
    void toArrayEmptySetWithLargerArraySetsFirstElementToNull() {
        // given: empty set, larger array pre‑filled with a placeholder
        String[] largerArray = new String[2];
        Arrays.fill(largerArray, "placeholder");
        // when
        String[] result = set.toArray(largerArray);
        // then
        assertSame(largerArray, result, "Original array should be returned");
        assertEquals(2, result.length);
        assertNull(result[0], "First element must be null as terminator");
        // The second slot should remain unchanged (still placeholder or null)
        assertTrue(result[1] == null || "placeholder".equals(result[1]), "Remaining slots must not be overwritten with data");
    }

    @Test
    void toArrayWithObjectArraySupertype() {
        // given: set contains a String, caller supplies Object[]
        set.add("hello");
        Object[] objArray = new Object[1];
        // when
        Object[] result = set.toArray(objArray);
        // then
        assertSame(objArray, result, "The supplied Object[] should be returned");
        assertEquals(1, result.length);
        assertEquals("hello", result[0]);
    }

    @Test
    void toArrayIncompatibleArrayComponentTypeThrowsArrayStoreException() {
        // given: set contains a String, caller supplies Integer[]
        set.add("hello");
        Integer[] intArray = new Integer[1];
        // when / then
        assertThrows(ArrayStoreException.class, () -> set.toArray(intArray), "Storing a String into an Integer[] must throw ArrayStoreException");
    }

    @Test
    void toArrayAllocatesNewArrayWithIncompatibleComponentTypeAndThrowsArrayStoreException() {
        // given: set contains two Strings, caller supplies too‑small Integer[]
        set.addAll(Arrays.asList("x", "y"));
        // too small, will trigger allocation
        Integer[] intArray = new Integer[1];
        // when / then
        assertThrows(ArrayStoreException.class, () -> set.toArray(intArray), "Allocating a new Integer[] and copying Strings should throw ArrayStoreException");
    }

    @Test
    void toArrayClassCastExceptionWhenElementCannotBeCastToArrayType() {
        // given: set holds an Integer, caller supplies String[]
        ConcurrentSet<Integer> intSet = new ConcurrentSet<>();
        intSet.add(123);
        String[] stringArray = new String[1];
        // when / then
        assertThrows(ClassCastException.class, () -> intSet.toArray(stringArray), "Casting Integer to String should throw ClassCastException");
    }

    @Test
    void toArrayNullArrayArgumentThrowsNullPointerException() {
        // given: a non‑empty set
        set.add("value");
        // when / then
        assertThrows(NullPointerException.class, () -> set.toArray(null), "Passing null as the array argument must throw NullPointerException");
    }
}
