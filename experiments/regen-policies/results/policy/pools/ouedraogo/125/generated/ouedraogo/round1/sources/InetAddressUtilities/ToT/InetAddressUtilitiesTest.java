package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link InetAddressUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical successful execution of each public static method.</li>
 *   <li>Verification that the utility class cannot be instantiated (private constructor).</li>
 *   <li>Edge‑case handling when {@code InetAddress.getLocalHost()} throws an exception.
 *       This is simulated by installing a {@link SecurityManager} that forbids
 *       the required network operation, causing the utility methods to fall back
 *       to their error‑path return values.</li>
 * </ul>
 *
 * No external mocking frameworks are used; the tests rely only on the
 * public API present in the source code and the standard JDK classes.
 */
public class InetAddressUtilitiesTest {

    // -----------------------------------------------------------------------
    // Helper SecurityManager to force an exception from InetAddress.getLocalHost()
    // -----------------------------------------------------------------------
    private static class DenyNetworkSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(java.security.Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkConnect(String host, int port) {
            // Disallow any attempt to resolve a host – this will cause
            // InetAddress.getLocalHost() to throw a SecurityException which is
            // caught by the utility methods and triggers the error path.
            throw new SecurityException("Network access denied for testing");
        }
    }

    private static SecurityManager originalSecurityManager;

    @BeforeClass
    public static void saveOriginalSecurityManager() {
        originalSecurityManager = System.getSecurityManager();
    }

    @AfterClass
    public static void restoreOriginalSecurityManager() {
        System.setSecurityManager(originalSecurityManager);
    }

    // -----------------------------------------------------------------------
    // 1. Test that the private constructor cannot be invoked externally.
    // -----------------------------------------------------------------------
    @Test
    public void testPrivateConstructorIsInaccessible() throws Exception {
        Constructor<InetAddressUtilities> ctor = InetAddressUtilities.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", java.lang.reflect.Modifier.isPrivate(ctor.getModifiers()));

        // Even though it is private, we can make it accessible via reflection.
        // The class is designed to be non‑instantiable, so invoking it should
        // succeed but produce an object that cannot be used meaningfully.
        // We verify that no exception is thrown and that the instance is of the correct type.
        ctor.setAccessible(true);
        Object instance = ctor.newInstance();
        assertNotNull("Instance created via reflection should not be null", instance);
        assertTrue("Instance should be of type InetAddressUtilities", instance instanceof InetAddressUtilities);
    }

    // -----------------------------------------------------------------------
    // 2. Test getLocalHost() – normal operation.
    // -----------------------------------------------------------------------
    @Test
    public void testGetLocalHostSuccess() throws UnknownHostException {
        InetAddress result = InetAddressUtilities.getLocalHost();
        assertNotNull("getLocalHost() must not return null", result);

        // The result should be equal to the standard JDK call.
        InetAddress expected = InetAddress.getLocalHost();
        assertEquals("Utility method should return the same address as InetAddress.getLocalHost()", expected, result);
    }

    // -----------------------------------------------------------------------
    // 3. Test getIpAddress() – normal operation.
    // -----------------------------------------------------------------------
    @Test
    public void testGetIpAddressSuccess() throws UnknownHostException {
        byte[] ipFromUtility = InetAddressUtilities.getIpAddress();
        assertNotNull("IP address byte array must not be null", ipFromUtility);
        assertTrue("IP address byte array must have length 4 (IPv4) or 16 (IPv6)",
                ipFromUtility.length == 4 || ipFromUtility.length == 16);

        // Verify that the bytes correspond to the address returned by InetAddress.getLocalHost().
        byte[] expected = InetAddress.getLocalHost().getAddress();
        assertArrayEquals("Utility IP bytes must match InetAddress.getLocalHost() bytes", expected, ipFromUtility);
    }

    // -----------------------------------------------------------------------
    // 4. Test getHostName() – normal operation.
    // -----------------------------------------------------------------------
    @Test
    public void testGetHostNameSuccess() throws UnknownHostException {
        String hostName = InetAddressUtilities.getHostName();
        assertNotNull("Host name must not be null", hostName);
        assertFalse("Host name must not be empty", hostName.isEmpty());

        // The name should be the same as the one returned by the JDK.
        String expected = InetAddress.getLocalHost().getHostName();
        assertEquals("Utility host name must match InetAddress.getLocalHost() host name", expected, hostName);
    }

    // -----------------------------------------------------------------------
    // 5. Test error path for getIpAddress() – force an exception.
    // -----------------------------------------------------------------------
    @Test
    public void testGetIpAddressErrorPath() {
        System.setSecurityManager(new DenyNetworkSecurityManager());
        try {
            byte[] ip = InetAddressUtilities.getIpAddress();
            assertNotNull("Even on error, method must return a non‑null array", ip);
            assertArrayEquals("Error path must return 0.0.0.0", new byte[]{0, 0, 0, 0}, ip);
        } finally {
            // Restore the original security manager so later tests are unaffected.
            System.setSecurityManager(originalSecurityManager);
        }
    }

    // -----------------------------------------------------------------------
    // 6. Test error path for getHostName() – force an exception.
    // -----------------------------------------------------------------------
    @Test
    public void testGetHostNameErrorPath() {
        System.setSecurityManager(new DenyNetworkSecurityManager());
        try {
            String host = InetAddressUtilities.getHostName();
            assertNotNull("Even on error, method must return a non‑null string", host);
            assertEquals("Error path must return the literal \"localhost\"", "localhost", host);
        } finally {
            System.setSecurityManager(originalSecurityManager);
        }
    }

    // -----------------------------------------------------------------------
    // 7. Test error path for getLocalHost() – ensure the exception propagates.
    // -----------------------------------------------------------------------
    @Test(expected = SecurityException.class)
    public void testGetLocalHostErrorPath() {
        System.setSecurityManager(new DenyNetworkSecurityManager());
        try {
            // This call should throw a SecurityException because the security manager
            // blocks the underlying network operation. The utility method does not
            // catch this exception; it propagates it to the caller.
            InetAddressUtilities.getLocalHost();
        } finally {
            System.setSecurityManager(originalSecurityManager);
        }
    }
}
