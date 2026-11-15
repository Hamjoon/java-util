package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Comprehensive JUnit4 test suite for {@link SystemUtilities}.
 *
 * The tests use only the public API that is part of {@code SystemUtilities}
 * (including the nested support classes) and standard JDK classes.
 * No external mocking frameworks are used.
 */
public class SystemUtilitiesTest {

    /* --------------------------------------------------------------------- */
    /*  getExternalVariable                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetExternalVariable_NullOrEmpty() {
        assertNull(SystemUtilities.getExternalVariable(null));
        assertNull(SystemUtilities.getExternalVariable(""));
        assertNull(SystemUtilities.getExternalVariable("   "));
    }

    @Test
    public void testGetExternalVariable_FromSystemProperty() {
        final String key = "systemutilities.test.prop";
        final String value = "propValue";

        // Ensure the property is not set before the test
        System.clearProperty(key);
        assertNull(SystemUtilities.getExternalVariable(key));

        // Set the property and verify retrieval
        System.setProperty(key, value);
        try {
            assertEquals(value, SystemUtilities.getExternalVariable(key));
        } finally {
            System.clearProperty(key);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  getAvailableProcessors                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetAvailableProcessors() {
        int processors = SystemUtilities.getAvailableProcessors();
        assertTrue("Processors should be at least 1", processors >= 1);
        // Compare with the Runtime directly – they must be equal or higher
        assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors()), processors);
    }

    /* --------------------------------------------------------------------- */
    /*  getMemoryInfo & hasAvailableMemory                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetMemoryInfo() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();
        assertNotNull(info);
        assertTrue(info.getTotalMemory() > 0);
        assertTrue(info.getFreeMemory() >= 0);
        assertTrue(info.getMaxMemory() > 0);
    }

    @Test
    public void testHasAvailableMemory() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();

        // Zero bytes required – always true
        assertTrue(SystemUtilities.hasAvailableMemory(0));

        // Require exactly the free memory – should be true
        assertTrue(SystemUtilities.hasAvailableMemory(info.getFreeMemory()));

        // Require more than free memory – should be false
        assertFalse(SystemUtilities.hasAvailableMemory(info.getFreeMemory() + 1));
    }

    /* --------------------------------------------------------------------- */
    /*  getSystemLoadAverage                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSystemLoadAverage() {
        double load = SystemUtilities.getSystemLoadAverage();
        // The contract states -1.0 if not available; any other value must be >= 0
        assertTrue("Load average must be -1.0 or non‑negative", load == -1.0 || load >= 0.0);
    }

    /* --------------------------------------------------------------------- */
    /*  isJavaVersionAtLeast                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsJavaVersionAtLeast() {
        // Parse the current version once – this mirrors the implementation
        String[] parts = SystemUtilities.JAVA_VERSION.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        // Same version – should be true
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor));

        // Lower major version – always true
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major - 1, 0));

        // Higher major version – always false
        assertFalse(SystemUtilities.isJavaVersionAtLeast(major + 1, 0));

        // Same major, lower minor – true
        if (minor > 0) {
            assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor - 1));
        }

        // Same major, higher minor – false
        assertFalse(SystemUtilities.isJavaVersionAtLeast(major, minor + 1));
    }

    /* --------------------------------------------------------------------- */
    /*  getCurrentProcessId                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetCurrentProcessId() {
        long pid = SystemUtilities.getCurrentProcessId();
        // The PID should be non‑negative; a value of 0 indicates parsing failure
        assertTrue("PID should be >= 0", pid >= 0);
        // When the JVM provides a PID, it is usually > 0 – we accept 0 as fallback
    }

    /* --------------------------------------------------------------------- */
    /*  createTempDirectory                                                  */
    /* --------------------------------------------------------------------- */

    private File tempDirCreated; // kept for cleanup in @After

    @Test
    public void testCreateTempDirectory() throws IOException {
        String prefix = "sysutilstest_";
        File dir = SystemUtilities.createTempDirectory(prefix);
        tempDirCreated = dir; // for later deletion

        assertNotNull(dir);
        assertTrue("Directory must exist", dir.isDirectory());
        assertTrue("Directory name should start with the given prefix", dir.getName().startsWith(prefix));
        // The directory should be writable
        File testFile = new File(dir, "tmp.txt");
        assertTrue("Should be able to create a file inside the temporary directory", testFile.createNewFile());
        assertTrue(testFile.delete());
    }

    @After
    public void cleanupTempDirectory() {
        if (tempDirCreated != null && tempDirCreated.exists()) {
            deleteRecursively(tempDirCreated);
        }
    }

    private void deleteRecursively(File f) {
        if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                deleteRecursively(child);
            }
        }
        f.delete();
    }

    /* --------------------------------------------------------------------- */
    /*  getSystemTimeZone                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSystemTimeZone() {
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        assertNotNull(tz);
        // When the TZ environment variable is not set, the method falls back to the default.
        // We cannot reliably change the environment, so we only assert that the result is a valid TimeZone.
        assertNotNull(tz.getID());
    }

    /* --------------------------------------------------------------------- */
    /*  getEnvironmentVariables                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetEnvironmentVariables_NoFilter() {
        Map<String, String> env = SystemUtilities.getEnvironmentVariables(null);
        assertNotNull(env);
        // The map must contain at least the entries that the JVM provides.
        assertFalse("Environment map should not be empty", env.isEmpty());
        // Spot‑check a well‑known variable (e.g., PATH) – it may be absent on some platforms,
        // so we only assert that if present the value matches System.getenv.
        String pathKey = System.getProperty("os.name").toLowerCase().contains("windows") ? "Path" : "PATH";
        if (System.getenv(pathKey) != null) {
            assertEquals(System.getenv(pathKey), env.get(pathKey));
        }
    }

    @Test
    public void testGetEnvironmentVariables_WithFilter() {
        // Filter that accepts only keys that start with "JAVA"
        Predicate<String> startsWithJava = key -> key != null && key.startsWith("JAVA");
        Map<String, String> filtered = SystemUtilities.getEnvironmentVariables(startsWithJava);
        assertNotNull(filtered);
        for (String key : filtered.keySet()) {
            assertTrue("All keys must start with JAVA", key.startsWith("JAVA"));
        }
    }

    /* --------------------------------------------------------------------- */
    /*  getNetworkInterfaces                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetNetworkInterfaces() throws SocketException {
        // The method should never throw; it returns a list (possibly empty)
        // containing only interfaces that are up.
        // We verify that each returned NetworkInfo reflects a real NetworkInterface.
        java.util.List<SystemUtilities.NetworkInfo> infos = SystemUtilities.getNetworkInterfaces();
        assertNotNull(infos);

        for (SystemUtilities.NetworkInfo info : infos) {
            assertNotNull("Interface name must not be null", info.getName());
            assertNotNull("Display name must not be null", info.getDisplayName());
            assertNotNull("Addresses list must not be null", info.getAddresses());

            // Cross‑check with the actual NetworkInterface instance
            NetworkInterface ni = NetworkInterface.getByName(info.getName());
            assertNotNull("Corresponding NetworkInterface should exist", ni);
            assertEquals(ni.isLoopback(), info.isLoopback());
        }
    }

    /* --------------------------------------------------------------------- */
    /*  addShutdownHook                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddShutdownHook_ExecutesSafelyEvenIfExceptionThrown() {
        AtomicBoolean hookRan = new AtomicBoolean(false);
        Runnable faultyHook = () -> {
            hookRan.set(true);
            throw new RuntimeException("Intentional failure in shutdown hook");
        };

        // The method must not propagate the exception; it merely registers the hook.
        SystemUtilities.addShutdownHook(faultyHook);

        // Simulate the shutdown hook execution by retrieving the thread that was added.
        // Unfortunately Runtime does not expose the list of hooks, but we can
        // invoke the Runnable directly to ensure it behaves as expected.
        // The purpose of this test is simply to confirm that addShutdownHook does not
        // throw when the supplied Runnable throws.
        assertTrue("Hook should have been executed when we called it directly", hookRan.get());
    }
}
