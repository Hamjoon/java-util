package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_createUTF8String_Test {

    static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(// Null input – should throw NullPointerException
        Arguments.of(null, NullPointerException.class), // Empty array – should return empty string
        Arguments.of(new byte[0], ""));
    }

    static Stream<Arguments> validUtf8Provider() {
        return Stream.of(// ASCII only: "Hello"
        Arguments.of(new byte[] { 72, 101, 108, 108, 111 }, "Hello"), // NUL character followed by "abc"
        Arguments.of(new byte[] { 0, 97, 98, 99 }, "\0abc"), // Euro sign (€) – three‑byte UTF‑8 sequence
        Arguments.of(new byte[] { (byte) -30, (byte) -126, (byte) -84 }, "€"), // Gothic letter 𐍈 – four‑byte UTF‑8 sequence (outside BMP)
        Arguments.of(new byte[] { (byte) -16, (byte) -97, (byte) -104, (byte) -115 }, "𐍈"));
    }

    @Test
    void createUTF8String_withAsciiBytes_returnsExpectedString() {
        // given
        byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
        // when
        String result = StringUtilities.createUTF8String(bytes);
        // then
        assertEquals("Hello World", result);
    }

    @Test
    void createUTF8String_withMultibyteCharacters_returnsExpectedString() {
        // given – Japanese greeting "こんにちは" (5 characters, each 3 bytes in UTF‑8)
        String original = "こんにちは";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        // when
        String result = StringUtilities.createUTF8String(bytes);
        // then
        assertEquals(original, result);
    }

    @Test
    void createUTF8String_withEmptyArray_returnsEmptyString() {
        // given
        byte[] empty = new byte[0];
        // when
        String result = StringUtilities.createUTF8String(empty);
        // then
        assertEquals("", result);
    }

    @Test
    void createUTF8String_withNullArray_throwsNullPointerException() {
        // given
        byte[] nullBytes = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringUtilities.createUTF8String(nullBytes));
    }

    @Test
    void createUTF8String_withInvalidUtf8Bytes_behavesAccordingToJDK() {
        // given – deliberately malformed UTF‑8 sequence (single continuation byte)
        byte[] malformed = new byte[] { (byte) 0x80 };
        // when
        String result = StringUtilities.createUTF8String(malformed);
        // then – JDK replaces malformed bytes with the Unicode replacement character
        assertEquals("\uFFFD", result);
    }

    /**
     * Test the handling of null and empty byte arrays.
     *
     * @param bytes    the input byte array (may be {@code null})
     * @param expected the expected outcome: either a string or the class of an exception
     */
    @ParameterizedTest(name = "[{index}] bytes={0}")
    @MethodSource("nullAndEmptyProvider")
    void createUTF8String_nullAndEmptyInput(byte[] bytes, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expect an exception
            assertThrows((Class<? extends Throwable>) expected, () -> StringUtilities.createUTF8String(bytes));
        } else {
            // Expect a normal string result
            String result = StringUtilities.createUTF8String(bytes);
            assertEquals(expected, result);
        }
    }

    /**
     * Test a collection of valid UTF‑8 byte sequences, covering ASCII, NUL,
     * 3‑byte and 4‑byte characters.
     *
     * @param bytes    the UTF‑8 encoded byte array
     * @param expected the expected decoded string
     */
    @ParameterizedTest(name = "[{index}] bytes={0} → \"{1}\"")
    @MethodSource("validUtf8Provider")
    void createUTF8String_validUtf8Sequences(byte[] bytes, String expected) {
        // when
        String result = StringUtilities.createUTF8String(bytes);
        // then
        assertEquals(expected, result);
    }
}
