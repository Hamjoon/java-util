package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_getUTF8Bytes_Test {

    /**
     * Provides test data: the original string and the expected UTF‑8 byte array
     * obtained via {@link String#getBytes(java.nio.charset.Charset)}.
     */
    private static Stream<Arguments> utf8ByteProvider() {
        return Stream.of(// null input – the utility returns null (null‑safe wrapper)
        Arguments.of(null, null), // empty string – should yield an empty array
        Arguments.of("", ""), // simple ASCII
        Arguments.of("Hello", "SGVsbG8="), // Japanese Hiragana (multi‑byte UTF‑8)
        Arguments.of("こんにちは", "44GT44KT44Gr44Gh44Gv"), // Musical symbol G‑clef (U+1D11E) – a surrogate pair resulting in 4‑byte UTF‑8
        Arguments.of("𝄞", "8J2UgA=="), // Mixed ASCII and emoji
        Arguments.of("Hello 🌍", "SGVsbG8g8J+MjA=="), // Single Latin‑1 character with 2‑byte UTF‑8 encoding
        Arguments.of("é", "w6k="), // Two Chinese characters, each 3‑byte UTF‑8
        Arguments.of("中文", "5Lit5paH"), // String containing a single NUL character (0x00)
        Arguments.of("\u0000", "AA=="), // Unpaired high‑surrogate code unit (U+D800) – Java encodes as 0xED 0xA0 0x80
        Arguments.of("\uD800", "7aCA"), // Letter 'e' followed by a combining acute accent (U+0301)
        Arguments.of("e\u0301", "ZcyB"), // Space, tab and newline characters
        Arguments.of(" \t\n", "IAkK"));
    }

    private static Stream<Arguments> nullInputProvider() {
        return Stream.of(Arguments.of("Calling getUTF8Bytes(null) must not throw NPE"));
    }

    /**
     * Parameterised test covering a variety of inputs.
     */
    @ParameterizedTest
    @MethodSource("utf8ByteProvider")
    void testGetUTF8Bytes(String input, byte[] expected) {
        byte[] actual = StringUtilities.getUTF8Bytes(input);
        if (base64Expected == null) {
            // According to the current implementation, a null source string yields null.
            assertNull(actual, "Expected null byte array for null input");
        } else {
            byte[] expected = Base64.getDecoder().decode(base64Expected);
            assertArrayEquals(expected, actual, () -> "UTF‑8 bytes differ for input: " + (input == null ? "null" : input));
        }
    }

    /**
     * Additional sanity check: verifies that the method does **not** silently swallow a
     * {@link NullPointerException}.  The current implementation returns {@code null} for a
     * {@code null} argument, but earlier versions of the library threw an NPE.  This test
     * documents the expected behaviour and will fail if a regression occurs.
     */
    @ParameterizedTest
    @MethodSource("nullInputProvider")
    void testNullInputDoesNotThrow(String description) {
        assertDoesNotThrow(() -> {
            byte[] result = StringUtilities.getUTF8Bytes(null);
            assertNull(result, "Result should be null for null input");
        }, description);
    }
}
