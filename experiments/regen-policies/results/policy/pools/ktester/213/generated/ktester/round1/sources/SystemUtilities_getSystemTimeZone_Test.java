package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.security.Permission;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_getSystemTimeZone_Test {

    /**
     * Holds the original value of the TZ environment variable (may be {@code null}).
     */
    private String originalTZ;

    /**
     * Holds the original SecurityManager so we can restore it after each test.
     */
    private SecurityManager originalSecurityManager;

    /**
     * SecurityManager that throws a {@link SecurityException} for any
     * {@code RuntimePermission} whose name starts with the supplied prefix.
     */
    private static class RestrictiveSecurityManager extends SecurityManager {

        private final String deniedPermissionPrefix;

        RestrictiveSecurityManager(String deniedPermissionPrefix) {
            this.deniedPermissionPrefix = deniedPermissionPrefix;
        }

        @Override
        public void checkPermission(Permission perm) {
            if (perm instanceof RuntimePermission && perm.getName().startsWith(deniedPermissionPrefix)) {
                throw new SecurityException("Access denied to permission: " + perm.getName());
            }
            // Allow everything else
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Preserve the original TZ value so we can restore it after each test
        originalTZ = System.getenv("TZ");
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Restore original TZ
        setEnv("TZ", originalTZ);
        // Restore original SecurityManager
        System.setSecurityManager(originalSecurityManager);
    }

    /**
     * Helper that uses reflection to modify the internal environment map.
     */
    @SuppressWarnings("unchecked")
    private static void setEnv(String key, String value) throws Exception {
        // Get the unmodifiable map that backs System.getenv()
        Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
        Field theUnmodifiableEnvironmentField = processEnvironmentClass.getDeclaredField("theUnmodifiableEnvironment");
        theUnmodifiableEnvironmentField.setAccessible(true);
        Map<String, String> unmodifiableEnv = (Map<String, String>) theUnmodifiableEnvironmentField.get(null);
        // The actual mutable map is stored in the private field "theEnvironment"
        Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
        theEnvironmentField.setAccessible(true);
        Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
        // Apply the change to both maps (the mutable one and the unmodifiable wrapper)
        if (value == null) {
            env.remove(key);
            unmodifiableEnv.remove(key);
        } else {
            env.put(key, value);
            unmodifiableEnv.put(key, value);
        }
    }

    @Test
    void getSystemTimeZone_usesTZEnvironmentVariableWhenPresent() throws Exception {
        // Arrange – set a known, valid time‑zone identifier
        setEnv("TZ", "UTC");
        // Act
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        // Assert – the returned TimeZone should match the one we set
        assertNotNull(tz, "TimeZone should never be null");
        assertEquals("UTC", tz.getID(), "Expected TimeZone ID to be 'UTC' when TZ env var is set to UTC");
    }

    @Test
    void getSystemTimeZone_fallsBackToDefaultWhenTZNotSet() throws Exception {
        // Arrange – ensure the TZ variable is absent
        setEnv("TZ", null);
        // Act
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        // Assert – should be the JVM default time zone
        TimeZone defaultTz = TimeZone.getDefault();
        assertNotNull(tz, "TimeZone should never be null");
        assertEquals(defaultTz.getID(), tz.getID(), "When TZ env var is not defined, getSystemTimeZone() must return the default TimeZone");
    }

    @Test
    void getSystemTimeZone_returnsDefaultWhenTZIsInvalid() throws Exception {
        // Arrange – set an invalid identifier; TimeZone.getTimeZone will return GMT
        setEnv("TZ", "THIS_IS_NOT_A_TIMEZONE");
        // Act
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        // Assert – the method should still return a TimeZone (GMT) rather than throwing
        assertNotNull(tz, "TimeZone should never be null even for an invalid TZ value");
        // TimeZone.getTimeZone returns a GMT zone for unknown IDs
        assertEquals("GMT", tz.getID(), "Invalid TZ values should result in the GMT fallback TimeZone");
    }

    @ParameterizedTest
    @ValueSource(strings = { "America/New_York", "Europe/Paris", "Asia/Tokyo" })
    void getSystemTimeZone_returnsZoneFromValidTZEnv(String tzId) throws Exception {
        // Arrange
        setEnv("TZ", tzId);
        // Act
        TimeZone result = SystemUtilities.getSystemTimeZone();
        // Assert
        assertNotNull(result, "Resulting TimeZone must not be null");
        assertEquals(tzId, result.getID(), () -> "Expected TimeZone ID to match the TZ env var value '" + tzId + "'");
    }

    @Test
    void getSystemTimeZone_withEmptyStringTZ_returnsGMT() throws Exception {
        setEnv("TZ", "");
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        assertNotNull(tz);
        assertEquals("GMT", tz.getID(), "Empty TZ should be interpreted as GMT by TimeZone.getTimeZone");
    }

    @Test
    void getSystemTimeZone_withBlankStringTZ_returnsGMT() throws Exception {
        setEnv("TZ", "   ");
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        assertNotNull(tz);
        assertEquals("GMT", tz.getID(), "Blank TZ should be interpreted as GMT by TimeZone.getTimeZone");
    }

    @Test
    void getSystemTimeZone_whenTimeZoneGetTimeZoneThrows_fallsBackToDefault() throws Exception {
        // Arrange – set any non‑null TZ value
        setEnv("TZ", "anyValue");
        // Mock static TimeZone#getTimeZone to throw a RuntimeException
        try (MockedStatic<TimeZone> mocked = Mockito.mockStatic(TimeZone.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> TimeZone.getTimeZone("anyValue")).thenThrow(new RuntimeException("simulated failure"));
            // Act
            TimeZone result = SystemUtilities.getSystemTimeZone();
            // Assert – should fall back to the default zone
            assertNotNull(result);
            assertEquals(TimeZone.getDefault().getID(), result.getID(), "When TimeZone.getTimeZone throws, method should return the default TimeZone");
        }
    }

    @Test
    void getSystemTimeZone_whenSecurityManagerDeniesEnvAccess_returnsDefault() throws Exception {
        // Install a SecurityManager that denies getenv.* permissions
        System.setSecurityManager(new RestrictiveSecurityManager("getenv."));
        // Even if TZ is set, getenv will throw; method should fall back to default
        // value is irrelevant because getenv will be blocked
        setEnv("TZ", "Europe/London");
        TimeZone result = SystemUtilities.getSystemTimeZone();
        assertNotNull(result);
        assertEquals(TimeZone.getDefault().getID(), result.getID(), "When getenv is blocked, method must return the default TimeZone");
    }

    @Test
    void getSystemTimeZone_whenDefaultTimeZoneThrows_propagatesSecurityException() {
        // Arrange – mock TimeZone.getDefault to throw SecurityException
        try (MockedStatic<TimeZone> mocked = Mockito.mockStatic(TimeZone.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(TimeZone::getDefault).thenThrow(new SecurityException("default timezone denied"));
            // Act & Assert – the exception should propagate
            SecurityException ex = assertThrows(SecurityException.class, SystemUtilities::getSystemTimeZone, "Expected SecurityException to propagate from TimeZone.getDefault()");
            assertEquals("default timezone denied", ex.getMessage());
        }
    }
}
