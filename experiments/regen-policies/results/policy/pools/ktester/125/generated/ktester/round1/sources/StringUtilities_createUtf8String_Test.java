package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class StringUtilities_createUtf8String_Test {

    private static Stream<Arguments> provideCreateUtf8StringCases() {
        return Stream.of(// Standard ASCII string
        Arguments.of("Standard UTF‑8 byte array representing \"Hello World\"", new byte[] { 72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100 }, "Hello World"), // Empty array
        Arguments.of("Empty byte array should yield empty string", new byte[] {}, ""), // Invalid 2‑byte sequence (0xC3 0x28) → replacement char + '('
        Arguments.of("Invalid UTF‑8 sequence 0xC3 0x28 results in replacement char followed by '('", new byte[] { (byte) 0xC3, (byte) 0x28 }, "\uFFFD("), // Euro sign (€) – three‑byte UTF‑8
        Arguments.of("Three‑byte UTF‑8 sequence for Euro sign", new byte[] { (byte) 0xE2, (byte) 0x82, (byte) 0xAC }, "\u20AC"), // Emoji 😁 (U+1F601) – four‑byte UTF‑8, represented as surrogate pair in Java
        Arguments.of("Four‑byte UTF‑8 sequence for emoji 😁", new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x81 }, "\uD83D\uDE01"), // Cent sign (¢) – two‑byte UTF‑8
        Arguments.of("Two‑byte UTF‑8 sequence for cent sign", new byte[] { (byte) 0xC2, (byte) 0xA2 }, "\u00A2"), // Lone high surrogate (U+D800) – valid UTF‑8 but illegal Unicode scalar value
        Arguments.of("Byte sequence decoding to a lone high surrogate (U+D800)", new byte[] { (byte) 0xED, (byte) 0xA0, (byte) 0x80 }, "\uD800"), // Null bytes interleaved with ASCII characters
        Arguments.of("Null bytes interleaved with ASCII characters", new byte[] { 0x00, 0x41, 0x00, 0x42 }, "\u0000A\u0000B"), // Completely malformed bytes → three replacement characters
        Arguments.of("All bytes invalid in UTF‑8 should yield three replacement chars", new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, "\uFFFD\uFFFD\uFFFD"));
    }

    @Test
    void createUtf8String_givenValidUtf8Bytes_returnsExpectedString() {
        // "Hello, 世界" encoded in UTF‑8
        byte[] utf8Bytes = "Hello, 世界".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String result = StringUtilities.createUtf8String(utf8Bytes);
        assertEquals("Hello, 世界", result);
    }

    @Test
    void createUtf8String_givenEmptyByteArray_returnsEmptyString() {
        byte[] empty = new byte[0];
        String result = StringUtilities.createUtf8String(empty);
        assertEquals("", result);
    }

    @Test
    void createUtf8String_givenNull_returnsNull() {
        String result = StringUtilities.createUtf8String(null);
        assertNull(result);
    }

    @Test
    void createUtf8String_givenInvalidUtf8Sequence_returnsStringWithReplacementChars() {
        // Create a byte sequence that is not valid UTF‑8 (e.g., isolated continuation byte)
        byte[] invalid = new byte[] { (byte) 0x80, (byte) 0x80, (byte) 0x80 };
        String result = StringUtilities.createUtf8String(invalid);
        // The JDK replaces malformed sequences with the Unicode replacement character  (U+FFFD)
        // The exact result depends on the implementation, but it must be a non‑null string.
        assertNotNull(result);
        assertTrue(result.contains("\uFFFD"));
    }

    /* ----------------------------------------------------------------------
     * Parameterized tests covering the exhaustive JSON test matrix
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideCreateUtf8StringCases")
    void createUtf8String_various(String description, byte[] bytes, String expected) {
        String actual = StringUtilities.createUtf8String(bytes);
        assertEquals(expected, actual, description);
    }

    /* ----------------------------------------------------------------------
     * Explicit test for the null‑argument contract (expected NPE)
     * ---------------------------------------------------------------------- */
    @Test
    void createUtf8String_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtilities.createUtf8String(null), "Passing null should result in NullPointerException");
    }
}
