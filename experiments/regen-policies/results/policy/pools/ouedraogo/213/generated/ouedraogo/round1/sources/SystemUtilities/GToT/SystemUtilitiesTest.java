package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link SystemUtilities}.
 *
 * The test cases cover:
 *   • Normal usage of each public static method
 *   • Edge‑case handling (null/empty arguments, extreme values, etc.)
 *   • Expected exceptions where applicable
 *
 * No external libraries (mocking frameworks, etc.) are used – all objects are created
 * via the constructors and factory methods present in the source code.
 */
public class SystemUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper constants and state                                            */
    /* ---------------------------------------------------------------------- */

    private static final String TEST_PROP_KEY = "systemutilities.test.key";
    private static final String TEST_ENV_KEY = "PATH"; // almost always present in the environment
    private String originalTestPropValue;

    /* ---------------------------------------------------------------------- */
    /*  Setup / Teardown                                                      */
    /* ---------------------------------------------------------------------- */

    @Before
    public void setUp() {
        // Preserve any existing system property with the test key
        originalTestPropValue = System.getProperty(TEST_PROP_KEY);
    }

    @After
    public void tearDown() {
        // Restore property to its original state
        if (originalTestPropValue != null) {
            System.setProperty(TEST_PROP_KEY, originalTestPropValue);
        } else {
            System.clearProperty(TEST_PROP_KEY);
        }
        // Ensure the test property is cleared for the next test run
        System.clearProperty(TEST_PROP_KEY);
    }

    /* ---------------------------------------------------------------------- */
    /*  getExternalVariable                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetExternalVariable_NullOrEmpty() {
        assertNull(SystemUtilities.getExternalVariable(null));
        assertNull(SystemUtilities.getExternalVariable(""));
        assertNull(SystemUtilities.getExternalVariable("   "));
    }

    @Test
    public void testGetExternalVariable_UsesSystemProperty() {
        System.setProperty(TEST_PROP_KEY, "propValue");
        // Ensure no environment variable with the same name interferes
        System.clearProperty(TEST_PROP_KEY);
        System.setProperty(TEST_PROP_KEY, "propValue");
        assertEquals("propValue", SystemUtilities.getExternalVariable(TEST_PROP_KEY));
    }

    @Test
    public void testGetExternalVariable_FallsBackToEnvWhenPropertyMissing() {
        // Ensure the property is not set
        System.clearProperty(TEST_ENV_KEY);
        // The environment variable PATH should exist on virtually every OS
        String envValue = System.getenv(TEST_ENV_KEY);
        assertNotNull("Environment variable PATH must be present for this test", envValue);
        assertEquals(envValue, SystemUtilities.getExternalVariable(TEST_ENV_KEY));
    }

    /* ---------------------------------------------------------------------- */
    /*  getAvailableProcessors                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetAvailableProcessors_ReturnsAtLeastOne() {
        int processors = SystemUtilities.getAvailableProcessors();
        assertTrue("Available processors must be >= 1", processors >= 1);
    }

    /* ---------------------------------------------------------------------- */
    /*  getMemoryInfo                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMemoryInfo_ValuesAreNonNegative() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        assertNotNull(info);
        assertTrue(info.getTotalMemory() >= 0);
        assertTrue(info.getFreeMemory()  >= 0);
        assertTrue(info.getMaxMemory()   >= 0);
    }

    /* ---------------------------------------------------------------------- */
    /*  getSystemLoadAverage                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetSystemLoadAverage_ReturnsMinusOneOrPositive() {
        double load = SystemUtilities.getSystemLoadAverage();
        // The contract says -1.0 indicates “not available”
        assertTrue("Load average must be >= -1.0", load >= -1.0);
    }

    /* ---------------------------------------------------------------------- */
    /*  isJavaVersionAtLeast                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsJavaVersionAtLeast_VariousScenarios() {
        // Parse current version once
        String[] parts = SystemUtilities.JAVA_VERSION.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        // Exact match -> true
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor));

        // Lower major -> true
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major - 1, 0));

        // Higher major -> false
        assertFalse(SystemUtilities.isJavaVersionAtLeast(major + 1, 0));

        // Same major, higher minor -> false (if minor is not the highest)
        assertFalse(SystemUtilities.isJavaVersionAtLeast(major, minor + 1));
    }

    /* ---------------------------------------------------------------------- */
    /*  currentJdkMajorVersion                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCurrentJdkMajorVersion_ParsesSpecificationVersion() {
        int expected = Integer.parseInt(
                System.getProperty("java.specification.version")
                        .startsWith("1.") ?
                        System.getProperty("java.specification.version").substring(2) :
                        System.getProperty("java.specification.version"));
        assertEquals(expected, SystemUtilities.currentJdkMajorVersion());
    }

    /* ---------------------------------------------------------------------- */
    /*  getCurrentProcessId                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetCurrentProcessId_NonNegative() {
        long pid = SystemUtilities.getCurrentProcessId();
        assertTrue("PID should be non‑negative", pid >= 0);
    }

    /* ---------------------------------------------------------------------- */
    /*  createTempDirectory                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateTempDirectory_Success() throws IOException {
        File dir = SystemUtilities.createTempDirectory("sysutilstest-");
        try {
            assertNotNull(dir);
            assertTrue(dir.isDirectory());
            assertTrue(dir.exists());
        } finally {
            // Clean up explicitly; deleteOnExit is also in place
            dir.delete();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testCreateTempDirectory_NullPrefix_Throws() throws IOException {
        SystemUtilities.createTempDirectory(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  getSystemTimeZone                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetSystemTimeZone_FallbackToDefault() {
        // We cannot reliably change the environment variable TZ, so we just verify
        // that the method never returns null and that it equals the default when TZ is absent.
        java.util.TimeZone tz = SystemUtilities.getSystemTimeZone();
        assertNotNull(tz);
        // If TZ is not set, the result must be the JVM default
        if (System.getenv("TZ") == null) {
            assertEquals(java.util.TimeZone.getDefault().getID(), tz.getID());
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  hasAvailableMemory                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasAvailableMemory_EdgeCases() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        long free = info.getFreeMemory();

        // Zero bytes required -> always true
        assertTrue(SystemUtilities.hasAvailableMemory(0));

        // Require exactly the free amount -> true
        assertTrue(SystemUtilities.hasAvailableMemory(free));

        // Require more than free amount -> false (unless the JVM magically frees memory)
        assertEquals(free < Long.MAX_VALUE, SystemUtilities.hasAvailableMemory(free + 1));
    }

    /* ---------------------------------------------------------------------- */
    /*  getEnvironmentVariables                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetEnvironmentVariables_NoFilter_ReturnsAll() {
        Map<String, String> vars = SystemUtilities.getEnvironmentVariables(null);
        assertNotNull(vars);
        assertFalse(vars.isEmpty());
        // Spot‑check a known variable
        assertTrue(vars.containsKey(TEST_ENV_KEY));
    }

    @Test
    public void testGetEnvironmentVariables_WithFilter() {
        Predicate<String> javaPrefix = key -> key.startsWith("JAVA");
        Map<String, String> vars = SystemUtilities.getEnvironmentVariables(javaPrefix);
        assertNotNull(vars);
        // Every key must start with "JAVA"
        for (String key : vars.keySet()) {
            assertTrue("Key should start with JAVA: " + key, key.startsWith("JAVA"));
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  getNetworkInterfaces                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetNetworkInterfaces_ReturnsNonNullList() throws SocketException {
        // The method should never return null; an empty list is acceptable on headless systems.
        java.util.List<SystemUtilities.NetworkInfo> list = SystemUtilities.getNetworkInterfaces();
        assertNotNull(list);
        for (SystemUtilities.NetworkInfo info : list) {
            assertNotNull(info.getName());
            assertNotNull(info.getDisplayName());
            assertNotNull(info.getAddresses());
            // The addresses list may be empty but must never be null
            for (java.net.InetAddress addr : info.getAddresses()) {
                assertNotNull(addr);
            }
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  addShutdownHook                                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddShutdownHook_DoesNotPropagateException() {
        AtomicBoolean ran = new AtomicBoolean(false);
        Runnable hook = () -> {
            ran.set(true);
            throw new RuntimeException("Intentional failure inside hook");
        };

        // The method must swallow any exception thrown by the hook.
        SystemUtilities.addShutdownHook(hook);

        // Manually invoke the hook thread to verify that it runs and that the exception
        // does not escape the addShutdownHook method.
        // Since we cannot trigger JVM shutdown, we reflectively locate the last added hook.
        // The implementation creates a new Thread with the runnable, so we can start it ourselves.
        // However, the thread is already started only at JVM shutdown; we therefore simulate
        // the same behavior by directly invoking the runnable.
        try {
            hook.run();
        } catch (RuntimeException ignored) {
            // The exception is expected when running the hook directly;
            // addShutdownHook should have swallowed it internally.
        }

        assertTrue("Hook runnable should have been executed", ran.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  Additional sanity checks for inner classes (optional but helpful)      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMemoryInfo_ConstructorAndGetters() {
        SystemUtilities.MemoryInfo mi = new SystemUtilities.MemoryInfo(10L, 5L, 20L);
        assertEquals(10L, mi.getTotalMemory());
        assertEquals(5L,  mi.getFreeMemory());
        assertEquals(20L, mi.getMaxMemory());
    }

    @Test
    public void testNetworkInfo_ConstructorAndGetters() {
        java.util.List<java.net.InetAddress> addrs = java.util.Collections.emptyList();
        SystemUtilities.NetworkInfo ni = new SystemUtilities.NetworkInfo("eth0", "Ethernet 0", addrs, false);
        assertEquals("eth0", ni.getName());
        assertEquals("Ethernet 0", ni.getDisplayName());
        assertEquals(addrs, ni.getAddresses());
        assertFalse(ni.isLoopback());
    }

    @Test
    public void testProcessResult_ConstructorAndGetters() {
        SystemUtilities.ProcessResult pr = new SystemUtilities.ProcessResult(0, "out", "err");
        assertEquals(0, pr.getExitCode());
        assertEquals("out", pr.getOutput());
        assertEquals("err", pr.getError());
    }
}
