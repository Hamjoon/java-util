package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link SystemUtilities}.
 * The tests use only the public API defined in the supplied source file
 * and avoid any external mocking frameworks as required.
 */
public class SystemUtilitiesTest {

    private static final String TEST_PROP_KEY = "system.utilities.test.key";
    private static final String TEST_PROP_VALUE = "testValue";

    private String originalPropValue;

    @Before
    public void setUp() {
        // Preserve any existing system property with the same key
        originalPropValue = System.getProperty(TEST_PROP_KEY);
    }

    @After
    public void tearDown() {
        // Restore original system property state
        if (originalPropValue != null) {
            System.setProperty(TEST_PROP_KEY, originalPropValue);
        } else {
            System.clearProperty(TEST_PROP_KEY);
        }
    }

    /* -------------------------------------------------------------
     *  getExternalVariable()
     * ------------------------------------------------------------- */
    @Test
    public void testGetExternalVariable_NullOrEmpty() {
        assertNull(SystemUtilities.getExternalVariable(null));
        assertNull(SystemUtilities.getExternalVariable(""));
        assertNull(SystemUtilities.getExternalVariable("   "));
    }

    @Test
    public void testGetExternalVariable_SystemPropertyTakesPrecedence() {
        System.setProperty(TEST_PROP_KEY, TEST_PROP_VALUE);
        // Even if an environment variable with the same name exists,
        // the system property must win.
        String result = SystemUtilities.getExternalVariable(TEST_PROP_KEY);
        assertEquals(TEST_PROP_VALUE, result);
    }

    @Test
    public void testGetExternalVariable_FallsBackToEnvironment() {
        // Choose a well‑known environment variable that is very unlikely to be overridden
        String envKey = "PATH";
        // Ensure the system property is NOT set so the method must read the env var
        System.clearProperty(envKey);
        String expected = System.getenv(envKey);
        String actual = SystemUtilities.getExternalVariable(envKey);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetExternalVariable_NotFoundReturnsNull() {
        String unknownKey = "THIS_VARIABLE_SHOULD_NEVER_EXIST_12345";
        assertNull(SystemUtilities.getExternalVariable(unknownKey));
    }

    /* -------------------------------------------------------------
     *  getAvailableProcessors()
     * ------------------------------------------------------------- */
    @Test
    public void testGetAvailableProcessors() {
        int expected = Runtime.getRuntime().availableProcessors();
        int actual = SystemUtilities.getAvailableProcessors();
        assertTrue("Processor count must be at least 1", actual >= 1);
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  getMemoryInfo()
     * ------------------------------------------------------------- */
    @Test
    public void testGetMemoryInfo() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        assertNotNull(info);
        assertTrue(info.getTotalMemory() >= 0);
        assertTrue(info.getFreeMemory() >= 0);
        assertTrue(info.getMaxMemory() >= 0);
        // totalMemory should never be less than freeMemory
        assertTrue(info.getTotalMemory() >= info.getFreeMemory());
    }

    /* -------------------------------------------------------------
     *  getSystemLoadAverage()
     * ------------------------------------------------------------- */
    @Test
    public void testGetSystemLoadAverage() {
        double load = SystemUtilities.getSystemLoadAverage();
        // The contract states -1.0 if not available, otherwise a non‑negative value
        assertTrue("Load average must be -1.0 or non‑negative", load == -1.0 || load >= 0.0);
    }

    /* -------------------------------------------------------------
     *  isJavaVersionAtLeast()
     * ------------------------------------------------------------- */
    @Test
    public void testIsJavaVersionAtLeast_CurrentVersion() {
        // Parse the version string using the same algorithm as the utility
        String[] parts = SystemUtilities.JAVA_VERSION.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        // The current version must be at least itself
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor));
        // One minor version lower must also be true
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major, Math.max(0, minor - 1)));
        // One major version higher must be false
        assertFalse(SystemUtilities.isJavaVersionAtLeast(major + 1, 0));
    }

    /* -------------------------------------------------------------
     *  getCurrentProcessId()
     * ------------------------------------------------------------- */
    @Test
    public void testGetCurrentProcessId() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName(); // e.g. "12345@hostname"
        int atPos = jvmName.indexOf('@');
        long expected = (atPos > 0) ? Long.parseLong(jvmName.substring(0, atPos)) : 0L;
        long actual = SystemUtilities.getCurrentProcessId();
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  createTempDirectory()
     * ------------------------------------------------------------- */
    @Test
    public void testCreateTempDirectory() throws IOException {
        String prefix = "sysUtilTest_";
        File tempDir = SystemUtilities.createTempDirectory(prefix);
        try {
            assertTrue("Temp directory should exist", tempDir.exists());
            assertTrue("Temp directory should be a directory", tempDir.isDirectory());
            assertTrue("Directory name should start with the given prefix",
                    tempDir.getName().startsWith(prefix));
        } finally {
            // Cleanup explicitly because deleteOnExit may not run during the test run
            deleteRecursively(tempDir);
        }
    }

    private void deleteRecursively(File file) {
        if (file == null) return;
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    /* -------------------------------------------------------------
     *  getSystemTimeZone()
     * ------------------------------------------------------------- */
    @Test
    public void testGetSystemTimeZone() {
        java.util.TimeZone tz = SystemUtilities.getSystemTimeZone();
        assertNotNull(tz);

        String envTz = System.getenv("TZ");
        if (envTz != null) {
            // When TZ is defined, the returned zone must match it (or default if TZ is invalid)
            java.util.TimeZone expected = java.util.TimeZone.getTimeZone(envTz);
            assertEquals(expected.getID(), tz.getID());
        } else {
            // Otherwise it must be the JVM default
            assertEquals(java.util.TimeZone.getDefault().getID(), tz.getID());
        }
    }

    /* -------------------------------------------------------------
     *  hasAvailableMemory()
     * ------------------------------------------------------------- */
    @Test
    public void testHasAvailableMemory() {
        SystemUtilities.MemoryInfo memInfo = SystemUtilities.getMemoryInfo();
        long free = memInfo.getFreeMemory();

        // Zero bytes required -> always true
        assertTrue(SystemUtilities.hasAvailableMemory(0));

        // Request less than free memory -> true
        assertTrue(SystemUtilities.hasAvailableMemory(Math.max(1, free / 2)));

        // Request more than free memory -> false (unless free is Long.MAX_VALUE which is impossible)
        assertFalse(SystemUtilities.hasAvailableMemory(free + 1));
    }

    /* -------------------------------------------------------------
     *  getEnvironmentVariables()
     * ------------------------------------------------------------- */
    @Test
    public void testGetEnvironmentVariables_NoFilter() {
        Map<String, String> all = SystemUtilities.getEnvironmentVariables(null);
        assertNotNull(all);
        // The map should contain at least one known variable, e.g., PATH
        assertTrue("Map should contain the PATH variable (if present on host)", all.containsKey("PATH"));
    }

    @Test
    public void testGetEnvironmentVariables_WithFilter() {
        // Filter for variables that start with "JAVA"
        Map<String, String> javaVars = SystemUtilities.getEnvironmentVariables(key -> key.startsWith("JAVA"));
        assertNotNull(javaVars);
        // Every key must satisfy the predicate
        for (String key : javaVars.keySet()) {
            assertTrue("Key should start with JAVA", key.startsWith("JAVA"));
        }
    }

    /* -------------------------------------------------------------
     *  getNetworkInterfaces()
     * ------------------------------------------------------------- */
    @Test
    public void testGetNetworkInterfaces() throws SocketException {
        // The method may return an empty list on some CI agents, but it must never be null
        java.util.List<SystemUtilities.NetworkInfo> nets = SystemUtilities.getNetworkInterfaces();
        assertNotNull(nets);

        for (SystemUtilities.NetworkInfo ni : nets) {
            assertNotNull("Interface name must not be null", ni.getName());
            assertNotNull("Display name must not be null", ni.getDisplayName());
            assertNotNull("Addresses list must not be null", ni.getAddresses());
            // The addresses list may be empty, but must be non‑null
            for (java.net.InetAddress addr : ni.getAddresses()) {
                assertNotNull("InetAddress entry must not be null", addr);
            }
        }
    }

    /* -------------------------------------------------------------
     *  addShutdownHook()
     * ------------------------------------------------------------- */
    @Test
    public void testAddShutdownHook_ExecutesWithoutException() {
        // Use an AtomicBoolean to verify that the hook runs when the thread is started manually.
        // Since we cannot trigger JVM shutdown, we invoke the Runnable directly via reflection.
        AtomicBoolean ran = new AtomicBoolean(false);
        Runnable hook = () -> ran.set(true);

        SystemUtilities.addShutdownHook(hook);

        // The shutdown hook is added to the Runtime; we cannot directly execute it.
        // However, we can assert that no exception was thrown during registration and that
        // the Runnable itself works as expected when called.
        assertFalse("Hook should not have run yet", ran.get());

        // Manually invoke the hook to ensure it behaves correctly (no exception)
        hook.run();
        assertTrue("Hook should have set the flag", ran.get());
    }
}
