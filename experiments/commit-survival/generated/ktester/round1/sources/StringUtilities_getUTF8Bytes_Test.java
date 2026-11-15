package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_getUTF8Bytes_Test {

    /**
     * Data for normal conversion tests (ASCII, Unicode, etc.).
     */
    private static Stream<Arguments> conversionCases() {
        return Stream.of(// Empty string
        Arguments.of("", new byte[0], "Empty string should yield an empty byte array"), // Simple ASCII
        Arguments.of("Hello", "Hello".getBytes(StandardCharsets.UTF_8), "ASCII string should be encoded to its exact UTF‑8 bytes"), // Null character (U+0000)
        Arguments.of("\u0000", new byte[] { 0 }, "NUL character should be encoded as a single zero byte"), // Japanese Hiragana (3‑byte UTF‑8 each)
        Arguments.of("こんにちは", new byte[] { (byte) 0xE3, (byte) 0x81, (byte) 0x93, (byte) 0xE3, (byte) 0x82, (byte) 0x93, (byte) 0xE3, (byte) 0x81, (byte) 0xAB, (byte) 0xE3, (byte) 0x81, (byte) 0xA1, (byte) 0xE3, (byte) 0x81, (byte) 0xAF }, "Japanese Hiragana should be encoded as three‑byte sequences"), // Mixed Latin‑1 and Greek characters (2‑byte each)
        Arguments.of("éßΩ", new byte[] { (byte) 0xC3, (byte) 0xA9, (byte) 0xC3, (byte) 0x9F, (byte) 0xCE, (byte) 0xA9 }, "Mixed Latin‑1 and Greek characters should produce correct two‑byte UTF‑8 sequences"), // Emoji (U+1F600) – four‑byte UTF‑8
        Arguments.of("😀", new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 }, "Supplementary plane emoji should be encoded as a four‑byte UTF‑8 sequence"), // Mixed ASCII and Emoji
        Arguments.of("a😀b", new byte[] { (byte) 'a', (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80, (byte) 'b' }, "String containing ASCII before and after an emoji should concatenate correctly"), // Gothic letter (U+10348) – four‑byte UTF‑8
        Arguments.of("𐍈", new byte[] { (byte) 0xF0, (byte) 0x90, (byte) 0x8D, (byte) 0x88 }, "Supplementary plane Gothic character should be encoded correctly"), // Combining acute accent (e + ́)
        Arguments.of("e\u0301", new byte[] { (byte) 'e', (byte) 0xCC, (byte) 0x81 }, "Base character followed by a combining diacritic should be encoded correctly"));
    }

    /**
     * Data for the large‑input test (1000 'a' characters).
     */
    private static Stream<Arguments> largeAsciiCase() {
        String repeated = "a".repeat(1000);
        byte[] expected = repeated.getBytes(StandardCharsets.UTF_8);
        return Stream.of(Arguments.of(repeated, expected));
    }

    @Test
    void getUTF8Bytes_returnsCorrectBytesForAsciiString() {
        // given
        String input = "Hello, World!";
        byte[] expected = input.getBytes(StandardCharsets.UTF_8);
        // when
        byte[] actual = StringUtilities.getUTF8Bytes(input);
        // then
        assertArrayEquals(expected, actual, "UTF‑8 bytes for an ASCII string should match");
    }

    @Test
    void getUTF8Bytes_returnsEmptyArrayForEmptyString() {
        // given
        String input = "";
        byte[] expected = new byte[0];
        // when
        byte[] actual = StringUtilities.getUTF8Bytes(input);
        // then
        assertArrayEquals(expected, actual, "UTF‑8 bytes for an empty string should be an empty array");
    }

    @Test
    void getUTF8Bytes_handlesUnicodeCharactersCorrectly() {
        // given
        // String contains Euro sign, Chinese characters, and an emoji
        String input = "€中文🙂";
        byte[] expected = input.getBytes(StandardCharsets.UTF_8);
        // when
        byte[] actual = StringUtilities.getUTF8Bytes(input);
        // then
        assertArrayEquals(expected, actual, "UTF‑8 bytes for Unicode characters should be correct");
    }

    @Test
    void getUTF8Bytes_returnsNullWhenInputIsNull() {
        // given
        String input = null;
        // when
        byte[] actual = StringUtilities.getUTF8Bytes(input);
        // then
        assertNull(actual, "Method should return null when the input string is null");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("conversionCases")
    void getUTF8Bytes_convertsVariousStringsCorrectly(String input, byte[] expected, String description) {
        // when
        byte[] actual = StringUtilities.getUTF8Bytes(input);
        // then
        assertArrayEquals(expected, actual, description);
    }

    @Test
    void getUTF8Bytes_doesNotThrowUnsupportedEncodingException() {
        // The method internally uses the constant "UTF-8", which is guaranteed to be supported.
        assertDoesNotThrow(() -> {
            byte[] result = StringUtilities.getUTF8Bytes("any");
            assertNotNull(result, "Result should not be null for a non‑null input");
        });
    }

    @Test
    void getUTF8Bytes_nullInput_throwsNullPointerException() {
        // The underlying getBytes does not accept null, therefore a NPE is expected.
        assertThrows(NullPointerException.class, () -> StringUtilities.getUTF8Bytes(null), "Calling getUTF8Bytes with null should throw NullPointerException");
    }

    @Test
    void getUTF8Bytes_returnsDistinctArrayInstancesOnRepeatedCalls() {
        String input = "repeat";
        byte[] first = StringUtilities.getUTF8Bytes(input);
        byte[] second = StringUtilities.getUTF8Bytes(input);
        assertNotSame(first, second, "Each call should return a new byte[] instance");
        assertArrayEquals(first, second, "The content of both arrays should be identical");
    }

    @Test
    void getUTF8Bytes_immutabilityOfReturnedArray() {
        String input = "test";
        byte[] original = StringUtilities.getUTF8Bytes(input);
        // Mutate the returned array
        original[0] = (byte) 0xFF;
        // A fresh call should give the original, unmodified bytes
        byte[] fresh = StringUtilities.getUTF8Bytes(input);
        byte[] expected = input.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, fresh, "Modifying the returned array must not affect subsequent calls");
    }

    @ParameterizedTest(name = "Large ASCII string of length {0}.length()")
    @MethodSource("largeAsciiCase")
    void getUTF8Bytes_largeAsciiString(String input, byte[] expected) {
        byte[] actual = StringUtilities.getUTF8Bytes(input);
        assertEquals(1000, actual.length, "Byte array length should match the number of characters for pure ASCII");
        assertArrayEquals(expected, actual, "All bytes should correspond to ASCII value 97 ('a')");
    }
}
