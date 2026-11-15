package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_encode_Test {

    private static Stream<Arguments> randomByteArrays() {
        return Stream.of(Arguments.of(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56 }), Arguments.of(new byte[] { (byte) 0x00 }), Arguments.of(new byte[] { (byte) 0xFF, (byte) 0x00, (byte) 0x7F, (byte) 0x80 }), Arguments.of(new byte[] { (byte) 0x1A, (byte) 0x2B, (byte) 0x3C, (byte) 0x4D, (byte) 0x5E }));
    }

    private static Stream<Arguments> standardEncodingCases() {
        return Stream.of(// Mixed array covering all nibble ranges
        Arguments.of("Encode a mixed array of bytes covering all nibble ranges (0‑9 and a‑f)", new byte[] { 0, 15, 16, 26, (byte) 0xFF }, "000f101aff"), // Empty array
        Arguments.of("Encode an empty byte array", new byte[] {}, ""));
    }

    private static Stream<Arguments> singleByteEncodingCases() {
        return Stream.of(Arguments.of("Encode a single zero byte", new byte[] { 0 }, "00"), Arguments.of("Encode a single byte whose value is 0x0F", new byte[] { 0x0F }, "0f"), Arguments.of("Encode a single byte with the maximum unsigned value 0xFF (signed -1)", new byte[] { (byte) 0xFF }, "ff"));
    }

    /**
     * Simple deterministic cases **
     */
    @Test
    void encode_emptyArray_returnsEmptyString() {
        byte[] input = new byte[0];
        String result = StringUtilities.encode(input);
        assertEquals("", result);
    }

    @Test
    void encode_singleByte_returnsTwoHexDigits() {
        byte[] input = new byte[] { (byte) 0x0F };
        String result = StringUtilities.encode(input);
        assertEquals("0f", result);
    }

    @Test
    void encode_multipleBytes_returnsConcatenatedHex() {
        byte[] input = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0x01 };
        String result = StringUtilities.encode(input);
        assertEquals("abcd01", result);
    }

    @Test
    void encode_negativeByte_isHandledAsUnsigned() {
        byte[] input = new byte[] { (byte) 0xFF, (byte) 0x80 };
        // 0xFF -> "ff", 0x80 -> "80"
        String result = StringUtilities.encode(input);
        assertEquals("ff80", result);
    }

    /**
     * Null handling – should throw NPE **
     */
    @Test
    void encode_nullArray_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtilities.encode(null));
    }

    /**
     * Parameterized test with a variety of random data **
     */
    @ParameterizedTest
    @MethodSource("randomByteArrays")
    void encode_randomByteArray_isHexStringOfCorrectLength(byte[] input) {
        String result = StringUtilities.encode(input);
        // Each byte should become exactly two hex characters
        assertEquals(input.length * 2, result.length());
        // Verify that each pair of characters is a valid hex digit
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            assertTrue((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'), "Invalid hex character: " + c);
        }
        // Ensure the original array is not modified
        byte[] copy = input.clone();
        assertArrayEquals(copy, input);
    }

    /**
     * Tests derived from the provided JSON test cases **
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("standardEncodingCases")
    void encode_standardEncoding(String description, byte[] input, String expected) {
        String result = StringUtilities.encode(input);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("singleByteEncodingCases")
    void encode_singleByteEncoding(String description, byte[] input, String expected) {
        String result = StringUtilities.encode(input);
        assertEquals(expected, result, description);
    }

    @Test
    void encode_nullInput_handlingFromJson() {
        // The JSON expects a NullPointerException for null input
        assertThrows(NullPointerException.class, () -> StringUtilities.encode(null));
    }
}
