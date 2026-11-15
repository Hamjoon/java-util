package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toString_Test {

    /**
     * A mock Converter – the method under test does not use it, but we provide one to satisfy the signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Real Converter instance that could be used if needed.
     */
    private Converter realConverter;

    /**
     * Simple POJO with a custom {@code toString()} implementation used for testing.
     */
    private static final class Dummy {

        private final String value;

        Dummy(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Dummy:" + value;
        }
    }

    /**
     * Object whose {@code toString()} returns the literal {@code "Custom"}
     */
    private static final class CustomObject {

        @Override
        public String toString() {
            return "Custom";
        }
    }

    /**
     * Object whose {@code toString()} deliberately returns {@code null}
     */
    private static final class NullToStringObject {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * Object whose {@code toString()} returns an empty string
     */
    private static final class EmptyToStringObject {

        @Override
        public String toString() {
            return "";
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // initialise a real Converter once for the whole test run
        ConverterOptions options = new ConverterOptions();
        realConverter = new Converter(options);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a real Converter with default options – not required for the current tests,
        // but kept for completeness and future extensions.
        ConverterOptions options = new ConverterOptions();
        realConverter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // reset mock after each test – already done in setup, but kept for symmetry
        reset(mockConverter);
    }

    /**
     * Provides all test cases described in the JSON specification.
     *
     * Each {@link Arguments} entry contains:
     *   1. the source object (may be {@code null}),
     *   2. the {@link Converter} to pass (may be {@code null}),
     *   3. the expected string result,
     *   4. a short description used as the test name.
     */
    static Stream<Arguments> provideToStringCases() {
        return Stream.of(// 1. null source, null converter
        Arguments.of(null, null, "null", "When the source object is null the method should return the literal \"null\" regardless of the converter (ignored)."), // 2. Integer wrapper
        Arguments.of(Integer.valueOf(42), null, "42", "A non‑null wrapper object (Integer) is concatenated with an empty string, resulting in its toString() value."), // 3. Custom object with overridden toString()
        Arguments.of(new CustomObject(), null, "Custom", "A user‑defined object whose toString() returns \"Custom\" should be propagated unchanged."), // 4. Regular String with a dummy converter
        Arguments.of("test", realConverter, "test", "When a regular String is supplied, the method returns the same string; the supplied Converter instance is not used."), // 5. Boolean wrapper
        Arguments.of(Boolean.TRUE, realConverter, "true", "A Boolean wrapper should be converted to its string representation \"true\"; the supplied Converter is ignored."), // 6. Double wrapper
        Arguments.of(Double.valueOf(3.14), null, "3.14", "A Double wrapper should be concatenated with an empty string, yielding its standard toString() value."), // 7. Object whose toString() returns null
        Arguments.of(new NullToStringObject(), null, "null", "When an object's toString() method returns null, concatenating it with \"\" results in the literal string \"null\"."), // 8. Object whose toString() returns empty string
        Arguments.of(new EmptyToStringObject(), realConverter, "", "An object whose toString() returns an empty string should produce an empty result; the Converter is not consulted."), // 9. StringBuilder with null converter
        Arguments.of(new StringBuilder("sb"), null, "sb", "StringBuilder's toString() yields its contents; the method should return \"sb\" unchanged."), // 10. StringBuilder with non‑null converter
        Arguments.of(new StringBuilder("sb"), realConverter, "sb", "Even with a non‑null Converter, a StringBuilder is converted by simple concatenation, returning its string content."), // 11. Empty string literal with non‑null converter
        Arguments.of("", realConverter, "", "An empty String literal should be returned unchanged; the Converter is ignored."));
    }

    @Test
    void toString_returnsNullStringWhenInputIsNull() {
        // given
        Object input = null;
        // when
        String result = CharacterConversions.toString(input, mockConverter);
        // then
        assertEquals("null", result, "String representation of null should be the literal \"null\"");
    }

    @Test
    void toString_returnsSameStringWhenInputIsString() {
        // given
        String input = "Hello, World!";
        // when
        String result = CharacterConversions.toString(input, realConverter);
        // then
        assertSame(input, result, "When the input is already a String, the method should return the same instance");
    }

    @Test
    void toString_convertsNumberToString() {
        // given
        Integer input = 42;
        // when
        String result = CharacterConversions.toString(input, mockConverter);
        // then
        assertEquals("42", result);
    }

    @Test
    void toString_usesObjectToStringImplementation() {
        // given
        Dummy dummy = new Dummy("test");
        // when
        String result = CharacterConversions.toString(dummy, mockConverter);
        // then
        assertEquals("Dummy:test", result);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("provideToStringCases")
    void toString_variousInputs(Object from, Converter converter, String expected, String description) {
        // when
        String result = CharacterConversions.toString(from, converter);
        // then
        assertEquals(expected, result, description);
    }
}
