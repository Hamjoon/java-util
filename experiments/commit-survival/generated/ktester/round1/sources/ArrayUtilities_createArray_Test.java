package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_createArray_Test {

    class Dummy {

        final int id;

        Dummy(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Dummy) && ((Dummy) o).id == this.id;
        }
    }

    /**
     * Supplies the data for {@link #createArray_NormalCases(String, Object[], int, Object[])}.
     *
     * <p>Each {@link Arguments} instance encodes:
     * <ul>
     *   <li>a description (for the test name),</li>
     *   <li>the var‑args elements,</li>
     *   <li>the expected length, and</li>
     *   <li>the expected element values.</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideCreateArrayCases() {
        return Stream.of(// 0) No arguments → empty Object[]
        org.junit.jupiter.params.provider.Arguments.of("Calling createArray() with no arguments should return an empty array", new Object[] {}, 0, new Object[] {}), // 1) Single Integer element
        org.junit.jupiter.params.provider.Arguments.of("Single non‑null Integer element", new Object[] { 5 }, 1, new Object[] { 5 }), // 2) Multiple String elements
        org.junit.jupiter.params.provider.Arguments.of("Multiple String elements", new Object[] { "alpha", "beta", "gamma" }, 3, new Object[] { "alpha", "beta", "gamma" }), // 3) Explicit null element
        org.junit.jupiter.params.provider.Arguments.of("Explicit null element", new Object[] { null }, 1, new Object[] { null }), // 4) Boolean objects
        org.junit.jupiter.params.provider.Arguments.of("Array of Boolean objects", new Object[] { Boolean.TRUE, Boolean.FALSE, Boolean.TRUE }, 3, new Object[] { Boolean.TRUE, Boolean.FALSE, Boolean.TRUE }));
    }

    @Test
    void createArray_WithMultipleElements_ReturnsArrayContainingThoseElements() {
        // given
        Integer[] expected = new Integer[] { 1, 2, 3 };
        // when
        Integer[] actual = ArrayUtilities.createArray(1, 2, 3);
        // then
        assertArrayEquals(expected, actual, "The array returned by createArray should contain the supplied elements in order");
        // The returned array should be of the same runtime type as the var‑args array.
        assertEquals(Integer[].class, actual.getClass(), "The runtime type of the returned array should match the generic type");
    }

    @Test
    void createArray_WithNoElements_ReturnsEmptyObjectArray() {
        // when
        Object[] result = ArrayUtilities.createArray();
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.length, "Resulting array should be empty");
        assertEquals(Object[].class, result.getClass(), "When called without arguments the array type should be Object[]");
    }

    @Test
    void createArray_WithSingleNullElement_ReturnsArrayContainingNull() {
        // when
        String[] result = ArrayUtilities.createArray((String) null);
        // then
        assertNotNull(result, "Result should not be null even if the element is null");
        assertEquals(1, result.length, "Array should have exactly one element");
        assertNull(result[0], "The sole element should be null");
    }

    @Test
    void createArray_WithCustomType_ReturnsArrayOfThatType() {
        // given
        class Dummy {

            final int id;

            Dummy(int id) {
                this.id = id;
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof Dummy) && ((Dummy) o).id == this.id;
            }
        }
        Dummy d1 = new Dummy(10);
        Dummy d2 = new Dummy(20);
        Dummy[] expected = new Dummy[] { d1, d2 };
        // when
        Dummy[] actual = ArrayUtilities.createArray(d1, d2);
        // then
        assertArrayEquals(expected, actual, "Array should preserve the custom objects in the order supplied");
        assertEquals(Dummy[].class, actual.getClass(), "Returned array should be of the custom type");
    }

    /**
     * Executes the normal test scenarios described in the JSON payload.
     *
     * @param description   a short textual description (used only for readability)
     * @param inputElements the elements passed to {@code createArray}
     * @param expectedLen   the expected length of the returned array
     * @param expectedElems the expected contents of the returned array
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideCreateArrayCases")
    void createArray_NormalCases(String description, Object[] inputElements, int expectedLen, Object[] expectedElems) {
        // when – invoke the var‑args method.  Passing the array directly is
        // equivalent to spreading the elements because the method signature is
        // T... (i.e. T[]).
        Object[] actual = ArrayUtilities.createArray(inputElements);
        // then – basic structural assertions
        assertNotNull(actual, "Resulting array must never be null");
        assertEquals(expectedLen, actual.length, "Array length should match the expected length");
        // then – content assertions
        assertArrayEquals(expectedElems, actual, "Array contents should be exactly as supplied");
        // then – runtime type check (the component type should be the most specific
        // type that can be inferred from the supplied arguments)
        Class<?> expectedComponent = expectedElems.length == 0 ? Object.class : expectedElems[0] == null ? Object.class : expectedElems[0].getClass();
        assertEquals(java.lang.reflect.Array.newInstance(expectedComponent, 0).getClass(), actual.getClass(), "Runtime array type should match the inferred component type");
    }

    @Test
    void createArray_NullVarArgsArray_ThrowsNullPointerException() {
        // given – a null reference cast to the appropriate array type
        String[] nullArray = null;
        // when / then – the method should throw NPE because the var‑args
        // parameter itself is null (the Javadoc documents this behaviour).
        assertThrows(NullPointerException.class, () -> ArrayUtilities.createArray(nullArray), "Passing a null var‑args array must result in NullPointerException");
    }
}
