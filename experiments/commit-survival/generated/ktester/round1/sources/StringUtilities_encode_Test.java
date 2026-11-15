package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_encode_Test {

    /**
     * Supplies the data for {@link #encode_basicAndNibbleCases(byte[], String)}.
     */
    static Stream<Arguments> basicAndNibbleCases() {
        return Stream.of(// basic functionality
        Arguments.of(new byte[] { 0x00 }, "00"), Arguments.of(new byte[] { 0x0F }, "0f"), Arguments.of(new byte[] { (byte) 0xFF }, "ff"), Arguments.of(new byte[] { 0x12, 0x34 }, "1234"), Arguments.of(new byte[] {}, ""), // nibble edge cases
        Arguments.of(new byte[] { 0x05 }, "05"), Arguments.of(new byte[] { 0x50 }, "50"), Arguments.of(new byte[] { (byte) 0xA5 }, "a5"), Arguments.of(new byte[] { (byte) 0xF0 }, "f0"));
    }

    static Stream<Arguments> signedByteCases() {
        return Stream.of(// -128
        Arguments.of(new byte[] { (byte) 0x80 }, "80"), Arguments.of(new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF }, "abcdef"));
    }

    /**
     * Utility that converts a byte array to a readable hex string (upper‑case) for
     * assertion messages. It does not affect the actual test logic.
     */
    private static String byteArrayToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("0x%02X", bytes[i]));
            if (i < bytes.length - 1)
                sb.append(", ");
        }
        sb.append(']');
        return sb.toString();
    }

    @Test
    void encodeEmptyArrayReturnsEmptyString() {
        byte[] empty = new byte[0];
        String result = StringUtilities.encode(empty);
        assertEquals("", result, "Encoding an empty byte array should yield an empty string");
    }

    @Test
    void encodeKnownValuesProduceExpectedHexString() {
        // 0x0F, 0xA0, 0x00, 0x7F  ->  "0fa0007f"
        byte[] data = new byte[] { 0x0F, (byte) 0xA0, 0x00, 0x7F };
        String expected = "0fa0007f";
        String actual = StringUtilities.encode(data);
        assertEquals(expected, actual);
    }

    @Test
    void encodeHandlesNegativeBytesCorrectly() {
        // In Java a byte is signed; -1 == 0xFF, -128 == 0x80, etc.
        byte[] data = new byte[] { (byte) 0xFF, (byte) 0x80, (byte) 0x01 };
        // Expected hex: "ff8001"
        String expected = "ff8001";
        String actual = StringUtilities.encode(data);
        assertEquals(expected, actual);
    }

    @Test
    void encodeDoesNotMutateInputArray() {
        byte[] original = new byte[] { 0x12, 0x34 };
        byte[] copy = original.clone();
        // Call the method under test
        StringUtilities.encode(original);
        // Verify the original content is unchanged
        assertArrayEquals(copy, original, "The encode method must not modify the input byte array");
    }

    @Test
    void encodeAllPossibleByteValues() {
        // Build an array containing every possible byte value (-128 .. 127)
        byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            allBytes[i] = (byte) i;
        }
        // Build the expected hex string manually
        StringBuilder expectedBuilder = new StringBuilder(512);
        for (byte b : allBytes) {
            int high = (b >> 4) & 0xF;
            int low = b & 0xF;
            expectedBuilder.append(Character.toLowerCase(Character.forDigit(high, 16)));
            expectedBuilder.append(Character.toLowerCase(Character.forDigit(low, 16)));
        }
        String expected = expectedBuilder.toString();
        String actual = StringUtilities.encode(allBytes);
        assertEquals(expected, actual, "Encoding all 256 byte values should produce the correct hex representation");
    }

    @Test
    void encodeNullArrayThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtilities.encode(null), "Passing a null byte array should result in a NullPointerException");
    }

    /**
     * Test a collection of simple inputs that are described in the JSON test case file.
     * Each row contains the byte array to encode and the expected lower‑case hex string.
     */
    @ParameterizedTest(name = "[{index}] encode({0}) → \"{1}\"")
    @MethodSource("basicAndNibbleCases")
    void encode_basicAndNibbleCases(byte[] input, String expected) {
        String actual = StringUtilities.encode(input);
        assertEquals(expected, actual, () -> "Encoding " + byteArrayToHex(input) + " should produce \"" + expected + "\"");
    }

    @ParameterizedTest(name = "[{index}] encodeSigned({0}) → \"{1}\"")
    @MethodSource("signedByteCases")
    void encode_signedByteHandling(byte[] input, String expected) {
        String actual = StringUtilities.encode(input);
        assertEquals(expected, actual, () -> "Encoding signed bytes " + byteArrayToHex(input) + " should yield \"" + expected + "\"");
    }

    @Test
    void encode_preservesOrderAcrossMultipleBytes() {
        byte[] data = new byte[] { 0x00, 0x0F, (byte) 0xFF };
        String expected = "000fff";
        assertEquals(expected, StringUtilities.encode(data));
    }

    @Test
    void encode_doesNotMutateInputArray() {
        byte[] original = new byte[] { 0x12, 0x34 };
        byte[] copy = original.clone();
        // Act
        StringUtilities.encode(original);
        // Assert – the original content is unchanged
        assertArrayEquals(copy, original, "The encode method must not modify the input byte array");
    }

    @Test
    void encode_largeArrayProducesCorrectHexString() {
        byte[] data = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
        String expected = "000102030405060708090a0b0c0d0e0f000102030405060708090a0b0c0d0e0f";
        assertEquals(expected, StringUtilities.encode(data));
    }

    @Test
    void encode_allPossibleByteValues() {
        // Build an array containing every possible byte value (-128 .. 127)
        byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            allBytes[i] = (byte) i;
        }
        // Build the expected hex string using the same algorithm the method uses
        StringBuilder expectedBuilder = new StringBuilder(512);
        for (byte b : allBytes) {
            int high = (b >> 4) & 0xF;
            int low = b & 0xF;
            expectedBuilder.append(Character.toLowerCase(Character.forDigit(high, 16)));
            expectedBuilder.append(Character.toLowerCase(Character.forDigit(low, 16)));
        }
        String expected = expectedBuilder.toString();
        assertEquals(expected, StringUtilities.encode(allBytes), "Encoding all 256 byte values should produce the correct hex representation");
    }

    @Test
    void encode_nullArrayThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtilities.encode(null), "Passing a null byte array should result in a NullPointerException");
    }
}
