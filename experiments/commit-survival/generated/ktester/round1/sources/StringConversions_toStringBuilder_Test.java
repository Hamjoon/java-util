package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringConversions_toStringBuilder_Test {

    /**
     * Mocked {@link Converter} – not used by the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Functional interface used to encapsulate the verification logic for each test case.
     */
    @FunctionalInterface
    private interface ResultVerifier {

        /**
         * Verify the result of {@link StringConversions#toStringBuilder(Object, Converter)}.
         *
         * @param result the {@link StringBuilder} returned by the method (may be {@code null})
         * @param source the original source object passed to the method
         */
        void verify(StringBuilder result, Object source);
    }

    /**
     * Supplies test data for the parameterized test {@link #toStringBuilder_VariousScenarios}.
     *
     * Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the source object (may be {@code null})</li>
     *   <li>the {@link Converter} to pass (may be {@code null} or the mock)</li>
     *   <li>a description used only for readability</li>
     *   <li>a {@link ResultVerifier} that performs the appropriate assertions</li>
     * </ul>
     */
    private static Stream<Arguments> provideTestCases() {
        // Helper lambda to verify normal (non‑exception) results
        ResultVerifier normalVerifier = (result, source) -> {
            assertNotNull(result, "Result should not be null for non‑null source");
            assertEquals(String.valueOf(source), result.toString(), "StringBuilder must contain source.toString()");
        };
        // 1. StringBuilder input – must produce a *new* instance with same content
        ResultVerifier copyVerifier = (result, source) -> {
            assertNotNull(result);
            assertEquals(source.toString(), result.toString(), "Content must match the original StringBuilder's toString()");
            // Mutate the original after the call and ensure the result stays unchanged
            ((StringBuilder) source).append("changed");
            assertEquals("already", result.toString(), "Returned StringBuilder must be independent of the original");
        };
        // 2. Empty string from custom toString()
        ResultVerifier emptyVerifier = (result, source) -> {
            assertNotNull(result);
            assertEquals(0, result.length(), "Resulting StringBuilder should be empty");
        };
        // 3. Exception propagation from toString()
        ResultVerifier exceptionVerifier = (result, source) -> {
            // This verifier is never called because we expect an exception.
        };
        // 4. Null return from toString() leading to NPE
        ResultVerifier npeVerifier = (result, source) -> {
            // Not reached; assertThrows handles it.
        };
        return Stream.of(// Group: Additional Scenarios – StringBuffer
        arguments(new StringBuffer("buf"), null, "StringBuffer should be converted via toString()", normalVerifier), // Group: Additional Scenarios – Empty toString()
        arguments(new Object() {

            @Override
            public String toString() {
                return "";
            }
        }, null, "Object with empty toString() yields empty StringBuilder", emptyVerifier), // Group: Additional Scenarios – toString() throws IllegalStateException
        arguments(new Object() {

            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        }, null, "Object whose toString() throws should propagate the exception", exceptionVerifier), // Group: Additional Scenarios – Unicode handling with a real mockConverter
        arguments("Unicode \u2603", mockConverter, "Unicode character should be preserved", normalVerifier), // Group: Additional Scenarios – Independent copy for StringBuilder input
        arguments(new StringBuilder("mutable"), null, "Returned StringBuilder must be an independent copy", copyVerifier), // Group: toString returns null – expect NullPointerException
        arguments(new Object() {

            @Override
            public String toString() {
                return null;
            }
        }, null, "toString() returning null should cause NullPointerException", npeVerifier));
    }

    @Test
    void toStringBuilder_ReturnsNullWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        StringBuilder result = StringConversions.toStringBuilder(source, mockConverter);
        // THEN
        assertNull(result, "Result should be null when the source object is null");
    }

    @Test
    void toStringBuilder_ReturnsBuilderWithStringContent_ForStringInput() {
        // GIVEN
        String source = "Hello, world!";
        // WHEN
        StringBuilder result = StringConversions.toStringBuilder(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuilder should not be null for non‑null input");
        assertEquals(source, result.toString(), "StringBuilder should contain the exact string representation of the source");
    }

    @Test
    void toStringBuilder_ReturnsBuilderWithStringContent_ForIntegerInput() {
        // GIVEN
        Integer source = 42;
        // WHEN
        StringBuilder result = StringConversions.toStringBuilder(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(source.toString(), result.toString(), "StringBuilder should contain the integer's toString() value");
    }

    @Test
    void toStringBuilder_UsesCustomToStringImplementation() {
        // GIVEN
        Object source = new Object() {

            @Override
            public String toString() {
                return "custom-toString";
            }
        };
        // WHEN
        StringBuilder result = StringConversions.toStringBuilder(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals("custom-toString", result.toString(), "StringBuilder should reflect the custom toString() implementation");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("provideTestCases")
    void toStringBuilder_VariousScenarios(Object source, Converter converter, String description, ResultVerifier verifier) {
        if ("Object whose toString() throws should propagate the exception".equals(description)) {
            // Expect the IllegalStateException to be thrown unchanged
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> StringConversions.toStringBuilder(source, converter), "Method must propagate runtime exceptions from source.toString()");
            assertEquals("boom", ex.getMessage());
            return;
        }
        if ("toString() returning null should cause NullPointerException".equals(description)) {
            // Expect NPE because StringBuilder constructor receives null
            assertThrows(NullPointerException.class, () -> StringConversions.toStringBuilder(source, converter), "Method must throw NullPointerException when source.toString() returns null");
            return;
        }
        // Normal (non‑exception) path
        StringBuilder result = StringConversions.toStringBuilder(source, converter);
        verifier.verify(result, source);
    }
}
