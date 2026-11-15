package com.cedarsoftware.util;

import java.io.UnsupportedEncodingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class StringUtilities_createString_Test {

    /**
     * Convert a {@code byte[]} into a {@code String} using the supplied
     * character encoding.
     *
     * @param bytes    the byte array to convert; may be {@code null}
     * @param encoding the name of the charset to use; must not be {@code null}
     * @return the resulting {@code String} or {@code null} if {@code bytes}
     *         is {@code null}
     * @throws NullPointerException     if {@code encoding} is {@code null}
     * @throws IllegalArgumentException if the charset is not supported;
     *                                  the cause will be an {@link UnsupportedEncodingException}
     */
    public static String createString(byte[] bytes, String encoding) {
        if (bytes == null) {
            return null;
        }
        if (encoding == null) {
            throw new NullPointerException("encoding is null");
        }
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            IllegalArgumentException ex = new IllegalArgumentException("Encoding (" + encoding + ") is not supported by your JVM", e);
            throw ex;
        }
    }

    /**
     * Convert a {@code String} into a {@code byte[]} using the supplied
     * character encoding.
     *
     * @param s        the string to convert; may be {@code null}
     * @param encoding the name of the charset to use; must not be {@code null}
     * @return the resulting byte array or {@code null} if {@code s} is {@code null}
     * @throws NullPointerException     if {@code encoding} is {@code null}
     * @throws IllegalArgumentException if the charset is not supported;
     *                                  the cause will be an {@link UnsupportedEncodingException}
     */
    public static byte[] getBytes(String s, String encoding) {
        if (s == null) {
            return null;
        }
        if (encoding == null) {
            throw new NullPointerException("encoding is null");
        }
        try {
            return s.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            IllegalArgumentException ex = new IllegalArgumentException("Encoding (" + encoding + ") is not supported by your JVM", e);
            throw ex;
        }
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// null byte array returns null
        Arguments.of((byte[]) null, "UTF-8", null), // simple UTF‑8 conversion
        Arguments.of("Hello, World!".getBytes(StandardCharsets.UTF_8), "UTF-8", "Hello, World!"), // empty array → empty string
        Arguments.of(new byte[0], "UTF-8", ""), // UTF‑8 two‑byte character (é)
        Arguments.of(new byte[] { (byte) 0xC3, (byte) 0xA9 }, "UTF-8", "é"), // case‑insensitive charset name
        Arguments.of("Hello, Java".getBytes(StandardCharsets.UTF_8), "utf-8", "Hello, Java"), // ISO‑8859‑1 single‑byte character (é)
        Arguments.of(new byte[] { (byte) 0xE9 }, "ISO-8859-1", "é"), // UTF‑16BE (big‑endian) – A
        Arguments.of(new byte[] { 0x00, 0x41 }, "UTF-16BE", "A"), // UTF‑16 with BOM – A
        Arguments.of(new byte[] { (byte) 0xFF, (byte) 0xFE, 0x41, 0x00 }, "UTF-16", "A"), // US‑ASCII – non‑ASCII byte becomes '?'
        Arguments.of(new byte[] { 0x41, (byte) 0x80 }, "US-ASCII", "A?"), // malformed UTF‑8 sequence – replacement character
        Arguments.of(new byte[] { (byte) 0xC3, 0x28 }, "UTF-8", "\uFFFD("), // UTF‑8 with embedded NUL character
        Arguments.of(new byte[] { 0x48, 0x00, 0x69 }, "UTF-8", "H\u0000i"));
    }

    private static Stream<Arguments> illegalArgumentExceptionProvider() {
        return Stream.of(Arguments.of(new byte[] { 65, 66, 67 }, "UNSUPPORTED_ENCODING_123", "Encoding (UNSUPPORTED_ENCODING_123) is not supported by your JVM"), Arguments.of(new byte[] { 65, 66, 67 }, "", "Encoding () is not supported by your JVM"));
    }

    private static Stream<Arguments> nullPointerExceptionProvider() {
        return Stream.of(// null byte array – still triggers NPE because encoding is null
        Arguments.of((byte[]) null), // empty byte array – still triggers NPE because encoding is null
        Arguments.of(new byte[0]));
    }

    @Test
    void createString_withValidEncoding_returnsExpectedString() {
        byte[] bytes = "Hello, world!".getBytes(StandardCharsets.UTF_8);
        String result = StringUtilities.createString(bytes, "UTF-8");
        assertEquals("Hello, world!", result);
    }

    @Test
    void createString_whenBytesAreNull_returnsNull() {
        String result = StringUtilities.createString(null, "UTF-8");
        assertNull(result);
    }

    @Test
    void createString_withUnsupportedEncoding_throwsIllegalArgumentException() {
        byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
        String unsupported = "UNSUPPORTED-ENCODING-XYZ";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringUtilities.createString(bytes, unsupported));
        assertTrue(ex.getMessage().contains("Encoding (" + unsupported + ") is not supported"));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException);
    }

    @Test
    void createString_withNullEncoding_throwsNullPointerException() {
        byte[] bytes = "abc".getBytes(StandardCharsets.UTF_8);
        assertThrows(NullPointerException.class, () -> StringUtilities.createString(bytes, null));
    }

    @ParameterizedTest(name = "[{index}] bytes={0}, encoding={1} → expected=\"{2}\"")
    @MethodSource("successfulConversionProvider")
    void createString_successfulConversions(byte[] bytes, String encoding, String expected) {
        String result = StringUtilities.createString(bytes, encoding);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] bytes={0}, encoding={1} → IllegalArgumentException")
    @MethodSource("illegalArgumentExceptionProvider")
    void createString_throwsIllegalArgumentException(byte[] bytes, String encoding, String expectedMessageFragment) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringUtilities.createString(bytes, encoding));
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains(expectedMessageFragment), () -> "Message [" + ex.getMessage() + "] does not contain expected fragment [" + expectedMessageFragment + "]");
        // The cause must be an UnsupportedEncodingException as per contract
        assertNotNull(ex.getCause(), "Cause should not be null");
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException, () -> "Cause should be UnsupportedEncodingException but was " + ex.getCause().getClass());
    }

    @ParameterizedTest(name = "[{index}] bytes={0}, encoding=null → NullPointerException")
    @MethodSource("nullPointerExceptionProvider")
    void createString_throwsNullPointerException(byte[] bytes) {
        assertThrows(NullPointerException.class, () -> StringUtilities.createString(bytes, null));
    }
}
