package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_toArray_2_Test {

    private CaseInsensitiveSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set for each test
        set = new CaseInsensitiveSet<>();
        // Populate with a mixture of String (different cases) and non‑String elements
        set.add("One");
        set.add("two");
        set.add("THREE");
        set.add(42);
        set.add('c');
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid accidental cross‑test contamination
        set.clear();
    }

    /**
     * Helper to create a set that contains exactly the strings "alpha", "beta", "gamma"
     * in insertion order.
     */
    private static CaseInsensitiveSet<String> stringOnlySet() {
        CaseInsensitiveSet<String> s = new CaseInsensitiveSet<>();
        s.add("alpha");
        s.add("beta");
        s.add("gamma");
        return s;
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// new String[0] -> allocate new array of size 3
        Arguments.of("empty array, allocate new", new String[0], new String[] { "alpha", "beta", "gamma" }, false), // new String[5] -> larger than set, fill first three, null at index 3, rest unchanged (null)
        Arguments.of("larger array, trailing nulls", new String[5], new String[] { "alpha", "beta", "gamma", null, null }, true));
    }

    private static Stream<Arguments> exactSizeArrayProvider() {
        return Stream.of(// new String[3]
        Arguments.of("exact sized String[]", new String[3], new String[] { "alpha", "beta", "gamma" }, true), // new Object[3] (supertype)
        Arguments.of("exact sized Object[]", new Object[3], new Object[] { "alpha", "beta", "gamma" }, true));
    }

    private static Stream<Arguments> emptySetProvider() {
        return Stream.of(// new String[0] -> same empty array
        Arguments.of("empty array on empty set", new String[0], new String[0], true), // new String[4] -> first element null, rest unchanged (null)
        Arguments.of("larger array on empty set", new String[4], new String[] { null, null, null, null }, true));
    }

    @Test
    void toArray_WithExactSizedObjectArray_ReturnsSameArray() {
        Object[] supplied = new Object[5];
        Object[] result = set.toArray(supplied);
        // The method should return the supplied array (same reference)
        assertSame(supplied, result, "toArray should return the supplied array when it fits");
        // Verify contents – order is the iteration order of the backing map
        // Since the default backing map preserves insertion order, we can assert the exact order
        Object[] expected = { "One", "two", "THREE", 42, 'c' };
        assertArrayEquals(expected, result);
    }

    @Test
    void toArray_WithLargerArray_LeavesTrailingNull() {
        // larger than set size
        Object[] supplied = new Object[10];
        Object[] result = set.toArray(supplied);
        // Should return the same array instance
        assertSame(supplied, result);
        // Elements up to set size must be present, the rest null
        Object[] expected = { "One", "two", "THREE", 42, 'c', null, null, null, null, null };
        assertArrayEquals(expected, result);
    }

    @Test
    void toArray_WithSmallerArray_ReturnsNewArrayOfSameRuntimeType() {
        // Provide an array that is too small; the implementation should allocate a new one
        String[] supplied = new String[2];
        String[] result = set.toArray(supplied);
        // The returned array should NOT be the supplied one
        assertNotSame(supplied, result, "A new array must be allocated when the supplied one is too small");
        // Runtime type must be String[] (the component type of the supplied array)
        assertEquals(String[].class, result.getClass());
        // Verify that only the String elements are present, in insertion order
        String[] expected = { "One", "two", "THREE", null, null };
        // The set contains non‑String elements (42, 'c'), they are omitted because the target array type is String
        // The JDK's toArray(T[]) contract discards non‑compatible elements; the implementation delegates to map.keySet()
        // which follows the same rule.
        assertArrayEquals(expected, result);
    }

    @Test
    void toArray_WithStringArrayAndMixedElements_PreservesOriginalCase() {
        // The set contains three strings with various cases and two non‑String elements.
        // let the method allocate the correct size
        String[] supplied = new String[0];
        String[] result = set.toArray(supplied);
        // The resulting array should contain the strings in their original case, in insertion order
        String[] expected = { "One", "two", "THREE" };
        assertArrayEquals(expected, result);
    }

    @Test
    void toArray_OnEmptySet_ReturnsEmptyArray() {
        CaseInsensitiveSet<Object> empty = new CaseInsensitiveSet<>();
        Object[] supplied = new Object[0];
        Object[] result = empty.toArray(supplied);
        // Should return the same empty array instance
        assertSame(supplied, result);
        assertEquals(0, result.length);
    }

    @Test
    void toArray_WithNullArray_ThrowsNullPointerException() {
        // According to the contract of Collection.toArray(T[]), a null argument should cause NPE
        assertThrows(NullPointerException.class, () -> set.toArray((Object[]) null));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicFunctionalityProvider")
    void toArray_BasicFunctionality(String description, String[] supplied, String[] expected, boolean sameInstance) {
        CaseInsensitiveSet<String> s = stringOnlySet();
        String[] result = s.toArray(supplied);
        if (sameInstance) {
            assertSame(supplied, result, "Should return the supplied array when it fits");
        } else {
            assertNotSame(supplied, result, "Should allocate a new array when supplied one is too small");
        }
        assertArrayEquals(expected, result);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exactSizeArrayProvider")
    void toArray_ExactSizeArray(String description, Object[] supplied, Object[] expected, boolean sameInstance) {
        CaseInsensitiveSet<String> s = stringOnlySet();
        Object[] result = s.toArray(supplied);
        if (sameInstance) {
            assertSame(supplied, result, "Should return the supplied array when size matches");
        } else {
            assertNotSame(supplied, result);
        }
        assertArrayEquals(expected, result);
    }

    @Test
    void toArray_SmallerArray_AllocatesNewArray() {
        CaseInsensitiveSet<String> s = stringOnlySet();
        String[] supplied = new String[2];
        String[] result = s.toArray(supplied);
        assertNotSame(supplied, result, "A new array must be allocated when the supplied one is too small");
        assertEquals(String[].class, result.getClass(), "Runtime type must match component type of supplied array");
        assertArrayEquals(new String[] { "alpha", "beta", "gamma" }, result);
    }

    @Test
    void toArray_LargerArrayWithPreFilledValues_LeavesExtraElementsUntouched() {
        CaseInsensitiveSet<String> s = stringOnlySet();
        String[] supplied = new String[] { "x", "y", "z", "w", "v" };
        String[] result = s.toArray(supplied);
        // Should return the same instance
        assertSame(supplied, result);
        // First three positions replaced, index 3 set to null, index 4 unchanged ("v")
        String[] expected = { "alpha", "beta", "gamma", null, "v" };
        assertArrayEquals(expected, result);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("emptySetProvider")
    void toArray_EmptySet(String description, String[] supplied, String[] expected, boolean sameInstance) {
        CaseInsensitiveSet<String> empty = new CaseInsensitiveSet<>();
        String[] result = empty.toArray(supplied);
        if (sameInstance) {
            assertSame(supplied, result, "Should return the supplied array for empty set");
        } else {
            assertNotSame(supplied, result);
        }
        assertArrayEquals(expected, result);
    }

    @Test
    void toArray_NullArrayArgument_ThrowsNullPointerException() {
        CaseInsensitiveSet<String> s = stringOnlySet();
        assertThrows(NullPointerException.class, () -> s.toArray((String[]) null));
    }

    @Test
    void toArray_IncompatibleArrayType_IntegerArray_ThrowsArrayStoreException() {
        CaseInsensitiveSet<String> s = stringOnlySet();
        Integer[] supplied = new Integer[3];
        assertThrows(ArrayStoreException.class, () -> s.toArray(supplied));
    }

    @Test
    void toArray_SupertypeCharSequenceArray_FillsAndReturnsSameInstance() {
        CaseInsensitiveSet<String> s = stringOnlySet();
        CharSequence[] supplied = new CharSequence[3];
        CharSequence[] result = s.toArray(supplied);
        assertSame(supplied, result);
        assertArrayEquals(new CharSequence[] { "alpha", "beta", "gamma" }, result);
    }

    @Test
    void toArray_IncompatibleSubclassArray_StringBuilder_ThrowsArrayStoreException() {
        CaseInsensitiveSet<String> s = stringOnlySet();
        StringBuilder[] supplied = new StringBuilder[3];
        assertThrows(ArrayStoreException.class, () -> s.toArray(supplied));
    }

    @Test
    void toArray_PrimitiveArrayType_intArray_ThrowsArrayStoreException() {
        CaseInsensitiveSet<String> s = stringOnlySet();
        int[] supplied = new int[3];
        // The method expects an Object[]; passing a primitive array leads to a compile‑time error.
        // We need to use reflection to invoke the method with a primitive array to simulate the runtime scenario.
        assertThrows(ArrayStoreException.class, () -> {
            // Using reflection to bypass compile‑time type checking
            java.lang.reflect.Method m = CaseInsensitiveSet.class.getMethod("toArray", Object[].class);
            m.invoke(s, (Object) supplied);
        });
    }
}
