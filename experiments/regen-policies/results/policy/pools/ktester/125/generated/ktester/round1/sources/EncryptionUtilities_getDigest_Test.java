package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.MessageDigest;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getDigest_Test {

    static Stream<String> validAlgorithmProvider() {
        return Stream.of("MD5", "SHA-1", "SHA-256", "SHA-512");
    }

    static Stream<String> invalidAlgorithmProvider() {
        return Stream.of(// not available in default JDK
        "SHA3-256", // lower‑case is not recognised
        "md5", // trailing whitespace
        "MD5 ", // leading whitespace
        "   SHA-1", // empty string
        "", // null argument
        null, // completely unknown
        "UNKNOWN-ALGO", // internal space
        "SHA 256", // space after hyphen
        "SHA- 256", // newline at end
        "SHA-256\n", // tab at end
        "SHA-256\t");
    }

    static Stream<String> causeVerificationProvider() {
        return Stream.of("SHA3-256", // empty string – still results in NoSuchAlgorithmException per JDK spec
        "");
    }

    @Test
    @DisplayName("getDigest returns a MessageDigest for a supported algorithm")
    void getDigest_returnsDigestForSupportedAlgorithm() {
        // given
        String algorithm = "SHA-256";
        // when
        MessageDigest md = EncryptionUtilities.getDigest(algorithm);
        // then
        assertNotNull(md, "MessageDigest should not be null");
        assertEquals(algorithm, md.getAlgorithm(), "Returned MessageDigest should be for the requested algorithm");
    }

    @Test
    @DisplayName("getDigest throws IllegalArgumentException for an unknown algorithm")
    void getDigest_throwsIllegalArgumentExceptionForUnknownAlgorithm() {
        // given
        String unknownAlgorithm = "NON-EXISTENT-ALG";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.getDigest(unknownAlgorithm));
        // The exception message should contain the algorithm name
        assertTrue(ex.getMessage().contains(unknownAlgorithm), "Exception message should contain the requested algorithm name");
    }

    @Test
    @DisplayName("getDigest works with algorithm names used by helper methods")
    void getDigest_compatibleWithHelperMethods() {
        // given
        String[] helperAlgorithms = { "MD5", "SHA-1", "SHA-256", "SHA-512" };
        for (String alg : helperAlgorithms) {
            // when
            MessageDigest md = EncryptionUtilities.getDigest(alg);
            // then
            assertNotNull(md, "MessageDigest for " + alg + " should not be null");
            assertEquals(alg, md.getAlgorithm(), "MessageDigest algorithm should match the requested value");
        }
    }

    /* ----------------------------------------------------------------------
     *  Parameterized tests for invalid algorithm names (message verification)
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] Invalid algorithm: \"{0}\"")
    @MethodSource("invalidAlgorithmProvider")
    @DisplayName("getDigest throws IllegalArgumentException for unsupported algorithms")
    void getDigest_throwsIllegalArgumentExceptionForUnsupportedAlgorithm(String algorithm) {
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.getDigest(algorithm), "Expected IllegalArgumentException for algorithm: " + algorithm);
        // The exception message must contain the supplied algorithm name
        String expectedMessageFragment = String.format("The requested MessageDigest (%s) does not exist", algorithm);
        assertTrue(ex.getMessage().contains(expectedMessageFragment), () -> "Exception message should contain: \"" + expectedMessageFragment + "\" but was: \"" + ex.getMessage() + "\"");
    }

    /* ----------------------------------------------------------------------
     *  Tests that the IllegalArgumentException wraps the original NoSuchAlgorithmException
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] Cause verification for algorithm: \"{0}\"")
    @MethodSource("causeVerificationProvider")
    @DisplayName("IllegalArgumentException should wrap NoSuchAlgorithmException")
    void getDigest_exceptionCauseIsNoSuchAlgorithmException(String algorithm) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.getDigest(algorithm));
        // Verify the cause type
        assertNotNull(ex.getCause(), "Cause should not be null");
        assertTrue(ex.getCause() instanceof NoSuchAlgorithmException, () -> "Cause should be NoSuchAlgorithmException but was: " + ex.getCause().getClass().getName());
        // Verify message still contains the algorithm name
        String expectedFragment = String.format("The requested MessageDigest (%s) does not exist", algorithm);
        assertTrue(ex.getMessage().contains(expectedFragment), () -> "Exception message should contain: \"" + expectedFragment + "\"");
    }

    /* ----------------------------------------------------------------------
     *  Additional explicit test for null argument (covers the JSON case)
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("getDigest throws IllegalArgumentException when digest argument is null")
    void getDigest_nullArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.getDigest(null));
        String expectedFragment = "The requested MessageDigest (null) does not exist";
        assertTrue(ex.getMessage().contains(expectedFragment), () -> "Exception message should contain: \"" + expectedFragment + "\"");
        assertNotNull(ex.getCause(), "Cause should not be null");
        assertTrue(ex.getCause() instanceof NoSuchAlgorithmException, () -> "Cause should be NoSuchAlgorithmException but was: " + ex.getCause().getClass().getName());
    }
}
