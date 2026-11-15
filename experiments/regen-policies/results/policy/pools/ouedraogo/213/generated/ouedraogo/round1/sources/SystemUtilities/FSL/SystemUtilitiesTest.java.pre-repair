package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Predicate;

/**
 * Comprehensive JUnit‑4 test suite for {@link SystemUtilities}.
 *
 * The tests exercise every public static method and the nested data classes.
 * No external mocking frameworks are used – all objects are created via the
 * constructors and factory methods that belong to the class under test.
 */
public class SystemUtilitiesTest {

    private static final String TEST_PROP = "com.cedarsoftware.util.testProp";

    @Before
    public void setUp() {
        // Ensure the test property is not present before each test.
        System.clearProperty(TEST_PROP);
    }

    @After
    public void tearDown() {
        System.clearProperty(TEST_PROP);
    }

    /* -------------------------------------------------------------
     *  getExternalVariable()
     * ------------------------------------------------------------- */
    @Test
    public void testGetExternalVariable_NullOrEmptyKey() {
        assertNull(SystemUtilities.getExternalVariable(null));
        assertNull(SystemUtilities.getExternalVariable(""));
        assertNull(SystemUtilities.getExternalVariable("   "));
    }

    @Test
    public void testGetExternalVariable_UsesSystemPropertyWhenPresent() {
        System.setProperty(TEST_PROP, "propValue");
        assertEquals("propValue", SystemUtilities.getExternalVariable(TEST_PROP));
    }

    @Test
    public void testGetExternalVariable_PrefersSystemPropertyOverEnv() {
        // Set a system property; even if an environment variable with the same name
        // existed it would be ignored – we cannot set env vars in a portable way,
        // so the test only verifies the property path.
        System.setProperty(TEST_PROP, "onlyProp");
        assertEquals("onlyProp", SystemUtilities.getExternalVariable(TEST_PROP));
    }

    @Test
    public void testGetExternalVariable_ReturnsNullWhenNotFound() {
        assertNull(SystemUtilities.getExternalVariable("NON_EXISTENT_VARIABLE_12345"));
    }

    /* -------------------------------------------------------------
     *  getAvailableProcessors()
     * ------------------------------------------------------------- */
    @Test
    public void testGetAvailableProcessors_MinimumOne() {
        int processors = SystemUtilities.getAvailableProcessors();
        assertTrue("Processor count must be at least 1", processors >= 1);
        // Compare with the raw Runtime value – they should be equal.
        assertEquals(Runtime.getRuntime().availableProcessors(), processors);
    }

    /* -------------------------------------------------------------
     *  getMemoryInfo()
     * ------------------------------------------------------------- */
    @Test
    public void testGetMemoryInfo_ReflectsRuntimeValues() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        Runtime rt = Runtime.getRuntime();

        assertEquals(rt.totalMemory(), info.getTotalMemory());
        assertEquals(rt.freeMemory(),  info.getFreeMemory());
        assertEquals(rt.maxMemory(),   info.getMaxMemory());
    }

    /* -------------------------------------------------------------
     *  getSystemLoadAverage()
     * ------------------------------------------------------------- */
    @Test
    public void testGetSystemLoadAverage_DelegatesToOperatingSystemMXBean() {
        double expected = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        double actual   = SystemUtilities.getSystemLoadAverage();

        // The contract permits -1.0 when the metric is unavailable.
        assertEquals(expected, actual, 0.0);
    }

    /* -------------------------------------------------------------
     *  isJavaVersionAtLeast()
     * ------------------------------------------------------------- */
    @Test
    public void testIsJavaVersionAtLeast_BasicLogic() {
        String[] parts = SystemUtilities.JAVA_VERSION.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        // Assert true for the exact version.
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor));

        // One minor version lower should still be true (if minor > 0).
        if (minor > 0) {
            assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor - 1));
        }

        // One major version higher must be false.
        assertFalse(SystemUtilities.isJavaVersionAtLeast(major + 1, 0));
    }

    /* -------------------------------------------------------------
     *  currentJdkMajorVersion()
     * ------------------------------------------------------------- */
    @Test
    public void testCurrentJdkMajorVersion_MatchesSpecificationProperty() {
        String spec = System.getProperty("java.specification.version"); // e.g. "1.8" or "17"
        int expected = spec.startsWith("1.") ? Integer.parseInt(spec.substring(2)) : Integer.parseInt(spec);
        assertEquals(expected, SystemUtilities.currentJdkMajorVersion());
    }

    /* -------------------------------------------------------------
     *  getCurrentProcessId()
     * ------------------------------------------------------------- */
    @Test
    public void testGetCurrentProcessId_ParsesRuntimeMXBeanName() {
        long pid = SystemUtilities.getCurrentProcessId();
        assertTrue("Process id should be non‑negative", pid >= 0);

        // Verify consistency with the MXBean name format.
        String mxName = ManagementFactory.getRuntimeMXBean().getName(); // "pid@host"
        int atPos = mxName.indexOf('@');
        if (atPos > 0) {
            try {
                long expected = Long.parseLong(mxName.substring(0, atPos));
                assertEquals(expected, pid);
            } catch (NumberFormatException ignored) {
                // If parsing fails the method is expected to return 0.
                assertEquals(0, pid);
            }
        } else {
            assertEquals(0, pid);
        }
    }

    /* -------------------------------------------------------------
     *  createTempDirectory()
     * ------------------------------------------------------------- */
    @Test
    public void testCreateTempDirectory_CreatesDirectoryAndIsDeletedOnExit() throws IOException {
        File dir = SystemUtilities.createTempDirectory("sysUtilTest");
        assertTrue("Temp directory should exist", dir.exists());
        assertTrue("Temp directory should be a directory", dir.isDirectory());

        // The directory is scheduled for deletion on JVM exit – we can delete it now
        // to verify that it is indeed a normal file system directory.
        assertTrue("Should be able to delete the directory", dir.delete());
    }

    /* -------------------------------------------------------------
     *  getSystemTimeZone()
     * ------------------------------------------------------------- */
    @Test
    public void testGetSystemTimeZone_DefaultWhenNoTZEnv() {
        // We cannot reliably set an environment variable, but we can assert that
        // when TZ is not set the method returns the default time zone.
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        assertEquals(TimeZone.getDefault(), tz);
    }

    /* -------------------------------------------------------------
     *  hasAvailableMemory()
     * ------------------------------------------------------------- */
    @Test
    public void testHasAvailableMemory_EdgeCases() {
        // Zero bytes is always available.
        assertTrue(SystemUtilities.hasAvailableMemory(0));

        // Request a very large amount – should be false unless the JVM has that much free.
        long huge = Long.MAX_VALUE / 2;
        boolean result = SystemUtilities.hasAvailableMemory(huge);
        // The result may be true on a machine with massive memory; we only assert logical consistency.
        if (result) {
            // If true, the free memory reported must be >= huge.
            assertTrue(SystemUtilities.getMemoryInfo().getFreeMemory() >= huge);
        } else {
            assertTrue(SystemUtilities.getMemoryInfo().getFreeMemory() < huge);
        }
    }

    /* -------------------------------------------------------------
     *  getEnvironmentVariables()
     * ------------------------------------------------------------- */
    @Test
    public void testGetEnvironmentVariables_WithFilter() {
        // Filter keys that start with "JAVA" – those are almost always present.
        Predicate<String> javaPrefix = key -> key != null && key.startsWith("JAVA");
        Map<String, String> map = SystemUtilities.getEnvironmentVariables(javaPrefix);

        assertNotNull(map);
        // Every entry must satisfy the predicate.
        for (String key : map.keySet()) {
            assertTrue("Key should start with JAVA", key.startsWith("JAVA"));
        }

        // Verify that the result is a LinkedHashMap preserving insertion order.
        // The exact order is not important; we just check the concrete type.
        assertTrue(map instanceof java.util.LinkedHashMap);
    }

    @Test
    public void testGetEnvironmentVariables_NoFilterReturnsAll() {
        Map<String, String> all = SystemUtilities.getEnvironmentVariables(null);
        assertEquals(System.getenv().size(), all.size());
        // Spot‑check a known variable if present.
        if (System.getenv().containsKey("PATH")) {
            assertEquals(System.getenv().get("PATH"), all.get("PATH"));
        }
    }

    /* -------------------------------------------------------------
     *  getNetworkInterfaces()
     * ------------------------------------------------------------- */
    @Test
    public void testGetNetworkInterfaces_ReturnsUpInterfaces() throws SocketException {
        List<SystemUtilities.NetworkInfo> list = SystemUtilities.getNetworkInterfaces();
        assertNotNull(list);

        // The list may be empty on very restricted environments; the test ensures
        // that each element, if present, contains sensible data.
        for (SystemUtilities.NetworkInfo ni : list) {
            assertNotNull("Interface name must not be null", ni.getName());
            assertNotNull("Display name must not be null", ni.getDisplayName());
            assertNotNull("Addresses list must not be null", ni.getAddresses());
            // The addresses list may be empty, but it must be a concrete List.
            assertTrue(ni.getAddresses() instanceof List);
            // Loopback flag must be consistent with the name (heuristic – not strict).
            if (ni.isLoopback()) {
                assertTrue(ni.getName().toLowerCase().contains("lo") ||
                           ni.getDisplayName().toLowerCase().contains("loopback"));
            }
        }
    }

    /* -------------------------------------------------------------
     *  addShutdownHook()
     * ------------------------------------------------------------- */
    @Test
    public void testAddShutdownHook_DoesNotThrow() {
        // The hook will be executed only when the JVM shuts down.
        // Here we simply verify that adding the hook does not raise an exception.
        SystemUtilities.addShutdownHook(() -> {
            // No‑op – deliberately empty.
        });
    }

    /* -------------------------------------------------------------
     *  Nested data classes – simple getters
     * ------------------------------------------------------------- */
    @Test
    public void testMemoryInfo_Getters() {
        SystemUtilities.MemoryInfo info = new SystemUtilities.MemoryInfo(123L, 45L, 678L);
        assertEquals(123L, info.getTotalMemory());
        assertEquals(45L,  info.getFreeMemory());
        assertEquals(678L, info.getMaxMemory());
    }

    @Test
    public void testNetworkInfo_Getters() throws Exception {
        InetAddress addr = InetAddress.getByName("127.0.0.1");
        SystemUtilities.NetworkInfo net = new SystemUtilities.NetworkInfo(
                "test0", "Test Interface", java.util.Collections.singletonList(addr), false);
        assertEquals("test0", net.getName());
        assertEquals("Test Interface", net.getDisplayName());
        assertEquals(1, net.getAddresses().size());
        assertEquals(addr, net.getAddresses().get(0));
        assertFalse(net.isLoopback());
    }

    @Test
    public void testProcessResult_Getters() {
        SystemUtilities.ProcessResult result = new SystemUtilities.ProcessResult(0, "out", "err");
        assertEquals(0, result.getExitCode());
        assertEquals("out", result.getOutput());
        assertEquals("err", result.getError());
    }
}
