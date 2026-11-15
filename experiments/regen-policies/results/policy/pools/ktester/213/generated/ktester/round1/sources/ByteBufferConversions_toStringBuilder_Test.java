package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toStringBuilder_Test {

    /**
     * Shared {@link Converter} instance used for all tests.
     */
    private static Converter converter;

    /**
     * Initialise the shared {@link Converter} before any test runs.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // The default ConverterOptions provide the standard conversion logic
        // for String, CharBuffer, ByteBuffer, byte[], Map, etc.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Helper that asserts the {@code StringBuilder} result matches a direct {@code String} conversion.
     */
    private void assertStringBuilderMatchesConversion(Object source) {
        String expected = (conv == null) ? String.valueOf(source) : conv.convert(source, String.class);
        StringBuilder actualBuilder = ByteBufferConversions.toStringBuilder(source, conv);
        assertNotNull(actualBuilder, "StringBuilder should never be null");
        assertEquals(expected, actualBuilder.toString(), () -> "Conversion mismatch for source type: " + (source == null ? "null" : source.getClass()));
    }

    /**
     * Provides normal (non‑exceptional) test cases.
     *
     * Each {@link Arguments} consists of:
     *   1. the source object {@code from}
     *   2. the {@link Converter} to use (may be {@code null})
     *   3. the expected {@code String} content of the resulting {@code StringBuilder}
     *   4. a short description (used only for readability)
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideValidCases() {
        return Stream.of(// plain String, converter null (the method should still work)
        org.junit.jupiter.params.provider.Arguments.of("Hello World", null, "Hello World", "Plain String source with null converter"), // ByteBuffer containing ASCII bytes for "Hello"
        org.junit.jupiter.params.provider.Arguments.of(ByteBuffer.wrap(new byte[] { 72, 101, 108, 108, 111 }), null, "Hello", "ByteBuffer source with null converter"), // raw byte array representing "Hello"
        org.junit.jupiter.params.provider.Arguments.of(new byte[] { 72, 101, 108, 108, 111 }, null, "Hello", "byte[] source with null converter"), // null source, null converter – should produce literal "null"
        org.junit.jupiter.params.provider.Arguments.of(null, null, "null", "null source with null converter"), // object with overridden toString()
        org.junit.jupiter.params.provider.Arguments.of(new Object() {

            @Override
            public String toString() {
                return "Custom";
            }
        }, null, "Custom", "Object with custom toString() and null converter"), // CharBuffer directly (wrapped in a helper for readability)
        org.junit.jupiter.params.provider.Arguments.of(CharBuffer.wrap("Test"), null, "Test", "CharBuffer source with null converter"), // Empty string – expect empty StringBuilder
        org.junit.jupiter.params.provider.Arguments.of("", null, "", "Empty string source"), // Unicode surrogate pair (😀)
        org.junit.jupiter.params.provider.Arguments.of("\uD83D\uDE00", null, "\uD83D\uDE00", "Unicode surrogate pair (smiley) source"));
    }

    /**
     * Provides exceptional test cases.
     *
     * Each {@link Arguments} consists of:
     *   1. the source object {@code from}
     *   2. the {@link Converter} to use (may be {@code null})
     *   3. the expected exception class
     *   4. a description
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideExceptionCases() {
        return Stream.of(// Unsupported source type (Integer) with null converter
        org.junit.jupiter.params.provider.Arguments.of(12345, null, IllegalArgumentException.class, "Integer source with null converter should cause IllegalArgumentException"), // Map source with null converter – conversion requires a non‑null Converter
        org.junit.jupiter.params.provider.Arguments.of(new HashMap<>(), null, NullPointerException.class, "Map source with null converter should cause NullPointerException"), // Read‑only ByteBuffer – internal conversion may try to access backing array
        org.junit.jupiter.params.provider.Arguments.of(ByteBuffer.wrap(new byte[] { 65, 66, 67 }).asReadOnlyBuffer(), null, UnsupportedOperationException.class, "Read‑only ByteBuffer should cause UnsupportedOperationException"), // Object whose toString() throws RuntimeException
        org.junit.jupiter.params.provider.Arguments.of(new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("boom");
            }
        }, null, RuntimeException.class, "Object with toString() that throws should propagate RuntimeException"), // Invalid UTF‑8 byte sequence – decoder should reject it
        org.junit.jupiter.params.provider.Arguments.of(new byte[] { (byte) 0xC3, (byte) 0x28 }, null, IllegalArgumentException.class, "Invalid UTF‑8 byte array should cause IllegalArgumentException"));
    }

    @Test
    void testStringInput() {
        String input = "Hello, world!";
        assertStringBuilderMatchesConversion(input, converter);
    }

    @Test
    void testCharBufferInput() {
        CharBuffer input = CharBuffer.wrap("CharBuffer content");
        assertStringBuilderMatchesConversion(input, converter);
    }

    @Test
    void testByteBufferInput() {
        ByteBuffer input = ByteBuffer.wrap(new byte[] { 'J', 'a', 'v', 'a' });
        assertStringBuilderMatchesConversion(input, converter);
    }

    @Test
    void testByteArrayInput() {
        byte[] input = new byte[] { 't', 'e', 's', 't' };
        assertStringBuilderMatchesConversion(input, converter);
    }

    @Test
    void testMapInput() {
        // The Converter treats a Map with a single entry whose key is the constant
        // MapConversions.VALUE (i.e. "_v") as a value holder.
        Map<String, Object> input = new HashMap<>();
        input.put(MapConversions.VALUE, "Map‑based value");
        assertStringBuilderMatchesConversion(input, converter);
    }

    @Test
    void testNullInput() {
        // By contract the Converter returns the string "null" for a null source.
        assertStringBuilderMatchesConversion(null, converter);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideValidCases")
    void testValidConversions(Object from, Converter conv, String expected, String description) {
        StringBuilder sb = ByteBufferConversions.toStringBuilder(from, conv);
        assertNotNull(sb, "Resulting StringBuilder must not be null");
        assertEquals(expected, sb.toString(), () -> "Unexpected content for case: " + description);
        // Additional check for the empty‑string case
        if (expected.isEmpty()) {
            assertEquals(0, sb.length(), () -> "StringBuilder length should be zero for empty input");
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideExceptionCases")
    void testExceptionalConversions(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> ByteBufferConversions.toStringBuilder(from, conv), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
