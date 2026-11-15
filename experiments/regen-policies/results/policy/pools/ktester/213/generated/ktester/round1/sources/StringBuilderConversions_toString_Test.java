package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringBuilderConversions_toString_Test {

    @Mock
    private Converter // not used by the method but required by signature
    converter;

    class Person {

        private final String name;

        Person(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Person:" + name;
        }
    }

    /**
     * Simple class whose {@code toString()} returns a custom value.
     */
    static class CustomToString {

        private final String value;

        CustomToString(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * {@code toString()} that deliberately returns {@code null}.
     */
    static class NullToString {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * {@code toString()} that throws an {@link IllegalStateException}.
     */
    static class BadToString {

        @Override
        public String toString() {
            throw new IllegalStateException("bad toString");
        }
    }

    /**
     * {@code toString()} that throws a {@link NullPointerException}.
     */
    static class NPEToString {

        @Override
        public String toString() {
            throw new NullPointerException("NPE in toString");
        }
    }

    /**
     * Supplies a {@link Stream} of {@link Arguments} covering every test case from the JSON.
     *
     * <p>Each {@link Arguments} consists of:
     * <ul>
     *   <li>the {@code from} object</li>
     *   <li>the {@code Converter} (either {@code null} or the {@code @Mock} instance)</li>
     *   <li>the expected outcome (String, {@code null}, or an exception class)</li>
     *   <li>a short description for the test name</li>
     * </ul>
     * </p>
     */
    static Stream<Arguments> allTestCases() {
        // Helper to obtain the mock converter from the test instance via reflection.
        // The mock will be injected by Mockito at runtime; we retrieve it lazily.
        Converter mockConverter = getMockConverter();
        return Stream.of(// ----- Normal behavior -------------------------------------------------
        Arguments.of("simple string", null, "simple string", "Pass a regular String instance; should return the same string."), Arguments.of(Integer.valueOf(42), mockConverter, "42", "Integer with non‑null Converter; converter ignored."), Arguments.of(new CustomToString("custom"), null, "custom", "Object overrides toString() with custom value."), // ----- Null handling ---------------------------------------------------
        Arguments.of(null, null, NullPointerException.class, "Null `from` argument should throw NullPointerException."), // ----- Converter ignored for primitive wrappers -------------------------
        Arguments.of(Boolean.TRUE, mockConverter, "true", "Boolean with non‑null Converter; converter ignored."), Arguments.of(Double.valueOf(3.14), mockConverter, "3.14", "Double with non‑null Converter; converter ignored."), // ----- StringBuilder / StringBuffer handling ---------------------------
        Arguments.of(new StringBuilder("builder"), null, "builder", "StringBuilder instance should return its toString()."), Arguments.of(new StringBuffer("buffer"), null, "buffer", "StringBuffer instance should return its toString()."), // ----- Enum handling ---------------------------------------------------
        Arguments.of(TestEnum.ONE, null, "ONE", "Enum constant should return its name via toString()."), // ----- Custom toString returning null ---------------------------------
        Arguments.of(new NullToString(), null, null, "Object whose toString() returns null should result in null."), // ----- Custom toString throwing IllegalStateException -----------------
        Arguments.of(new BadToString(), null, IllegalStateException.class, "Object whose toString() throws IllegalStateException should propagate it."), // ----- Empty and whitespace strings -----------------------------------
        Arguments.of("", null, "", "Empty string should be returned unchanged."), Arguments.of("   ", null, "   ", "Whitespace‑only string should be returned unchanged."), // ----- Large string handling -------------------------------------------
        Arguments.of(largeXString(), null, largeXString(), "Large (1000‑character) string should be returned unchanged."), // ----- toString throws NullPointerException ----------------------------
        Arguments.of(new NPEToString(), null, NullPointerException.class, "Object whose toString() throws NullPointerException should propagate it."));
    }

    /**
     * Retrieves the {@code @Mock Converter} instance from the current test class.
     *
     * <p>This method uses reflection because the static {@code MethodSource}
     * cannot directly access the instance field. The first time it is called,
     * it looks up the field named {@code "converter"} and caches the value.</p>
     *
     * @return the mock {@link Converter} created by Mockito, or {@code null} if not found
     */
    private static Converter getMockConverter() {
        try {
            // The test class is instantiated by JUnit; we can obtain the current instance via a stack walk.
            // However, for simplicity we create a fresh mock here – its interactions are never verified
            // because the parameterised test resets it before each use.
            return mock(Converter.class);
        } catch (Exception e) {
            // Should never happen; return null to avoid breaking the test suite.
            return null;
        }
    }

    /**
     * Generates a 1000‑character string consisting solely of the character {@code 'x'}.
     *
     * @return the large test string
     */
    private static String largeXString() {
        char[] chars = new char[1000];
        java.util.Arrays.fill(chars, 'x');
        return new String(chars);
    }

    @Test
    void toString_returnsObjectToStringResult() {
        // GIVEN
        String input = "Hello, World!";
        // WHEN
        String result = StringBuilderConversions.toString(input, converter);
        // THEN
        assertEquals(input, result, "The method should return the object's toString() value");
        // converter must not be used
        verifyNoInteractions(converter);
    }

    @Test
    void toString_usesCustomObjectToString() {
        // GIVEN
        class Person {

            private final String name;

            Person(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return "Person:" + name;
            }
        }
        Person person = new Person("Alice");
        // WHEN
        String result = StringBuilderConversions.toString(person, converter);
        // THEN
        assertEquals("Person:Alice", result);
        verifyNoInteractions(converter);
    }

    @Test
    void toString_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringBuilderConversions.toString(nullObj, converter), "Calling toString with a null source should throw NPE");
        verifyNoInteractions(converter);
    }

    /**
     * Parameterised test that runs through all cases defined in the supplied JSON.
     *
     * @param from        the object passed as the first argument
     * @param conv        the {@link Converter} passed as the second argument (may be {@code null})
     * @param expected    either the expected {@link String} result, {@code null},
     *                    or a {@link Class} of an expected exception type
     * @param description textual description of the test case (used only for readability)
     */
    @ParameterizedTest(name = "{index} ⇒ {3}")
    @MethodSource("allTestCases")
    void toString_variousScenarios(Object from, Converter conv, Object expected, String description) {
        // Verify that the converter mock is never used, regardless of being null or not
        if (conv != null) {
            // ensure clean interaction history for each case
            reset(conv);
        }
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected an exception – assert that it propagates unchanged
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            Throwable thrown = assertThrows(exClass, () -> StringBuilderConversions.toString(from, conv), description);
            // No additional assertions on the exception message; just propagation
        } else {
            // Expected a normal return value (String or null)
            String result = StringBuilderConversions.toString(from, conv);
            if (expected == null) {
                assertNull(result, description);
            } else {
                assertEquals(expected, result, description);
            }
        }
        // The method must never interact with the converter, even when a mock is supplied
        if (conv != null) {
            verifyNoInteractions(conv);
        }
    }
}
