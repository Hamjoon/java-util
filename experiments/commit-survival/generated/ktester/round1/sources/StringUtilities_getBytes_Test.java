package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.UnsupportedEncodingException;
import java.util.stream.Stream;

class StringUtilities_getBytes_Test {

    /* --------------------------------------------------------------
     *  Parameterized test for the null‑input‑and‑null‑encoding combination
     * -------------------------------------------------------------- */
    private static Stream<Arguments> nullInputScenarios() {
        return Stream.of(Arguments.of(null, "UTF-8"), Arguments.of(null, null));
    }

    @Test
    void getBytes_returnsCorrectBytesForValidEncoding() {
        // given
        String input = "Hello, 世界";
        String encoding = "UTF-8";
        // when
        byte[] result = StringUtilities.getBytes(input, encoding);
        // then
        assertNotNull(result, "Result should not be null for non‑null input");
        assertArrayEquals(input.getBytes(encoding), result, "Byte array should match the standard Java conversion");
    }

    @Test
    void getBytes_returnsNullWhenInputIsNull() {
        // given
        String input = null;
        String encoding = "UTF-8";
        // when
        byte[] result = StringUtilities.getBytes(input, encoding);
        // then
        assertNull(result, "Result should be null when the input string is null");
    }

    @Test
    void getBytes_throwsIllegalArgumentExceptionForUnsupportedEncoding() {
        // given
        String input = "test";
        String badEncoding = "UNSUPPORTED_ENCODING_123";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringUtilities.getBytes(input, badEncoding), "Unsupported encoding should cause IllegalArgumentException");
        assertTrue(ex.getMessage().contains(badEncoding), "Exception message should contain the problematic encoding");
    }

    @Test
    void getBytes_emptyStringReturnsEmptyArray() throws Exception {
        // given
        String input = "";
        String encoding = "UTF-8";
        // when
        byte[] result = StringUtilities.getBytes(input, encoding);
        // then
        assertNotNull(result, "Result should not be null for empty string");
        assertEquals(0, result.length, "Empty string should produce zero‑length byte array");
    }

    @Test
    void getBytes_iso88591ReplacesUnmappableCharsWithQuestionMark() throws Exception {
        // given: string contains characters not representable in ISO‑8859‑1
        String input = "Hello, 世界";
        String encoding = "ISO-8859-1";
        // when
        byte[] result = StringUtilities.getBytes(input, encoding);
        // then
        // Java's getBytes will replace each unmappable char with '?'
        assertArrayEquals(input.getBytes(encoding), result, "Method must delegate to String.getBytes and return the same replacement bytes");
    }

    @Test
    void getBytes_emojiUtf8ProducesFourBytes() throws Exception {
        // given: a single emoji (surrogate pair) encoded in UTF‑8 yields 4 bytes
        String input = "😀";
        String encoding = "UTF-8";
        // when
        byte[] result = StringUtilities.getBytes(input, encoding);
        // then
        assertNotNull(result);
        assertEquals(4, result.length, "Emoji should be encoded as 4 bytes in UTF‑8");
        assertArrayEquals(input.getBytes(encoding), result);
    }

    @Test
    void getBytes_emptyStringUtf16BEReturnsEmptyArray() throws Exception {
        // given
        String input = "";
        String encoding = "UTF-16BE";
        // when
        byte[] result = StringUtilities.getBytes(input, encoding);
        // then
        assertNotNull(result);
        assertEquals(0, result.length, "Empty string with UTF‑16BE should still be zero length");
    }

    @Test
    void getBytes_nullEncodingPropagatesNullPointerException() {
        // given
        String input = "test";
        String encoding = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringUtilities.getBytes(input, encoding), "Passing null encoding should result in NullPointerException");
    }

    @Test
    void getBytes_bothParametersNullReturnsNull() {
        // given
        String input = null;
        String encoding = null;
        // when
        byte[] result = StringUtilities.getBytes(input, encoding);
        // then
        assertNull(result, "When both parameters are null the method should return null");
    }

    @Test
    void getBytes_emptyEncodingStringThrowsIllegalArgumentException() {
        // given
        String input = "test";
        String encoding = "";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringUtilities.getBytes(input, encoding), "Empty charset name should cause IllegalArgumentException");
        assertTrue(ex.getMessage().contains("Encoding () is not supported by your JVM"), "Exception message must contain the empty encoding placeholder");
    }

    @Test
    void getBytes_usAsciiReplacesNonAsciiWithQuestionMark() throws Exception {
        // given: string contains a non‑ASCII character (é)
        String input = "café";
        String encoding = "US-ASCII";
        // when
        byte[] result = StringUtilities.getBytes(input, encoding);
        // then
        // Java replaces 'é' with '?' (0x3F) in US‑ASCII
        assertArrayEquals(input.getBytes(encoding), result, "Method must return the same bytes as String.getBytes for US‑ASCII");
    }

    @ParameterizedTest
    @MethodSource("nullInputScenarios")
    void getBytes_nullStringAlwaysReturnsNull(String input, String encoding) {
        // when
        byte[] result = StringUtilities.getBytes(input, encoding);
        // then
        assertNull(result, "Result should be null whenever the string argument is null");
    }
}
