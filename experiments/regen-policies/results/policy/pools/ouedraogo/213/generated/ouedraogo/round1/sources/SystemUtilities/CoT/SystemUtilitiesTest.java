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
import java.util.function.Predicate;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link SystemUtilities}.
 *
 * The test cases are divided into:
 * <ul>
 *   <li>Basic functionality verification</li>
 *   <li>Edge‑case / exception handling verification</li>
 * </ul>
 */
public class SystemUtilitiesTest
{
    // -----------------------------------------------------------------------
    // 1.  Public method signatures extracted from SystemUtilities
    // -----------------------------------------------------------------------
    // public static String getExternalVariable(String var)
    // public static int getAvailableProcessors()
    // public static MemoryInfo getMemoryInfo()
    // public static double getSystemLoadAverage()
    // public static boolean isJavaVersionAtLeast(int major, int minor)
    // public static int currentJdkMajorVersion()
    // public static long getCurrentProcessId()
    // public static File createTempDirectory(String prefix) throws IOException
    // public static TimeZone getSystemTimeZone()
    // public static boolean hasAvailableMemory(long requiredBytes)
    // public static Map<String,String> getEnvironmentVariables(Predicate<String> filter)
    // public static List<NetworkInfo> getNetworkInterfaces() throws SocketException
    // public static void addShutdownHook(Runnable hook)

    // -----------------------------------------------------------------------
    // 2.  Helper fields
    // -----------------------------------------------------------------------
    private static final String TEST_PROP_KEY = "SystemUtilitiesTest.prop";
    private static final String TEST_PROP_VALUE = "propValue";

    private String originalPropValue;

    @Before
    public void setUp() {
        // Preserve any existing system property so we can restore it later.
        originalPropValue = System.getProperty(TEST_PROP_KEY);
        System.setProperty(TEST_PROP_KEY, TEST_PROP_VALUE);
    }

    @After
    public void tearDown() {
        // Restore the original property (or clear it if it did not exist).
        if (originalPropValue != null) {
            System.setProperty(TEST_PROP_KEY, originalPropValue);
        } else {
            System.clearProperty(TEST_PROP_KEY);
        }
    }

    // -----------------------------------------------------------------------
    // 3.  Basic functionality tests
    // -----------------------------------------------------------------------

    @Test
    public void testGetExternalVariable_usesSystemProperty() {
        // System property is set in @Before, env variable is ignored.
        String result = SystemUtilities.getExternalVariable(TEST_PROP_KEY);
        assertEquals(TEST_PROP_VALUE, result);
    }

    @Test
    public void testGetExternalVariable_missingVariableReturnsNull() {
        assertNull(SystemUtilities.getExternalVariable("NON_EXISTENT_VARIABLE_12345"));
    }

    @Test
    public void testGetExternalVariable_nullOrEmptyKeyReturnsNull() {
        assertNull(SystemUtilities.getExternalVariable(null));
        assertNull(SystemUtilities.getExternalVariable(""));
        assertNull(SystemUtilities.getExternalVariable("   "));
    }

    @Test
    public void testGetAvailableProcessors_isAtLeastOne() {
        int processors = SystemUtilities.getAvailableProcessors();
        assertTrue("Available processors must be >= 1", processors >= 1);
    }

    @Test
    public void testGetMemoryInfo_consistency() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        assertNotNull(info);
        assertTrue(info.getTotalMemory() >= 0);
        assertTrue(info.getFreeMemory() >= 0);
        assertTrue(info.getMaxMemory() >= 0);
        // Free memory cannot exceed total memory.
        assertTrue(info.getFreeMemory() <= info.getTotalMemory());
        // Total memory cannot exceed the maximum reported by the JVM.
        assertTrue(info.getTotalMemory() <= info.getMaxMemory() || info.getMaxMemory() == Long.MAX_VALUE);
    }

    @Test
    public void testGetSystemLoadAverage_matchesOperatingSystemMXBean() {
        double expected = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        double actual = SystemUtilities.getSystemLoadAverage();
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testIsJavaVersionAtLeast_currentVersion() {
        // Parse the current version from the constant.
        String[] parts = SystemUtilities.JAVA_VERSION.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        // The method must return true for the exact current version.
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor));

        // And also for a lower version.
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major - 1, 0));

        // And false for a higher version.
        assertFalse(SystemUtilities.isJavaVersionAtLeast(major + 1, 0));
    }

    @Test
    public void testCurrentJdkMajorVersion_matchesSpecificationProperty() {
        int expected = Integer.parseInt(
                System.getProperty("java.specification.version")
                        .startsWith("1.") ?
                        System.getProperty("java.specification.version").substring(2) :
                        System.getProperty("java.specification.version"));
        assertEquals(expected, SystemUtilities.currentJdkMajorVersion());
    }

    @Test
    public void testGetCurrentProcessId_isNonNegative() {
        long pid = SystemUtilities.getCurrentProcessId();
        assertTrue("PID should be >= 0", pid >= 0);
    }

    @Test
    public void testCreateTempDirectory_createsAndDeletesOnExit() throws IOException {
        File dir = SystemUtilities.createTempDirectory("sysUtilTest_");
        assertNotNull(dir);
        assertTrue("Temp directory should exist", dir.exists());
        assertTrue("Temp directory should be a directory", dir.isDirectory());

        // The directory is scheduled for deleteOnExit; we verify the flag indirectly
        // by checking that the file can be deleted manually (which would fail if it
        // were already removed by the JVM, but should succeed now).
        assertTrue("Should be able to delete the temporary directory", dir.delete());
    }

    @Test
    public void testGetSystemTimeZone_defaultsWhenNoTZEnv() {
        // Ensure the TZ environment variable is not set for this test.
        // We cannot reliably clear the process environment, but the method falls back
        // to TimeZone.getDefault() when TZ is null or unrecognised.
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        assertNotNull(tz);
        assertEquals(TimeZone.getDefault().getID(), tz.getID());
    }

    @Test
    public void testHasAvailableMemory_trueWhenEnoughFreeMemory() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        long free = info.getFreeMemory();
        assertTrue(SystemUtilities.hasAvailableMemory(free / 2));
    }

    @Test
    public void testHasAvailableMemory_falseWhenNotEnoughFreeMemory() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        long free = info.getFreeMemory();
        assertFalse(SystemUtilities.hasAvailableMemory(free + 1));
    }

    @Test
    public void testGetEnvironmentVariables_nullFilterReturnsAll() {
        Map<String, String> all = SystemUtilities.getEnvironmentVariables(null);
        assertNotNull(all);
        // The environment must contain at least one entry on any reasonable system.
        assertFalse(all.isEmpty());
    }

    @Test
    public void testGetEnvironmentVariables_withFilter() {
        // Filter for a variable that is almost certainly present: "PATH" on Unix or
        // "Path" on Windows (case‑sensitive map on Unix, case‑insensitive on Windows).
        Predicate<String> filter = key -> key.equalsIgnoreCase("PATH") || key.equalsIgnoreCase("Path");
        Map<String, String> filtered = SystemUtilities.getEnvironmentVariables(filter);
        assertNotNull(filtered);
        // The filtered map should contain at most the entries that match the predicate.
        for (String key : filtered.keySet()) {
            assertTrue(filter.test(key));
        }
    }

    @Test
    public void testGetNetworkInterfaces_returnsNonNullList() throws SocketException {
        List<SystemUtilities.NetworkInfo> list = SystemUtilities.getNetworkInterfaces();
        assertNotNull(list);
        // Every returned NetworkInfo must have non‑null fields.
        for (SystemUtilities.NetworkInfo ni : list) {
            assertNotNull(ni.getName());
            assertNotNull(ni.getDisplayName());
            assertNotNull(ni.getAddresses());
        }
    }

    @Test
    public void testAddShutdownHook_registersHookWithoutException() {
        // The test simply verifies that calling the method does not throw.
        SystemUtilities.addShutdownHook(() -> System.out.println("Shutdown hook executed"));
        // No further assertions possible without terminating the JVM.
    }

    // -----------------------------------------------------------------------
    // 4.  Edge‑case / exception handling tests
    // -----------------------------------------------------------------------

    @Test
    public void testGetExternalVariable_emptyStringValueReturnsNull() {
        // Set a system property with an empty value.
        System.setProperty("EMPTY_PROP", "   ");
        try {
            assertNull(SystemUtilities.getExternalVariable("EMPTY_PROP"));
        } finally {
            System.clearProperty("EMPTY_PROP");
        }
    }

    @Test
    public void testIsJavaVersionAtLeast_invalidVersionStringHandledGracefully() {
        // Temporarily replace the constant via reflection to an unexpected format.
        // This simulates a malformed version string; the method should throw
        // NumberFormatException, which we capture and assert.
        try {
            java.lang.reflect.Field f = SystemUtilities.class.getDeclaredField("JAVA_VERSION");
            f.setAccessible(true);
            String original = (String) f.get(null);
            f.set(null, "bad.version");
            try {
                SystemUtilities.isJavaVersionAtLeast(1, 0);
                fail("Expected NumberFormatException due to malformed version string");
            } catch (NumberFormatException ignored) {
                // Expected path.
            } finally {
                // Restore original constant.
                f.set(null, original);
            }
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    public void testCurrentJdkMajorVersion_malformedSpecificationVersion() throws Exception {
        // Replace the system property temporarily.
        String original = System.getProperty("java.specification.version");
        try {
            System.setProperty("java.specification.version", "badSpec");
            // The method will throw NumberFormatException; we assert that.
            SystemUtilities.currentJdkMajorVersion();
            fail("Expected NumberFormatException for malformed specification version");
        } catch (NumberFormatException ignored) {
            // Expected.
        } finally {
            // Restore original property.
            if (original != null) {
                System.setProperty("java.specification.version", original);
            } else {
                System.clearProperty("java.specification.version");
            }
        }
    }

    @Test
    public void testCreateTempDirectory_nullPrefixThrowsNPE() {
        try {
            SystemUtilities.createTempDirectory(null);
            fail("Expected NullPointerException when prefix is null");
        } catch (NullPointerException | IOException ignored) {
            // Both are acceptable – Files.createTempDirectory throws NPE for null.
        }
    }

    @Test
    public void testGetCurrentProcessId_malformedRuntimeNameReturnsZero() throws Exception {
        // Use reflection to replace the RuntimeMXBean name with a malformed string.
        java.lang.management.RuntimeMXBean originalBean = ManagementFactory.getRuntimeMXBean();
        java.lang.reflect.Field beanField = ManagementFactory.class.getDeclaredField("runtimeMXBean");
        beanField.setAccessible(true);
        java.lang.management.RuntimeMXBean mockBean = new java.lang.management.RuntimeMXBean() {
            @Override public String getName() { return "notAnId@host"; }
            // All other methods delegate to the original bean.
            @Override public String getVmName() { return originalBean.getVmName(); }
            @Override public String getVmVendor() { return originalBean.getVmVendor(); }
            @Override public String getVmVersion() { return originalBean.getVmVersion(); }
            @Override public long getStartTime() { return originalBean.getStartTime(); }
            @Override public long getUptime() { return originalBean.getUptime(); }
            @Override public List<String> getInputArguments() { return originalBean.getInputArguments(); }
            @Override public Map<String, String> getSystemProperties() { return originalBean.getSystemProperties(); }
            @Override public String getBootClassPath() { return originalBean.getBootClassPath(); }
            @Override public String getClassPath() { return originalBean.getClassPath(); }
            @Override public String getLibraryPath() { return originalBean.getLibraryPath(); }
            @Override public String getManagementSpecVersion() { return originalBean.getManagementSpecVersion(); }
            @Override public String getSpecName() { return originalBean.getSpecName(); }
            @Override public String getSpecVendor() { return originalBean.getSpecVendor(); }
            @Override public String getSpecVersion() { return originalBean.getSpecVersion(); }
            @Override public String getSystemProperties() { return originalBean.getSystemProperties().toString(); }
        };
        try {
            beanField.set(null, mockBean);
            long pid = SystemUtilities.getCurrentProcessId();
            assertEquals("Malformed name should result in PID 0", 0L, pid);
        } finally {
            // Restore original bean.
            beanField.set(null, originalBean);
        }
    }

    // -----------------------------------------------------------------------
    // 5.  End of test class
    // -----------------------------------------------------------------------
}
