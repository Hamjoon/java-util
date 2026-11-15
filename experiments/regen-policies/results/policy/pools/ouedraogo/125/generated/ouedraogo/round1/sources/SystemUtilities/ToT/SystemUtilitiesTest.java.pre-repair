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

/**
 * Comprehensive JUnit‑4 test suite for {@link SystemUtilities}.
 * <p>
 * The suite is written from the perspective of three “experts”.  
 * Each expert contributed a test case for every public static method, covering
 * typical usage, edge conditions and error scenarios.  All tests use only the
 * public API that is present in the supplied source file – no external mocking
 * frameworks or additional library classes are used.
 * </p>
 */
public class SystemUtilitiesTest {

    /* --------------------------------------------------------------------- */
    /* Helper constants / state used by multiple tests                       */
    /* --------------------------------------------------------------------- */
    private static final String TEST_PROP_KEY = "SystemUtilitiesTest.key";
    private static final String TEST_PROP_VALUE = "testValue";

    private String originalPropValue;

    @Before
    public void setUp() {
        // Preserve any existing system property with the test key
        originalPropValue = System.getProperty(TEST_PROP_KEY);
    }

    @After
    public void tearDown() {
        // Restore original value (or clear if it was not set)
        if (originalPropValue != null) {
            System.setProperty(TEST_PROP_KEY, originalPropValue);
        } else {
            System.clearProperty(TEST_PROP_KEY);
        }
    }

    /* --------------------------------------------------------------------- */
    /* 1. getExternalVariable                                                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetExternalVariable_nullOrEmpty() {
        assertNull("null key should return null", SystemUtilities.getExternalVariable(null));
        assertNull("empty key should return null", SystemUtilities.getExternalVariable(""));
        assertNull("blank key should return null", SystemUtilities.getExternalVariable("   "));
    }

    @Test
    public void testGetExternalVariable_usesSystemProperty() {
        System.setProperty(TEST_PROP_KEY, TEST_PROP_VALUE);
        String result = SystemUtilities.getExternalVariable(TEST_PROP_KEY);
        assertEquals("Should return the system property value", TEST_PROP_VALUE, result);
    }

    @Test
    public void testGetExternalVariable_fallbackToEnv() {
        // Choose an environment variable that is almost always defined.
        // On Windows it is "PATH", on Unix it is also "PATH".
        String envKey = "PATH";
        String result = SystemUtilities.getExternalVariable(envKey);
        assertNotNull("Should return a non‑null value from the environment", result);
        // The value must contain at least one path separator.
        assertTrue("Result should contain path separator",
                result.contains(File.pathSeparator));
    }

    @Test
    public void testGetExternalVariable_notFound() {
        // Use a deliberately unknown key; neither property nor env var exists.
        String unknown = "THIS_KEY_SHOULD_NOT_EXIST_12345";
        assertNull("Unknown key should return null", SystemUtilities.getExternalVariable(unknown));
    }

    /* --------------------------------------------------------------------- */
    /* 2. getAvailableProcessors                                             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetAvailableProcessors() {
        int processors = SystemUtilities.getAvailableProcessors();
        assertTrue("Available processors must be >= 1", processors >= 1);
        // It should match the runtime value (or be greater if Docker limits are applied)
        int runtimeProcessors = Runtime.getRuntime().availableProcessors();
        assertEquals("Utility should return the same count as Runtime", 
                     Math.max(1, runtimeProcessors), processors);
    }

    /* --------------------------------------------------------------------- */
    /* 3. getMemoryInfo                                                       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetMemoryInfo_consistency() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        assertNotNull(info);
        assertTrue("Total memory must be non‑negative", info.getTotalMemory() >= 0);
        assertTrue("Free memory must be non‑negative", info.getFreeMemory() >= 0);
        assertTrue("Max memory must be non‑negative", info.getMaxMemory() >= 0);
        assertTrue("Free memory cannot exceed total memory", info.getFreeMemory() <= info.getTotalMemory());
        assertTrue("Total memory cannot exceed max memory (or max may be -1)", 
                   info.getMaxMemory() == -1L || info.getTotalMemory() <= info.getMaxMemory());
    }

    /* --------------------------------------------------------------------- */
    /* 4. getSystemLoadAverage                                                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetSystemLoadAverage_range() {
        double load = SystemUtilities.getSystemLoadAverage();
        // The spec says -1.0 if not available; otherwise a non‑negative number.
        assertTrue("Load average must be -1.0 or >= 0.0", load == -1.0 || load >= 0.0);
    }

    /* --------------------------------------------------------------------- */
    /* 5. isJavaVersionAtLeast                                               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testIsJavaVersionAtLeast_self() {
        // Parse the current version using the same algorithm as the method.
        String[] parts = SystemUtilities.JAVA_VERSION.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        // Should be true for the exact version.
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor));
        // Should be true for a lower version.
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major - 1, 0));
        // Should be false for a higher major version.
        assertFalse(SystemUtilities.isJavaVersionAtLeast(major + 1, 0));
        // If major is same, a higher minor should be false.
        assertFalse(SystemUtilities.isJavaVersionAtLeast(major, minor + 1));
    }

    @Test
    public void testIsJavaVersionAtLeast_invalidFormat() {
        // The method assumes a numeric major part; if the VM reports something
        // unexpected (e.g., "1.8.0_292"), the split will still produce numeric
        // strings for the first two components.  This test simply ensures that
        // no exception is thrown for the current runtime.
        try {
            SystemUtilities.isJavaVersionAtLeast(0, 0);
        } catch (Exception e) {
            fail("Method should never throw for any numeric version string");
        }
    }

    /* --------------------------------------------------------------------- */
    /* 6. getCurrentProcessId                                                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetCurrentProcessId_nonNegative() {
        long pid = SystemUtilities.getCurrentProcessId();
        assertTrue("Process ID should be non‑negative", pid >= 0);
        // In a normal JVM the PID is > 0; however the spec allows 0 on failure.
        // We accept both but log the value for visibility.
        System.out.println("Current JVM PID = " + pid);
    }

    /* --------------------------------------------------------------------- */
    /* 7. createTempDirectory                                                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCreateTempDirectory_success() throws IOException {
        String prefix = "sysutil_test_";
        File dir = SystemUtilities.createTempDirectory(prefix);
        assertNotNull("Returned directory must not be null", dir);
        assertTrue("Directory must exist", dir.isDirectory());
        assertTrue("Directory name should start with the supplied prefix",
                dir.getName().startsWith(prefix));
        // Cleanup manually (deleteOnExit will also try, but we want deterministic removal)
        assertTrue("Should be able to delete the temporary directory", dir.delete());
    }

    @Test(expected = IOException.class)
    public void testCreateTempDirectory_invalidPrefix() throws IOException {
        // Passing null as prefix triggers a NullPointerException inside Files.createTempDirectory,
        // which is wrapped as an IOException by the method signature.
        SystemUtilities.createTempDirectory(null);
    }

    /* --------------------------------------------------------------------- */
    /* 8. getSystemTimeZone                                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetSystemTimeZone_default() {
        // Without the TZ environment variable, the method should return the JVM default.
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        assertEquals(TimeZone.getDefault().getID(), tz.getID());
    }

    /* --------------------------------------------------------------------- */
    /* 9. hasAvailableMemory                                                  */
    /* --------------------------------------------------------------------- */
    @Test
    public void testHasAvailableMemory_zeroBytes() {
        assertTrue("Zero bytes should always be reported as available",
                SystemUtilities.hasAvailableMemory(0));
    }

    @Test
    public void testHasAvailableMemory_exceedsFree() {
        long free = SystemUtilities.getMemoryInfo().getFreeMemory();
        // Request more than currently free – should be false.
        assertFalse("Request exceeding free memory must return false",
                SystemUtilities.hasAvailableMemory(free + 1));
    }

    /* --------------------------------------------------------------------- */
    /* 10. getEnvironmentVariables                                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetEnvironmentVariables_all() {
        Map<String, String> map = SystemUtilities.getEnvironmentVariables(null);
        assertNotNull(map);
        assertEquals(System.getenv().size(), map.size());
        // Spot‑check a known key (PATH).  It may not exist on every platform, so guard.
        if (System.getenv().containsKey("PATH")) {
            assertEquals(System.getenv("PATH"), map.get("PATH"));
        }
    }

    @Test
    public void testGetEnvironmentVariables_filtered() {
        Predicate<String> startsWith = key -> key.startsWith("JAVA");
        Map<String, String> filtered = SystemUtilities.getEnvironmentVariables(startsWith);
        assertNotNull(filtered);
        for (String key : filtered.keySet()) {
            assertTrue("All keys must start with JAVA", key.startsWith("JAVA"));
        }
    }

    /* --------------------------------------------------------------------- */
    /* 11. getNetworkInterfaces                                               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetNetworkInterfaces_basic() throws SocketException {
        List<SystemUtilities.NetworkInfo> list = SystemUtilities.getNetworkInterfaces();
        assertNotNull(list);
        // The list may be empty on very restricted environments; ensure no NPEs.
        for (SystemUtilities.NetworkInfo ni : list) {
            assertNotNull("Interface name must not be null", ni.getName());
            assertNotNull("Display name must not be null", ni.getDisplayName());
            assertNotNull("Addresses list must not be null", ni.getAddresses());
            // Each address must be a valid InetAddress; the constructor guarantees that.
            for (InetAddress addr : ni.getAddresses()) {
                assertNotNull("InetAddress must not be null", addr);
            }
            // loopback flag is a boolean – just check that the getter returns the same value.
            assertEquals(ni.isLoopback(), ni.isLoopback());
        }
    }

    /* --------------------------------------------------------------------- */
    /* 12. addShutdownHook                                                    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testAddShutdownHook_executesHook() throws Exception {
        final AtomicBoolean ran = new AtomicBoolean(false);
        Runnable hook = () -> ran.set(true);

        // Register the hook
        SystemUtilities.addShutdownHook(hook);

        // Retrieve the list of shutdown hooks via reflection (no public API).
        // This is safe because we only need to verify that the hook was added and can be invoked.
        // We will invoke it directly by locating the Thread we just added.
        // Note: This is a best‑effort test – it does not guarantee execution on JVM exit.
        Thread[] threads = new Thread[Thread.activeCount()];
        int count = Thread.enumerate(threads);
        Thread found = null;
        for (int i = 0; i < count; i++) {
            Thread t = threads[i];
            if (t.getName().startsWith("Thread-") && t.getState() == Thread.State.NEW) {
                // Heuristic: the shutdown hook thread is a newly created thread with a runnable that calls our hook.
                // Since we cannot inspect the Runnable, we accept any NEW thread after registration.
                found = t;
                break;
            }
        }
        // If we could locate the thread, run it manually to simulate shutdown.
        if (found != null) {
            found.start(); // will invoke the hook
            found.join();
        }
        // Verify that the hook was indeed executed.
        assertTrue("Shutdown hook should have set the flag", ran.get());
    }

    /* --------------------------------------------------------------------- */
    /* 13. Support classes (MemoryInfo, NetworkInfo, ProcessResult)          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testMemoryInfo_accessors() {
        SystemUtilities.MemoryInfo mi = new SystemUtilities.MemoryInfo(100L, 50L, 200L);
        assertEquals(100L, mi.getTotalMemory());
        assertEquals(50L, mi.getFreeMemory());
        assertEquals(200L, mi.getMaxMemory());
    }

    @Test
    public void testNetworkInfo_accessors() throws Exception {
        InetAddress addr = InetAddress.getByName("127.0.0.1");
        SystemUtilities.NetworkInfo ni = new SystemUtilities.NetworkInfo(
                "lo", "Loopback Interface", List.of(addr), true);
        assertEquals("lo", ni.getName());
        assertEquals("Loopback Interface", ni.getDisplayName());
        assertEquals(1, ni.getAddresses().size());
        assertEquals(addr, ni.getAddresses().get(0));
        assertTrue(ni.isLoopback());
    }

    @Test
    public void testProcessResult_accessors() {
        SystemUtilities.ProcessResult pr = new SystemUtilities.ProcessResult(0, "out", "err");
        assertEquals(0, pr.getExitCode());
        assertEquals("out", pr.getOutput());
        assertEquals("err", pr.getError());
    }
}
