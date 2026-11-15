package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.security.MessageDigest;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getDigest_Test {

    static Stream<String> validAlgorithmsProvider() {
        return Stream.of("MD5", "SHA-256", "SHA-512");
    }

    static Stream<String> invalidAlgorithmsProvider() {
        return Stream.of(// completely unknown name
        "UNKNOWN-ALG", // empty string
        "", // whitespace only
        "   ", // wrong case – MessageDigest is case‑sensitive
        "md5", // excessively long bogus name
        "A.................................................................................................................................................................................................................................................................................................................................................................................................");
    }

    @Test
    @DisplayName("getDigest returns a MessageDigest for a supported algorithm")
    void shouldReturnMessageDigestWhenAlgorithmIsSupported() {
        // given
        String algorithm = "SHA-256";
        // when
        MessageDigest digest = EncryptionUtilities.getDigest(algorithm);
        // then
        assertNotNull(digest, "MessageDigest should not be null for a valid algorithm");
        assertEquals(algorithm, digest.getAlgorithm(), "Returned MessageDigest should be for the requested algorithm");
    }

    @Test
    @DisplayName("getDigest throws IllegalArgumentException for an unknown algorithm")
    void shouldThrowIllegalArgumentExceptionWhenAlgorithmIsUnsupported() {
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.getDigest(algorithm), "Expected IllegalArgumentException for an unknown algorithm");
        // verify that the exception message contains the exact input string
        assertTrue(ex.getMessage().contains(algorithm), () -> "Exception message should contain the algorithm name [" + algorithm + "]");
        // verify that the root cause is NoSuchAlgorithmException
        assertNotNull(ex.getCause(), "Cause should not be null");
        assertTrue(ex.getCause() instanceof NoSuchAlgorithmException, () -> "Cause should be NoSuchAlgorithmException, but was " + ex.getCause());
    }

    @Test
    @DisplayName("getDigest throws NullPointerException when null is passed")
    void shouldPropagateNullPointerExceptionWhenDigestIsNull() {
        // when / then
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.getDigest(null), "Passing null should result in NullPointerException from MessageDigest.getInstance");
    }

    @Test
    @DisplayName("Algorithm with surrounding whitespace is considered invalid")
    void shouldThrowIllegalArgumentExceptionWhenAlgorithmHasSurroundingWhitespace() {
        String algorithm = "  SHA-256  ";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.getDigest(algorithm), "Algorithm with surrounding whitespace should be rejected");
        assertTrue(ex.getMessage().contains(algorithm), "Exception message must contain the original (whitespace‑included) string");
        assertNotNull(ex.getCause(), "Cause should not be null");
        assertTrue(ex.getCause() instanceof NoSuchAlgorithmException, "Cause must be NoSuchAlgorithmException");
    }
}
