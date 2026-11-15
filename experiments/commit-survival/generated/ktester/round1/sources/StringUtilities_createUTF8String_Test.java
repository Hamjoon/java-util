package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.UnsupportedEncodingException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_createUTF8String_Test {

    /**
     * Simple holder for normal test case data.
     */
    private static final class ArgumentsHolder {

        final byte[] bytes;

        final String expected;

        final String description;

        ArgumentsHolder(byte[] bytes, String expected, String description) {
            this.bytes = bytes;
            this.expected = expected;
            this.description = description;
        }
    }

    /**
     * Simple holder for exception‑expectation test case data.
     */
    private static final class ExceptionCase {

        final byte[] bytes;

        final Class<? extends Throwable> expectedException;

        final String description;

        ExceptionCase(byte[] bytes, Class<? extends Throwable> expectedException, String description) {
            this.bytes = bytes;
            this.expectedException = expectedException;
            this.description = description;
        }
    }

    /**
     * Supplies the normal (non‑exception) test cases.
     */
    private static Stream<ArgumentsHolder> normalCases() {
        return Stream.of(// Standard ASCII bytes → "Hello"
        new ArgumentsHolder(new byte[] { 72, 101, 108, 108, 111 }, "Hello", "Standard ASCII bytes should be converted to the expected UTF‑8 string."), // Multi‑byte character é → "é"
        new ArgumentsHolder(new byte[] { (byte) 0xC3, (byte) 0xA9 }, "é", "Multi‑byte UTF‑8 character (é) should be correctly decoded."), // Four‑byte emoji 😀
        new ArgumentsHolder(new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 }, // Java representation of 😀
        "\uD83D\uDE00", "Four‑byte UTF‑8 sequence representing the emoji U+1F600 should be decoded correctly."), // BOM (U+FEFF) + "Hello"
        new ArgumentsHolder(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 72, 101, 108, 108, 111 }, "\uFEFFHello", "Byte Order Mark (BOM) should be decoded as ZERO WIDTH NO‑BREAK SPACE followed by the text."), // Embedded null byte → "A\u0000B"
        new ArgumentsHolder(new byte[] { 65, 0, 66 }, "A\u0000B", "A null byte (0x00) embedded in the array should be preserved in the resulting Java string."), // Two‑byte cent sign → "¢"
        new ArgumentsHolder(new byte[] { (byte) 0xC2, (byte) 0xA2 }, "¢", "Two‑byte UTF‑8 sequence with negative signed byte values should decode to the cent sign."), // Large 10 000‑character 'a' array
        new ArgumentsHolder(generateLargeAsciiArray('a', 10_000), "a".repeat(10_000), "Large byte array (10 000 ASCII 'a') should be handled without truncation."));
    }

    /**
     * Supplies the cases that are expected to throw an exception.
     */
    private static Stream<ExceptionCase> exceptionCases() {
        return Stream.of(// Invalid UTF‑8 sequence → UnsupportedEncodingException (or a wrapper)
        new ExceptionCase(new byte[] { (byte) 0xFF, (byte) 0xFE }, UnsupportedEncodingException.class, "Invalid UTF‑8 byte sequence should cause an UnsupportedEncodingException."));
    }

    /**
     * Generates a large ASCII byte array filled with the given character.
     */
    private static byte[] generateLargeAsciiArray(char ch, int length) {
        byte[] arr = new byte[length];
        byte b = (byte) ch;
        for (int i = 0; i < length; i++) {
            arr[i] = b;
        }
        return arr;
    }

    @Test
    @DisplayName("createUTF8String returns correct String for a valid UTF‑8 byte array")
    void testCreateUTF8StringValidBytes() {
        // given
        String original = "Hello, 世界 🌍";
        byte[] utf8Bytes = original.getBytes(StandardCharsets.UTF_8);
        // when
        String result = StringUtilities.createUTF8String(utf8Bytes);
        // then
        assertEquals(original, result, "The decoded string should match the original");
    }

    @Test
    @DisplayName("createUTF8String returns an empty String for an empty byte array")
    void testCreateUTF8StringEmptyBytes() {
        // given
        byte[] empty = new byte[0];
        // when
        String result = StringUtilities.createUTF8String(empty);
        // then
        assertEquals("", result, "An empty byte array should produce an empty string");
    }

    @Test
    @DisplayName("createUTF8String throws NullPointerException when passed null")
    void testCreateUTF8StringNullBytes() {
        // given
        byte[] nullBytes = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringUtilities.createUTF8String(nullBytes), "Passing null should result in a NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalCases")
    @DisplayName("createUTF8String – normal decoding cases")
    void testCreateUTF8StringNormal(ArgumentsHolder holder) {
        // when
        String result = StringUtilities.createUTF8String(holder.bytes);
        // then
        assertEquals(holder.expected, result, holder.description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionCases")
    @DisplayName("createUTF8String – exception cases")
    void testCreateUTF8StringExceptions(ExceptionCase ec) {
        // when / then
        assertThrows(ec.expectedException, () -> StringUtilities.createUTF8String(ec.bytes), ec.description);
    }

    @Test
    @DisplayName("createUTF8String – immutability test (modifying source array after call)")
    void testCreateUTF8StringImmutability() {
        // given
        // "Hi"
        byte[] source = new byte[] { 72, 105 };
        String before = StringUtilities.createUTF8String(source);
        // mutate original array
        source[0] = (byte) 'B';
        source[1] = (byte) 'y';
        // when
        String after = StringUtilities.createUTF8String(source);
        // then
        assertEquals("Hi", before, "Result from first call must not be affected by later mutation.");
        assertEquals("By", after, "Second call should reflect the mutated content.");
    }
}
