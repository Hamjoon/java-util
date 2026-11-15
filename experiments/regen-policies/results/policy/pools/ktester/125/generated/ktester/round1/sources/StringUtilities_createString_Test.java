package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.UnsupportedEncodingException;
import java.util.stream.Stream;

class StringUtilities_createString_Test {

    private static Stream<Arguments> nullBytesProvider() {
        return Stream.of(// bytes == null → result null
        Arguments.of(null, "UTF-8", null), // empty byte array → empty string
        Arguments.of(new byte[0], "UTF-8", ""));
    }

    private static Stream<Arguments> unsupportedEncodingProvider() {
        return Stream.of(Arguments.of(new byte[] { 72, 101, 108, 108, 111 }, "UNSUPPORTED_ENCODING"));
    }

    private static Stream<Arguments> nullEncodingProvider() {
        return Stream.of(Arguments.of(new byte[] { 72, 101, 108, 108, 111 }, null));
    }

    @Test
    void createString_whenBytesIsNull_returnsNull() {
        String result = StringUtilities.createString(null, "UTF-8");
        assertNull(result, "Expected null when input byte array is null");
    }

    @Test
    void createString_withValidEncoding_returnsExpectedString() {
        // platform default charset, safe for ASCII
        byte[] bytes = "Hello, world!".getBytes();
        String result = StringUtilities.createString(bytes, "UTF-8");
        assertEquals("Hello, world!", result);
    }

    @Test
    void createString_withDifferentEncoding_respectsEncoding() throws Exception {
        // The string contains a character that differs between UTF-8 and ISO‑8859‑1
        String original = "café";
        byte[] utf8Bytes = original.getBytes("UTF-8");
        byte[] isoBytes = original.getBytes("ISO-8859-1");
        // UTF‑8 decoding should give the original string
        assertEquals(original, StringUtilities.createString(utf8Bytes, "UTF-8"));
        // Decoding ISO‑8859‑1 bytes with UTF‑8 should produce a different string
        String mismatched = StringUtilities.createString(isoBytes, "UTF-8");
        assertNotEquals(original, mismatched);
    }

    @Test
    void createString_withUnsupportedEncoding_throwsIllegalArgumentException() {
        // "ABC"
        byte[] data = { 65, 66, 67 };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringUtilities.createString(data, "UNSUPPORTED-ENCODING"));
        assertTrue(ex.getMessage().contains("UNSUPPORTED-ENCODING"), "Exception message should contain the unsupported encoding name");
    }

    @Test
    void createString_withNullEncoding_throwsNullPointerException() {
        byte[] data = { 65, 66, 67 };
        assertThrows(NullPointerException.class, () -> StringUtilities.createString(data, null));
    }

    /**
     * Group: Null bytes handling
     * Covers:
     *   - null byte array → null result
     *   - empty byte array → empty string
     */
    @ParameterizedTest(name = "[{index}] bytes={0}, encoding={1} → expected={2}")
    @MethodSource("nullBytesProvider")
    void createString_nullBytesHandling(byte[] bytes, String encoding, String expected) {
        String result = StringUtilities.createString(bytes, encoding);
        assertEquals(expected, result);
    }

    /**
     * Group: Unsupported encoding handling
     * Expects an IllegalArgumentException wrapping UnsupportedEncodingException.
     */
    @ParameterizedTest(name = "[{index}] encoding={1} is unsupported")
    @MethodSource("unsupportedEncodingProvider")
    void createString_unsupportedEncodingHandling(byte[] bytes, String encoding) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringUtilities.createString(bytes, encoding));
        // Verify that the cause is indeed UnsupportedEncodingException
        assertNotNull(ex.getCause(), "Cause should not be null");
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException, "Cause should be UnsupportedEncodingException");
    }

    /**
     * Group: Null encoding handling
     * When encoding is null the underlying String constructor throws NPE,
     * which should propagate unchanged.
     */
    @ParameterizedTest(name = "[{index}] encoding=null should throw NPE")
    @MethodSource("nullEncodingProvider")
    void createString_nullEncodingHandling(byte[] bytes, String encoding) {
        assertThrows(NullPointerException.class, () -> StringUtilities.createString(bytes, encoding));
    }
}
