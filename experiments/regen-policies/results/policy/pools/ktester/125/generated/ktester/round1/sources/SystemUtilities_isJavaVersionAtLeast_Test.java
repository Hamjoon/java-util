package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Permission;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_isJavaVersionAtLeast_Test {

    private static int currentMajor;

    private static int currentMinor;

    private static String originalJavaVersion;

    @BeforeAll
    static void setupBeforeAll() {
        // Determine the JVM version once for all tests.
        String version = System.getProperty("java.version");
        String[] parts = version.split("\\.");
        currentMajor = Integer.parseInt(parts[0]);
        currentMinor = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;
        // Preserve the original value so we can restore it after each test.
        originalJavaVersion = SystemUtilities.JAVA_VERSION;
    }

    @BeforeEach
    void resetVersion() throws Exception {
        // Ensure each test starts with the original version (may be overridden inside the test).
        setJavaVersion(originalJavaVersion);
    }

    @AfterAll
    static void restoreOriginalVersion() throws Exception {
        setJavaVersion(originalJavaVersion);
    }

    /**
     * Helper that uses reflection to overwrite the {@code JAVA_VERSION} static final field.
     */
    private static void setJavaVersion(String value) throws Exception {
        Field field = SystemUtilities.class.getDeclaredField("JAVA_VERSION");
        field.setAccessible(true);
        // Remove the final modifier (required for Java 12+ as well).
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, value);
    }

    /**
     * Provides a stream of test arguments covering the JSON test cases.
     *
     * Each argument consists of:
     * <ul>
     *   <li>the mocked {@code java.version} string</li>
     *   <li>required major version</li>
     *   <li>required minor version</li>
     *   <li>expected result – either a {@link Boolean} or a {@link Class} of the expected exception</li>
     *   <li>a description for readability</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> versionCases() {
        return Stream.of(// major version greater than required
        arguments("11.0", 8, 0, true, "Current JAVA_VERSION is higher than required major version (8,0)."), // major equal, required minor less
        arguments("11.1", 11, 0, true, "Required minor (0) is less than actual minor (1)."), // exact match
        arguments("11.1", 11, 1, true, "Required major/minor exactly match runtime version."), // major equal, required minor greater
        arguments("11.1", 11, 2, false, "Required minor (2) is greater than actual minor (1)."), // required major greater than runtime major
        arguments("11.0", 12, 0, false, "Required major (12) exceeds runtime major (11)."), // version string without minor component – match
        arguments("9", 9, 0, true, "JAVA_VERSION = \"9\" matches required (9,0)."), // version string without minor component – mismatch
        arguments("9", 9, 1, false, "JAVA_VERSION = \"9\" has derived minor 0, required 1 fails."), // version string with patch component – match
        arguments("11.0.2", 11, 0, true, "Patch component ignored; (11,0) matches."), // version string with patch component – mismatch
        arguments("11.0.2", 11, 1, false, "Patch ignored; required minor 1 > actual 0."), // negative required major
        arguments("11.0", -1, 0, true, "Negative required major always considered lower."), // zero major, negative minor
        arguments("11.0", 0, -5, true, "Runtime major > required major 0, minor negative ignored."), // malformed JAVA_VERSION – NumberFormatException
        arguments("invalid.version", 1, 0, NumberFormatException.class, "Non‑numeric version string causes NumberFormatException."), // null JAVA_VERSION – NullPointerException
        arguments(null, 1, 0, NullPointerException.class, "Null java.version leads to NPE when split is called."), // non‑numeric minor component – NumberFormatException
        arguments("11.invalid", 11, 0, NumberFormatException.class, "Minor part non‑numeric causes NumberFormatException."), // empty JAVA_VERSION string – NumberFormatException
        arguments("", 0, 0, NumberFormatException.class, "Empty string cannot be parsed as integer."), // whitespace in version string – NumberFormatException
        arguments(" 11.0", 11, 0, NumberFormatException.class, "Leading whitespace prevents Integer.parseInt from succeeding."));
    }

    @Test
    @DisplayName("isJavaVersionAtLeast returns true for the current JVM version")
    void testCurrentVersionIsAtLeast() {
        assertTrue(SystemUtilities.isJavaVersionAtLeast(currentMajor, currentMinor), "Current JVM version should be reported as at least itself");
    }

    @Test
    @DisplayName("isJavaVersionAtLeast returns true for a lower major version")
    void testLowerMajorVersion() {
        int lowerMajor = Math.max(0, currentMajor - 1);
        assertTrue(SystemUtilities.isJavaVersionAtLeast(lowerMajor, 0), "Any lower major version must be considered at least");
    }

    @Test
    @DisplayName("isJavaVersionAtLeast returns false for a higher major version")
    void testHigherMajorVersion() {
        int higherMajor = currentMajor + 1;
        assertFalse(SystemUtilities.isJavaVersionAtLeast(higherMajor, 0), "A higher major version must not be considered at least");
    }

    @Test
    @DisplayName("isJavaVersionAtLeast returns true for a lower minor version when major is equal")
    void testLowerMinorVersionSameMajor() {
        if (currentMinor > 0) {
            int lowerMinor = currentMinor - 1;
            assertTrue(SystemUtilities.isJavaVersionAtLeast(currentMajor, lowerMinor), "Same major with lower minor should be considered at least");
        } else {
            // If minor is 0, the lower‑minor case does not exist; test is skipped.
            Assumptions.assumeTrue(false, "Current minor version is 0, skipping lower‑minor test");
        }
    }

    @Test
    @DisplayName("isJavaVersionAtLeast returns false for a higher minor version when major is equal")
    void testHigherMinorVersionSameMajor() {
        int higherMinor = currentMinor + 1;
        assertFalse(SystemUtilities.isJavaVersionAtLeast(currentMajor, higherMinor), "Same major with higher minor must not be considered at least");
    }

    @Test
    @DisplayName("isJavaVersionAtLeast handles zero and negative inputs gracefully")
    void testZeroAndNegativeInputs() {
        // Zero major/minor should always be true for any real JVM version.
        assertTrue(SystemUtilities.isJavaVersionAtLeast(0, 0));
        // Negative values are not realistic but the implementation treats them as lower.
        assertTrue(SystemUtilities.isJavaVersionAtLeast(-1, -1));
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("versionCases")
    void testIsJavaVersionAtLeast(String mockedVersion, int requiredMajor, int requiredMinor, Object expected, String description) throws Exception {
        // Arrange: install the mocked version (may be null)
        setJavaVersion(mockedVersion);
        // Act & Assert
        if (expected instanceof Boolean) {
            boolean result = SystemUtilities.isJavaVersionAtLeast(requiredMajor, requiredMinor);
            assertEquals(expected, result, () -> "Expected " + expected + " for version \"" + mockedVersion + "\" with required (" + requiredMajor + "," + requiredMinor + ")");
        } else if (expected instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> SystemUtilities.isJavaVersionAtLeast(requiredMajor, requiredMinor), () -> "Expected exception " + exClass.getSimpleName() + " for version \"" + mockedVersion + "\"");
        } else {
            fail("Invalid expected value in test data");
        }
    }

    /**
     * Simulates a {@link SecurityException} that can be thrown when the JVM
     * denies access to the {@code java.version} system property.
     *
     * <p>The test loads {@code SystemUtilities} in a fresh {@link URLClassLoader}
     * after installing a restrictive {@link SecurityManager}. The static initializer
     * of {@code SystemUtilities} attempts to read the property and therefore throws
     * {@code SecurityException}.</p>
     */
    @Test
    @DisplayName("isJavaVersionAtLeast - SecurityException when reading java.version property")
    void testSecurityExceptionDuringClassInit() throws Exception {
        // Preserve the original SecurityManager.
        SecurityManager originalSM = System.getSecurityManager();
        // Install a SecurityManager that denies access to "java.version".
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkPropertyAccess(String key) {
                if ("java.version".equals(key)) {
                    throw new SecurityException("Access denied to java.version");
                }
            }
        });
        try {
            // Build a classloader that can load SystemUtilities anew.
            String classPath = System.getProperty("java.class.path");
            URL[] urls = Stream.of(classPath.split(System.getProperty("path.separator"))).map(path -> {
                try {
                    return new URL("file:" + path);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toArray(URL[]::new);
            try (URLClassLoader loader = new URLClassLoader(urls, null)) {
                // Load the class – this should trigger the static initializer.
                Class<?> sysUtilClass = Class.forName("com.cedarsoftware.util.SystemUtilities", true, loader);
                // Obtain the method via reflection.
                var method = sysUtilClass.getDeclaredMethod("isJavaVersionAtLeast", int.class, int.class);
                // Invoke and expect SecurityException wrapped in InvocationTargetException.
                assertThrows(SecurityException.class, () -> method.invoke(null, 1, 0), "Expected SecurityException when accessing java.version");
            }
        } finally {
            // Restore original SecurityManager.
            System.setSecurityManager(originalSM);
        }
    }
}
