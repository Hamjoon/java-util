package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.Permission;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TestUtil_isReleaseMode_Test {

    /**
     * Holds the original value of the system property so we can restore it after each test.
     */
    private String originalPerformRelease;

    @BeforeEach
    void setupBeforeEach() {
        // Preserve the current value (may be null) before each test runs.
        originalPerformRelease = System.getProperty("performRelease");
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original value (or clear it if it was not set).
        if (originalPerformRelease == null) {
            System.clearProperty("performRelease");
        } else {
            System.setProperty("performRelease", originalPerformRelease);
        }
    }

    /**
     * Supplies a variety of property values together with the expected boolean result.
     */
    private static Stream<Arguments> providePropertyValues() {
        return Stream.of(// Default (property absent) – already covered but kept for completeness
        Arguments.of(null, false, "Property not defined → default false"), // Explicit true/false (different capitalisations)
        Arguments.of("true", true, "\"true\" (lowercase)"), Arguments.of("True", true, "\"True\" (mixed case)"), Arguments.of("TRUE", true, "\"TRUE\" (upper case)"), Arguments.of("false", false, "\"false\" (lowercase)"), Arguments.of("False", false, "\"False\" (mixed case)"), Arguments.of("FALSE", false, "\"FALSE\" (upper case)"), // Unrecognised literals
        Arguments.of("yes", false, "\"yes\" (unrelated)"), Arguments.of("", false, "empty string"), // Whitespace edge cases
        Arguments.of(" tRuE", false, "leading whitespace"), Arguments.of("true ", false, "trailing whitespace"), Arguments.of("TRUE\n", false, "newline after word"), // Numeric strings
        Arguments.of("1", false, "\"1\" numeric string"), Arguments.of("0", false, "\"0\" numeric string"), // Literal "null"
        Arguments.of("null", false, "\"null\" literal"), // Common boolean‑like word
        Arguments.of("on", false, "\"on\" word"));
    }

    @Test
    void testIsReleaseModeWhenPropertyIsTrue() {
        System.setProperty("performRelease", "true");
        assertTrue(TestUtil.isReleaseMode(), "isReleaseMode() should return true when performRelease property is set to \"true\"");
    }

    @Test
    void testIsReleaseModeWhenPropertyIsFalse() {
        System.setProperty("performRelease", "false");
        assertFalse(TestUtil.isReleaseMode(), "isReleaseMode() should return false when performRelease property is set to \"false\"");
    }

    @Test
    void testIsReleaseModeWhenPropertyIsAbsent() {
        System.clearProperty("performRelease");
        assertFalse(TestUtil.isReleaseMode(), "isReleaseMode() should return false when performRelease property is not defined");
    }

    @Test
    void testIsReleaseModeWhenPropertyIsInvalid() {
        System.setProperty("performRelease", "notABoolean");
        // Boolean.parseBoolean returns false for any value other than "true" (ignoring case)
        assertFalse(TestUtil.isReleaseMode(), "isReleaseMode() should return false when performRelease property cannot be parsed as true");
    }

    @ParameterizedTest(name = "[{index}] Property value: {0} → expected: {1} ({2})")
    @MethodSource("providePropertyValues")
    void testIsReleaseModeVariousValues(String propertyValue, boolean expectedResult, String description) {
        // Arrange: set or clear the system property according to the test data
        if (propertyValue == null) {
            System.clearProperty("performRelease");
        } else {
            System.setProperty("performRelease", propertyValue);
        }
        // Act & Assert
        assertEquals(expectedResult, TestUtil.isReleaseMode(), () -> "Failed for case: " + description);
    }

    @Test
    void testIsReleaseModeWhenSecurityManagerDeniesAccess() {
        // Preserve the current security manager so we can restore it after the test.
        SecurityManager originalSecurityManager = System.getSecurityManager();
        // Install a SecurityManager that throws SecurityException on any property access.
        SecurityManager throwingSM = new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPropertyAccess(String key) {
                if ("performRelease".equals(key)) {
                    throw new SecurityException("Access to system property 'performRelease' denied");
                }
            }
        };
        try {
            System.setSecurityManager(throwingSM);
            // The call should propagate the SecurityException.
            assertThrows(SecurityException.class, TestUtil::isReleaseMode, "isReleaseMode() should propagate SecurityException when property access is denied");
        } finally {
            // Restore the original security manager to avoid side‑effects on other tests.
            System.setSecurityManager(originalSecurityManager);
        }
    }
}
