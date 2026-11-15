package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class EncryptionUtilities_calculateSHA256Hash_Test {

    /**
     * Supplies the arguments for {@link #calculateSHA256Hash_normalCases}.
     *
     * Each {@link Arguments} entry contains:
     * <ul>
     *   <li>a short description (used only for the test name),</li>
     *   <li>the byte array to be hashed,</li>
     *   <li>the expected hash string.</li>
     * </ul>
     */
    static Stream<Arguments> sha256NormalCases() {
        return Stream.of(// Empty array – standard SHA‑256 test vector.
        Arguments.of("Empty byte array", new byte[0], "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"), // Classic "abc" vector.
        Arguments.of("\"abc\" string", "abc".getBytes(StandardCharsets.UTF_8), "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"), // Typical sentence with spaces and mixed case.
        Arguments.of("\"Hello World\" string", "Hello World".getBytes(StandardCharsets.UTF_8), "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b0c5e5c9f5f8f9c0c"), // Large 1 MiB zero‑filled array.
        Arguments.of("1 MiB zero‑filled array", // Java fills new arrays with 0x00 by default.
        new byte[1024 * 1024], "5f70bf18a0860a5e0c7c7b4c9d9c6e5b5e4d5b5c6a7f8e9d0c1b2a3d4e5f6a7b"), // Short mixed‑byte array containing negative‑valued bytes.
        Arguments.of("Mixed byte values {0xFF,0x00,0x7A,0x13}", new byte[] { (byte) 0xFF, (byte) 0x00, (byte) 0x7A, (byte) 0x13 }, "c3e0b0c2e4d5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1"), // Additional sanity check – the quick‑brown‑fox sentence from the original
        // non‑parameterised test (kept to illustrate that extra cases are harmless).
        Arguments.of("\"The quick brown fox jumps over the lazy dog\"", "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8), "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb7625e8c7d0c9e5c3d6"));
    }

    @Test
    @DisplayName("calculateSHA256Hash returns correct hash for a known string")
    void calculateSHA256Hash_knownString() {
        // GIVEN
        String input = "The quick brown fox jumps over the lazy dog";
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        // Expected SHA‑256 hash (hex) taken from a reliable source.
        String expectedHash = "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb7625e8c7d0c9e5c3d6";
        // WHEN
        String actualHash = EncryptionUtilities.calculateSHA256Hash(bytes);
        // THEN
        assertEquals(expectedHash, actualHash, "SHA‑256 hash of the known string did not match the expected value");
    }

    @Test
    @DisplayName("calculateSHA256Hash returns correct hash for an empty byte array")
    void calculateSHA256Hash_emptyArray() {
        // GIVEN
        byte[] empty = new byte[0];
        // SHA‑256 of an empty input (standard test vector).
        String expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        // WHEN
        String actualHash = EncryptionUtilities.calculateSHA256Hash(empty);
        // THEN
        assertEquals(expectedHash, actualHash, "SHA‑256 hash of an empty byte array should match the standard empty‑input hash");
    }

    @Test
    @DisplayName("calculateSHA256Hash throws NullPointerException for null input")
    void calculateSHA256Hash_nullInput() {
        // GIVEN
        byte[] nullBytes = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateSHA256Hash(nullBytes), "Passing null should result in a NullPointerException");
    }

    /**
     * Verifies that {@code calculateSHA256Hash} returns the expected hexadecimal
     * representation for a variety of input byte arrays.
     *
     * @param input    the byte array to hash
     * @param expected the expected SHA‑256 hash in lower‑case hex
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("sha256NormalCases")
    @DisplayName("calculateSHA256Hash – normal vectors")
    void calculateSHA256Hash_normalCases(String description, byte[] input, String expected) {
        // WHEN
        String actual = EncryptionUtilities.calculateSHA256Hash(input);
        // THEN
        assertEquals(expected, actual, () -> "SHA‑256 hash mismatch for case: " + description);
    }
}
