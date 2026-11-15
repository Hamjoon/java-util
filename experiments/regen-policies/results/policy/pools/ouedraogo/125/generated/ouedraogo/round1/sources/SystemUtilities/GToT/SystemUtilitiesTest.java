package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.SocketException;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link SystemUtilities}.
 *
 * <p>The suite follows the collaborative “expert” workflow described in the
 * prompt:</p>
 * <ol>
 *   <li>Extract all public static methods.</li>
 *   <li>Provide a basic test that proves the nominal behaviour.</li>
 *   <li>Identify edge‑cases / exceptional conditions.</li>
 *   <li>Write dedicated tests for those scenarios.</li>
 *   <li>Combine everything into a single test class.</li>
 * </ol>
 *
 * <p>Only APIs that appear in the supplied source are used – no additional
 * libraries, no mocking frameworks, and no reflection tricks that would break
 * the “real objects only” rule.</p>
 */
public class SystemUtilitiesTest
{
    /* --------------------------------------------------------------------- */
    /*  Preserve original system properties so that tests do not leak state. */
    /* --------------------------------------------------------------------- */
    private String originalUserHome;
    private String originalTempDir;
    private String originalJavaVersion;
    private String originalOsName;

    @Before
    public void saveSystemProperties()
    {
        originalUserHome      = System.getProperty("user.home");
        originalTempDir       = System.getProperty("java.io.tmpdir");
        originalJavaVersion   = System.getProperty("java.version");
        originalOsName        = System.getProperty("os.name");
    }

    @After
    public void restoreSystemProperties()
    {
        setIfNotNull("user.home",      originalUserHome);
        setIfNotNull("java.io.tmpdir", originalTempDir);
        setIfNotNull("java.version",   originalJavaVersion);
        setIfNotNull("os.name",        originalOsName);
    }

    private void setIfNotNull(String key, String value)
    {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  1. getExternalVariable(String)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetExternalVariable_NullOrEmpty()
    {
        assertNull(SystemUtilities.getExternalVariable(null));
        assertNull(SystemUtilities.getExternalVariable(""));
        assertNull(SystemUtilities.getExternalVariable("   "));
    }

    @Test
    public void testGetExternalVariable_UsesSystemProperty()
    {
        final String key   = "SystemUtilitiesTest_PROP";
        final String value = "propertyValue";

        System.setProperty(key, value);
        try {
            String result = SystemUtilities.getExternalVariable(key);
            assertEquals(value, result);
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    public void testGetExternalVariable_FallbackToEnv()
    {
        // We cannot reliably set an environment variable from Java, but we can
        // pick an existing one that is guaranteed to be present on most OSes.
        // The test simply verifies that when the property is missing, the
        // method returns the (non‑null) environment value.
        String knownKey = System.getenv().keySet().stream()
                .filter(k -> System.getenv(k) != null && !System.getenv(k).isEmpty())
                .findFirst()
                .orElse(null);

        assertNotNull("Test machine must have at least one non‑empty env var", knownKey);
        System.clearProperty(knownKey);
        String envValue = System.getenv(knownKey);
        assertNotNull(envValue);

        String result = SystemUtilities.getExternalVariable(knownKey);
        assertEquals(envValue, result);
    }

    /* --------------------------------------------------------------------- */
    /*  2. getAvailableProcessors()                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetAvailableProcessors()
    {
        int processors = SystemUtilities.getAvailableProcessors();
        assertTrue("Should be at least 1 processor", processors >= 1);
        // The value must match Runtime.availableProcessors() (or be >0 after the max)
        assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors()), processors);
    }

    /* --------------------------------------------------------------------- */
    /*  3. getMemoryInfo()                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetMemoryInfo()
    {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        Runtime rt = Runtime.getRuntime();

        assertEquals(rt.totalMemory(), info.getTotalMemory());
        assertEquals(rt.freeMemory(),  info.getFreeMemory());
        assertEquals(rt.maxMemory(),   info.getMaxMemory());

        // sanity checks – all values must be non‑negative
        assertTrue(info.getTotalMemory() >= 0);
        assertTrue(info.getFreeMemory()  >= 0);
        assertTrue(info.getMaxMemory()   >= 0);
    }

    /* --------------------------------------------------------------------- */
    /*  4. getSystemLoadAverage()                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSystemLoadAverage()
    {
        double expected = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        double actual   = SystemUtilities.getSystemLoadAverage();

        assertEquals(expected, actual, 0.0001);
        // The contract allows -1.0 when not supported
        if (expected == -1.0) {
            assertEquals(-1.0, actual, 0.0);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  5. isJavaVersionAtLeast(int, int)                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsJavaVersionAtLeast_CurrentVersion()
    {
        // Parse the current version once – this mirrors the implementation.
        String[] parts = SystemUtilities.JAVA_VERSION.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        // The current version should be at least itself.
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor));

        // One minor version lower must also be true.
        if (minor > 0) {
            assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor - 1));
        }

        // One major version lower must be true.
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major - 1, 0));

        // A future version must be false.
        assertFalse(SystemUtilities.isJavaVersionAtLeast(major + 10, 0));
    }

    /* --------------------------------------------------------------------- */
    /*  6. getCurrentProcessId()                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetCurrentProcessId()
    {
        long pid = SystemUtilities.getCurrentProcessId();
        // The PID may be 0 on some exotic JVMs, but it must never be negative.
        assertTrue("PID should be >= 0", pid >= 0);
    }

    /* --------------------------------------------------------------------- */
    /*  7. createTempDirectory(String)                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCreateTempDirectory_Nominal() throws IOException
    {
        String prefix = "SysUtilTest_";
        File dir = SystemUtilities.createTempDirectory(prefix);

        try {
            assertTrue("Directory should exist", dir.exists());
            assertTrue("Should be a directory", dir.isDirectory());
            assertTrue("Name should start with the supplied prefix", dir.getName().startsWith(prefix));
        } finally {
            // Clean up – delete the directory manually because deleteOnExit()
            // runs only when the JVM terminates.
            if (dir.exists()) {
                assertTrue(dir.delete());
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void testCreateTempDirectory_NullPrefix() throws IOException
    {
        SystemUtilities.createTempDirectory(null);
    }

    /* --------------------------------------------------------------------- */
    /*  8. getSystemTimeZone()                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSystemTimeZone_Default()
    {
        // Since we cannot reliably set the TZ environment variable,
        // we verify that the method never returns null and that the
        // returned zone matches the JVM default when TZ is absent.
        java.util.TimeZone result = SystemUtilities.getSystemTimeZone();
        assertNotNull(result);
        assertEquals(java.util.TimeZone.getDefault().getID(), result.getID());
    }

    /* --------------------------------------------------------------------- */
    /*  9. hasAvailableMemory(long)                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testHasAvailableMemory()
    {
        // Request a tiny amount of memory – should always be true.
        assertTrue(SystemUtilities.hasAvailableMemory(1L));

        // Request more memory than the JVM currently reports as free – should be false.
        long huge = SystemUtilities.getMemoryInfo().getFreeMemory() + 1;
        assertFalse(SystemUtilities.hasAvailableMemory(huge));
    }

    /* --------------------------------------------------------------------- */
    /* 10. getEnvironmentVariables(Predicate<String>)                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetEnvironmentVariables_NoFilter()
    {
        Map<String, String> all = SystemUtilities.getEnvironmentVariables(null);
        assertEquals(System.getenv().size(), all.size());
        // Spot‑check a known key
        for (String key : System.getenv().keySet()) {
            assertEquals(System.getenv(key), all.get(key));
            break;
        }
    }

    @Test
    public void testGetEnvironmentVariables_WithFilter()
    {
        // Choose a key that is guaranteed to exist on the running platform.
        String someKey = System.getenv().keySet().stream().findFirst().orElse(null);
        assertNotNull("Platform must expose at least one env var", someKey);

        Predicate<String> filter = k -> k.equals(someKey);
        Map<String, String> filtered = SystemUtilities.getEnvironmentVariables(filter);

        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey(someKey));
        assertEquals(System.getenv(someKey), filtered.get(someKey));
    }

    /* --------------------------------------------------------------------- */
    /* 11. getNetworkInterfaces() throws SocketException                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetNetworkInterfaces() throws SocketException
    {
        // The call must not throw and must return a List (possibly empty).
        assertNotNull(SystemUtilities.getNetworkInterfaces());
    }

    /* --------------------------------------------------------------------- */
    /* 12. addShutdownHook(Runnable)                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddShutdownHook()
    {
        // The hook simply toggles a flag; we verify that adding the hook does
        // not throw. We cannot trigger JVM shutdown in a unit test, but we can
        // ensure the hook thread is alive after registration.
        final boolean[] ran = {false};
        Runnable r = () -> ran[0] = true;

        SystemUtilities.addShutdownHook(r);
        // The hook runs only on JVM shutdown – we just assert that the
        // registration succeeded (no exception) and that the flag is still false.
        assertFalse(ran[0]);
    }

    /* --------------------------------------------------------------------- */
    /*  13. Inner classes – simple getter validation (optional)              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMemoryInfoGetters()
    {
        SystemUtilities.MemoryInfo mi = new SystemUtilities.MemoryInfo(10L, 5L, 20L);
        assertEquals(10L, mi.getTotalMemory());
        assertEquals(5L,  mi.getFreeMemory());
        assertEquals(20L, mi.getMaxMemory());
    }

    @Test
    public void testNetworkInfoGetters()
    {
        SystemUtilities.NetworkInfo ni = new SystemUtilities.NetworkInfo(
                "eth0", "Ethernet 0", java.util.Collections.emptyList(), false);
        assertEquals("eth0", ni.getName());
        assertEquals("Ethernet 0", ni.getDisplayName());
        assertTrue(ni.getAddresses().isEmpty());
        assertFalse(ni.isLoopback());
    }

    @Test
    public void testProcessResultGetters()
    {
        SystemUtilities.ProcessResult pr = new SystemUtilities.ProcessResult(0, "out", "err");
        assertEquals(0, pr.getExitCode());
        assertEquals("out", pr.getOutput());
        assertEquals("err", pr.getError());
    }
}
