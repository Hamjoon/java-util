package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toStringBuffer_Test {

    /**
     * Mocked Converter used for all test cases.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Returns an empty string for any input – used for the “null source, DummyConverter” case.
     */
    static class DummyConverter implements Converter {

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            // Simple behaviour: if the target is String, return an empty string for null,
            // otherwise attempt a basic conversion.
            if (toClass == String.class) {
                if (from == null)
                    return toClass.cast("");
                if (from instanceof byte[])
                    return toClass.cast(new String((byte[]) from));
                return toClass.cast(from.toString());
            }
            throw new IllegalArgumentException("Unsupported conversion");
        }
    }

    /**
     * Upper‑cases a String input.
     */
    static class UpperCaseConverter implements Converter {

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            if (toClass == String.class && from instanceof String) {
                return toClass.cast(((String) from).toUpperCase());
            }
            return toClass.cast(String.valueOf(from));
        }
    }

    /**
     * Converts a byte[] into a hex string (two hex digits per byte).
     */
    static class HexConverter implements Converter {

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            if (toClass == String.class && from instanceof byte[]) {
                StringBuilder sb = new StringBuilder();
                for (byte b : (byte[]) from) {
                    sb.append(String.format("%02x", b));
                }
                return toClass.cast(sb.toString());
            }
            throw new IllegalArgumentException("HexConverter only supports byte[]");
        }
    }

    /**
     * Returns null for any conversion – used to trigger NullPointerException in StringBuffer ctor.
     */
    static class NullReturningConverter implements Converter {

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            return null;
        }
    }

    /**
     * Throws a RuntimeException when invoked.
     */
    static class ExceptionThrowingConverter implements Converter {

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            throw new RuntimeException("Converter failure");
        }
    }

    /**
     * Object whose toString() deliberately throws an exception.
     */
    static class BadToStringObject {

        @Override
        public String toString() {
            throw new RuntimeException("Bad toString");
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify no unexpected interactions with the mock.
        verifyNoMoreInteractions(mockConverter);
    }

    private static Stream<Arguments> successfulCases() {
        return Stream.of(// Null source, DummyConverter -> empty string
        Arguments.of(null, new DummyConverter(), ""), // Null source, null converter -> NullPointerException (handled in exception test)
        // Non‑null source, null converter (String)
        Arguments.of("simple string", null, "simple string"), // Non‑null source, null converter (byte[])
        Arguments.of(new byte[] { 72, 101, 108, 108, 111 }, null, "Hello"), // Byte array with DummyConverter (should behave like default)
        Arguments.of(new byte[] { 65, 66, 67 }, new DummyConverter(), "ABC"), // Empty byte array with DummyConverter
        Arguments.of(new byte[] {}, new DummyConverter(), ""), // Char array, null converter
        Arguments.of(new char[] { 'x', 'y', 'z' }, null, "xyz"), // Empty char array, null converter
        Arguments.of(new char[] {}, null, ""), // String source with DummyConverter (should bypass conversion)
        Arguments.of("already a string", new DummyConverter(), "already a string"), // ByteBuffer source, null converter
        Arguments.of(ByteBuffer.wrap(new byte[] { 68, 69, 70 }), null, "DEF"), // CharBuffer source, null converter
        Arguments.of(CharBuffer.wrap(new char[] { 'p', 'q', 'r' }), null, "pqr"), // UpperCaseConverter transforms string
        Arguments.of("original", new UpperCaseConverter(), "ORIGINAL"), // HexConverter transforms byte[]
        Arguments.of(new byte[] { 49, 50, 51 }, new HexConverter(), "313233"), // Unsupported source type (Integer) with null converter
        Arguments.of(12345, null, "12345"));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// Both source and converter null -> NullPointerException from internal toString()
        Arguments.of(null, null, NullPointerException.class), // Converter throws RuntimeException
        Arguments.of("trigger", new ExceptionThrowingConverter(), RuntimeException.class), // Converter returns null -> StringBuffer ctor receives null -> NullPointerException
        Arguments.of("any string", new NullReturningConverter(), NullPointerException.class), // Source object's toString throws RuntimeException, converter null
        Arguments.of(new BadToStringObject(), null, RuntimeException.class));
    }

    /**
     * Verifies that {@code toStringBuffer} returns a {@link StringBuffer}
     * containing the string produced by the supplied {@link Converter}.
     */
    @Test
    void toStringBuffer_returnsStringBufferWithConvertedString() {
        // GIVEN
        // "ABC" in ASCII
        byte[] source = new byte[] { 0x41, 0x42, 0x43 };
        String expected = "converted‑ABC";
        // The ByteArrayConversions.toString(..) implementation ultimately calls
        // converter.convert(source, String.class).  We stub that call.
        when(mockConverter.convert(source, String.class)).thenReturn(expected);
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer should not be null");
        assertEquals(expected, result.toString(), "StringBuffer should contain the string returned by the Converter");
        // Verify that the mock was used exactly as expected.
        verify(mockConverter).convert(source, String.class);
    }

    /**
     * Verifies that a {@code null} source value is handled without throwing
     * and that the resulting {@link StringBuffer} contains the string
     * returned by the converter (which may be {@code null}).
     */
    @Test
    void toStringBuffer_withNullSource_returnsStringBufferContainingConverterResult() {
        // GIVEN
        Object source = null;
        String expected = "null‑value";
        when(mockConverter.convert(source, String.class)).thenReturn(expected);
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer should not be null even when source is null");
        assertEquals(expected, result.toString(), "StringBuffer should contain the string returned by the Converter for a null source");
        verify(mockConverter).convert(source, String.class);
    }

    /**
     * Verifies that when the converter returns {@code null}, the
     * {@link StringBuffer} is created with a {@code "null"} string
     * (the behaviour of {@link StringBuffer#StringBuffer(String)}).
     */
    @Test
    void toStringBuffer_whenConverterReturnsNull_createsStringBufferWithLiteralNull() {
        // GIVEN
        String[] source = new String[] { "foo" };
        when(mockConverter.convert(source, String.class)).thenReturn(null);
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(source, mockConverter);
        // THEN
        assertNotNull(result, "StringBuffer instance must be non‑null");
        assertEquals("null", result.toString(), "When the converter returns null, StringBuffer should contain the literal \"null\"");
        verify(mockConverter).convert(source, String.class);
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("successfulCases")
    void toStringBuffer_successful(Object from, Converter converter, String expected) {
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(from, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer must not be null");
        assertEquals(expected, result.toString(), () -> "Expected StringBuffer to contain \"" + expected + "\"");
        // No interaction verification needed for non‑mocked converters.
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("exceptionCases")
    void toStringBuffer_exceptions(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> ByteArrayConversions.toStringBuffer(from, converter));
    }

    @Test
    void toStringBuffer_returnsStringBufferWithConvertedString_usingMockito() {
        // GIVEN
        // "ABC"
        byte[] source = new byte[] { 0x41, 0x42, 0x43 };
        String expected = "converted‑ABC";
        when(mockConverter.convert(source, String.class)).thenReturn(expected);
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.toString());
        verify(mockConverter).convert(source, String.class);
    }

    @Test
    void toStringBuffer_withNullSource_returnsStringBufferContainingConverterResult_usingMockito() {
        // GIVEN
        Object source = null;
        String expected = "null‑value";
        when(mockConverter.convert(source, String.class)).thenReturn(expected);
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.toString());
        verify(mockConverter).convert(source, String.class);
    }

    @Test
    void toStringBuffer_whenConverterReturnsNull_createsStringBufferWithLiteralNull_usingMockito() {
        // GIVEN
        String[] source = new String[] { "foo" };
        when(mockConverter.convert(source, String.class)).thenReturn(null);
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals("null", result.toString());
        verify(mockConverter).convert(source, String.class);
    }
}
