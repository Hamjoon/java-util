package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toCharBuffer_Test {

    /**
     * Shared {@link Converter} instance used by all test methods.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance used by the happy‑path tests.
     */
    private static Converter defaultConverter;

    /**
     * Simple wrapper that holds a {@link CharBuffer}.  It implements {@link CharSequence}
     *  so that the conversion logic treats it like any other character sequence.
     */
    private static final class CharBufferWrapper implements CharSequence {

        private final CharBuffer buffer;

        CharBufferWrapper(CharBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public int length() {
            return buffer.length();
        }

        @Override
        public char charAt(int index) {
            return buffer.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return buffer.subSequence(start, end);
        }

        @Override
        public String toString() {
            return buffer.toString();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use a real Converter with default options (UTF‑8) for the basic tests.
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the
        // test class tidy if additional resources are added later.
        converter = null;
    }

    /**
     * Creates a mock {@link Converter} that returns a mock {@code Options}
     *  whose {@code getCharset()} method yields the supplied {@code charset}.
     */
    private static Converter mockConverter(Charset charset) {
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getCharset()).thenReturn(charset);
        return converter;
    }

    /**
     * Generates a byte array of the given length filled with the supplied byte value.
     */
    private static byte[] largeByteArray(int length, byte value) {
        byte[] arr = new byte[length];
        for (int i = 0; i < length; i++) {
            arr[i] = value;
        }
        return arr;
    }

    private static Stream<Arguments> typicalSuccessProvider() {
        return Stream.of(// byte[] → "abc"
        Arguments.of(new byte[] { 97, 98, 99 }, StandardCharsets.UTF_8, "abc", "Converts a UTF‑8 encoded byte array to a CharBuffer; verifies normal path for byte[] input."), // ByteBuffer → "ABC"
        Arguments.of(ByteBuffer.wrap(new byte[] { 0x41, 0x42, 0x43 }), StandardCharsets.UTF_8, "ABC", "Passes a ByteBuffer directly; the method should decode it using the supplied charset."), // String → "hello world"
        Arguments.of("hello world", StandardCharsets.UTF_8, "hello world", "String input is first turned into a byte[] (via the converter) then decoded; checks the happy‑path for CharSequence."), // empty byte[]
        Arguments.of(new byte[0], StandardCharsets.UTF_8, "", "Empty byte array should result in an empty CharBuffer without errors."), // ISO‑8859‑1 bytes → "Ã©"
        Arguments.of(new byte[] { (byte) 0xC3, (byte) 0xA9 }, StandardCharsets.ISO_8859_1, "Ã©", "Same bytes decoded with ISO‑8859‑1 produce different characters than UTF‑8; verifies charset handling."), // large array of 'z' (0x7A)
        Arguments.of(largeByteArray(10_000, (byte) 0x7A), StandardCharsets.UTF_8, "z".repeat(10_000), "Stress test with a large input to ensure buffer handling does not truncate data."));
    }

    private static Stream<Arguments> advancedSuccessProvider() {
        return Stream.of(// ByteBuffer with position 6, limit 11 → "World"
        Arguments.of(ByteBuffer.wrap(new byte[] { 0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x20, 0x57, 0x6F, 0x72, 0x6C, 0x64 }).position(6).limit(11), StandardCharsets.UTF_8, "World", "ByteBuffer with a non‑zero position and a limited range; decode only the remaining slice."), // Read‑only ByteBuffer → "ABC"
        Arguments.of(ByteBuffer.wrap(new byte[] { 0x41, 0x42, 0x43 }).asReadOnlyBuffer(), StandardCharsets.US_ASCII, "ABC", "Read‑only ByteBuffer should be accepted and decoded correctly."), // Direct ByteBuffer → "xyz"
        Arguments.of(ByteBuffer.allocateDirect(3).put(new byte[] { 0x78, 0x79, 0x7A }).flip(), StandardCharsets.UTF_8, "xyz", "Direct ByteBuffer (off‑heap) containing UTF‑8 bytes for \"xyz\" must be decoded without error."), // StringBuilder → "builder"
        Arguments.of(new StringBuilder("builder"), StandardCharsets.UTF_8, "builder", "A CharSequence implementation other than String (StringBuilder) should be converted to bytes via the Converter and then decoded."), // CharBuffer wrapped in dummy wrapper → "charbuf"
        Arguments.of(new CharBufferWrapper(CharBuffer.wrap("charbuf")), StandardCharsets.UTF_8, "charbuf", "CharBuffer itself (wrapped in a dummy wrapper) should be treated as a CharSequence and correctly converted."), // UTF‑8 two‑byte sequence for 'é' → "é"
        Arguments.of(new byte[] { (byte) 0xC3, (byte) 0xA9 }, StandardCharsets.UTF_8, "é", "Two‑byte UTF‑8 sequence for the character 'é' must be decoded to the proper Unicode character."), // UTF‑16BE encoded "AB" → "AB"
        Arguments.of(new byte[] { 0x00, 0x41, 0x00, 0x42 }, StandardCharsets.UTF_16BE, "AB", "Byte array encoded in UTF‑16BE should be decoded correctly when the Converter’s charset is set to UTF‑16BE."), // Emoji 😀 (four‑byte UTF‑8) → "😀"
        Arguments.of(new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 }, StandardCharsets.UTF_8, "😀", "Multi‑byte UTF‑8 character (emoji) must survive the conversion and appear as a single Unicode code point."), // Invalid US‑ASCII byte 0x80 → replacement character
        Arguments.of(new byte[] { (byte) 0x80 }, StandardCharsets.US_ASCII, "\uFFFD", "When the charset cannot decode a byte (US‑ASCII with 0x80), the decoder replaces it with the Unicode replacement character."));
    }

    /**
     * Verify that a {@link String} source is correctly converted to a {@link CharBuffer}
     * using the Converter's charset (UTF‑8 by default).
     */
    @Test
    void toCharBuffer_fromString_returnsCorrectCharBuffer() {
        // GIVEN
        String source = "Hello, ByteBufferConversions!";
        // WHEN
        CharBuffer result = ByteBufferConversions.toCharBuffer(source, defaultConverter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer must not be null");
        assertEquals(source, result.toString(), "CharBuffer content should match the original string");
    }

    /**
     * Verify that a {@code byte[]} source is correctly converted.
     */
    @Test
    void toCharBuffer_fromByteArray_returnsCorrectCharBuffer() {
        // GIVEN
        String expected = "Byte array conversion";
        byte[] source = expected.getBytes(StandardCharsets.UTF_8);
        // WHEN
        CharBuffer result = ByteBufferConversions.toCharBuffer(source, defaultConverter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.toString(), "CharBuffer should decode the byte array using UTF‑8");
    }

    /**
     * Verify that a {@link ByteBuffer} source is correctly converted.
     */
    @Test
    void toCharBuffer_fromByteBuffer_returnsCorrectCharBuffer() {
        // GIVEN
        String expected = "ByteBuffer source";
        ByteBuffer source = ByteBuffer.wrap(expected.getBytes(StandardCharsets.UTF_8));
        // WHEN
        CharBuffer result = ByteBufferConversions.toCharBuffer(source, defaultConverter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.toString(), "CharBuffer should decode the ByteBuffer using UTF‑8");
    }

    /**
     * Verify that a {@code null} source yields an empty {@link CharBuffer}
     * (the underlying {@code toByteBuffer} implementation returns an empty buffer for null).
     */
    @Test
    void toCharBuffer_fromNull_returnsEmptyCharBuffer() {
        // GIVEN
        Object source = null;
        // WHEN
        CharBuffer result = ByteBufferConversions.toCharBuffer(source, converter);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.length(), "Converting null should produce an empty CharBuffer");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("typicalSuccessProvider")
    void toCharBuffer_typicalSuccess(Object from, Charset charset, String expected, String _description) {
        // GIVEN
        Converter converter = mockConverter(charset);
        // WHEN
        CharBuffer result = ByteBufferConversions.toCharBuffer(from, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer must not be null");
        assertEquals(expected, result.toString(), "Decoded CharBuffer content does not match expected value");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("advancedSuccessProvider")
    void toCharBuffer_advancedSuccess(Object from, Charset charset, String expected, String _description) {
        // GIVEN
        Converter converter = mockConverter(charset);
        // WHEN
        CharBuffer result = ByteBufferConversions.toCharBuffer(from, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer must not be null");
        assertEquals(expected, result.toString(), "Decoded CharBuffer does not match expected value");
    }

    @Test
    void toCharBuffer_nullSource_throwsNPE() {
        // GIVEN
        Object source = null;
        Converter converter = mockConverter(StandardCharsets.UTF_8);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toCharBuffer(source, converter), "Method should reject a null source object.");
    }

    @Test
    void toCharBuffer_nullConverter_throwsNPE() {
        // GIVEN
        Object source = "test";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toCharBuffer(source, null), "A null Converter cannot provide charset options; expect NPE.");
    }

    @Test
    void toCharBuffer_unsupportedSource_throwsIllegalArgumentException() {
        // GIVEN
        // not a supported type
        Object source = new Object();
        Converter converter = mockConverter(StandardCharsets.UTF_8);
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> ByteBufferConversions.toCharBuffer(source, converter), "Unsupported source type should cause an IllegalArgumentException.");
    }

    @Test
    void toCharBuffer_converterWithNullCharset_throwsNPE() {
        // GIVEN
        byte[] source = new byte[] { 0x61, 0x62, 0x63 };
        // Mock a Converter whose Options return null charset
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getCharset()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toCharBuffer(source, converter), "Converter with a null charset should trigger NPE when decode() is called.");
    }

    @Test
    void toCharBuffer_converterReturnsNullOptions_throwsNPE() {
        // GIVEN
        byte[] source = new byte[] { 0x61, 0x62, 0x63 };
        Converter converter = mock(Converter.class);
        // options are null
        when(converter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toCharBuffer(source, converter), "A Converter that returns null Options should cause NPE when the method attempts to call getCharset().");
    }

    @Test
    void toCharBuffer_mismatchedByteOrderAndCharset_throwsIllegalArgumentException() {
        // GIVEN
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 0x41, 0x42, 0x43 }).order(// non‑native order
        ByteOrder.LITTLE_ENDIAN);
        // expects 2‑byte chars
        Converter converter = mockConverter(StandardCharsets.UTF_16LE);
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> ByteBufferConversions.toCharBuffer(buffer, converter), "Decoder should reject mismatched byte order/charset combination.");
    }
}
