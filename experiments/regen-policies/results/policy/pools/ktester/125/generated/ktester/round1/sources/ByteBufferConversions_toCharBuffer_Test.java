package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.Charset;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toCharBuffer_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Default Converter uses UTF‑8 charset – suitable for the majority of cases
        ConverterOptions options = new ConverterOptions();
        options.setCharset(StandardCharsets.UTF_8);
        converter = new Converter(options);
    }

    /**
     * Supplies the data for {@link #toCharBuffer_standardConversions(Object, Charset, String)}.
     */
    private static Stream<Arguments> standardConversionProvider() {
        return Stream.of(// byte[] → "abc" (UTF‑8)
        Arguments.of(new byte[] { 97, 98, 99 }, StandardCharsets.UTF_8, "abc"), // String → same string (UTF‑8)
        Arguments.of("hello", StandardCharsets.UTF_8, "hello"), // ByteBuffer containing UTF‑8 bytes for the Euro sign → "€"
        Arguments.of(ByteBuffer.wrap(new byte[] { (byte) 0xE2, (byte) 0x82, (byte) 0xAC }), StandardCharsets.UTF_8, "€"), // byte[] encoded with UTF‑16BE → "test"
        Arguments.of("test".getBytes(StandardCharsets.UTF_16BE), StandardCharsets.UTF_16BE, "test"));
    }

    @Test
    void toCharBuffer_convertsStringCorrectly() {
        // given
        String source = "Hello, World!";
        // when
        CharBuffer result = ByteBufferConversions.toCharBuffer(source, converter);
        // then
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals(source, result.toString(), "CharBuffer content should match the original string");
    }

    @Test
    void toCharBuffer_convertsByteArrayCorrectly() {
        // given
        byte[] source = "ByteArray".getBytes(StandardCharsets.UTF_8);
        // when
        CharBuffer result = ByteBufferConversions.toCharBuffer(source, converter);
        // then
        assertNotNull(result);
        assertEquals("ByteArray", result.toString());
    }

    @Test
    void toCharBuffer_convertsByteBufferCorrectly() {
        // given
        ByteBuffer source = ByteBuffer.wrap("ByteBuffer".getBytes(StandardCharsets.UTF_8));
        // when
        CharBuffer result = ByteBufferConversions.toCharBuffer(source, converter);
        // then
        assertNotNull(result);
        assertEquals("ByteBuffer", result.toString());
    }

    @Test
    void toCharBuffer_withNullInputReturnsEmptyBuffer() {
        // given
        Object source = null;
        // when
        CharBuffer result = ByteBufferConversions.toCharBuffer(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0, result.length(), "A null source should produce an empty CharBuffer");
    }

    /* -------------------------------------------------------------
     *  Standard conversion scenarios (parameterized)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => from={0}, charset={1}, expected=''{2}''")
    @MethodSource("standardConversionProvider")
    void toCharBuffer_standardConversions(Object from, Charset charset, String expected) {
        // given – a Converter configured with the required charset
        ConverterOptions options = new ConverterOptions();
        options.setCharset(charset);
        Converter conv = new Converter(options);
        // when
        CharBuffer result = ByteBufferConversions.toCharBuffer(from, conv);
        // then
        assertNotNull(result, "Resulting CharBuffer must not be null");
        assertEquals(expected, result.toString(), () -> "Decoded CharBuffer content should match expected string");
    }

    /* -------------------------------------------------------------
     *  Null handling
     * ------------------------------------------------------------- */
    @Test
    void toCharBuffer_nullSource_throwsNullPointerException() {
        // given a null source object
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toCharBuffer(source, converter), "Passing a null source should raise NullPointerException");
    }

    @Test
    void toCharBuffer_nullConverter_throwsNullPointerException() {
        // given a valid source but a null Converter
        String source = "some text";
        // when / then
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toCharBuffer(source, null), "Passing a null Converter should raise NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Unsupported source types
     * ------------------------------------------------------------- */
    @Test
    void toCharBuffer_unsupportedInteger_throwsIllegalArgumentException() {
        Integer source = Integer.valueOf(123);
        assertThrows(IllegalArgumentException.class, () -> ByteBufferConversions.toCharBuffer(source, converter), "Unsupported source type Integer should cause IllegalArgumentException");
    }

    @Test
    void toCharBuffer_unsupportedObject_throwsIllegalArgumentException() {
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> ByteBufferConversions.toCharBuffer(source, converter), "Unsupported source type Object should cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     *  Converter option edge cases (using Mockito)
     * ------------------------------------------------------------- */
    @Test
    void toCharBuffer_converterWithNullOptions_throwsNullPointerException(@Mock Converter mockConverter) {
        // Mock a Converter whose getOptions() returns null
        when(mockConverter.getOptions()).thenReturn(null);
        byte[] source = "test".getBytes(StandardCharsets.UTF_8);
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toCharBuffer(source, mockConverter), "Converter with null options should raise NullPointerException");
    }

    @Test
    void toCharBuffer_converterWithNullCharset_throwsNullPointerException(@Mock Converter mockConverter) {
        // Options exist but charset is null
        ConverterOptions opts = new ConverterOptions();
        // explicitly null charset
        opts.setCharset(null);
        when(mockConverter.getOptions()).thenReturn(opts);
        byte[] source = "test".getBytes(StandardCharsets.UTF_8);
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toCharBuffer(source, mockConverter), "Converter whose options contain a null charset should raise NullPointerException");
    }
}
