package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateSHA256Hash_Test {

    /**
     * Helper to convert a byte array to a lower‑case hex string (compatible with EncryptionUtilities).
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    /**
     * Supplies the normal‑input test cases extracted from the JSON description.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>The raw string that will be converted to UTF‑8 bytes.</li>
     *   <li>A short description (used for the test name).</li>
     * </ul>
     */
    private static Stream<Arguments> provideNormalInputs() {
        return Stream.of(Arguments.of("hello world", "Typical ASCII input – verifies known reference hash for \"hello world\""), Arguments.of("", "Empty byte array – SHA‑256 of empty input is a well‑known constant"), Arguments.of("çø∂é", "Non‑ASCII characters – confirms correct handling of UTF‑8 multibyte sequences"));
    }

    @Test
    void calculateSHA256Hash_KnownString() throws NoSuchAlgorithmException {
        // GIVEN
        String input = "The quick brown fox jumps over the lazy dog";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        // Expected SHA‑256 computed with standard JDK MessageDigest
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] expectedBytes = md.digest(inputBytes);
        String expectedHash = bytesToHex(expectedBytes);
        // WHEN
        String actualHash = EncryptionUtilities.calculateSHA256Hash(inputBytes);
        // THEN
        assertEquals(expectedHash, actualHash, "SHA‑256 hash of a known string should match the JDK calculation");
    }

    @Test
    void calculateSHA256Hash_EmptyArray() throws NoSuchAlgorithmException {
        // GIVEN
        byte[] empty = new byte[0];
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] expectedBytes = md.digest(empty);
        String expectedHash = bytesToHex(expectedBytes);
        // WHEN
        String actualHash = EncryptionUtilities.calculateSHA256Hash(empty);
        // THEN
        assertEquals(expectedHash, actualHash, "SHA‑256 hash of an empty byte array should match the JDK calculation");
    }

    @Test
    void calculateSHA256Hash_NullInput_ThrowsNPE() {
        // GIVEN
        byte[] nullBytes = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateSHA256Hash(nullBytes), "Passing null should result in a NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for normal (non‑error) inputs
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("provideNormalInputs")
    void calculateSHA256Hash_normalInputs(String input, String description) throws NoSuchAlgorithmException {
        // GIVEN
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        // Expected value calculated with the JDK MessageDigest (the source of truth)
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] expectedBytes = md.digest(inputBytes);
        String expectedHash = bytesToHex(expectedBytes);
        // WHEN
        String actualHash = EncryptionUtilities.calculateSHA256Hash(inputBytes);
        // THEN
        assertEquals(expectedHash, actualHash, () -> "SHA‑256 hash mismatch for input: \"" + description + "\"");
    }
}
