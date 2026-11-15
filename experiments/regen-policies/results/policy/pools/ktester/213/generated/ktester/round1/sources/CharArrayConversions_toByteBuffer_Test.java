package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toByteBuffer_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default options – the default charset is UTF‑8.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    /**
     * Convert a {@link ByteBuffer} to a byte array (consumes the buffer).
     */
    private static byte[] toByteArray(ByteBuffer buffer) {
        byte[] arr = new byte[buffer.remaining()];
        buffer.get(arr);
        return arr;
    }

    /**
     * Convert a hex string (e.g. "68 65 6C") to a byte array.
     */
    private static byte[] hexToBytes(String hex) {
        String[] parts = hex.trim().split("\\s+");
        byte[] bytes = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            bytes[i] = (byte) Integer.parseInt(parts[i], 16);
        }
        return bytes;
    }

    /**
     * Create a {@link Converter} mock that returns the supplied {@link Charset}.
     */
    private static Converter mockConverterWithCharset(Charset charset) {
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getCharset()).thenReturn(charset);
        return converter;
    }

    /**
     * Normal conversions – various source types and charsets.
     */
    private static Stream<Arguments> normalConversionsProvider() {
        return Stream.of(// String → UTF‑8
        Arguments.of("hello", Charset.forName("UTF-8"), hexToBytes("68 65 6C 6C 6F"), "Convert a plain String using UTF‑8 charset."), // char[] → UTF‑16BE
        Arguments.of(new char[] { 'a', 'b', 'c' }, Charset.forName("UTF-16BE"), hexToBytes("00 61 00 62 00 63"), "Convert a char[] using UTF‑16BE charset."), // CharBuffer → US‑ASCII
        Arguments.of(CharBuffer.wrap("test"), Charset.forName("US-ASCII"), hexToBytes("74 65 73 74"), "Pass an existing CharBuffer; encode with US‑ASCII."), // Empty String → UTF‑8
        Arguments.of("", Charset.forName("UTF-8"), new byte[0], "Convert an empty String; expect empty ByteBuffer."), // Empty char[] → UTF‑8
        Arguments.of(new char[0], Charset.forName("UTF-8"), new byte[0], "Convert an empty char array; expect empty ByteBuffer."));
    }

    /**
     * Unicode and surrogate‑pair handling.
     */
    private static Stream<Arguments> unicodeProvider() {
        return Stream.of(// Emoji (U+1F600) → UTF‑8
        Arguments.of("😀", Charset.forName("UTF-8"), hexToBytes("F0 9F 98 80"), "Emoji encoded with UTF‑8."), // Emoji → UTF‑16LE
        Arguments.of("😀", Charset.forName("UTF-16LE"), hexToBytes("3D D8 00 DE"), "Emoji encoded with UTF‑16LE."));
    }

    /**
     * Large input handling.
     */
    private static Stream<Arguments> largeInputProvider() {
        StringBuilder sb = new StringBuilder(10000);
        for (int i = 0; i < 10000; i++) sb.append('x');
        byte[] expected = new byte[10000];
        // ASCII 'x'
        for (int i = 0; i < expected.length; i++) expected[i] = 0x78;
        return Stream.of(Arguments.of(sb.toString(), Charset.forName("US-ASCII"), expected, "10 000‑character ASCII string."));
    }

    /**
     * Other supported source types.
     */
    private static Stream<Arguments> otherSourcesProvider() {
        // Custom CharSequence implementation
        CharSequence customSeq = new CharSequence() {

            private final String data = "seq";

            @Override
            public int length() {
                return data.length();
            }

            @Override
            public char charAt(int index) {
                return data.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return data.subSequence(start, end);
            }

            @Override
            public String toString() {
                return data;
            }
        };
        return Stream.of(// StringBuilder → UTF‑8
        Arguments.of(new StringBuilder("builder"), Charset.forName("UTF-8"), "builder".getBytes(Charset.forName("UTF-8")), "StringBuilder conversion."), // StringBuffer → UTF‑8
        Arguments.of(new StringBuffer("buffer"), Charset.forName("UTF-8"), "buffer".getBytes(Charset.forName("UTF-8")), "StringBuffer conversion."), // Custom CharSequence → UTF‑8
        Arguments.of(customSeq, Charset.forName("UTF-8"), "seq".getBytes(Charset.forName("UTF-8")), "Custom CharSequence conversion."));
    }

    @Test
    void toByteBuffer_encodesCharArrayUsingConverterCharset() {
        // GIVEN
        // includes ASCII and non‑ASCII characters
        char[] source = "Hello, 世界".toCharArray();
        // default UTF‑8
        Charset charset = converter.getOptions().getCharset();
        // WHEN
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer should not be null");
        // Verify that the bytes represent the original characters when decoded with the same charset.
        CharBuffer decoded = charset.decode(result);
        assertEquals(CharBuffer.wrap(source), decoded, "Decoded characters must match the original source");
    }

    @Test
    void toByteBuffer_encodesStringUsingConverterCharset() {
        // GIVEN
        String source = "JUnit Test 🚀";
        Charset charset = converter.getOptions().getCharset();
        // WHEN
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result);
        CharBuffer decoded = charset.decode(result);
        assertEquals(source, decoded.toString());
    }

    @Test
    void toByteBuffer_encodesCharBufferDirectly() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("Direct CharBuffer".toCharArray());
        Charset charset = converter.getOptions().getCharset();
        // WHEN
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result);
        CharBuffer decoded = charset.decode(result);
        assertEquals(source, decoded);
    }

    @Test
    void toByteBuffer_returnsEmptyBufferForEmptyInput() {
        // GIVEN
        char[] empty = new char[0];
        Charset charset = converter.getOptions().getCharset();
        // WHEN
        ByteBuffer result = CharArrayConversions.toByteBuffer(empty, converter);
        // THEN
        assertNotNull(result);
        assertEquals(0, result.remaining(), "ByteBuffer should be empty for empty input");
        // Decoding an empty buffer should yield an empty CharBuffer
        CharBuffer decoded = charset.decode(result);
        assertEquals(0, decoded.length());
    }

    @Test
    void toByteBuffer_handlesNullInputGracefully() {
        // GIVEN
        // The contract of Converter is to return null for object‑type targets when source is null.
        // CharArrayConversions delegates to ConverterOptions charset.encode(...), which will
        // throw a NullPointerException if we pass a null CharBuffer. Therefore we expect
        // a NPE for a null source.
        // This test documents the current behaviour.
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteBuffer(null, converter));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalConversionsProvider")
    void toByteBuffer_normalConversions(Object source, Charset charset, byte[] expectedBytes, String description) {
        Converter converter = mockConverterWithCharset(charset);
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converter);
        assertNotNull(result, "Result must not be null");
        byte[] actual = toByteArray(result);
        assertArrayEquals(expectedBytes, actual, "Encoded bytes differ for: " + description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("unicodeProvider")
    void toByteBuffer_unicodeHandling(Object source, Charset charset, byte[] expectedBytes, String description) {
        Converter converter = mockConverterWithCharset(charset);
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converter);
        assertNotNull(result, "Result must not be null");
        byte[] actual = toByteArray(result);
        assertArrayEquals(expectedBytes, actual, "Unicode encoding mismatch: " + description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("largeInputProvider")
    void toByteBuffer_largeInput(Object source, Charset charset, byte[] expectedBytes, String description) {
        Converter converter = mockConverterWithCharset(charset);
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converter);
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedBytes.length, result.remaining(), "ByteBuffer length should match expected for large input.");
        byte[] actual = toByteArray(result);
        assertArrayEquals(expectedBytes, actual, "Large input bytes differ.");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("otherSourcesProvider")
    void toByteBuffer_otherSupportedSources(Object source, Charset charset, byte[] expectedBytes, String description) {
        Converter converter = mockConverterWithCharset(charset);
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converter);
        assertNotNull(result, "Result must not be null");
        byte[] actual = toByteArray(result);
        assertArrayEquals(expectedBytes, actual, "Conversion failed for: " + description);
    }

    @Test
    void toByteBuffer_nullSource_throwsNPE() {
        Converter converter = mockConverterWithCharset(Charset.forName("UTF-8"));
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteBuffer(null, converter), "Null source should trigger NullPointerException");
    }

    @Test
    void toByteBuffer_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteBuffer("test", null), "Null converter should trigger NullPointerException");
    }

    @Test
    void toByteBuffer_converterReturnsNullOptions_throwsNPE() {
        Converter converter = mock(Converter.class);
        // simulate missing options
        when(converter.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteBuffer("sample", converter), "Converter with null options should cause NPE");
    }

    @Test
    void toByteBuffer_optionsWithNullCharset_throwsNPE() {
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        // charset missing
        when(options.getCharset()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteBuffer("sample", converter), "Null charset in options should cause NPE");
    }

    @Test
    void toByteBuffer_unsupportedSourceType_throwsIAE() {
        Converter converter = mockConverterWithCharset(Charset.forName("UTF-8"));
        // not a CharSequence, char[], nor CharBuffer
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> CharArrayConversions.toByteBuffer(unsupported, converter), "Unsupported source type should raise IllegalArgumentException");
    }
}
