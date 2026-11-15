package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.security.MessageDigest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecurityException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getDigest_Test {

    private static Stream<Arguments> invalidAlgorithmProvider() {
        return Stream.of(// algorithm, expected cause class
        Arguments.of("NON_EXISTENT_ALGO", NoSuchAlgorithmException.class), Arguments.of("", NoSuchAlgorithmException.class), Arguments.of(null, NullPointerException.class), Arguments.of("   ", NoSuchAlgorithmException.class), Arguments.of("   MD5   ", NoSuchAlgorithmException.class), Arguments.of("md5", NoSuchAlgorithmException.class), Arguments.of("SHA-256\n", NoSuchAlgorithmException.class), Arguments.of("MD5\u0000", NoSuchAlgorithmException.class), // non‑breaking space
        Arguments.of("\u00A0MD5", NoSuchAlgorithmException.class));
    }

    @Test
    @DisplayName("getDigest returns a MessageDigest for a supported algorithm")
    void getDigest_validAlgorithm_returnsDigest() {
        // GIVEN
        String algorithm = "MD5";
        // WHEN
        MessageDigest digest = EncryptionUtilities.getDigest(algorithm);
        // THEN
        assertNotNull(digest, "Digest should not be null for a valid algorithm");
        assertEquals(algorithm, digest.getAlgorithm(), "Returned MessageDigest should be configured for the requested algorithm");
    }

    @Test
    @DisplayName("getDigest throws IllegalArgumentException for an unknown algorithm")
    void getDigest_invalidAlgorithm_throwsIllegalArgumentException() {
        // GIVEN
        String unknownAlgorithm = "NON-EXISTENT-ALG";
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.getDigest(unknownAlgorithm), "Expected IllegalArgumentException for an unsupported algorithm");
        // Verify that the exception message contains the problematic algorithm name
        assertTrue(ex.getMessage().contains(unknownAlgorithm), "Exception message should mention the unknown algorithm");
        // The cause should be a NoSuchAlgorithmException
        assertNotNull(ex.getCause(), "Cause of IllegalArgumentException should not be null");
        assertTrue(ex.getCause() instanceof java.security.NoSuchAlgorithmException, "Cause should be NoSuchAlgorithmException");
    }

    @ParameterizedTest(name = "Valid algorithm ''{0}'' should return a MessageDigest")
    @ValueSource(strings = { "MD5", "SHA-256", "SHA-512", "SHA-1", "MD2", // SHA‑3 is available on recent JDKs; if not, the test will fail and indicate lack of support.
    "SHA3-256" })
    void getDigest_supportedAlgorithms_returnDigest(String algorithm) {
        // WHEN
        MessageDigest digest = EncryptionUtilities.getDigest(algorithm);
        // THEN
        assertNotNull(digest, "Digest must not be null for algorithm: " + algorithm);
        assertEquals(algorithm, digest.getAlgorithm(), "Digest algorithm should match the requested one");
    }

    @ParameterizedTest(name = "Invalid algorithm ''{0}'' should throw IllegalArgumentException")
    @MethodSource("invalidAlgorithmProvider")
    void getDigest_invalidOrMalformedAlgorithms_throwIllegalArgumentException(String algorithm, Class<? extends Throwable> expectedCause) {
        // WHEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.getDigest(algorithm), "Expected IllegalArgumentException for algorithm: " + algorithm);
        // THEN – message must contain the supplied algorithm (or the literal "null")
        String expectedInMessage = String.valueOf(algorithm);
        assertTrue(ex.getMessage().contains(expectedInMessage), () -> "Exception message should contain the algorithm value [" + expectedInMessage + "]");
        // AND – the cause must be of the expected type
        assertNotNull(ex.getCause(), "Cause of IllegalArgumentException must not be null for algorithm: " + algorithm);
        assertTrue(expectedCause.isInstance(ex.getCause()), () -> "Expected cause to be a " + expectedCause.getSimpleName() + " but was " + ex.getCause().getClass().getSimpleName());
    }

    @Test
    @DisplayName("getDigest propagates SecurityException when a SecurityManager denies access")
    void getDigest_securityManagerDenial_propagatesSecurityException() {
        // Preserve the original SecurityManager so we can restore it after the test
        SecurityManager original = System.getSecurityManager();
        // Install a SecurityManager that denies any MessageDigest permission
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(java.security.Permission perm) {
                // Allow everything except MessageDigest accesses
                if (perm instanceof java.security.SecurityPermission && perm.getName().startsWith("getMessageDigest")) {
                    throw new SecurityException("Access to MessageDigest algorithm denied");
                }
                // otherwise allow
            }

            @Override
            public void checkPermission(java.security.Permission perm, Object context) {
                checkPermission(perm);
            }
        });
        try {
            // The call should now throw a SecurityException that is NOT wrapped
            SecurityException se = assertThrows(SecurityException.class, () -> EncryptionUtilities.getDigest("MD5"), "Expected SecurityException when SecurityManager denies MessageDigest access");
            assertEquals("Access to MessageDigest algorithm denied", se.getMessage(), "SecurityException message should match the one thrown by the custom SecurityManager");
        } finally {
            // Restore the original SecurityManager to avoid side‑effects on other tests
            System.setSecurityManager(original);
        }
    }
}
