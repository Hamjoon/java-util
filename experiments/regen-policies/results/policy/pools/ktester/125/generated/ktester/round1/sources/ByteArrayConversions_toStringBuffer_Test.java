package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class ByteArrayConversions_toStringBuffer_Test {

    /**
     * Converter used for all tests – created once because it has no mutable state that
     *  interferes with the static methods we are exercising.
     */
    private static Converter converter;

    /**
     * Converter used for the majority of tests – it has no mutable state.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Default options are sufficient for the simple conversions we need.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Supplies all basic and edge‑case inputs described in the JSON test matrix.
     */
    private static Stream<Arguments> basicAndEdgeCaseProvider() {
        return Stream.of(// ---- basic types -------------------------------------------------
        Arguments.of("null source", null, "null"), Arguments.of("String source", "Hello World", "Hello World"), Arguments.of("byte[] source (ASCII)", new byte[] { 65, 66, 67 }, "ABC"), Arguments.of("char[] source", new char[] { 'x', 'y', 'z' }, "xyz"), Arguments.of("ByteBuffer source", ByteBuffer.wrap(new byte[] { 68, 69, 70 }), "DEF"), Arguments.of("CharBuffer source", CharBuffer.wrap("ghi"), "ghi"), // ---- edge cases -------------------------------------------------
        Arguments.of("empty byte[]", new byte[] {}, ""), Arguments.of("empty char[]", new char[] {}, ""), Arguments.of("empty ByteBuffer (capacity 0)", ByteBuffer.allocate(0), ""), Arguments.of("empty CharBuffer (capacity 0)", CharBuffer.allocate(0), ""), // ByteBuffer with non‑zero position/limit (remaining bytes only)
        Arguments.of("ByteBuffer partial view", ByteBuffer.wrap(new byte[] { 72, 105, 33 }, 1, 2), "i!"), // CharBuffer with position set (remaining chars only)
        Arguments.of("CharBuffer partial view", CharBuffer.wrap("partial").position(2), "rtial"), // Additional supported types
        Arguments.of("StringBuilder source", new StringBuilder("builder"), "builder"), Arguments.of("StringBuffer source", new StringBuffer("buf"), "buf"), Arguments.of("Boolean true source", true, "true"), Arguments.of("Integer source", 12345, "12345"), Arguments.of("Collection source", Arrays.asList(1, 2, 3), "[1, 2, 3]"), // UTF‑8 multibyte character (é)
        Arguments.of("byte[] with UTF‑8 multibyte", new byte[] { (byte) 0xC3, (byte) 0xA9 }, "é"));
    }

    @Test
    void toStringBuffer_returnsStringBufferWithSameContentForStringInput() {
        // GIVEN
        String source = "Hello, World!";
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer must not be null");
        assertEquals(source, result.toString(), "StringBuffer should contain the same characters as the original String");
    }

    @Test
    void toStringBuffer_convertsByteArrayToStringBuffer() {
        // GIVEN
        // UTF‑8 default charset is fine for this test
        byte[] source = "byte‑array".getBytes();
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer must not be null");
        assertEquals(new String(source), result.toString(), "StringBuffer should contain the String representation of the byte array");
    }

    @Test
    void toStringBuffer_handlesNullInputGracefully() {
        // GIVEN
        Object source = null;
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer must not be null even when input is null");
        // The most common contract for a toString‑like utility is to return the literal "null".
        // If the implementation differs, the assertion will highlight the deviation.
        assertEquals("null", result.toString(), "StringBuffer should contain the literal \"null\" when the source object is null");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for the large matrix of supported inputs
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicAndEdgeCaseProvider")
    void toStringBuffer_variousSupportedTypes(String description, Object source, String expectedString) {
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(source, defaultConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer must not be null");
        assertEquals(expectedString, result.toString(), "Unexpected StringBuffer content for case: " + description);
    }

    /* -------------------------------------------------------------
     *  Converter that throws an exception
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_converterThrowsException_propagates() {
        // GIVEN: a mock Converter that always throws RuntimeException
        Converter throwingConverter = mock(Converter.class);
        when(throwingConverter.convert(any(), any())).thenThrow(new RuntimeException("conversion failure"));
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ByteArrayConversions.toStringBuffer("any value", throwingConverter));
        assertEquals("conversion failure", ex.getMessage(), "Exception message should be propagated unchanged");
    }

    /* -------------------------------------------------------------
     *  Converter that returns null (causing NPE in StringBuffer ctor)
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_converterReturnsNull_resultsInNPE() {
        // GIVEN: a mock Converter that returns null for any conversion request
        Converter nullReturningConverter = mock(Converter.class);
        when(nullReturningConverter.convert(any(), any())).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toStringBuffer("null‑producing", nullReturningConverter), "StringBuffer constructor does not accept a null argument");
    }

    /* -------------------------------------------------------------
     *  Unsupported source type without a custom converter
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_unsupportedSourceWithoutConverter_throwsIllegalArgumentException() {
        // plain Object is not a known convertible type
        Object unsupported = new Object();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ByteArrayConversions.toStringBuffer(unsupported, null));
        assertTrue(ex.getMessage().toLowerCase().contains("unsupported"), "Exception message should indicate unsupported source type");
    }

    /* -------------------------------------------------------------
     *  Invalid UTF‑8 byte array handling
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_invalidUtf8ByteArray_throwsException() {
        // 0xC3 must be followed by 0x80‑0xBF
        byte[] malformed = new byte[] { (byte) 0xC3, (byte) 0x28 };
        // The implementation may throw IllegalArgumentException or a CharacterCodingException wrapped in a RuntimeException.
        // We accept any RuntimeException that indicates a decoding problem.
        Exception ex = assertThrows(RuntimeException.class, () -> ByteArrayConversions.toStringBuffer(malformed, defaultConverter));
        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("utf") || msg.contains("character") || msg.contains("illegal"), "Exception should be related to malformed UTF‑8 data");
    }
}
