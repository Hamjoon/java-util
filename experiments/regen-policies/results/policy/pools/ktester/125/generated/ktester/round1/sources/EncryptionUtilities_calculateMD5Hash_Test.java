package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.io.ByteArrayOutputStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateMD5Hash_Test {

    /**
     * Helper that computes the MD5 hash using the JDK directly.
     *
     * @param data the input bytes
     * @return the lower‑case hexadecimal MD5 digest
     */
    private static String md5Hex(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // MD5 is guaranteed to be present in any Java runtime.
            throw new AssertionError("MD5 algorithm not available", e);
        }
    }

    /**
     * Generates a byte array that contains every possible byte value (0‑255)
     * in order.
     *
     * @return a 256‑byte array with values 0,1,…,255
     */
    private static byte[] allByteValues() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        IntStream.range(0, 256).forEach(i -> baos.write(i));
        return baos.toByteArray();
    }

    /**
     * Provides normal‑case inputs together with the expected MD5 hex strings.
     *
     * The data mirrors the JSON test definition:
     * <ul>
     *   <li>empty array</li>
     *   <li>"hello" UTF‑8 bytes</li>
     *   <li>1 MiB zero‑filled array</li>
     *   <li>array containing every byte value 0‑255</li>
     * </ul>
     *
     * @return a stream of arguments for {@link #calculateMD5Hash_normalCases(byte[], String, String)}
     */
    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(Arguments.of(new byte[0], "d41d8cd98f00b204e9800998ecf8427e", "MD5 hash of an empty byte array should match the well‑known empty string hash."), Arguments.of("hello".getBytes(StandardCharsets.UTF_8), "5d41402abc4b2a76b9719d911017c592", "MD5 hash of the UTF‑8 bytes for the string \"hello\"."), Arguments.of(new byte[1024 * 1024], "7707d6ae4e027c70eea2a935c2296f21", "MD5 hash of a 1 MiB array filled with zeros; verifies handling of large inputs."), Arguments.of(allByteValues(), "e2c865db4162bed963bfaa9ef6ac18f0", "MD5 hash of a byte array containing every possible byte value (0‑255)."));
    }

    @Test
    @DisplayName("calculateMD5Hash returns correct MD5 for a known string")
    void calculateMD5Hash_knownString() {
        // GIVEN
        String input = "The quick brown fox jumps over the lazy dog";
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        String expected = md5Hex(bytes);
        // WHEN
        String actual = EncryptionUtilities.calculateMD5Hash(bytes);
        // THEN
        assertEquals(expected, actual, "MD5 hash of the known string should match the JDK implementation");
    }

    @Test
    @DisplayName("calculateMD5Hash returns correct MD5 for an empty byte array")
    void calculateMD5Hash_emptyArray() {
        // GIVEN
        byte[] empty = new byte[0];
        String expected = md5Hex(empty);
        // WHEN
        String actual = EncryptionUtilities.calculateMD5Hash(empty);
        // THEN
        assertEquals(expected, actual, "MD5 hash of an empty array should be the well‑known MD5 of zero length");
    }

    @Test
    @DisplayName("calculateMD5Hash handles null input by throwing NullPointerException")
    void calculateMD5Hash_nullInput() {
        // GIVEN
        byte[] nullBytes = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateMD5Hash(nullBytes), "Passing null should result in a NullPointerException");
    }

    @Test
    @DisplayName("calculateMD5Hash is deterministic for repeated calls with the same input")
    void calculateMD5Hash_deterministic() {
        // GIVEN
        byte[] data = "deterministic".getBytes(StandardCharsets.UTF_8);
        String first = EncryptionUtilities.calculateMD5Hash(data);
        // WHEN
        String second = EncryptionUtilities.calculateMD5Hash(data);
        String third = EncryptionUtilities.calculateMD5Hash(data);
        // THEN
        assertAll(() -> assertEquals(first, second, "Second call should match first call"), () -> assertEquals(first, third, "Third call should match first call"));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalCasesProvider")
    @DisplayName("calculateMD5Hash normal cases")
    void calculateMD5Hash_normalCases(byte[] input, String expected, String description) {
        // WHEN
        String actual = EncryptionUtilities.calculateMD5Hash(input);
        // THEN
        assertEquals(expected, actual, description);
    }

    @Test
    @DisplayName("calculateMD5Hash matches JDK implementation for a random payload")
    void calculateMD5Hash_matchesJDKImplementation() {
        // GIVEN
        byte[] random = new byte[256];
        for (int i = 0; i < random.length; i++) {
            // deterministic pseudo‑random data
            random[i] = (byte) (i * 31);
        }
        String expected = md5Hex(random);
        // WHEN
        String actual = EncryptionUtilities.calculateMD5Hash(random);
        // THEN
        assertEquals(expected, actual, "MD5 hash should be identical to JDK's MessageDigest output");
    }
}
