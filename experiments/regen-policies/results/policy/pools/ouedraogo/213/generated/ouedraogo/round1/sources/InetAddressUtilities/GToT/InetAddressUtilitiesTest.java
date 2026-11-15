package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Permission;

/**
 * Comprehensive JUnit‑4 test suite for {@link InetAddressUtilities}.
 *
 * The suite covers:
 *   • Normal (“happy‑path”) behaviour of all public methods.
 *   • Edge‑cases where the underlying {@code InetAddress.getLocalHost()}
 *     call fails and the utility class must return its fallback values.
 *
 * No mocking frameworks are used – only the standard JDK APIs and the
 * class under test.
 */
public class InetAddressUtilitiesTest {

    /** Keep a reference to the original SecurityManager so we can restore it. */
    private SecurityManager originalSecurityManager;

    /** --------------------------------------------------------------------
     *  Helper SecurityManager that denies every permission, causing
     *  InetAddress.getLocalHost() to throw a SecurityException.
     *  --------------------------------------------------------------------
     */
    private static class DenyAllSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            // allow everything else (e.g., reflection) – we only care about
            // socket/inet permissions which are checked via the overloaded method.
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // same as above
        }

        @Override
        public void checkConnect(String host, int port) {
            throw new SecurityException("Connection denied by test SecurityManager");
        }

        @Override
        public void checkConnect(String host, int port, Object context) {
            throw new SecurityException("Connection denied by test SecurityManager");
        }

        @Override
        public void checkListen(int port) {
            throw new SecurityException("Listen denied by test SecurityManager");
        }

        @Override
        public void checkAccept(String host, int port) {
            throw new SecurityException("Accept denied by test SecurityManager");
        }
    }

    /** --------------------------------------------------------------------
     *  Setup / teardown – preserve the original SecurityManager.
     *  --------------------------------------------------------------------
     */
    @Before
    public void setUp() {
        originalSecurityManager = System.getSecurityManager();
    }

    @After
    public void tearDown() {
        System.setSecurityManager(originalSecurityManager);
    }

    // -----------------------------------------------------------------------
    //  1️⃣  Happy‑path tests
    // -----------------------------------------------------------------------

    /** Verify that getLocalHost() returns the same value as InetAddress.getLocalHost(). */
    @Test
    public void testGetLocalHost_HappyPath() throws UnknownHostException {
        InetAddress expected = InetAddress.getLocalHost();
        InetAddress actual   = InetAddressUtilities.getLocalHost();
        assertNotNull("Returned InetAddress must not be null", actual);
        assertEquals("Utility method should return the JVM's local host address",
                     expected, actual);
    }

    /** Verify that getIpAddress() returns the byte array of the local host address. */
    @Test
    public void testGetIpAddress_HappyPath() throws UnknownHostException {
        byte[] expected = InetAddress.getLocalHost().getAddress();
        byte[] actual   = InetAddressUtilities.getIpAddress();

        assertNotNull("IP address byte array must not be null", actual);
        assertArrayEquals("IP address byte array should match the JVM's local address",
                          expected, actual);
    }

    /** Verify that getHostName() returns the host name of the local machine. */
    @Test
    public void testGetHostName_HappyPath() throws UnknownHostException {
        String expected = InetAddress.getLocalHost().getHostName();
        String actual   = InetAddressUtilities.getHostName();

        assertNotNull("Host name must not be null", actual);
        assertFalse("Host name must not be empty", actual.isEmpty());
        assertEquals("Host name should match the JVM's local host name",
                     expected, actual);
    }

    // -----------------------------------------------------------------------
    //  2️⃣  Edge‑case / exception handling tests
    // -----------------------------------------------------------------------

    /**
     * The only way to provoke an {@code UnknownHostException} from
     * {@code InetAddressUtilities.getLocalHost()} without mocking is to
     * temporarily replace the system property that drives name resolution
     * with an invalid value and then call the method directly.
     *
     * The test asserts that the declared exception is indeed thrown.
     */
    @Test(expected = UnknownHostException.class)
    public void testGetLocalHost_ThrowsUnknownHostException() throws UnknownHostException {
        // Save the original property so we can restore it after the test.
        String original = System.getProperty("java.net.preferIPv4Stack");
        try {
            // Force the JVM to try an impossible configuration.
            System.setProperty("java.net.preferIPv4Stack", "invalid_boolean_value");
            // The call below should now fail with UnknownHostException.
            InetAddressUtilities.getLocalHost();
        } finally {
            // Restore original property (or clear if it was null).
            if (original == null) {
                System.clearProperty("java.net.preferIPv4Stack");
            } else {
                System.setProperty("java.net.preferIPv4Stack", original);
            }
        }
    }

    /** When a SecurityException is thrown inside getIpAddress(), the fallback {0,0,0,0} must be returned. */
    @Test
    public void testGetIpAddress_FallbackOnSecurityException() {
        // Install the restrictive SecurityManager.
        System.setSecurityManager(new DenyAllSecurityManager());

        byte[] result = InetAddressUtilities.getIpAddress();

        assertNotNull("Result must never be null", result);
        assertArrayEquals("When permission is denied, fallback IP must be 0.0.0.0",
                          new byte[] {0, 0, 0, 0},
                          result);
    }

    /** When a SecurityException is thrown inside getHostName(), the fallback "localhost" must be returned. */
    @Test
    public void testGetHostName_FallbackOnSecurityException() {
        // Install the restrictive SecurityManager.
        System.setSecurityManager(new DenyAllSecurityManager());

        String result = InetAddressUtilities.getHostName();

        assertNotNull("Result must never be null", result);
        assertEquals("When permission is denied, fallback host name must be 'localhost'",
                     "localhost", result);
    }
}
