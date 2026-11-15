package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.CharBuffer;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toString_Test {

    @Mock
    private Converter // a dummy converter – its behaviour is not needed for the tested scenarios
    mockConverter;

    /**
     * Simple identity converter used throughout the tests.
     */
    private static final Converter IDENTITY_CONVERTER = new IdentityConverter();

    /**
     * A minimal {@link Converter} implementation that returns the supplied object unchanged.
     */
    private static class IdentityConverter implements Converter {

        @Override
        public Object convert(Object o) {
            return o;
        }
    }

    /**
     * Simple immutable holder used by the parameterised test source.
     */
    private static final class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final String expected;

        final String description;

        ArgumentsHolder(Object from, Converter converter, String expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Provides arguments for the parameterised tests that cover the “happy path” inputs.
     */
    private static Stream<ArgumentsHolder> happyPathProvider() {
        return Stream.of(// Null input – should produce the string "null"
        new ArgumentsHolder(null, IDENTITY_CONVERTER, "null", "Null input should be converted to the literal string \"null\""), // Simple String – should be returned unchanged
        new ArgumentsHolder("Simple test string", IDENTITY_CONVERTER, "Simple test string", "String input should be returned as‑is"), // CharBuffer – toString should delegate to CharBuffer#toString()
        new ArgumentsHolder(CharBuffer.wrap("Hello ByteBufferConversions"), IDENTITY_CONVERTER, "Hello ByteBufferConversions", "CharBuffer should be converted via its own toString()"), // byte[] ASCII – “Hello”
        new ArgumentsHolder(new byte[] { 72, 101, 108, 108, 111 }, IDENTITY_CONVERTER, "Hello", "ASCII byte array should be decoded to the expected string"), // empty byte[]
        new ArgumentsHolder(new byte[] {}, IDENTITY_CONVERTER, "", "Empty byte array should yield an empty string"), // ByteBuffer wrapping ASCII bytes – “ABC”
        new ArgumentsHolder(ByteBuffer.wrap(new byte[] { 65, 66, 67 }), IDENTITY_CONVERTER, "ABC", "ByteBuffer should be decoded to the expected string"), // char[]
        new ArgumentsHolder(new char[] { 'x', 'y', 'z' }, IDENTITY_CONVERTER, "xyz", "char[] should be turned into the matching string"), // StringBuilder
        new ArgumentsHolder(new StringBuilder("builder"), IDENTITY_CONVERTER, "builder", "StringBuilder content should be converted to a string"), // StringBuffer
        new ArgumentsHolder(new StringBuffer("buf"), IDENTITY_CONVERTER, "buf", "StringBuffer content should be converted to a string"), // Large input – 10 000 ‘a’ characters
        new ArgumentsHolder(generateLargeString(10_000), IDENTITY_CONVERTER, generateLargeString(10_000), "Large input should be handled without truncation"));
    }

    /**
     * Utility to create a long string consisting of the given character repeated {@code length} times.
     */
    private static String generateLargeString(int length) {
        char[] chars = new char[length];
        java.util.Arrays.fill(chars, 'a');
        return new String(chars);
    }

    /**
     * Verify that a {@link CharBuffer} is converted to a {@link String}
     * by delegating to {@code CharBuffer#toString()}.
     */
    @Test
    void toString_givenCharBuffer_returnsItsStringRepresentation() {
        // GIVEN
        CharBuffer input = CharBuffer.wrap("Hello ByteBufferConversions");
        // WHEN
        String result = ByteBufferConversions.toString(input, mockConverter);
        // THEN
        assertEquals("Hello ByteBufferConversions", result);
    }

    /**
     * Verify that a {@link String} input is correctly transformed to a {@link String}
     * (i.e., the method returns the original string). The internal conversion
     * from {@code String} to {@code CharBuffer} is exercised indirectly.
     */
    @Test
    void toString_givenString_returnsSameString() {
        // GIVEN
        String input = "Simple test string";
        // WHEN
        String result = ByteBufferConversions.toString(input, mockConverter);
        // THEN
        assertEquals(input, result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("happyPathProvider")
    void toString_happyPath(ArgumentsHolder holder) {
        // WHEN
        String result = ByteBufferConversions.toString(holder.from, holder.converter);
        // THEN
        assertEquals(holder.expected, result);
    }

    @Test
    void toString_nullConverter_throwsNullPointerException() {
        // GIVEN
        String input = "test";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toString(input, null), "Providing a null Converter should raise NullPointerException");
    }

    @Test
    void toString_unsupportedObject_throwsIllegalArgumentException() {
        // GIVEN
        Object unsupported = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> ByteBufferConversions.toString(unsupported, IDENTITY_CONVERTER), "Unsupported object type should trigger IllegalArgumentException");
    }

    @Test
    void toString_unsupportedDate_throwsIllegalArgumentException() {
        // GIVEN
        Date date = new Date();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> ByteBufferConversions.toString(date, IDENTITY_CONVERTER), "Date objects are not supported and should trigger IllegalArgumentException");
    }

    @Test
    void toString_converterThrowsRuntimeException_propagates() {
        // GIVEN
        Converter badConverter = new Converter() {

            @Override
            public Object convert(Object o) {
                throw new RuntimeException("forced");
            }
        };
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ByteBufferConversions.toString("trigger", badConverter));
        assertEquals("forced", ex.getMessage());
    }

    @Test
    void toString_converterThrowsIllegalStateException_propagates() {
        // GIVEN
        Converter badConverter = new Converter() {

            @Override
            public Object convert(Object o) {
                throw new IllegalStateException("bad state");
            }
        };
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ByteBufferConversions.toString(new byte[] { 65, 66 }, badConverter));
        assertEquals("bad state", ex.getMessage());
    }
}
