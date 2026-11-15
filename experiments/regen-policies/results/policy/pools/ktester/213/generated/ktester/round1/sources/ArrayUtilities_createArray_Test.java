package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_createArray_Test {

    /**
     * Supplies the arguments for {@link #createArrayBasicScenarios}.
     */
    private static Stream<Arguments> basicScenarioProvider() {
        return Stream.of(// 1. Empty var‑args → empty array
        Arguments.of("Empty var‑args returns an empty array", new Object[] {}, 0, new Object[] {}, false), // 2. Single non‑null element
        Arguments.of("Single non‑null element is preserved", new Object[] { "hello" }, 1, new Object[] { "hello" }, false), // 3. Multiple Integer elements (auto‑boxed)
        Arguments.of("Multiple Integer elements preserve order", new Object[] { 1, 2, 3 }, 3, new Object[] { 1, 2, 3 }, false), // 4. Single null element
        Arguments.of("Single null element results in length‑1 array containing null", new Object[] { null }, 1, new Object[] { null }, false), // 5. Existing array passed as the sole var‑arg element (no flattening)
        Arguments.of("Passing an existing array as the only var‑arg element preserves the reference", new Object[] { new Integer[] { 5, 6 } }, 1, new Object[] { new Integer[] { 5, 6 } }, true));
    }

    @Test
    void createArrayReturnsSameReferenceForNonEmptyArray() {
        String[] source = { "alpha", "beta", "gamma" };
        String[] result = ArrayUtilities.createArray(source);
        // The method should return the exact same array instance
        assertSame(source, result, "createArray should return the original array reference");
        assertArrayEquals(source, result, "Returned array contents must match the source");
    }

    @Test
    void createArrayHandlesEmptyVarargs() {
        // Calling with no arguments yields an empty Object[] (length 0)
        Object[] result = ArrayUtilities.createArray();
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.length, "Resulting array should be empty");
    }

    @Test
    void createArrayPreservesNullElements() {
        String[] source = { null, "test", null };
        String[] result = ArrayUtilities.createArray(source);
        assertSame(source, result, "Reference must be identical even when containing nulls");
        assertArrayEquals(source, result, "Array contents (including nulls) must be unchanged");
    }

    @Test
    void createArrayWithExplicitGenericType() {
        // Explicit generic type ensures the returned array is of the specified component type
        Integer[] result = ArrayUtilities.<Integer>createArray(1, 2, 3);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);
        // Verify the runtime component type
        assertEquals(Integer.class, result.getClass().getComponentType(), "Component type should be Integer");
    }

    @Test
    void createArraySingleElementVararg() {
        // Varargs with a single element should still return the same underlying array
        String[] result = ArrayUtilities.createArray("only");
        assertEquals(1, result.length);
        assertEquals("only", result[0]);
    }

    /**
     * Parameterized test covering the "basic scenarios" group.
     * Each argument set contains:
     *   - a description (used only for readability)
     *   - the var‑args elements to pass to {@code createArray}
     *   - the expected length of the returned array
     *   - the expected content (as an Object array)
     *   - a flag indicating whether we need to perform an identity check on the first element.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("basicScenarioProvider")
    void createArrayBasicScenarios(String description, Object[] elements, int expectedLength, Object[] expectedContent, boolean identityCheck) {
        // The var‑args call – the compiler infers the generic type from the supplied elements.
        Object[] result = ArrayUtilities.createArray(elements);
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedLength, result.length, "Array length should match the expected length");
        // Verify the content of each element (using deep equality where appropriate)
        assertArrayEquals(expectedContent, result, "Array content should match the expected values");
        // If the test case demands an identity check, ensure the first element is the exact same object.
        if (identityCheck && expectedLength > 0) {
            assertSame(expectedContent[0], result[0], "First element should be the same instance as supplied");
        }
    }

    /**
     * Verifies the behaviour when a *null* var‑args array is supplied.
     * According to the Javadoc, a {@code NullPointerException} is not thrown;
     * the method simply returns {@code null}.
     */
    @Test
    void createArrayNullVarargsReturnsNull() {
        String[] result = ArrayUtilities.createArray((String[]) null);
        assertNull(result, "Passing a null var‑args array should return null");
    }

    /**
     * Additional explicit test for the identity‑preserving case where the
     * supplied element itself is an array. This mirrors the JSON case that
     * requires an identity check on the first element.
     */
    @Test
    void createArrayWithArrayAsSingleElementPreservesIdentity() {
        Integer[] inner = new Integer[] { 5, 6 };
        // The generic type inferred is Integer[] → the returned type is Integer[][].
        Integer[][] result = ArrayUtilities.createArray(inner);
        assertEquals(1, result.length, "Resulting array should contain exactly one element");
        assertSame(inner, result[0], "The inner array reference must be preserved");
    }
}
