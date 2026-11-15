package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_toArray_2_Test {

    private ConcurrentList<Integer> intList;

    private ConcurrentList<String> stringList;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a list with a few elements for integer tests
        intList = new ConcurrentList<>();
        intList.add(10);
        intList.add(20);
        intList.add(30);
        // Initialise a list for generic type mismatch test (only a & b for now;
        // additional elements are added inside the parameterised tests where needed)
        stringList = new ConcurrentList<>();
        stringList.add("a");
        stringList.add("b");
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        intList = null;
        stringList = null;
    }

    /**
     * Provides all test scenarios for {@code toArray(T[])}.
     */
    private static Stream<Arguments> toArrayTestCases() {
        // The list used for these cases must contain exactly three elements: "a","b","c"
        // The @BeforeEach only adds "a" and "b".  We therefore create a fresh list inside
        // each argument where necessary.
        return Stream.of(// ---------- basic functionality ----------
        Arguments.of("Zero‑length array – new array allocated", new String[0], new String[] { "a", "b", "c" }, null, false), Arguments.of("Larger array – reuse supplied, null sentinel", new String[5], new String[] { "a", "b", "c", null, null }, null, true), Arguments.of("Exact size but supertype (Object[]) – reuse supplied", new Object[3], new Object[] { "a", "b", "c" }, null, true), // ---------- edge cases and type handling ----------
        Arguments.of("Exact size – reuse supplied array", new String[3], new String[] { "a", "b", "c" }, null, true), Arguments.of("Smaller array – allocate new array", new String[2], new String[] { "a", "b", "c" }, null, false), Arguments.of("Incompatible component type – ArrayStoreException", new Integer[3], null, ArrayStoreException.class, false), Arguments.of("Null array reference – NullPointerException", null, null, NullPointerException.class, false), Arguments.of("Zero‑length Object array – allocate new Object[]", new Object[0], new Object[] { "a", "b", "c" }, null, false), Arguments.of("Larger Object array – reuse supplied, null sentinel", new Object[5], new Object[] { "a", "b", "c", null, null }, null, true), Arguments.of("Larger String array – reuse supplied, null sentinel", new String[5], new String[] { "a", "b", "c", null, null }, null, true), Arguments.of("Very small array – allocate new array", new String[1], new String[] { "a", "b", "c" }, null, false));
    }

    @Test
    void testToArrayExactSize() {
        Integer[] supplied = new Integer[3];
        Integer[] result = intList.toArray(supplied);
        // The supplied array should be returned when it is exactly the right size
        assertSame(supplied, result, "Should return the supplied array when size matches");
        assertArrayEquals(new Integer[] { 10, 20, 30 }, result);
    }

    @Test
    void testToArrayLargerArray() {
        Integer[] supplied = new Integer[5];
        Integer[] result = intList.toArray(supplied);
        // When the supplied array is larger, the list elements occupy the first positions
        // and the element after the last list element must be set to null per List contract
        assertSame(supplied, result, "Should return the supplied array when it is larger");
        assertEquals(10, result[0]);
        assertEquals(20, result[1]);
        assertEquals(30, result[2]);
        assertNull(result[3], "Element after the last populated slot must be null");
        assertNull(result[4]);
    }

    @Test
    void testToArraySmallerArray() {
        Integer[] supplied = new Integer[2];
        Integer[] result = intList.toArray(supplied);
        // When the supplied array is too small, a new array of the correct runtime type is created
        assertNotSame(supplied, result, "Should create a new array when supplied one is too small");
        assertArrayEquals(new Integer[] { 10, 20, 30 }, result);
        // The runtime type of the returned array must be Integer[]
        assertEquals(Integer[].class, result.getClass(), "Returned array must be of type Integer[]");
    }

    @Test
    void testToArrayWithDifferentRuntimeType() {
        // List<Integer> cannot be stored into a String[] – expect ArrayStoreException
        String[] supplied = new String[3];
        assertThrows(ArrayStoreException.class, () -> intList.toArray(supplied));
    }

    @Test
    void testToArrayEmptyList() {
        ConcurrentList<Integer> empty = new ConcurrentList<>();
        Integer[] supplied = new Integer[0];
        Integer[] result = empty.toArray(supplied);
        // For an empty list, the supplied array (even if zero length) should be returned
        assertSame(supplied, result);
        assertEquals(0, result.length);
    }

    @Test
    void testToArrayDoesNotReflectSubsequentModifications() {
        Integer[] supplied = new Integer[3];
        Integer[] snapshot = intList.toArray(supplied);
        // Modify the original list after taking the snapshot
        intList.add(40);
        intList.remove(Integer.valueOf(10));
        // The previously returned array must remain unchanged
        assertArrayEquals(new Integer[] { 10, 20, 30 }, snapshot);
    }

    /**
     * Executes the parameterised test cases.
     *
     * @param description      human readable description (used only for readability)
     * @param suppliedArray    the array passed to {@code toArray}
     * @param expectedContents the expected array contents (null when an exception is expected)
     * @param expectedException the exception type that should be thrown (null when no exception)
     * @param expectSameArray  true if the supplied array should be returned unchanged
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("toArrayTestCases")
    void testToArrayParameterized(String description, Object suppliedArray, Object[] expectedContents, Class<? extends Throwable> expectedException, boolean expectSameArray) {
        // Ensure the list under test contains exactly "a","b","c"
        ConcurrentList<String> list = new ConcurrentList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        if (expectedException != null) {
            // The contract specifies that a null argument causes NPE and an
            // incompatible array type causes ArrayStoreException.
            assertThrows(expectedException, () -> list.toArray((Object[]) suppliedArray), description);
            return;
        }
        // Normal (non‑exceptional) path
        Object[] result = list.toArray((Object[]) suppliedArray);
        // Verify whether the same array instance was returned
        if (expectSameArray) {
            assertSame(suppliedArray, result, "Expected the method to reuse the supplied array");
        } else {
            assertNotSame(suppliedArray, result, "Expected a new array to be allocated");
        }
        // Validate the content of the returned array
        assertArrayEquals(expectedContents, result, "Array contents differ from expected");
    }
}
