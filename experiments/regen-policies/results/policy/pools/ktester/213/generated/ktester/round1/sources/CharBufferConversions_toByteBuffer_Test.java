package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Method;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toByteBuffer_Test {

    @Mock
    private Converter // mock of the Converter used by the method
    converter;

    @Mock
    private ConverterOptions // mock of ConverterOptions returned by converter
    options;

    private static final Charset TEST_CHARSET = StandardCharsets.UTF_8;

    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private static final Charset UTF16BE = StandardCharsets.UTF_16BE;

    private static final Charset UTF16LE = StandardCharsets.UTF_16LE;

    /* -------------------------------------------------------------
     *  Simple holder class for parameterized test data
     * ------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final Object source;

        final Charset charset;

        final byte[] expected;

        ArgumentsHolder(Object source, Charset charset, byte[] expected) {
            this.source = source;
            this.charset = charset;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "source=" + source + ", charset=" + charset.name();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy:
        // converter.getOptions() -> options
        // options.getCharset()  -> TEST_CHARSET
        when(converter.getOptions()).thenReturn(options);
        when(options.getCharset()).thenReturn(TEST_CHARSET);
        // Default behaviour: UTF‑8 charset
        when(converter.getOptions()).thenReturn(options);
        when(options.getCharset()).thenReturn(UTF8);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions after each test to keep tests independent.
        reset(converter, options);
        reset(converter, options);
    }

    /**
     * Creates a fresh mock Converter that returns the supplied charset.
     */
    private static Converter mockConverterWithCharset(Charset charset) {
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getCharset()).thenReturn(charset);
        return mockConverter;
    }

    /**
     * Reads all remaining bytes from a ByteBuffer into a new byte array.
     */
    private static byte[] readAll(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    /**
     * Returns a hex‑string representation of a byte array, useful for debugging.
     */
    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString().trim();
    }

    /**
     * Data for standard type conversion tests (String, CharBuffer, char[], etc.).
     */
    private static Stream<ArgumentsHolder> standardConversionProvider() {
        return Stream.of(new ArgumentsHolder("hello", UTF8, new byte[] { 0x68, 0x65, 0x6c, 0x6c, 0x6f }), new ArgumentsHolder(CharBuffer.wrap("world"), UTF8, new byte[] { 0x77, 0x6f, 0x72, 0x6c, 0x64 }), new ArgumentsHolder(new char[] { 'a', 'b', 'c' }, UTF8, new byte[] { 0x61, 0x62, 0x63 }), new ArgumentsHolder(new StringBuilder("test"), UTF8, new byte[] { 0x74, 0x65, 0x73, 0x74 }), new ArgumentsHolder(new StringBuffer("buffer"), UTF8, new byte[] { 0x62, 0x75, 0x66, 0x66, 0x65, 0x72 }));
    }

    /**
     * Data for empty / whitespace handling.
     */
    private static Stream<ArgumentsHolder> emptyWhitespaceProvider() {
        return Stream.of(new ArgumentsHolder("", UTF8, new byte[] {}), new ArgumentsHolder("   \t\n", UTF8, new byte[] { 0x20, 0x20, 0x20, 0x09, 0x0a }));
    }

    /**
     * Data for Unicode handling.
     */
    private static Stream<ArgumentsHolder> unicodeProvider() {
        return Stream.of(new ArgumentsHolder("π", UTF8, new byte[] { (byte) 0xcf, (byte) 0x80 }), new ArgumentsHolder("😀", UTF8, new byte[] { (byte) 0xf0, (byte) 0x9f, (byte) 0x98, (byte) 0x80 }), new ArgumentsHolder("漢字", UTF8, new byte[] { (byte) 0xe6, (byte) 0xbc, (byte) 0xa2, (byte) 0xe5, (byte) 0xad, (byte) 0x97 }));
    }

    /**
     * Data for charset variation tests.
     */
    private static Stream<ArgumentsHolder> charsetVariationProvider() {
        return Stream.of(new ArgumentsHolder("hi", UTF16BE, new byte[] { 0x00, 0x68, 0x00, 0x69 }), new ArgumentsHolder("hi", UTF16LE, new byte[] { 0x68, 0x00, 0x69, 0x00 }));
    }

    /**
     * Data for large input handling.
     */
    private static Stream<ArgumentsHolder> largeDataProvider() {
        String repeated = "a".repeat(10_000);
        byte[] expected = repeated.getBytes(UTF8);
        return Stream.of(new ArgumentsHolder(repeated, UTF8, expected));
    }

    /**
     * Verify that a simple {@link String} is correctly converted to a {@link ByteBuffer}
     * containing the UTF‑8 representation of the string.
     */
    @Test
    void toByteBuffer_convertsString_usingUtf8() {
        // GIVEN
        String source = "Hello, world!";
        // WHEN
        ByteBuffer result = CharBufferConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer should not be null");
        // The ByteBuffer returned by Charset.encode is already positioned at the start,
        // but its limit may be greater than the actual data length. We read the remaining
        // bytes to compare with the expected UTF‑8 byte array.
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        byte[] expected = source.getBytes(TEST_CHARSET);
        assertArrayEquals(expected, actual, "ByteBuffer should contain the UTF‑8 bytes of the source string");
    }

    /**
     * Verify that a {@link CharBuffer} source is passed through unchanged
     * (i.e., the resulting bytes correspond to the characters in the buffer).
     */
    @Test
    void toByteBuffer_convertsCharBuffer_usingUtf8() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("CharBufferTest");
        // WHEN
        ByteBuffer result = CharBufferConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result);
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        byte[] expected = "CharBufferTest".getBytes(TEST_CHARSET);
        assertArrayEquals(expected, actual);
    }

    /**
     * Verify that a {@code null} source does not cause an exception and yields an empty ByteBuffer.
     * The exact behaviour of {@code toCharBuffer(null, …)} is to return an empty CharBuffer,
     * which the charset encoder then turns into an empty ByteBuffer.
     */
    @Test
    void toByteBuffer_nullSource_returnsEmptyBuffer() {
        // GIVEN
        Object source = null;
        // WHEN
        ByteBuffer result = CharBufferConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(0, result.remaining(), "ByteBuffer for a null source should be empty");
    }

    @ParameterizedTest(name = "[{index}] Convert {0} using {1}")
    @MethodSource("standardConversionProvider")
    void toByteBuffer_standardConversions(ArgumentsHolder holder) {
        Converter conv = mockConverterWithCharset(holder.charset);
        ByteBuffer result = CharBufferConversions.toByteBuffer(holder.source, conv);
        assertNotNull(result, "Result must not be null");
        assertArrayEquals(holder.expected, readAll(result), () -> "Unexpected bytes for source [" + holder.source + "]; got " + toHex(readAll(result)));
    }

    @ParameterizedTest(name = "[{index}] Empty/whitespace source \"{0}\"")
    @MethodSource("emptyWhitespaceProvider")
    void toByteBuffer_emptyAndWhitespace(ArgumentsHolder holder) {
        Converter conv = mockConverterWithCharset(holder.charset);
        ByteBuffer result = CharBufferConversions.toByteBuffer(holder.source, conv);
        assertNotNull(result);
        assertEquals(holder.expected.length, result.remaining(), "ByteBuffer size should match expected length");
        assertArrayEquals(holder.expected, readAll(result));
    }

    @ParameterizedTest(name = "[{index}] Unicode source \"{0}\"")
    @MethodSource("unicodeProvider")
    void toByteBuffer_unicodeCharacters(ArgumentsHolder holder) {
        Converter conv = mockConverterWithCharset(holder.charset);
        ByteBuffer result = CharBufferConversions.toByteBuffer(holder.source, conv);
        assertNotNull(result);
        assertArrayEquals(holder.expected, readAll(result));
    }

    @ParameterizedTest(name = "[{index}] Charset variation \"{0}\" with {1}")
    @MethodSource("charsetVariationProvider")
    void toByteBuffer_charsetVariations(ArgumentsHolder holder) {
        Converter conv = mockConverterWithCharset(holder.charset);
        ByteBuffer result = CharBufferConversions.toByteBuffer(holder.source, conv);
        assertNotNull(result);
        assertArrayEquals(holder.expected, readAll(result));
    }

    @ParameterizedTest(name = "[{index}] Large data handling")
    @MethodSource("largeDataProvider")
    void toByteBuffer_largeData(ArgumentsHolder holder) {
        Converter conv = mockConverterWithCharset(holder.charset);
        ByteBuffer result = CharBufferConversions.toByteBuffer(holder.source, conv);
        assertNotNull(result);
        assertEquals(holder.expected.length, result.remaining(), "ByteBuffer length should equal the length of the UTF‑8 encoded data");
        assertArrayEquals(holder.expected, readAll(result));
    }

    /**
     * Null source should cause a NullPointerException (as per specification).
     */
    @Test
    void toByteBuffer_nullSource_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteBuffer(null, converter), "Passing null as the source must throw NullPointerException");
    }

    /**
     * Null converter argument must result in NullPointerException.
     */
    @Test
    void toByteBuffer_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteBuffer("test", null), "Passing null as the converter must throw NullPointerException");
    }

    /**
     * Converter returning null options should cause NPE when accessing charset.
     */
    @Test
    void toByteBuffer_nullOptions_throwsNPE() {
        when(converter.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteBuffer("sample", converter), "Null options should lead to NullPointerException");
    }

    /**
     * Converter options returning null charset should cause NPE during encoding.
     */
    @Test
    void toByteBuffer_nullCharset_throwsNPE() {
        when(options.getCharset()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteBuffer("test", converter), "Null charset should cause NullPointerException during encode");
    }

    /**
     * Passing an unsupported source type (Integer) must result in IllegalArgumentException.
     */
    @Test
    void toByteBuffer_unsupportedSource_integer_throwsIAE() {
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toByteBuffer(12345, converter), "Unsupported source type should throw IllegalArgumentException");
    }

    /**
     * Passing an unsupported source type (Boolean) must result in IllegalArgumentException.
     */
    @Test
    void toByteBuffer_unsupportedSource_boolean_throwsIAE() {
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toByteBuffer(true, converter), "Unsupported source type should throw IllegalArgumentException");
    }

    /**
     * Passing an unsupported source type (Double) must result in IllegalArgumentException.
     */
    @Test
    void toByteBuffer_unsupportedSource_double_throwsIAE() {
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toByteBuffer(3.14159, converter), "Unsupported source type should throw IllegalArgumentException");
    }
}
