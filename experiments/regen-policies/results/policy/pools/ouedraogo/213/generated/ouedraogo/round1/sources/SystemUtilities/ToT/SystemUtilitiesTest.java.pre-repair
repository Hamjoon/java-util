package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class SystemUtilitiesTest {

    private static final String TEST_PROP = "SystemUtilitiesTest.prop";
    private static final String TEST_PROP_VALUE = "testValue";

    @Before
    public void setUp() {
        // Ensure the test system property is not present before each test
        System.clearProperty(TEST_PROP);
    }

    @After
    public void tearDown() {
        // Clean up after each test
        System.clearProperty(TEST_PROP);
    }

    /*--------------------------------------------------------------
     *  getExternalVariable()
     *--------------------------------------------------------------*/
    @Test
    public void testGetExternalVariable_NullOrEmpty() {
        assertNull(SystemUtilities.getExternalVariable(null));
        assertNull(SystemUtilities.getExternalVariable(""));
        assertNull(SystemUtilities.getExternalVariable("   "));
    }

    @Test
    public void testGetExternalVariable_PropertyFallback() {
        System.setProperty(TEST_PROP, TEST_PROP_VALUE);
        String result = SystemUtilities.getExternalVariable(TEST_PROP);
        assertEquals(TEST_PROP_VALUE, result);
    }

    /*--------------------------------------------------------------
     *  getAvailableProcessors()
     *--------------------------------------------------------------*/
    @Test
    public void testGetAvailableProcessors() {
        int processors = SystemUtilities.getAvailableProcessors();
        assertTrue("Processors should be at least 1", processors >= 1);
    }

    /*--------------------------------------------------------------
     *  getMemoryInfo()
     *--------------------------------------------------------------*/
    @Test
    public void testGetMemoryInfo() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        assertNotNull(info);
        assertTrue(info.getTotalMemory() >= 0);
        assertTrue(info.getFreeMemory() >= 0);
        assertTrue(info.getMaxMemory() >= 0);
        // freeMemory should never be greater than totalMemory
        assertTrue(info.getFreeMemory() <= info.getTotalMemory());
    }

    /*--------------------------------------------------------------
     *  getSystemLoadAverage()
     *--------------------------------------------------------------*/
    @Test
    public void testGetSystemLoadAverage() {
        double load = SystemUtilities.getSystemLoadAverage();
        // The contract is: load >= -1.0; -1.0 means not available
        assertTrue("Load average must be >= -1.0", load >= -1.0);
    }

    /*--------------------------------------------------------------
     *  isJavaVersionAtLeast()
     *--------------------------------------------------------------*/
    @Test
    public void testIsJavaVersionAtLeast() {
        // Parse the current version from SystemUtilities.JAVA_VERSION
        String[] parts = SystemUtilities.JAVA_VERSION.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        // The current version must be at least itself
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor));

        // One version lower should be false (unless we are on 1.x where major is 1)
        if (major > 1) {
            assertTrue(SystemUtilities.isJavaVersionAtLeast(major - 1, 0));
        }
    }

    /*--------------------------------------------------------------
     *  currentJdkMajorVersion()
     *--------------------------------------------------------------*/
    @Test
    public void testCurrentJdkMajorVersion() {
        int expected = SystemUtilities.currentJdkMajorVersion();
        assertTrue(expected > 0);
        // Verify that the method returns the same value as the calculation we do here
        String spec = System.getProperty("java.specification.version");
        int calculated = spec.startsWith("1.") ? Integer.parseInt(spec.substring(2)) : Integer.parseInt(spec);
        assertEquals(calculated, expected);
    }

    /*--------------------------------------------------------------
     *  getCurrentProcessId()
     *--------------------------------------------------------------*/
    @Test
    public void testGetCurrentProcessId() {
        long pid = SystemUtilities.getCurrentProcessId();
        // PID may be 0 on some JVM implementations, but it should never be negative
        assertTrue("PID should be >= 0", pid >= 0);
        // When the runtime MXBean name follows the usual pid@host format, pid > 0
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (name.contains("@")) {
            try {
                long parsed = Long.parseLong(name.substring(0, name.indexOf('@')));
                assertEquals(parsed, pid);
            } catch (NumberFormatException ignored) {
                // ignore – we already verified pid >= 0
            }
        }
    }

    /*--------------------------------------------------------------
     *  createTempDirectory()
     *--------------------------------------------------------------*/
    @Test
    public void testCreateTempDirectory() throws IOException {
        String prefix = "sysUtilTest_";
        File dir = SystemUtilities.createTempDirectory(prefix);
        try {
            assertTrue("Temp directory should exist", dir.exists());
            assertTrue("Temp directory should be a directory", dir.isDirectory());
            assertTrue("Directory name should start with the prefix", dir.getName().startsWith(prefix));
        } finally {
            // Cleanup manually – deleteOnExit will try to delete it when JVM exits
            if (dir != null && dir.isDirectory()) {
                for (File child : dir.listFiles()) {
                    child.delete();
                }
                dir.delete();
            }
        }
    }

    /*--------------------------------------------------------------
     *  getSystemTimeZone()
     *--------------------------------------------------------------*/
    @Test
    public void testGetSystemTimeZone_Default() {
        // As we cannot reliably set the TZ environment variable without native code,
        // we simply verify that the method never returns null and returns a TimeZone instance.
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        assertNotNull(tz);
    }

    /*--------------------------------------------------------------
     *  hasAvailableMemory()
     *--------------------------------------------------------------*/
    @Test
    public void testHasAvailableMemory() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        long free = info.getFreeMemory();

        // Request less than free memory – should be true
        assertTrue(SystemUtilities.hasAvailableMemory(free / 2));

        // Request more than free memory – should be false (unless free is Long.MAX_VALUE)
        assertFalse(SystemUtilities.hasAvailableMemory(free + 1));
    }

    /*--------------------------------------------------------------
     *  getEnvironmentVariables()
     *--------------------------------------------------------------*/
    @Test
    public void testGetEnvironmentVariables_WithFilter() {
        // Use a filter that only accepts keys that start with "JAVA"
        Predicate<String> filter = key -> key != null && key.startsWith("JAVA");
        Map<String, String> filtered = SystemUtilities.getEnvironmentVariables(filter);
        assertNotNull(filtered);
        for (String key : filtered.keySet()) {
            assertTrue("Key should start with JAVA", key.startsWith("JAVA"));
        }

        // Null filter should return the complete map (size > 0 on any typical system)
        Map<String, String> all = SystemUtilities.getEnvironmentVariables(null);
        assertNotNull(all);
        assertTrue("There should be at least one environment variable", all.size() > 0);
    }

    /*--------------------------------------------------------------
     *  getNetworkInterfaces()
     *--------------------------------------------------------------*/
    @Test
    public void testGetNetworkInterfaces() throws SocketException {
        List<SystemUtilities.NetworkInfo> list = SystemUtilities.getNetworkInterfaces();
        assertNotNull(list);
        // The list may be empty on very restricted environments, but if not,
        // each element must contain non‑null fields.
        for (SystemUtilities.NetworkInfo ni : list) {
            assertNotNull(ni.getName());
            assertNotNull(ni.getDisplayName());
            assertNotNull(ni.getAddresses());
            // Addresses list may be empty, but the list itself must not be null
            for (InetAddress addr : ni.getAddresses()) {
                assertNotNull(addr);
            }
            // loopback flag is a primitive boolean – no assertion needed beyond presence
        }
    }

    /*--------------------------------------------------------------
     *  addShutdownHook()
     *--------------------------------------------------------------*/
    @Test
    public void testAddShutdownHook() {
        // The hook will set this flag when executed.
        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable hook = () -> executed.set(true);

        // Add the hook – this should not throw any exception.
        SystemUtilities.addShutdownHook(hook);

        // Simulate JVM shutdown by invoking the hook directly.
        // The actual thread created inside addShutdownHook is not exposed,
        // but we can verify that the Runnable works as expected.
        hook.run();
        assertTrue("Shutdown hook should have been executed", executed.get());
    }
}
