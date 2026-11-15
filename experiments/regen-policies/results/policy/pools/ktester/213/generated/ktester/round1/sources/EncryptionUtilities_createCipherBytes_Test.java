package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createCipherBytes_Test {

    /**
     * Helper that computes the expected byte array for a given key and bit length.
     */
    private static byte[] expectedBytes(String key, int bitsNeeded) {
        // MD5 hash of the key (hex string) – we rely on the production method to compute it.
        String md5Hex = EncryptionUtilities.calculateMD5Hash(key.getBytes(StandardCharsets.UTF_8));
        String sub = md5Hex.substring(0, bitsNeeded / 8);
        return sub.getBytes(StandardCharsets.UTF_8);
    }

    private static Stream<Arguments> normalCaseProvider() {
        return Stream.of(// Group: Normal behavior
        Arguments.of("password", 128, "5f4dcc3b5aa765d6"), Arguments.of("password", 0, ""), // 20/8 = 2 chars
        Arguments.of("password", 20, "5f"), // Group: Additional normal behavior
        Arguments.of("test", 64, "098f6bcd"), Arguments.of("test", 8, "0"), Arguments.of("test", 24, "098"), Arguments.of("", 128, "d41d8cd98f00b204"), Arguments.of("password", 256, "5f4dcc3b5aa765d61d8327deb882cf99"));
    }

    private static Stream<Arguments> exceptionCaseProvider() {
        return Stream.of(// Group: Exception cases
        Arguments.of("password", 300, StringIndexOutOfBoundsException.class), Arguments.of("password", -8, StringIndexOutOfBoundsException.class), Arguments.of(null, 128, NullPointerException.class), // Group: Additional exception cases
        Arguments.of("password", 264, StringIndexOutOfBoundsException.class), Arguments.of("password", Integer.MAX_VALUE, StringIndexOutOfBoundsException.class), Arguments.of("password", Integer.MIN_VALUE, StringIndexOutOfBoundsException.class));
    }

    @Test
    void createCipherBytes_128Bits_KnownKey() {
        // Arrange
        String key = "password";
        // 16 bytes
        int bitsNeeded = 128;
        // Act
        byte[] result = EncryptionUtilities.createCipherBytes(key, bitsNeeded);
        // Assert
        byte[] expected = expectedBytes(key, bitsNeeded);
        assertArrayEquals(expected, result, "The cipher bytes for a 128‑bit request should match the first 16 characters of the MD5 hash.");
    }

    @Test
    void createCipherBytes_64Bits_KnownKey() {
        // Arrange
        String key = "password";
        // 8 bytes
        int bitsNeeded = 64;
        // Act
        byte[] result = EncryptionUtilities.createCipherBytes(key, bitsNeeded);
        // Assert
        byte[] expected = expectedBytes(key, bitsNeeded);
        assertArrayEquals(expected, result, "The cipher bytes for a 64‑bit request should match the first 8 characters of the MD5 hash.");
    }

    @Test
    void createCipherBytes_ZeroBits_ReturnsEmptyArray() {
        // Arrange
        String key = "anyKey";
        int bitsNeeded = 0;
        // Act
        byte[] result = EncryptionUtilities.createCipherBytes(key, bitsNeeded);
        // Assert
        assertNotNull(result, "Result should never be null.");
        assertEquals(0, result.length, "Zero bits requested should produce an empty byte array.");
    }

    @Test
    void createCipherBytes_EmptyKey() {
        // Arrange
        String key = "";
        int bitsNeeded = 128;
        // Act
        byte[] result = EncryptionUtilities.createCipherBytes(key, bitsNeeded);
        // Assert
        byte[] expected = expectedBytes(key, bitsNeeded);
        assertArrayEquals(expected, result, "Empty key should still produce a deterministic MD5‑based byte array.");
    }

    @Test
    void createCipherBytes_BitsGreaterThanHashLength_ThrowsException() {
        // Arrange
        String key = "short";
        // MD5 hex string length is 32 characters; bitsNeeded/8 > 32 will overflow.
        // 300/8 = 37 > 32
        int bitsNeeded = 300;
        // Act & Assert
        assertThrows(StringIndexOutOfBoundsException.class, () -> EncryptionUtilities.createCipherBytes(key, bitsNeeded), "Requesting more bits than the MD5 hex string length should throw an exception.");
    }

    /**
     * Normal cases taken from the JSON specification, including:
     * <ul>
     *   <li>Standard 128‑bit request</li>
     *   <li>Zero‑bit request</li>
     *   <li>Non‑multiple‑of‑8 bit lengths (e.g., 20 bits → 2 chars)</li>
     *   <li>Different keys and bit lengths</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] key=''{0}'', bitsNeeded={1} → expected=''{2}''")
    @MethodSource("normalCaseProvider")
    void createCipherBytes_normalCases(String key, int bitsNeeded, String expectedHex) {
        // Act
        byte[] result = EncryptionUtilities.createCipherBytes(key, bitsNeeded);
        // Assert – compare the UTF‑8 string representation of the result with the expected hex substring
        String actualHex = new String(result, StandardCharsets.UTF_8);
        assertEquals(expectedHex, actualHex, () -> String.format("Unexpected cipher bytes for key='%s', bitsNeeded=%d", key, bitsNeeded));
    }

    /**
     * Exception cases where the method is expected to throw an exception.
     * Covers:
     * <ul>
     *   <li>bitsNeeded larger than the MD5 hex length</li>
     *   <li>Negative bitsNeeded</li>
     *   <li>Null key</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] key=''{0}'', bitsNeeded={1} → throws {2}")
    @MethodSource("exceptionCaseProvider")
    void createCipherBytes_exceptionCases(String key, int bitsNeeded, Class<? extends Throwable> expectedException) {
        // Act & Assert
        assertThrows(expectedException, () -> EncryptionUtilities.createCipherBytes(key, bitsNeeded), () -> String.format("Expected %s for key='%s', bitsNeeded=%d", expectedException.getSimpleName(), key, bitsNeeded));
    }
}
