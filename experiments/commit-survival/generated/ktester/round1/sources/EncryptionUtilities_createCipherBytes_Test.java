package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createCipherBytes_Test {

    private static Stream<Arguments> genericSuccessProvider() {
        return Stream.of(// typical case
        Arguments.of("password123", 128), // empty key
        Arguments.of("", 64), // minimum non‑zero bits (1 byte)
        Arguments.of("a", 8), // Unicode characters
        Arguments.of("特殊字符", 32), // zero bits – should return empty array
        Arguments.of("testKey", 0), // non‑standard but divisible by 8
        Arguments.of("testKey", 24));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// null key – NPE from key.getBytes(...)
        Arguments.of(null, 128, NullPointerException.class), // request more bytes than the MD5 hex string provides
        Arguments.of("overflowKey", 264, StringIndexOutOfBoundsException.class), // negative bits leads to negative substring index
        Arguments.of("negativeTest", -8, StringIndexOutOfBoundsException.class));
    }

    private static Stream<Arguments> exactVerificationProvider() {
        return Stream.of(// first 2 UTF‑8 bytes of "098f..." → '0' = 0x30, '9' = 0x39
        Arguments.of("test", 16, new byte[] { 0x30, 0x39 }), // first 4 UTF‑8 bytes → "098f" → 0x30,0x39,0x38,0x66
        Arguments.of("test", 32, new byte[] { 0x30, 0x39, 0x38, 0x66 }));
    }

    @Test
    void createCipherBytes_128Bits_ReturnsExpectedBytes() {
        // given
        String key = "mySecretKey";
        // 16 bytes
        int bitsNeeded = 128;
        // when
        byte[] actual = EncryptionUtilities.createCipherBytes(key, bitsNeeded);
        // then
        String md5 = EncryptionUtilities.calculateMD5Hash(key.getBytes(StandardCharsets.UTF_8));
        byte[] expected = md5.substring(0, bitsNeeded / 8).getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, actual, "The cipher bytes for 128 bits should match the first 16 characters of the MD5 hash");
    }

    @Test
    void createCipherBytes_64Bits_ReturnsExpectedBytes() {
        // given
        String key = "anotherKey";
        // 8 bytes
        int bitsNeeded = 64;
        // when
        byte[] actual = EncryptionUtilities.createCipherBytes(key, bitsNeeded);
        // then
        String md5 = EncryptionUtilities.calculateMD5Hash(key.getBytes(StandardCharsets.UTF_8));
        byte[] expected = md5.substring(0, bitsNeeded / 8).getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, actual, "The cipher bytes for 64 bits should match the first 8 characters of the MD5 hash");
    }

    @Test
    void createCipherBytes_BitsNotDivisibleByEight_ThrowsStringIndexOutOfBoundsException() {
        // given
        String key = "sampleKey";
        // not a multiple of 8, will cause substring length 16.25 -> truncates to 16, but Java substring expects int
        int bitsNeeded = 130;
        // when / then
        assertThrows(StringIndexOutOfBoundsException.class, () -> {
            EncryptionUtilities.createCipherBytes(key, bitsNeeded);
        });
    }

    @Test
    void createCipherBytes_BitsExceedHashLength_ThrowsStringIndexOutOfBoundsException() {
        // given
        String key = "short";
        // requires 32 characters, MD5 hash only provides 32, substring(0,32) is okay, but 256/8 = 32, still okay.
        int bitsNeeded = 256;
        // To actually exceed, request 288 bits (36 chars)
        int excessiveBits = 288;
        // when / then
        assertThrows(StringIndexOutOfBoundsException.class, () -> {
            EncryptionUtilities.createCipherBytes(key, excessiveBits);
        });
    }

    /**
     * Verifies that the method returns the exact slice of the MD5 hash for a variety of
     * keys and bit sizes. Expected bytes are derived at runtime using the same MD5
     * helper, ensuring the test stays in sync with the implementation.
     */
    @ParameterizedTest(name = "[{index}] key=''{0}'', bitsNeeded={1}")
    @MethodSource("genericSuccessProvider")
    void createCipherBytes_genericSuccess(String key, int bitsNeeded) {
        // when
        byte[] actual = EncryptionUtilities.createCipherBytes(key, bitsNeeded);
        // then – compute the expected value using the public MD5 helper
        String md5 = EncryptionUtilities.calculateMD5Hash(key.getBytes(StandardCharsets.UTF_8));
        byte[] expected = md5.substring(0, bitsNeeded / 8).getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, actual, () -> String.format("Cipher bytes for key='%s', bits=%d did not match expected slice", key, bitsNeeded));
    }

    @ParameterizedTest(name = "[{index}] key=''{0}'', bitsNeeded={1} => expect {2}")
    @MethodSource("exceptionProvider")
    void createCipherBytes_expectedExceptions(String key, int bitsNeeded, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> EncryptionUtilities.createCipherBytes(key, bitsNeeded));
    }

    /**
     * Checks that the method returns the exact UTF‑8 byte values for known MD5 output.
     * The MD5 of "test" is {@code 098f6bcd4621d373cade4e832627b4f6}.
     */
    @ParameterizedTest(name = "[{index}] key=''{0}'', bitsNeeded={1} => expected {2}")
    @MethodSource("exactVerificationProvider")
    void createCipherBytes_exactVerification(String key, int bitsNeeded, byte[] expected) {
        byte[] actual = EncryptionUtilities.createCipherBytes(key, bitsNeeded);
        assertArrayEquals(expected, actual, () -> String.format("Exact verification failed for key='%s', bits=%d", key, bitsNeeded));
    }
}
