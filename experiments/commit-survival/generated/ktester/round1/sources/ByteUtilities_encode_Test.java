package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteUtilities_encode_Test {

    /**
     * Normal cases – includes empty array and single‑byte variations.
     */
    private static Stream<Arguments> normalCases() {
        return Stream.of(// empty array
        Arguments.of(new byte[] {}, ""), // single byte values exercising each branch
        Arguments.of(new byte[] { 0x00 }, "00"), Arguments.of(new byte[] { 0x0F }, "0f"), Arguments.of(new byte[] { 0x10 }, "10"), Arguments.of(new byte[] { (byte) 0x9A }, "9a"), Arguments.of(new byte[] { (byte) 0xFF }, "ff"), // multiple bytes – all branches in one call
        Arguments.of(new byte[] { 0, 15, 16, (byte) 0x9A, (byte) 0xFF }, "000f109affff"));
    }

    /**
     * Negative byte values (signed representation).
     */
    private static Stream<Arguments> negativeValues() {
        return Stream.of(// 0x80
        Arguments.of(new byte[] { (byte) -128 }, "80"), // 0xFF
        Arguments.of(new byte[] { (byte) -1 }, "ff"));
    }

    /**
     * Mixed nibble cases – high nibble alphabetic / numeric combos.
     */
    private static Stream<Arguments> mixedNibbleCases() {
        return Stream.of(Arguments.of(new byte[] { (byte) 0xA1 }, "a1"), Arguments.of(new byte[] { (byte) 0xF0 }, "f0"), Arguments.of(new byte[] { 0x0A }, "0a"), Arguments.of(new byte[] { 0x5F }, "5f"), Arguments.of(new byte[] { 0x6C }, "6c"));
    }

    /**
     * Combination of several bytes, mixing negative and positive values.
     */
    private static Stream<Arguments> arrayCombination() {
        return Stream.of(Arguments.of(new byte[] { (byte) -128, (byte) -1, 0x0A, (byte) 0xA1 }, "80ff0aa1"));
    }

    /**
     * Verify that the produced string length matches 2 × number of bytes.
     */
    private static Stream<Arguments> lengthVerification() {
        byte[] tenBytes = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        return Stream.of(Arguments.of(tenBytes, "0102030405060708090a"));
    }

    @Test
    @DisplayName("encode returns empty string for empty byte array")
    void encodeEmptyArray() {
        byte[] input = new byte[0];
        String result = ByteUtilities.encode(input);
        assertEquals("", result, "Encoding an empty array should produce an empty string");
    }

    @Test
    @DisplayName("encode correctly converts a single byte to two hex characters")
    void encodeSingleByte() {
        assertAll(() -> assertEquals("00", ByteUtilities.encode(new byte[] { 0x00 })), () -> assertEquals("0f", ByteUtilities.encode(new byte[] { 0x0F })), () -> assertEquals("f0", ByteUtilities.encode(new byte[] { (byte) 0xF0 })), () -> assertEquals("ff", ByteUtilities.encode(new byte[] { (byte) 0xFF })));
    }

    @Test
    @DisplayName("encode correctly converts multiple bytes")
    void encodeMultipleBytes() {
        byte[] input = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };
        String expected = "abcdef";
        assertEquals(expected, ByteUtilities.encode(input));
    }

    @Test
    @DisplayName("encode throws NullPointerException when input is null")
    void encodeNullArray() {
        assertThrows(NullPointerException.class, () -> ByteUtilities.encode(null));
    }

    @ParameterizedTest(name = "[{index}] encode({0}) => \"{1}\"")
    @MethodSource("normalCases")
    @DisplayName("Normal Cases – encode produces expected hex strings")
    void encodeNormalCases(byte[] input, String expected) {
        assertEquals(expected, ByteUtilities.encode(input));
    }

    @ParameterizedTest(name = "[{index}] encode({0}) => \"{1}\"")
    @MethodSource("negativeValues")
    @DisplayName("Negative Values – signed bytes are treated as unsigned")
    void encodeNegativeValues(byte[] input, String expected) {
        assertEquals(expected, ByteUtilities.encode(input));
    }

    @ParameterizedTest(name = "[{index}] encode({0}) => \"{1}\"")
    @MethodSource("mixedNibbleCases")
    @DisplayName("Mixed Nibble Cases – high/low nibble alphabetic & numeric")
    void encodeMixedNibbleCases(byte[] input, String expected) {
        assertEquals(expected, ByteUtilities.encode(input));
    }

    @ParameterizedTest(name = "[{index}] encode({0}) => \"{1}\"")
    @MethodSource("arrayCombination")
    @DisplayName("Array Combination – concatenation of multiple byte encodings")
    void encodeArrayCombination(byte[] input, String expected) {
        assertEquals(expected, ByteUtilities.encode(input));
    }

    @ParameterizedTest(name = "[{index}] encode({0}) => \"{1}\"")
    @MethodSource("lengthVerification")
    @DisplayName("Length Verification – output length equals 2 × input length")
    void encodeLengthVerification(byte[] input, String expected) {
        String result = ByteUtilities.encode(input);
        assertEquals(expected, result);
        assertEquals(input.length * 2, result.length(), "Resulting string length should be exactly twice the input array length");
    }

    /**
     * This test demonstrates the behaviour when the requested {@code StringBuilder}
     * capacity overflows the {@code int} range. Creating an actual array of length
     * 2³⁰ (≈1 GB) would exhaust the JVM heap, so the test is disabled to keep the
     * suite fast and safe. The expectation is documented for completeness.
     */
    @Test
    @Disabled("Disabled to avoid OOM – verifies that an IllegalArgumentException would be thrown for capacity overflow")
    @DisplayName("encode throws IllegalArgumentException for capacity overflow")
    void encodeCapacityOverflow() {
        // The following line would allocate ~1 GB; we keep it commented.
        // byte[] huge = new byte[1 << 30];
        // assertThrows(IllegalArgumentException.class, () -> ByteUtilities.encode(huge));
    }
}
