package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createCipherBytes_Test {

    /**
     * Helper that returns the MD5 hash of "password" in lower‑case hex.
     * The value is taken from a known reference: 5f4dcc3b5aa765d61d8327deb882cf99
     */
    private static final String PASSWORD_MD5 = "5f4dcc3b5aa765d61d8327deb882cf99";

    /**
     * Simple data holder for the parameterized test.
     */
    private static final class TestCase {

        final String key;

        final int bitsNeeded;

        // hex string of expected bytes or exception name
        final String expected;

        final String description;

        TestCase(String key, int bitsNeeded, String expected, String description) {
            this.key = key;
            this.bitsNeeded = bitsNeeded;
            this.expected = expected;
            this.description = description;
        }
    }

    /**
     * Converts a hex string (e.g., "3063") to a byte array of ASCII characters.
     */
    private static byte[] hexStringToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            out[i] = (byte) ((hi << 4) + lo);
        }
        return out;
    }

    /* ----------------------- Normal Cases ----------------------- */
    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(new TestCase("a", 8, "30", "MD5 hash of \"a\" is \"0cc175b9c0f1b6a831c399e269772661\". bitsNeeded/8 = 1, so the method returns the first character \"0\" as UTF‑8 bytes (0x30)."), new TestCase("a", 16, "3063", "bitsNeeded/8 = 2, first two characters of the MD5 hash are \"0c\" → bytes 0x30 0x63."), new TestCase("a", 128, "3063633137356239633066316236613833316333393965323639373732363631", "bitsNeeded/8 = 16, first 16 characters of the MD5 hash are \"0cc175b9c0f1b6a8\" → corresponding UTF‑8 byte sequence.")).map(tc -> Arguments.of(tc));
    }

    /* ----------------------- Edge Cases ----------------------- */
    private static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(new TestCase("a", 0, "", "bitsNeeded/8 = 0, substring length 0 → empty byte array."), new TestCase("a", 12, "30", "bitsNeeded not a multiple of 8; integer division yields 1, same result as bitsNeeded = 8."), new TestCase("a", 256, "3063633137356239633066316236613833316333393965323639373732363631", "bitsNeeded/8 = 32, which equals the full length of the MD5 hex string; returns all 32 characters as bytes."), new TestCase("a", 264, "StringIndexOutOfBoundsException", "bitsNeeded/8 = 33, exceeds the 32‑character MD5 string → substring throws StringIndexOutOfBoundsException.")).map(Arguments::of);
    }

    /* ----------------------- Error Cases ----------------------- */
    private static Stream<Arguments> errorCasesProvider() {
        return Stream.of(new TestCase(null, 8, "NullPointerException", "Passing a null key causes key.getBytes(...) to throw NullPointerException."), new TestCase("a", -8, "StringIndexOutOfBoundsException", "Negative bitsNeeded leads to negative length for substring (0, -1) → throws StringIndexOutOfBoundsException.")).map(Arguments::of);
    }

    /* ----------------------- Additional Normal Cases ----------------------- */
    private static Stream<Arguments> additionalNormalCasesProvider() {
        return Stream.of(new TestCase("", 8, "64", "Empty key produces MD5 hash \"d41d8cd98f00b204e9800998ecf8427e\"; bitsNeeded/8 = 1, returns first character \"d\" (0x64)."), new TestCase("", 24, "643431", "Empty key, bitsNeeded/8 = 3, returns first three characters \"d41\" → bytes 0x64 0x34 0x31."), new TestCase("ABC", 24, "393032", "Key \"ABC\" MD5 = \"902fbdd2b1df0c4f70b4a5d23525e932\"; bitsNeeded/8 = 3, returns \"902\" → bytes 0x39 0x30 0x32."), new TestCase("ABC", 31, "393032", "bitsNeeded not a multiple of 8; integer division yields 3, same result as bitsNeeded = 24."), new TestCase("ABC", 32, "39303266", "bitsNeeded/8 = 4, returns first four characters \"902f\" → bytes 0x39 0x30 0x32 0x66."), new TestCase("ABC", 200, "39303266626464326231646630633466373062346135643233", "bitsNeeded/8 = 25, returns first 25 characters of MD5 hash; hex representation of those 25 ASCII bytes."), new TestCase("ABC", 248, "39303266626464326231646630633466373062346135643233353235653933", "bitsNeeded/8 = 31, returns first 31 characters of MD5 hash (all but the final \"2\")."), new TestCase("ABC", 256, "3930326662646432623164663063346637306234613564323335323565393332", "bitsNeeded/8 = 32, returns the full 32‑character MD5 hash as UTF‑8 bytes."), new TestCase("a", 9, "30", "bitsNeeded not a multiple of 8; integer division yields 1, same as bitsNeeded = 8."), new TestCase("a", 5, "", "bitsNeeded/8 = 0, substring length 0 → empty byte array.")).map(Arguments::of);
    }

    /* ----------------------- Additional Edge Cases ----------------------- */
    private static Stream<Arguments> additionalEdgeCasesProvider() {
        return Stream.of(new TestCase("a", 7, "", "bitsNeeded less than one byte; integer division yields 0, resulting in empty array."), new TestCase("a", 255, "StringIndexOutOfBoundsException", "bitsNeeded/8 = 31, exceeds MD5 length (32) by 1 → substring throws exception.")).map(Arguments::of);
    }

    /* ----------------------- Additional Error Cases ----------------------- */
    private static Stream<Arguments> additionalErrorCasesProvider() {
        return Stream.of(new TestCase("a", -1, "StringIndexOutOfBoundsException", "Negative bitsNeeded leads to negative substring length, causing StringIndexOutOfBoundsException."), new TestCase("a", Integer.MAX_VALUE, "StringIndexOutOfBoundsException", "bitsNeeded is Integer.MAX_VALUE; bitsNeeded/8 is far larger than MD5 length, causing substring to throw StringIndexOutOfBoundsException."), new TestCase("a", Integer.MIN_VALUE, "StringIndexOutOfBoundsException", "bitsNeeded is Integer.MIN_VALUE; bitsNeeded/8 = -268435456, a negative substring end index causes String.substring to throw StringIndexOutOfBoundsException.")).map(Arguments::of);
    }

    @Test
    void createCipherBytes_128Bits_ReturnsFirst16Chars() {
        // Arrange
        String key = "password";
        // 16 bytes
        int bitsNeeded = 128;
        // Act
        byte[] result = EncryptionUtilities.createCipherBytes(key, bitsNeeded);
        // Assert
        byte[] expected = PASSWORD_MD5.substring(0, bitsNeeded / 8).getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, result, "The returned byte array should contain the first 16 characters of the MD5 hash.");
    }

    @Test
    void createCipherBytes_64Bits_ReturnsFirst8Chars() {
        // Arrange
        String key = "password";
        // 8 bytes
        int bitsNeeded = 64;
        // Act
        byte[] result = EncryptionUtilities.createCipherBytes(key, bitsNeeded);
        // Assert
        byte[] expected = PASSWORD_MD5.substring(0, bitsNeeded / 8).getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, result, "The returned byte array should contain the first 8 characters of the MD5 hash.");
    }

    @Test
    void createCipherBytes_0Bits_ReturnsEmptyArray() {
        // Arrange
        String key = "anyKey";
        int bitsNeeded = 0;
        // Act
        byte[] result = EncryptionUtilities.createCipherBytes(key, bitsNeeded);
        // Assert
        assertNotNull(result, "Result should never be null.");
        assertEquals(0, result.length, "Zero bits requested should yield an empty byte array.");
    }

    @Test
    void createCipherBytes_NullKey_ThrowsNullPointerException() {
        // Arrange
        String key = null;
        int bitsNeeded = 128;
        // Act & Assert
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.createCipherBytes(key, bitsNeeded), "Passing a null key should result in a NullPointerException.");
    }

    @ParameterizedTest(name = "[Normal] {0.description}")
    @MethodSource("normalCasesProvider")
    void createCipherBytes_NormalCases(TestCase tc) {
        byte[] result = EncryptionUtilities.createCipherBytes(tc.key, tc.bitsNeeded);
        byte[] expected = hexStringToBytes(tc.expected);
        assertArrayEquals(expected, result, () -> "Failed for key='" + tc.key + "', bitsNeeded=" + tc.bitsNeeded);
    }

    @ParameterizedTest(name = "[Edge] {0.description}")
    @MethodSource("edgeCasesProvider")
    void createCipherBytes_EdgeCases(TestCase tc) {
        if ("StringIndexOutOfBoundsException".equals(tc.expected)) {
            assertThrows(StringIndexOutOfBoundsException.class, () -> EncryptionUtilities.createCipherBytes(tc.key, tc.bitsNeeded), () -> "Expected StringIndexOutOfBoundsException for key='" + tc.key + "', bitsNeeded=" + tc.bitsNeeded);
        } else {
            byte[] result = EncryptionUtilities.createCipherBytes(tc.key, tc.bitsNeeded);
            byte[] expected = hexStringToBytes(tc.expected);
            assertArrayEquals(expected, result, () -> "Failed for key='" + tc.key + "', bitsNeeded=" + tc.bitsNeeded);
        }
    }

    @ParameterizedTest(name = "[Error] {0.description}")
    @MethodSource("errorCasesProvider")
    void createCipherBytes_ErrorCases(TestCase tc) {
        if ("NullPointerException".equals(tc.expected)) {
            assertThrows(NullPointerException.class, () -> EncryptionUtilities.createCipherBytes(tc.key, tc.bitsNeeded), () -> "Expected NullPointerException for null key.");
        } else if ("StringIndexOutOfBoundsException".equals(tc.expected)) {
            assertThrows(StringIndexOutOfBoundsException.class, () -> EncryptionUtilities.createCipherBytes(tc.key, tc.bitsNeeded), () -> "Expected StringIndexOutOfBoundsException for bitsNeeded=" + tc.bitsNeeded);
        } else {
            fail("Unexpected expected value: " + tc.expected);
        }
    }

    @ParameterizedTest(name = "[Additional Normal] {0.description}")
    @MethodSource("additionalNormalCasesProvider")
    void createCipherBytes_AdditionalNormalCases(TestCase tc) {
        byte[] result = EncryptionUtilities.createCipherBytes(tc.key, tc.bitsNeeded);
        byte[] expected = hexStringToBytes(tc.expected);
        assertArrayEquals(expected, result, () -> "Failed for key='" + tc.key + "', bitsNeeded=" + tc.bitsNeeded);
    }

    @ParameterizedTest(name = "[Additional Edge] {0.description}")
    @MethodSource("additionalEdgeCasesProvider")
    void createCipherBytes_AdditionalEdgeCases(TestCase tc) {
        if ("StringIndexOutOfBoundsException".equals(tc.expected)) {
            assertThrows(StringIndexOutOfBoundsException.class, () -> EncryptionUtilities.createCipherBytes(tc.key, tc.bitsNeeded), () -> "Expected StringIndexOutOfBoundsException for bitsNeeded=" + tc.bitsNeeded);
        } else {
            byte[] result = EncryptionUtilities.createCipherBytes(tc.key, tc.bitsNeeded);
            byte[] expected = hexStringToBytes(tc.expected);
            assertArrayEquals(expected, result, () -> "Failed for key='" + tc.key + "', bitsNeeded=" + tc.bitsNeeded);
        }
    }

    @ParameterizedTest(name = "[Additional Error] {0.description}")
    @MethodSource("additionalErrorCasesProvider")
    void createCipherBytes_AdditionalErrorCases(TestCase tc) {
        assertThrows(StringIndexOutOfBoundsException.class, () -> EncryptionUtilities.createCipherBytes(tc.key, tc.bitsNeeded), () -> "Expected StringIndexOutOfBoundsException for bitsNeeded=" + tc.bitsNeeded);
    }
}
