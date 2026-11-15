package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringUtilities_createUtf8String_Test {

    /**
     * Supplies all valid (i.e. non‑exception) test data.
     * The byte arrays are built from the integer values defined in the JSON test cases.
     */
    static Stream<Arguments> provideValidByteArrays() {
        return Stream.of(// Empty Input
        arguments("Empty Input – empty byte array should produce empty string", new byte[0], ""), // ASCII Bytes
        arguments("ASCII Bytes – simple ASCII characters", // "Hello"
        toByteArray(72, 101, 108, 108, 111), "Hello"), // Multi‑byte UTF‑8 Characters (Japanese "こんにちは")
        arguments("Multi‑byte UTF‑8 – Japanese Hiragana characters", toByteArray(227, 129, 147, 227, 130, 147, 227, 129, 171, 227, 129, 161, 227, 129, 175), "こんにちは"), // Emoji UTF‑8 (grinning face)
        arguments("Emoji UTF‑8 – single 4‑byte emoji", // 0xF0 0x9F 0x98 0x80
        toByteArray(-16, -97, -104, -128), "😀"), // Invalid UTF‑8 Sequence (malformed)
        arguments("Invalid UTF‑8 – malformed sequence should insert replacement character", // 0xC3 '('  (0xC3 not followed by continuation)
        toByteArray(-61, 40), "\uFFFD("), // Zero Byte (NUL character)
        arguments("Zero Byte – single NUL byte should become a NUL character in the string", new byte[] { 0 }, "\u0000"), // Valid Negative‑value UTF‑8 (Cent sign)
        arguments("Valid Negative‑value UTF‑8 – cent sign (¢)", // 0xC2 0xA2
        toByteArray(-62, -94), "¢"), // Large Input – 1000 repetitions of "abc"
        arguments("Large Input – 1000 repetitions of \"abc\" (3000 characters)", repeatBytes("abc".getBytes(StandardCharsets.UTF_8), 1000), repeatString("abc", 1000)));
    }

    /**
     * Helper that converts a var‑args list of int values (0‑255, signed as Java bytes)
     * into a {@code byte[]} array.
     */
    private static byte[] toByteArray(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }

    /**
     * Repeats a given byte array {@code times} times, concatenating the copies.
     */
    private static byte[] repeatBytes(byte[] pattern, int times) {
        byte[] result = new byte[pattern.length * times];
        for (int i = 0; i < times; i++) {
            System.arraycopy(pattern, 0, result, i * pattern.length, pattern.length);
        }
        return result;
    }

    /**
     * Repeats a given {@code String} {@code times} times.
     */
    private static String repeatString(String s, int times) {
        StringBuilder sb = new StringBuilder(s.length() * times);
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    @Test
    @DisplayName("createUtf8String returns the correct ASCII string")
    void createUtf8String_asciiBytes() {
        // given
        byte[] asciiBytes = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        // when
        String result = StringUtilities.createUtf8String(asciiBytes);
        // then
        assertEquals("Hello, World!", result, "The UTF‑8 string created from ASCII bytes should match the original text.");
    }

    @Test
    @DisplayName("createUtf8String correctly decodes UTF‑8 multi‑byte characters")
    void createUtf8String_unicodeBytes() {
        // given
        // Japanese greeting + Earth emoji
        String original = "こんにちは世界 🌍";
        byte[] utf8Bytes = original.getBytes(StandardCharsets.UTF_8);
        // when
        String result = StringUtilities.createUtf8String(utf8Bytes);
        // then
        assertEquals(original, result, "The UTF‑8 string created from multi‑byte characters should be identical to the source.");
    }

    @Test
    @DisplayName("createUtf8String returns empty string for empty byte array")
    void createUtf8String_emptyArray() {
        // given
        byte[] empty = new byte[0];
        // when
        String result = StringUtilities.createUtf8String(empty);
        // then
        assertEquals("", result, "An empty byte array should produce an empty string.");
    }

    @Test
    @DisplayName("createUtf8String throws NullPointerException when input is null")
    void createUtf8String_nullInput() {
        // given
        byte[] nullBytes = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringUtilities.createUtf8String(nullBytes), "Passing a null byte array should result in a NullPointerException.");
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized tests for all non‑exception scenarios.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideValidByteArrays")
    @DisplayName("createUtf8String converts various byte arrays to expected UTF‑8 strings")
    void createUtf8String_validInputs(String description, byte[] input, String expected) {
        // when
        String result = StringUtilities.createUtf8String(input);
        // then
        assertEquals(expected, result, () -> "Failed case: " + description + ". Expected \"" + expected + "\" but got \"" + result + "\"");
    }
}
