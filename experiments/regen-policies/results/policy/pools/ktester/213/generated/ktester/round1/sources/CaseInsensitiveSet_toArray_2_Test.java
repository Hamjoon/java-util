package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.Serializable;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_toArray_2_Test {

    /**
     * The set used in the tests – created fresh for each test method.
     */
    private CaseInsensitiveSet<String> set;

    /**
     * Elements added to the set before each test.
     */
    private static final List<String> ELEMENTS = List.of("One", "Two", "Three");

    @BeforeEach
    void setupBeforeEach() {
        set = new CaseInsensitiveSet<>();
        set.addAll(ELEMENTS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to help GC; not strictly required.
        set.clear();
    }

    private static Stream<Arguments> provideArraySizeAndTypeVariations() {
        // Helper set with three strings
        CaseInsensitiveSet<String> three = new CaseInsensitiveSet<>();
        three.addAll(List.of("alpha", "beta", "gamma"));
        // Empty set for the first case
        CaseInsensitiveSet<String> empty = new CaseInsensitiveSet<>();
        return Stream.of(// empty set, empty Object[]
        Arguments.of("Empty set with empty Object[] – same instance", empty, new Object[0], new Object[0], true, null), // exact size String[]
        Arguments.of("Exact size String[] – same instance", three, new String[3], new String[] { "alpha", "beta", "gamma" }, true, null), // smaller String[]
        Arguments.of("Smaller String[] – new array allocated", three, new String[2], new String[] { "alpha", "beta", "gamma" }, false, null), // larger String[]
        Arguments.of("Larger String[] – same instance with null terminator", three, new String[5], new String[] { "alpha", "beta", "gamma", null, null }, true, null), // null array
        Arguments.of("Null array – NullPointerException", three, null, null, false, NullPointerException.class), // incompatible Integer[]
        Arguments.of("Incompatible Integer[] – ArrayStoreException", three, new Integer[3], null, false, ArrayStoreException.class), // supertype Object[]
        Arguments.of("Object[] supertype – same instance", three, new Object[3], new Object[] { "alpha", "beta", "gamma" }, true, null), // CharSequence[]
        Arguments.of("CharSequence[] supertype – same instance", three, new CharSequence[3], new CharSequence[] { "alpha", "beta", "gamma" }, true, null));
    }

    @Test
    void toArrayExactSizeArray_returnsSameArrayWithElementsInInsertionOrder() {
        // GIVEN
        String[] supplied = new String[ELEMENTS.size()];
        // WHEN
        String[] result = set.toArray(supplied);
        // THEN
        assertSame(supplied, result, "When the supplied array is exactly the right size, the same instance must be returned");
        assertArrayEquals(ELEMENTS.toArray(new String[0]), result, "Elements must be returned in the insertion order and preserve original case");
    }

    @Test
    void toArraySmallerArray_createsNewArrayContainingAllElements() {
        // GIVEN
        // smaller than the set size (3)
        String[] supplied = new String[1];
        // WHEN
        String[] result = set.toArray(supplied);
        // THEN
        assertNotSame(supplied, result, "A new array must be allocated when the supplied one is too small");
        assertEquals(ELEMENTS.size(), result.length, "Result array length must match the set size");
        assertArrayEquals(ELEMENTS.toArray(new String[0]), result, "All elements must be present in insertion order");
    }

    @Test
    void toArrayLargerArray_fillsElementsAndSetsTrailingNull() {
        // GIVEN
        // larger than the set size (3)
        String[] supplied = new String[5];
        // WHEN
        String[] result = set.toArray(supplied);
        // THEN
        assertSame(supplied, result, "When the supplied array is larger, the same instance must be returned");
        // First three positions contain the set elements
        assertArrayEquals(ELEMENTS.toArray(new String[0]), Arrays.copyOf(result, ELEMENTS.size()));
        // The element immediately after the last should be null
        assertNull(result[ELEMENTS.size()], "The element after the last populated slot must be null");
        // Remaining positions (if any) are left untouched – we only assert the first null slot.
    }

    @Test
    void toArrayWithSupertypeArray_returnsArrayOfSupertype() {
        // GIVEN
        Object[] supplied = new Object[ELEMENTS.size()];
        // WHEN
        Object[] result = set.toArray(supplied);
        // THEN
        assertSame(supplied, result, "When the supplied array type is a supertype, the same instance should be returned");
        assertArrayEquals(ELEMENTS.toArray(new Object[0]), result, "Elements must be correctly placed in the returned array");
    }

    @Test
    void toArrayPreservesOriginalCaseEvenWhenLookupIsCaseInsensitive() {
        // GIVEN
        // duplicate in case‑insensitive sense, should not be added
        set.add("HELLO");
        String[] supplied = new String[set.size()];
        // WHEN
        String[] result = set.toArray(supplied);
        // THEN
        // The set should still contain only the original case versions
        List<String> expected = List.of("One", "Two", "Three");
        assertArrayEquals(expected.toArray(new String[0]), result);
    }

    @Test
    void toArrayWithCharSequenceArray_returnsSameArray() {
        // GIVEN
        CharSequence[] supplied = new CharSequence[ELEMENTS.size()];
        // WHEN
        CharSequence[] result = set.toArray(supplied);
        // THEN
        assertSame(supplied, result, "CharSequence[] is a supertype of String – the same array should be reused");
        assertArrayEquals(ELEMENTS.toArray(new CharSequence[0]), result);
    }

    /* -------------------------------------------------------------
     *  5. Null array argument – must throw NullPointerException.
     * ------------------------------------------------------------- */
    @Test
    void toArrayNullArray_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> set.toArray((String[]) null), "Passing a null array reference must result in NullPointerException");
    }

    /* -------------------------------------------------------------
     *  6. Incompatible array component type – must throw ArrayStoreException.
     * ------------------------------------------------------------- */
    @Test
    void toArrayIncompatibleArray_throwsArrayStoreException() {
        Integer[] supplied = new Integer[3];
        assertThrows(ArrayStoreException.class, () -> set.toArray(supplied), "Supplying an array of incompatible component type must cause ArrayStoreException");
    }

    /* -------------------------------------------------------------
     *  7. Empty set – various array sizes and types.
     * ------------------------------------------------------------- */
    @Test
    void toArrayEmptySetWithExactEmptyObjectArray_returnsSameInstance() {
        CaseInsensitiveSet<String> empty = new CaseInsensitiveSet<>();
        Object[] supplied = new Object[0];
        Object[] result = empty.toArray(supplied);
        assertSame(supplied, result, "Empty set with empty array should return the supplied instance");
    }

    @Test
    void toArrayEmptySetWithOversizedObjectArray_returnsSameArrayWithNullTerminator() {
        CaseInsensitiveSet<String> empty = new CaseInsensitiveSet<>();
        Object[] supplied = new Object[5];
        Object[] result = empty.toArray(supplied);
        assertSame(supplied, result, "Oversized array must be returned unchanged");
        assertNull(result[0], "The first element must be set to null as the termination marker");
    }

    @Test
    void toArrayEmptySetWithOversizedStringArray_returnsSameArrayWithNullTerminator() {
        CaseInsensitiveSet<String> empty = new CaseInsensitiveSet<>();
        String[] supplied = new String[4];
        String[] result = empty.toArray(supplied);
        assertSame(supplied, result, "Oversized array must be returned unchanged");
        assertNull(result[0], "The first element must be set to null as the termination marker");
    }

    @Test
    void toArrayEmptySetWithZeroLengthObjectArray_returnsSameInstance() {
        // According to the JDK contract, a zero‑length array is large enough for an empty set.
        CaseInsensitiveSet<String> empty = new CaseInsensitiveSet<>();
        Object[] supplied = new Object[0];
        Object[] result = empty.toArray(supplied);
        assertSame(supplied, result, "For an empty set, a zero‑length array should be returned unchanged");
    }

    @Test
    void toArrayEmptySetWithZeroLengthStringArray_returnsSameInstance() {
        CaseInsensitiveSet<String> empty = new CaseInsensitiveSet<>();
        String[] supplied = new String[0];
        String[] result = empty.toArray(supplied);
        assertSame(supplied, result, "For an empty set, a zero‑length array should be returned unchanged");
    }

    /* -------------------------------------------------------------
     *  8. Non‑empty set with zero‑length array – a new array is allocated.
     * ------------------------------------------------------------- */
    @Test
    void toArrayNonEmptySetZeroLengthObjectArray_allocatesNewArray() {
        CaseInsensitiveSet<String> three = new CaseInsensitiveSet<>();
        three.addAll(List.of("alpha", "beta", "gamma"));
        Object[] supplied = new Object[0];
        Object[] result = three.toArray(supplied);
        assertNotSame(supplied, result, "A new array must be allocated for a zero‑length supplied array");
        assertArrayEquals(List.of("alpha", "beta", "gamma").toArray(), result);
    }

    @Test
    void toArrayNonEmptySetZeroLengthStringArray_allocatesNewArray() {
        CaseInsensitiveSet<String> three = new CaseInsensitiveSet<>();
        three.addAll(List.of("alpha", "beta", "gamma"));
        String[] supplied = new String[0];
        String[] result = three.toArray(supplied);
        assertNotSame(supplied, result, "A new array must be allocated for a zero‑length supplied array");
        assertArrayEquals(new String[] { "alpha", "beta", "gamma" }, result);
    }

    /* -------------------------------------------------------------
     *  9. Interface supertype arrays.
     * ------------------------------------------------------------- */
    @Test
    void toArrayWithComparableArrayExactSize_returnsSameArray() {
        CaseInsensitiveSet<String> three = new CaseInsensitiveSet<>();
        three.addAll(List.of("alpha", "beta", "gamma"));
        Comparable[] supplied = new Comparable[3];
        Comparable[] result = three.toArray(supplied);
        assertSame(supplied, result, "Exact‑size Comparable[] must be reused");
        assertArrayEquals(new String[] { "alpha", "beta", "gamma" }, result);
    }

    @Test
    void toArrayWithSerializableArraySmaller_allocatesNewArray() {
        CaseInsensitiveSet<String> three = new CaseInsensitiveSet<>();
        three.addAll(List.of("alpha", "beta", "gamma"));
        Serializable[] supplied = new Serializable[2];
        Serializable[] result = three.toArray(supplied);
        assertNotSame(supplied, result, "A new Serializable[] must be allocated when the supplied one is too small");
        assertEquals(3, result.length);
        assertArrayEquals(new String[] { "alpha", "beta", "gamma" }, result);
    }

    @Test
    void toArrayWithCharSequenceArrayLarger_fillsAndNullTerminates() {
        CaseInsensitiveSet<String> three = new CaseInsensitiveSet<>();
        three.addAll(List.of("alpha", "beta", "gamma"));
        CharSequence[] supplied = new CharSequence[5];
        CharSequence[] result = three.toArray(supplied);
        assertSame(supplied, result, "Larger CharSequence[] must be reused");
        assertArrayEquals(new String[] { "alpha", "beta", "gamma", null, null }, result);
    }

    /* -------------------------------------------------------------
     * 10. Case‑insensitive duplicate elimination.
     * ------------------------------------------------------------- */
    @Test
    void toArrayIgnoresCaseInsensitiveDuplicates() {
        CaseInsensitiveSet<String> dupSet = new CaseInsensitiveSet<>();
        dupSet.add("Alpha");
        // duplicate – should be ignored
        dupSet.add("alpha");
        String[] supplied = new String[2];
        String[] result = dupSet.toArray(supplied);
        // Expect a newly allocated array of size 1 containing the original case version
        assertEquals(1, result.length);
        assertEquals("Alpha", result[0]);
    }

    /* -------------------------------------------------------------
     * 11. Handling of a stored null element.
     * ------------------------------------------------------------- */
    @Test
    void toArrayWithStoredNullElement_placesNullAndTerminates() {
        // Using a raw set to allow null values of any type
        CaseInsensitiveSet<Object> nullSet = new CaseInsensitiveSet<>();
        nullSet.add(null);
        Object[] supplied = new Object[3];
        Object[] result = nullSet.toArray(supplied);
        assertSame(supplied, result, "The supplied array must be returned");
        assertNull(result[0], "The stored null element should appear at index 0");
        assertNull(result[1], "A terminating null must be placed at index 1");
        // The remaining slots are left untouched; they are also null in this case.
    }

    /* -------------------------------------------------------------
     * 12. Parameterized tests for size‑type variations (optional convenience).
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideArraySizeAndTypeVariations")
    void toArraySizeAndTypeVariations(String description, CaseInsensitiveSet<String> setUnderTest, Object suppliedArray, Object expectedArray, boolean expectSameInstance, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> setUnderTest.toArray(suppliedArray), description);
            return;
        }
        Object result = setUnderTest.toArray(suppliedArray);
        if (expectSameInstance) {
            assertSame(suppliedArray, result, description);
        } else {
            assertNotSame(suppliedArray, result, description);
        }
        assertArrayEquals((Object[]) expectedArray, (Object[]) result, description);
    }
}
