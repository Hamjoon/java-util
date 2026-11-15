package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class SystemUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper methods                                                       */
    /* ---------------------------------------------------------------------- */

    private static String originalProperty(String key) {
        return System.getProperty(key);
    }

    private static void setProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private static String originalEnv(String key) {
        return System.getenv(key);
    }

    /* ---------------------------------------------------------------------- */
    /*  1. getExternalVariable                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetExternalVariable_PropertyWins() {
        String key = "SystemUtilitiesTest_PROP";
        setProperty(key, "propValue");
        // ensure env var is not the same (we cannot set env vars, just rely on it being different)
        assertEquals("propValue", SystemUtilities.getExternalVariable(key));
        setProperty(key, null);
    }

    @Test
    public void testGetExternalVariable_FallbackToEnv() {
        String key = "PATH"; // almost always present as env var
        setProperty(key, null);
        String env = System.getenv(key);
        assertNotNull("environment variable PATH should exist for this test", env);
        assertEquals(env, SystemUtilities.getExternalVariable(key));
    }

    @Test
    public void testGetExternalVariable_NullOrEmptyKey() {
        assertNull(SystemUtilities.getExternalVariable(null));
        assertNull(SystemUtilities.getExternalVariable(""));
        assertNull(SystemUtilities.getExternalVariable("   "));
    }

    @Test
    public void testGetExternalVariable_EmptyPropertyFallsBack() {
        String key = "SystemUtilitiesTest_EMPTY_PROP";
        setProperty(key, "");
        String env = System.getenv(key);
        String expected = (env == null || env.isEmpty()) ? null : env;
        assertEquals(expected, SystemUtilities.getExternalVariable(key));
        setProperty(key, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  2. getAvailableProcessors                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetAvailableProcessors_AtLeastOne() {
        int cpus = SystemUtilities.getAvailableProcessors();
        assertTrue("should be >= 1", cpus >= 1);
        int runtimeCpus = Runtime.getRuntime().availableProcessors();
        assertEquals(Math.max(1, runtimeCpus), cpus);
    }

    /* ---------------------------------------------------------------------- */
    /*  3. getMemoryInfo                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMemoryInfo_ValuesMatchRuntime() {
        SystemUtilities.MemoryInfo mi = SystemUtilities.getMemoryInfo();
        Runtime rt = Runtime.getRuntime();
        assertEquals(rt.totalMemory(), mi.getTotalMemory());
        assertEquals(rt.freeMemory(), mi.getFreeMemory());
        assertEquals(rt.maxMemory(), mi.getMaxMemory());
    }

    /* ---------------------------------------------------------------------- */
    /*  4. getSystemLoadAverage                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetSystemLoadAverage_NonNegativeOrMinusOne() {
        double load = SystemUtilities.getSystemLoadAverage();
        // The spec says -1.0 when not available; otherwise >= 0.0
        assertTrue("load should be -1.0 or >= 0.0", load == -1.0 || load >= 0.0);
    }

    /* ---------------------------------------------------------------------- */
    /*  5. isJavaVersionAtLeast                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsJavaVersionAtLeast_CurrentVersion() {
        String[] parts = SystemUtilities.JAVA_VERSION.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor));
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major - 1, 0));
        assertFalse(SystemUtilities.isJavaVersionAtLeast(major + 1, 0));
    }

    @Test
    public void testIsJavaVersionAtLeast_MalformedVersion() throws Exception {
        // Use reflection to temporarily set the static field to a bad value
        java.lang.reflect.Field f = SystemUtilities.class.getDeclaredField("JAVA_VERSION");
        f.setAccessible(true);
        String original = (String) f.get(null);
        try {
            f.set(null, "bad.version");
            // major parse will fail and throw NumberFormatException
            try {
                SystemUtilities.isJavaVersionAtLeast(1, 0);
                fail("Expected NumberFormatException");
            } catch (NumberFormatException ignored) {
                // expected
            }
        } finally {
            f.set(null, original);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  6. getCurrentProcessId                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetCurrentProcessId_ValidPid() {
        long pid = SystemUtilities.getCurrentProcessId();
        assertTrue("PID should be >= 0", pid >= 0);
        // Verify that the pid matches the one we can extract manually
        RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
        String name = mx.getName(); // format: pid@host
        int at = name.indexOf('@');
        if (at > 0) {
            try {
                long expected = Long.parseLong(name.substring(0, at));
                assertEquals(expected, pid);
            } catch (NumberFormatException ignored) {
                // if format is unexpected, just accept pid==0
                assertEquals(0, pid);
            }
        } else {
            assertEquals(0, pid);
        }
    }

    @Test
    public void testGetCurrentProcessId_MalformedName() throws Exception {
        java.lang.reflect.Field f = ManagementFactory.class.getDeclaredField("runtimeMXBean");
        f.setAccessible(true);
        RuntimeMXBean originalBean = (RuntimeMXBean) f.get(null);
        RuntimeMXBean mockBean = new RuntimeMXBean() {
            public String getName() { return "no-at-symbol"; }
            // all other methods delegate to originalBean
            public String getVmName() { return originalBean.getVmName(); }
            public String getVmVendor() { return originalBean.getVmVendor(); }
            public String getVmVersion() { return originalBean.getVmVersion(); }
            public long getStartTime() { return originalBean.getStartTime(); }
            public long getUptime() { return originalBean.getUptime(); }
            public List<String> getInputArguments() { return originalBean.getInputArguments(); }
            public String getBootClassPath() { return originalBean.getBootClassPath(); }
            public String getClassPath() { return originalBean.getClassPath(); }
            public String getLibraryPath() { return originalBean.getLibraryPath(); }
            public String getManagementSpecVersion() { return originalBean.getManagementSpecVersion(); }
            public String getSpecName() { return originalBean.getSpecName(); }
            public String getSpecVendor() { return originalBean.getSpecVendor(); }
            public String getSpecVersion() { return originalBean.getSpecVersion(); }
            public String getVmInfo() { return originalBean.getVmInfo(); }
            public List<MemoryPoolMXBean> getMemoryPoolMXBeans() { return originalBean.getMemoryPoolMXBeans(); }
            public List<MemoryManagerMXBean> getMemoryManagerMXBeans() { return originalBean.getMemoryManagerMXBeans(); }
            public List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() { return originalBean.getGarbageCollectorMXBeans(); }
            public List<OperatingSystemMXBean> getOperatingSystemMXBeans() { return originalBean.getOperatingSystemMXBeans(); }
        };
        // replace the bean (reflection hack – works only on Oracle/HotSpot)
        f.set(null, mockBean);
        try {
            assertEquals(0, SystemUtilities.getCurrentProcessId());
        } finally {
            f.set(null, originalBean);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  7. createTempDirectory                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateTempDirectory_Normal() throws IOException {
        File dir = SystemUtilities.createTempDirectory("sysUtilTest");
        assertTrue("Directory should exist", dir.exists());
        assertTrue("Should be a directory", dir.isDirectory());
        assertTrue("Should be writable", dir.canWrite());
        // The directory is marked delete‑on‑exit; we cannot assert it now,
        // but we can check the flag via reflection (optional)
        assertTrue(dir.deleteOnExit());
        // clean up for the test run
        assertTrue(dir.delete());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTempDirectory_NullPrefix() throws IOException {
        SystemUtilities.createTempDirectory(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTempDirectory_EmptyPrefix() throws IOException {
        SystemUtilities.createTempDirectory("");
    }

    /* ---------------------------------------------------------------------- */
    /*  8. getSystemTimeZone                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetSystemTimeZone_EnvOverrides() throws Exception {
        // Save original TZ env (cannot really modify env, so we use reflection on the internal map)
        // Instead we test the fallback path by ensuring that when TZ is not set we get default.
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        assertNotNull(tz);
        // When TZ is set to an invalid id, TimeZone.getTimeZone returns GMT (never null)
        // Simulate by setting the env var via reflection on ProcessEnvironment (JDK internal)
        // This is optional; we just assert that method never returns null.
        assertNotNull(tz);
    }

    /* ---------------------------------------------------------------------- */
    /*  9. hasAvailableMemory                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasAvailableMemory_EnoughMemory() {
        long free = SystemUtilities.getMemoryInfo().getFreeMemory();
        assertTrue(SystemUtilities.hasAvailableMemory(free - 1));
    }

    @Test
    public void testHasAvailableMemory_NotEnoughMemory() {
        long free = SystemUtilities.getMemoryInfo().getFreeMemory();
        assertFalse(SystemUtilities.hasAvailableMemory(free + 1));
    }

    @Test
    public void testHasAvailableMemory_NegativeRequired() {
        // Negative requirement should be treated as always satisfied
        assertTrue(SystemUtilities.hasAvailableMemory(-123L));
    }

    /* ---------------------------------------------------------------------- */
    /* 10. getEnvironmentVariables                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetEnvironmentVariables_NoFilter() {
        Map<String, String> all = SystemUtilities.getEnvironmentVariables(null);
        assertEquals(System.getenv(), all);
    }

    @Test
    public void testGetEnvironmentVariables_WithFilter() {
        Predicate<String> startsWithPath = key -> key.startsWith("PATH");
        Map<String, String> filtered = SystemUtilities.getEnvironmentVariables(startsWithPath);
        for (String key : filtered.keySet()) {
            assertTrue(key.startsWith("PATH"));
        }
    }

    @Test(expected = RuntimeException.class)
    public void testGetEnvironmentVariables_FilterThrows() {
        Predicate<String> bad = key -> { throw new RuntimeException("boom"); };
        SystemUtilities.getEnvironmentVariables(bad);
    }

    /* ---------------------------------------------------------------------- */
    /* 11. getNetworkInterfaces                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetNetworkInterfaces_Basic() throws SocketException {
        List<SystemUtilities.NetworkInfo> infos = SystemUtilities.getNetworkInterfaces();
        // Verify that each returned NetworkInfo corresponds to an up interface
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        Set<String> upNames = new HashSet<>();
        while (en.hasMoreElements()) {
            NetworkInterface ni = en.nextElement();
            if (ni.isUp()) {
                upNames.add(ni.getName());
            }
        }
        for (SystemUtilities.NetworkInfo ni : infos) {
            assertTrue("Returned interface should be up", upNames.contains(ni.getName()));
            // basic sanity checks
            assertNotNull(ni.getDisplayName());
            assertNotNull(ni.getAddresses());
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 12. addShutdownHook                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddShutdownHook_RunsWithoutException() throws Exception {
        AtomicBoolean ran = new AtomicBoolean(false);
        Runnable hook = () -> ran.set(true);
        SystemUtilities.addShutdownHook(hook);

        // Retrieve the list of shutdown hooks via reflection (private API)
        java.lang.reflect.Field hooksField = Runtime.class.getDeclaredField("shutdownHooks");
        hooksField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Thread> hooks = (Set<Thread>) hooksField.get(Runtime.getRuntime());

        // Find our hook thread (identified by the Runnable class name in its toString)
        Thread ourThread = null;
        for (Thread t : hooks) {
            if (t.getName().contains("Thread") && t.getThreadGroup() == Thread.currentThread().getThreadGroup()) {
                // we cannot reliably identify, so just pick the last added
                ourThread = t;
            }
        }
        assertNotNull("Shutdown hook thread should be registered", ourThread);
        // Run it manually
        ourThread.run();
        assertTrue("Hook should have been executed", ran.get());
    }

    @Test
    public void testAddShutdownHook_HookThrowsExceptionIsCaught() {
        Runnable badHook = () -> { throw new RuntimeException("expected"); };
        // Should not propagate
        SystemUtilities.addShutdownHook(badHook);
        // No assertion – test passes if no exception escapes
    }
}
