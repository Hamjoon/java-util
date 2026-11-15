package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateMD5Hash_Test {

    /**
     * Provides a set of byte arrays together with their expected MD5 hex strings.
     */
    static Stream<Arguments> md5TestProvider() {
        return Stream.of(// ASCII string "hello"
        Arguments.of("hello".getBytes(StandardCharsets.UTF_8), "5d41402abc4b2a76b9719d911017c592"), // Classic pangram – longer UTF‑8 text
        Arguments.of("The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8), "9e107d9d372bb6826bd81d3542a419d6"), // Binary data (raw bytes)
        Arguments.of(new byte[] { (byte) 0xFF, (byte) 0x00, (byte) 0xAB, (byte) 0x7C }, "4c5a5c5b4b5e2c2c2e0d9e6c5f8e4d9b"));
    }

    @Test
    void calculateMD5Hash_knownString_returnsExpectedHash() {
        // given
        byte[] input = "hello".getBytes(StandardCharsets.UTF_8);
        String expected = "5d41402abc4b2a76b9719d911017c592";
        // when
        String actual = EncryptionUtilities.calculateMD5Hash(input);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void calculateMD5Hash_emptyArray_returnsDigestOfEmpty() {
        // given
        byte[] input = new byte[0];
        String expected = "d41d8cd98f00b204e9800998ecf8427e";
        // when
        String actual = EncryptionUtilities.calculateMD5Hash(input);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void calculateMD5Hash_nullInput_throwsNullPointerException() {
        // given
        byte[] input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateMD5Hash(input));
    }

    @ParameterizedTest(name = "MD5 hash of input #{index}")
    @MethodSource("md5TestProvider")
    void calculateMD5Hash_variousValidInputs(byte[] input, String expectedHash) {
        // when
        String actual = EncryptionUtilities.calculateMD5Hash(input);
        // then
        assertEquals(expectedHash, actual);
    }

    @Test
    void calculateMD5Hash_whenMD5AlgorithmMissing_throwsRuntimeException() {
        // Arrange: mock the static getMD5Digest() to throw NoSuchAlgorithmException
        try (MockedStatic<EncryptionUtilities> mockedStatic = Mockito.mockStatic(EncryptionUtilities.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(EncryptionUtilities::getMD5Digest).thenThrow(new NoSuchAlgorithmException("MD5 not available"));
            // Act & Assert: the wrapper method should propagate as an unchecked exception
            assertThrows(RuntimeException.class, () -> EncryptionUtilities.calculateMD5Hash(new byte[] { 1, 2, 3 }));
        }
    }
}
