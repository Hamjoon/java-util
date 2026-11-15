package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateMD5Hash_Test {

    /**
     * Expected MD5 digests for the test data – computed with the JDK MessageDigest.
     */
    private static MessageDigest md5Digest;

    @BeforeAll
    static void setupBeforeAll() throws NoSuchAlgorithmException {
        // Initialise a MessageDigest instance that will be reused for expected‑value calculations.
        md5Digest = MessageDigest.getInstance("MD5");
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if needed (none in this case).
        md5Digest = null;
    }

    /**
     * Helper method to convert a byte array to a lower‑case hex string.
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Supplies the test data extracted from the JSON spec.
     */
    private static Stream<Arguments> md5TestCases() {
        return Stream.of(// Empty array – well‑known MD5 digest.
        Arguments.of(new byte[0], "d41d8cd98f00b204e9800998ecf8427e", "MD5 hash of an empty byte array should match the well‑known digest for an empty string."), // "abc"
        Arguments.of(new byte[] { 97, 98, 99 }, "900150983cd24fb0d6963f7d28e17f72", "MD5 hash of the ASCII bytes for \"abc\"; verifies correct handling of typical small input."), // Mixed signed values.
        Arguments.of(new byte[] { -1, 0, 127 }, "e0c094e867ef46c35d8a7a0d7bda71f2", "MD5 hash of a byte array containing negative, zero and positive values to ensure signed byte handling is correct."), // Japanese "こんにちは" UTF‑8 bytes (shown as signed decimals).
        Arguments.of(new byte[] { -30, -126, -84, -30, -126, -84, -30, -126, -84, -30, -126, -84 }, "9d735278cfbdb946834416adfb5aaf6c", "MD5 hash of UTF‑8 encoded Japanese greeting \"こんにちは\" (bytes shown as signed decimal)."), // 1 000 000 times 'a' (ASCII 97).  Build array lazily to avoid huge literal.
        Arguments.of(createRepeatedArray((byte) 97, 1_000_000), "7707d6ae4e027c70eea2a935c2296f21", "MD5 hash of a 1 000 000‑byte array consisting of the ASCII character 'a' repeated one million times; validates handling of large inputs and performance stability."), // Null input – should throw NPE.
        Arguments.of(null, "NullPointerException", "Passing a null reference should cause a NullPointerException because the underlying MessageDigest cannot process a null byte array."));
    }

    /**
     * Utility to create a byte array filled with a repeated value.
     */
    private static byte[] createRepeatedArray(byte value, int length) {
        byte[] arr = new byte[length];
        for (int i = 0; i < length; i++) {
            arr[i] = value;
        }
        return arr;
    }

    @Test
    void calculateMD5Hash_knownString() {
        // GIVEN
        String input = "hello world";
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        // Expected MD5 using a fresh MessageDigest instance.
        byte[] expectedBytes = md5Digest.digest(bytes);
        String expectedHex = toHex(expectedBytes);
        // Reset the digest for potential further use.
        md5Digest.reset();
        // WHEN
        String actualHex = EncryptionUtilities.calculateMD5Hash(bytes);
        // THEN
        assertEquals(expectedHex, actualHex, "MD5 hash of \"hello world\" should match the JDK computed value.");
    }

    @Test
    void calculateMD5Hash_emptyArray() {
        // GIVEN
        byte[] empty = new byte[0];
        byte[] expectedBytes = md5Digest.digest(empty);
        String expectedHex = toHex(expectedBytes);
        md5Digest.reset();
        // WHEN
        String actualHex = EncryptionUtilities.calculateMD5Hash(empty);
        // THEN
        assertEquals(expectedHex, actualHex, "MD5 hash of an empty byte array should match the JDK computed value.");
    }

    @Test
    void calculateMD5Hash_nullInput_throwsNPE() {
        // GIVEN
        byte[] nullBytes = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateMD5Hash(nullBytes), "Passing a null byte array should result in a NullPointerException.");
    }

    @Test
    void calculateMD5Hash_differentInputsProduceDifferentHashes() {
        // GIVEN
        byte[] first = "first".getBytes(StandardCharsets.UTF_8);
        byte[] second = "second".getBytes(StandardCharsets.UTF_8);
        // WHEN
        String hashFirst = EncryptionUtilities.calculateMD5Hash(first);
        String hashSecond = EncryptionUtilities.calculateMD5Hash(second);
        // THEN
        assertNotEquals(hashFirst, hashSecond, "Different inputs must produce different MD5 hash strings.");
    }

    /**
     * -----------------------------------------------------------------------
     *  Parameterised test covering the exhaustive list of cases supplied in the
     *  JSON description.  Each argument consists of:
     *   – a byte[] input (or null)
     *   – the expected hex string, or the expected exception class name
     *   – a description used as the display name for the test.
     *  ---------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("md5TestCases")
    void testCalculateMD5Hash(byte[] input, String expectedHexOrException, String description) {
        if ("NullPointerException".equals(expectedHexOrException)) {
            // Expect a NPE when the input is null.
            assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateMD5Hash(input), description);
        } else {
            // Normal case – compare the returned hex string with the expected value.
            String actual = EncryptionUtilities.calculateMD5Hash(input);
            assertEquals(expectedHexOrException, actual, description);
        }
    }
}
