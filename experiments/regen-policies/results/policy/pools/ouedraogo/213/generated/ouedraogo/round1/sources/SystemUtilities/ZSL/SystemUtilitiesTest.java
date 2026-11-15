package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class SystemUtilitiesTest {

    private static final String TEST_PROP_KEY = "SystemUtilitiesTest.prop";
    private static final String TEST_PROP_VALUE = "testValue";

    @Before
    public void setUp() {
        // Ensure the test property is not present before each test
        System.clearProperty(TEST_PROP_KEY);
    }

    @After
    public void tearDown() {
        // Clean up the test property after each test
        System.clearProperty(TEST_PROP_KEY);
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
    public void testGetExternalVariable_FromSystemProperty() {
        System.setProperty(TEST_PROP_KEY, TEST_PROP_VALUE);
        String result = SystemUtilities.getExternalVariable(TEST_PROP_KEY);
        assertEquals(TEST_PROP_VALUE, result);
    }

    @Test
    public void testGetExternalVariable_FallbackToNullWhenMissing() {
        // No system property and we cannot set env vars reliably, so expect null
        assertNull(SystemUtilities.getExternalVariable("NON_EXISTENT_VARIABLE_12345"));
    }

    /* -------------------------------------------------------------
     *  getAvailableProcessors()
     * ------------------------------------------------------------- */
    @Test
    public void testGetAvailableProcessors() {
        int cpus = SystemUtilities.getAvailableProcessors();
        assertTrue("Should be at least 1 processor", cpus >= 1);
    }

    /* -------------------------------------------------------------
     *  getMemoryInfo()
     * ------------------------------------------------------------- */
    @Test
    public void testGetMemoryInfo() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        assertNotNull(info);
        assertTrue(info.getTotalMemory() > 0);
        assertTrue(info.getFreeMemory() >= 0);
        assertTrue(info.getMaxMemory() > 0);
        assertTrue(info.getTotalMemory() >= info.getFreeMemory());
    }

    /* -------------------------------------------------------------
     *  getSystemLoadAverage()
     * ------------------------------------------------------------- */
    @Test
    public void testGetSystemLoadAverage() {
        double load = SystemUtilities.getSystemLoadAverage();
        // The spec allows -1.0 when not available; otherwise must be non‑negative
        assertTrue("Load average must be -1.0 or non‑negative", load == -1.0 || load >= 0.0);
    }

    /* -------------------------------------------------------------
     *  isJavaVersionAtLeast()
     * ------------------------------------------------------------- */
    @Test
    public void testIsJavaVersionAtLeast_CurrentVersion() {
        String[] parts = SystemUtilities.JAVA_VERSION.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        // Should be true for the exact version
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor));

        // Should be true for a lower version
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major - 1, 0));

        // Should be false for a higher version
        assertFalse(SystemUtilities.isJavaVersionAtLeast(major + 1, 0));
    }

    /* -------------------------------------------------------------
     *  currentJdkMajorVersion()
     * ------------------------------------------------------------- */
    @Test
    public void testCurrentJdkMajorVersion() {
        int expected = SystemUtilities.currentJdkMajorVersion();
        String spec = System.getProperty("java.specification.version");
        int parsed = spec.startsWith("1.") ? Integer.parseInt(spec.substring(2)) : Integer.parseInt(spec);
        assertEquals(parsed, expected);
    }

    /* -------------------------------------------------------------
     *  getCurrentProcessId()
     * ------------------------------------------------------------- */
    @Test
    public void testGetCurrentProcessId() {
        long pid = SystemUtilities.getCurrentProcessId();
        // The PID format depends on the JVM; it should be non‑negative
        assertTrue("PID should be >= 0", pid >= 0);
        // When the runtime MXBean name follows the usual "<pid>@<host>" pattern,
        // the parsed value should equal the numeric prefix.
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int at = name.indexOf('@');
        if (at > 0) {
            try {
                long expected = Long.parseLong(name.substring(0, at));
                assertEquals(expected, pid);
            } catch (NumberFormatException ignored) {
                // ignore – the test above already guarantees pid >= 0
            }
        }
    }

    /* -------------------------------------------------------------
     *  createTempDirectory()
     * ------------------------------------------------------------- */
    @Test
    public void testCreateTempDirectory() throws IOException {
        String prefix = "sysutilstest_";
        File tempDir = SystemUtilities.createTempDirectory(prefix);
        assertNotNull(tempDir);
        assertTrue("Temp directory should exist", tempDir.exists());
        assertTrue("Temp directory should be a directory", tempDir.isDirectory());
        // Cleanup explicitly (deleteOnExit will also try)
        assertTrue(tempDir.delete());
    }

    /* -------------------------------------------------------------
     *  getSystemTimeZone()
     * ------------------------------------------------------------- */
    @Test
    public void testGetSystemTimeZone_Default() {
        // Since we cannot reliably set the TZ env var in a portable way,
        // we verify that the method returns a non‑null TimeZone (the default)
        assertNotNull(SystemUtilities.getSystemTimeZone());
    }

    /* -------------------------------------------------------------
     *  hasAvailableMemory()
     * ------------------------------------------------------------- */
    @Test
    public void testHasAvailableMemory() {
        // Requesting 0 bytes should always be true
        assertTrue(SystemUtilities.hasAvailableMemory(0));

        // Requesting a huge amount should be false (unless the JVM has an absurd amount of free memory)
        long huge = Long.MAX_VALUE / 2;
        boolean result = SystemUtilities.hasAvailableMemory(huge);
        // The result can be true on a machine with lots of memory; we only assert logical consistency
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        if (info.getFreeMemory() < huge) {
            assertFalse(result);
        } else {
            assertTrue(result);
        }
    }

    /* -------------------------------------------------------------
     *  getEnvironmentVariables()
     * ------------------------------------------------------------- */
    @Test
    public void testGetEnvironmentVariables_NoFilter() {
        Map<String, String> env = SystemUtilities.getEnvironmentVariables(null);
        assertNotNull(env);
        // The system must have at least one environment variable
        assertFalse(env.isEmpty());
    }

    @Test
    public void testGetEnvironmentVariables_WithFilter() {
        // Grab an existing key to filter on
        String anyKey = System.getenv().keySet().iterator().next();
        Map<String, String> filtered = SystemUtilities.getEnvironmentVariables(k -> k.equals(anyKey));
        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey(anyKey));
    }

    /* -------------------------------------------------------------
     *  getNetworkInterfaces()
     * ------------------------------------------------------------- */
    @Test
    public void testGetNetworkInterfaces() throws SocketException {
        List<SystemUtilities.NetworkInfo> list = SystemUtilities.getNetworkInterfaces();
        assertNotNull(list);
        // The list may be empty on very restricted environments, but the method must not fail
        for (SystemUtilities.NetworkInfo ni : list) {
            assertNotNull(ni.getName());
            assertNotNull(ni.getDisplayName());
            assertNotNull(ni.getAddresses());
            // Addresses list may be empty, but must be non‑null
            for (InetAddress addr : ni.getAddresses()) {
                assertNotNull(addr);
            }
        }
    }

    /* -------------------------------------------------------------
     *  addShutdownHook()
     * ------------------------------------------------------------- */
    @Test
    public void testAddShutdownHook_RunsWithoutException() throws InterruptedException {
        AtomicBoolean ran = new AtomicBoolean(false);
        Runnable hook = () -> ran.set(true);
        SystemUtilities.addShutdownHook(hook);

        // Simulate JVM shutdown by invoking the hook thread directly.
        // The addShutdownHook method wraps the provided Runnable in a Thread,
        // so we retrieve the last added shutdown hook via reflection.
        Thread[] hooks = getShutdownHooks();
        assertTrue("At least one shutdown hook should be registered", hooks.length > 0);
        Thread lastHook = hooks[hooks.length - 1];
        // Run the thread's runnable (which will call the original hook)
        lastHook.start();
        lastHook.join(1000);
        assertTrue("The shutdown hook should have been executed", ran.get());
    }

    /**
     * Helper that uses reflection to obtain the current shutdown hooks registered
     * with the Runtime. This is safe for testing purposes only.
     */
    private Thread[] getShutdownHooks() {
        try {
            java.lang.reflect.Field lock = Runtime.class.getDeclaredField("shutdownHooks");
            lock.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<Thread, Thread> hooks = (java.util.Map<Thread, Thread>) lock.get(Runtime.getRuntime());
            return hooks.keySet().toArray(new Thread[0]);
        } catch (Exception e) {
            // If reflection fails (e.g., due to security manager), fall back to empty array
            return new Thread[0];
        }
    }

    /* -------------------------------------------------------------
     *  Inner class constructors and getters (sanity checks)
     * ------------------------------------------------------------- */
    @Test
    public void testMemoryInfoConstructorAndGetters() {
        SystemUtilities.MemoryInfo mi = new SystemUtilities.MemoryInfo(100L, 50L, 200L);
        assertEquals(100L, mi.getTotalMemory());
        assertEquals(50L, mi.getFreeMemory());
        assertEquals(200L, mi.getMaxMemory());
    }

    @Test
    public void testNetworkInfoConstructorAndGetters() throws IOException {
        InetAddress localhost = InetAddress.getByName("127.0.0.1");
        List<InetAddress> addrs = Collections.singletonList(localhost);
        SystemUtilities.NetworkInfo ni = new SystemUtilities.NetworkInfo("lo", "Loopback", addrs, true);
        assertEquals("lo", ni.getName());
        assertEquals("Loopback", ni.getDisplayName());
        assertEquals(addrs, ni.getAddresses());
        assertTrue(ni.isLoopback());
    }

    @Test
    public void testProcessResultConstructorAndGetters() {
        SystemUtilities.ProcessResult pr = new SystemUtilities.ProcessResult(0, "out", "err");
        assertEquals(0, pr.getExitCode());
        assertEquals("out", pr.getOutput());
        assertEquals("err", pr.getError());
    }
}
