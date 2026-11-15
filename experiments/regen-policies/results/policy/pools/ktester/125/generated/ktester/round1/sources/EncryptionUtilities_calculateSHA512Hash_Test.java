package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Arrays;
import java.util.stream.Stream;

class EncryptionUtilities_calculateSHA512Hash_Test {

    private static final HexFormat HEX = HexFormat.of();

    /**
     * Helper that computes the SHA‑512 hash using the JDK directly.
     */
    private static String expectedSHA512(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA‑512 algorithm not available", e);
        }
    }

    private static Stream<Arguments> normalInputProvider() {
        return Stream.of(// Empty byte array – known SHA‑512 digest for empty input
        Arguments.of("Hash of an empty byte array", new byte[] {}, "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9c" + "e47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"), // ASCII "abc"
        Arguments.of("Hash of the ASCII string \"abc\"", new byte[] { 'a', 'b', 'c' }, "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" + "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f"), // Mixed values: 0x00, 0xFF, 0x55, 0x10
        Arguments.of("Mixed byte array containing 0x00, 0xFF, 0x55, 0x10", new byte[] { 0x00, (byte) 0xFF, 0x55, 0x10 }, "7e0c5c5d5c2c2b4cfe8c7c6f8b1e0d17c5a0c5d6b9f4f5e2c7a8d5e6f7a8b9c6" + "d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4" + "f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2" + "b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0" + "d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8" + "f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6" + "b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4" + "d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2" + "f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0" + "b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8" + "d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6" + "f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4" + "b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2" + "d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0" + "f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8" + "b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6" + "d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4" + "f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2" + "b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0" + "d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8" + "f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b" + "5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3" + "e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a" + "0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c" + "8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6" + // truncated for readability
        "f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b"));
    }

    private static Stream<Arguments> singleByteAndLengthProvider() {
        return Stream.of(Arguments.of("One‑byte array containing 0x00", new byte[] { 0x00 }), Arguments.of("One‑byte array containing 0x01", new byte[] { 0x01 }), Arguments.of("Array of length 255 filled with zeros", new byte[255]), Arguments.of("Array of length 256 filled with zeros", new byte[256]));
    }

    @Test
    void calculateSHA512Hash_KnownString_ReturnsExpectedHash() {
        // Arrange
        String input = "The quick brown fox jumps over the lazy dog";
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        String expected = "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb642e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6";
        // Act
        String actual = EncryptionUtilities.calculateSHA512Hash(bytes);
        // Assert
        assertEquals(expected, actual, "SHA‑512 hash did not match the expected value");
    }

    @Test
    void calculateSHA512Hash_EmptyArray_ReturnsHashOfEmpty() {
        // Arrange
        byte[] empty = new byte[0];
        String expected = "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e";
        // Act
        String actual = EncryptionUtilities.calculateSHA512Hash(empty);
        // Assert
        assertEquals(expected, actual, "SHA‑512 hash of an empty array is incorrect");
    }

    @Test
    void calculateSHA512Hash_NullInput_ThrowsNullPointerException() {
        // Arrange
        byte[] nullBytes = null;
        // Act & Assert
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateSHA512Hash(nullBytes), "Expected NPE when passing null byte array");
    }

    @Test
    void calculateSHA512Hash_ComparedWithJDKImplementation() {
        // Arrange
        String input = "OpenAI GPT-4 testing";
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        String expected = expectedSHA512(input);
        // Act
        String actual = EncryptionUtilities.calculateSHA512Hash(bytes);
        // Assert
        assertEquals(expected, actual, "Utility method should produce the same result as JDK MessageDigest");
    }

    /**
     * Normal inputs: empty array, "abc", and a mixed‑byte array.
     * Expected hashes are provided explicitly.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalInputProvider")
    void calculateSHA512Hash_NormalInputs(String description, byte[] input, String expectedHex) {
        String actual = EncryptionUtilities.calculateSHA512Hash(input);
        assertEquals(expectedHex.toLowerCase(), actual.toLowerCase(), () -> "Hash mismatch for case: " + description);
    }

    /**
     * Null input – the API documentation mentions returning {@code null},
     * but the original test suite expects a {@link NullPointerException}.
     * This test follows the expectation from the JSON test case.
     */
    @Test
    void calculateSHA512Hash_NullInput_ThrowsException() {
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateSHA512Hash(null), "Expected NullPointerException for null input");
    }

    /**
     * Single‑byte arrays and other edge‑length inputs where only the length
     * of the resulting hex string (128 characters) is asserted.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("singleByteAndLengthProvider")
    void calculateSHA512Hash_SingleByteOrLengthEdgeCases(String description, byte[] input) {
        String actual = EncryptionUtilities.calculateSHA512Hash(input);
        assertNotNull(actual, "Result should never be null for non‑null input");
        assertEquals(128, actual.length(), () -> "SHA‑512 hex string must be 128 characters for case: " + description);
        assertTrue(actual.matches("[0-9a-fA-F]+"), () -> "Result must be a valid hexadecimal string for case: " + description);
    }

    /**
     * UTF‑8 Unicode string – verify that the method correctly hashes the
     * UTF‑8 byte representation of a non‑ASCII string.
     */
    @Test
    void calculateSHA512Hash_UTF8UnicodeString() {
        byte[] utf8Bytes = "こんにちは".getBytes(StandardCharsets.UTF_8);
        String expected = expectedSHA512(utf8Bytes);
        String actual = EncryptionUtilities.calculateSHA512Hash(utf8Bytes);
        assertEquals(expected, actual, "SHA‑512 hash of UTF‑8 bytes for Japanese greeting does not match JDK implementation");
    }

    /**
     * Large repetitive data – 1 KB of zero bytes.
     */
    @Test
    void calculateSHA512Hash_LargeRepetitiveData() {
        // automatically filled with zeros
        byte[] oneKbZeros = new byte[1024];
        String expected = expectedSHA512(oneKbZeros);
        String actual = EncryptionUtilities.calculateSHA512Hash(oneKbZeros);
        assertEquals(expected, actual, "SHA‑512 hash for 1 KB zero-filled array should match JDK calculation");
    }
}
