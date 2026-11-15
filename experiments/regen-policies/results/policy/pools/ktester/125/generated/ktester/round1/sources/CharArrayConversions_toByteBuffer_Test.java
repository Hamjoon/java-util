package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.*;
import java.nio.charset.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toByteBuffer_Test {

    private static Converter converter;

    private static Charset charset;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default options – they provide UTF‑8 as the charset.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // should be UTF‑8
        charset = options.getCharset();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the pattern tidy.
        converter = null;
    }

    /**
     * Creates a mock {@link Converter} whose {@link ConverterOptions#getCharset()}
     * returns the supplied {@code charset}.  The mock also returns a non‑null
     * {@link ConverterOptions} instance.
     */
    private static Converter mockConverterWithCharset(Charset charset) {
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getCharset()).thenReturn(charset);
        return mockConverter;
    }

    /**
     * Creates a mock {@link Converter} whose {@code getOptions()} returns {@code null}.
     * Used to provoke a {@link NullPointerException} when the code attempts
     * {@code converter.getOptions().getCharset()}.
     */
    private static Converter mockConverterWithNullOptions() {
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(null);
        return mockConverter;
    }

    /**
     * Creates a mock {@link Converter} whose {@code getOptions()} returns a mock
     * options object whose {@code getCharset()} returns {@code null}.
     */
    private static Converter mockConverterWithNullCharset() {
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getCharset()).thenReturn(null);
        return mockConverter;
    }

    private static Stream<Arguments> provideStandardUtf8Cases() {
        Charset utf8 = StandardCharsets.UTF_8;
        return Stream.of(Arguments.of("char[] {'J','a','v','a'}", new char[] { 'J', 'a', 'v', 'a' }, "Java".getBytes(utf8)), Arguments.of("String \"Hello\"", "Hello", "Hello".getBytes(utf8)), Arguments.of("CharBuffer.wrap(\"ß\")", CharBuffer.wrap("ß"), "ß".getBytes(utf8)), Arguments.of("StringBuilder \"Builder\"", new StringBuilder("Builder"), "Builder".getBytes(utf8)), Arguments.of("StringBuffer \"Buffer\"", new StringBuffer("Buffer"), "Buffer".getBytes(utf8)), Arguments.of("char[] {'\\uD83D','\\uDE00'}  (😀 emoji)", new char[] { '\uD83D', '\uDE00' }, new String("\uD83D\uDE00").getBytes(utf8)), Arguments.of("empty char[]", new char[] {}, new byte[0]), Arguments.of("empty String", "", new byte[0]));
    }

    private static Stream<Arguments> provideUnsupportedSources() {
        return Stream.of(Arguments.of("plain Object", new Object()), Arguments.of("Integer 123", Integer.valueOf(123)), Arguments.of("byte[] {0x41,0x42}", new byte[] { 0x41, 0x42 }));
    }

    /**
     * Verify that a {@code char[]} is correctly converted to a {@link ByteBuffer}
     * using the charset defined in {@link ConverterOptions}.
     */
    @Test
    void toByteBuffer_convertsCharArray_usingConfiguredCharset() {
        // GIVEN
        char[] source = new char[] { 'h', 'e', 'l', 'l', 'o' };
        // WHEN
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer must not be null");
        // The buffer should contain the UTF‑8 bytes of the source string.
        byte[] expected = "hello".getBytes(charset);
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(expected, actual, "ByteBuffer content does not match expected charset encoding");
    }

    /**
     * Verify that a {@link String} is correctly converted to a {@link ByteBuffer}.
     */
    @Test
    void toByteBuffer_convertsString_usingConfiguredCharset() {
        // GIVEN
        String source = "JØØL";
        // WHEN
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result);
        byte[] expected = source.getBytes(charset);
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(expected, actual);
    }

    /**
     * Verify that a {@link CharBuffer} is correctly converted to a {@link ByteBuffer}.
     */
    @Test
    void toByteBuffer_convertsCharBuffer_usingConfiguredCharset() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("buffer-test");
        // WHEN
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result);
        byte[] expected = "buffer-test".getBytes(charset);
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(expected, actual);
    }

    /**
     * Verify that passing {@code null} returns {@code null} (the underlying
     * {@link Converter} returns {@code null} for object targets when the source is null).
     */
    @Test
    void toByteBuffer_nullInput_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converter);
        // THEN
        assertNull(result, "Converting a null source should yield a null ByteBuffer");
    }

    /* -------------------------------------------------------------
     *  Standard successful conversions (default UTF‑8)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideStandardUtf8Cases")
    void toByteBuffer_standardUtf8Conversions(String description, Object source, byte[] expectedBytes) {
        // WHEN
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result, "Result must not be null for " + description);
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(expectedBytes, actual, "ByteBuffer content mismatch for " + description);
    }

    /* -------------------------------------------------------------
     *  Charset‑specific conversions
     * ------------------------------------------------------------- */
    @Test
    void toByteBuffer_utf16beString() {
        Converter utf16beConverter = mockConverterWithCharset(StandardCharsets.UTF_16BE);
        String source = "AB";
        ByteBuffer buf = CharArrayConversions.toByteBuffer(source, utf16beConverter);
        assertNotNull(buf);
        byte[] expected = source.getBytes(StandardCharsets.UTF_16BE);
        byte[] actual = new byte[buf.remaining()];
        buf.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    void toByteBuffer_utf16leString() {
        Converter utf16leConverter = mockConverterWithCharset(StandardCharsets.UTF_16LE);
        String source = "AB";
        ByteBuffer buf = CharArrayConversions.toByteBuffer(source, utf16leConverter);
        assertNotNull(buf);
        byte[] expected = source.getBytes(StandardCharsets.UTF_16LE);
        byte[] actual = new byte[buf.remaining()];
        buf.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    void toByteBuffer_iso88591EuroSign() {
        Charset iso88591 = Charset.forName("ISO-8859-1");
        Converter isoConverter = mockConverterWithCharset(iso88591);
        // Euro sign
        char[] source = new char[] { '\u20AC' };
        ByteBuffer buf = CharArrayConversions.toByteBuffer(source, isoConverter);
        assertNotNull(buf);
        // 172 decimal
        byte[] expected = new byte[] { (byte) 0xAC };
        byte[] actual = new byte[buf.remaining()];
        buf.get(actual);
        assertArrayEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  Null / exceptional inputs
     * ------------------------------------------------------------- */
    @Test
    void toByteBuffer_nullSource_returnsNull() {
        // According to the original test suite, a null source yields null.
        ByteBuffer result = CharArrayConversions.toByteBuffer(null, converter);
        assertNull(result, "Converting a null source should yield a null ByteBuffer");
    }

    @Test
    void toByteBuffer_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteBuffer("test", null), "Passing a null Converter must cause NPE");
    }

    @Test
    void toByteBuffer_converterWithNullOptions_throwsNPE() {
        Converter badConverter = mockConverterWithNullOptions();
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteBuffer("test", badConverter), "Converter with null Options should cause NPE");
    }

    @Test
    void toByteBuffer_converterWithNullCharset_throwsNPE() {
        Converter badConverter = mockConverterWithNullCharset();
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteBuffer("test", badConverter), "Converter whose Options return null Charset should cause NPE");
    }

    @Test
    void toByteBuffer_nullSourceWithNonNullConverter_throwsNPE_fromToCharBuffer() {
        // The JSON spec expects an NPE when source is null and the converter is non‑null.
        // The implementation of toCharBuffer likely throws NPE for a null argument.
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteBuffer(null, converter), "Null source should trigger NPE inside toCharBuffer");
    }

    @Test
    void toByteBuffer_unmappableCharacter_US_ASCII_throwsUnmappableCharacterException() {
        Converter asciiConverter = mockConverterWithCharset(StandardCharsets.US_ASCII);
        // copyright sign not representable in US‑ASCII
        String source = "©";
        assertThrows(UnmappableCharacterException.class, () -> CharArrayConversions.toByteBuffer(source, asciiConverter), "Encoding non‑ASCII char with US‑ASCII should throw UnmappableCharacterException");
    }

    /* -------------------------------------------------------------
     *  Unsupported source types – should result in IllegalArgumentException
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: unsupported source {0}")
    @MethodSource("provideUnsupportedSources")
    void toByteBuffer_unsupportedSource_throwsIllegalArgumentException(String description, Object source) {
        assertThrows(IllegalArgumentException.class, () -> CharArrayConversions.toByteBuffer(source, converter), "Unsupported source " + description + " must cause IllegalArgumentException");
    }
}
