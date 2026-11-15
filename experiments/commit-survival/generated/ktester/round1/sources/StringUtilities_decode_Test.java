package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringUtilities_decode_Test {

    /* --------------------------------------------------------------------- */
    private static byte[] toByteArray(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }

    private static Stream<Arguments> provideValidAndEdgeCases() {
        return Stream.of(// Even length hex string with uppercase letters
        arguments("0A1B", toByteArray(0x0A, 0x1B), null), // Empty string – should return an empty byte array
        arguments("", new byte[0], null), // Odd length string – early return null
        arguments("ABC", null, null), // Non‑hex high nibble ('G' -> -1) combined with a valid low nibble
        arguments("G1", toByteArray(-15), null), // Mixed‑case hex characters
        arguments("aF", toByteArray(0xAF), null));
    }

    private static Stream<Arguments> provideAdditionalCases() {
        return Stream.of(// Full range of hex digits
        arguments("0123456789ABCDEF", toByteArray(0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF), null), // All lower‑case hex characters
        arguments("abcdef", toByteArray(0xAB, 0xCD, 0xEF), null), // Boundary values: 0x00 and 0xFF
        arguments("00FF", toByteArray(0x00, 0xFF), null), // Repeated "AA" pattern (0xAA = -86 as signed byte)
        arguments("AAAAAAAAAA", toByteArray(0xAA, 0xAA, 0xAA, 0xAA, 0xAA), null), // Both characters non‑hex ('Z' -> -1)
        arguments("ZZ", toByteArray(-17), null), // Null argument – should throw NullPointerException
        arguments(null, null, NullPointerException.class), // Whitespace as high nibble (non‑hex) and valid low nibble
        arguments(" 1", toByteArray(-15), null), // Valid high nibble, invalid low nibble
        arguments("0G", toByteArray(-1), null));
    }

    @Test
    @DisplayName("decode should correctly convert a valid even‑length hex string")
    void testDecodeValidHexString() {
        // given
        String hex = "0A1B2C3D";
        // when
        byte[] result = StringUtilities.decode(hex);
        // then
        byte[] expected = new byte[] { 0x0A, 0x1B, 0x2C, 0x3D };
        assertArrayEquals(expected, result, "Decoded bytes do not match expected values");
    }

    @Test
    @DisplayName("decode should return an empty array for an empty string")
    void testDecodeEmptyString() {
        // given
        String hex = "";
        // when
        byte[] result = StringUtilities.decode(hex);
        // then
        assertNotNull(result, "Result should not be null for empty input");
        assertEquals(0, result.length, "Resulting array should be empty");
    }

    @Test
    @DisplayName("decode should return null for strings with odd length")
    void testDecodeOddLengthReturnsNull() {
        // given
        // length 3, odd
        String hex = "ABC";
        // when
        byte[] result = StringUtilities.decode(hex);
        // then
        assertNull(result, "Result should be null when input length is odd");
    }

    @Test
    @DisplayName("decode should throw NullPointerException when input is null")
    void testDecodeNullInputThrowsNPE() {
        // given
        String hex = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringUtilities.decode(hex), "Calling decode with null should throw NullPointerException");
    }

    @Test
    @DisplayName("decode should handle mixed‑case hex digits")
    void testDecodeMixedCaseHex() {
        // given
        String hex = "aBcDeF";
        // when
        byte[] result = StringUtilities.decode(hex);
        // then
        byte[] expected = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };
        assertArrayEquals(expected, result, "Decoded bytes should be case‑insensitive");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] decode(\"{0}\")")
    @MethodSource("provideValidAndEdgeCases")
    @DisplayName("decode(String) - valid and edge cases")
    void testDecodeValidAndEdgeCases(String input, byte[] expected, Class<? extends Throwable> exception) {
        if (exception != null) {
            // Expect an exception (e.g., NullPointerException)
            assertThrows(exception, () -> StringUtilities.decode(input), () -> "Expected exception " + exception.getSimpleName() + " for input: " + input);
        } else {
            // Normal execution – verify returned byte array (including null)
            byte[] result = StringUtilities.decode(input);
            if (expected == null) {
                assertNull(result, () -> "Result should be null for input: " + input);
            } else {
                assertArrayEquals(expected, result, () -> "Decoded bytes differ for input: " + input);
            }
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] decode(\"{0}\")")
    @MethodSource("provideAdditionalCases")
    @DisplayName("decode(String) - additional cases")
    void testDecodeAdditionalCases(String input, byte[] expected, Class<? extends Throwable> exception) {
        if (exception != null) {
            assertThrows(exception, () -> StringUtilities.decode(input), () -> "Expected exception " + exception.getSimpleName() + " for input: " + input);
        } else {
            byte[] result = StringUtilities.decode(input);
            if (expected == null) {
                assertNull(result, () -> "Result should be null for input: " + input);
            } else {
                assertArrayEquals(expected, result, () -> "Decoded bytes differ for input: " + input);
            }
        }
    }
}
