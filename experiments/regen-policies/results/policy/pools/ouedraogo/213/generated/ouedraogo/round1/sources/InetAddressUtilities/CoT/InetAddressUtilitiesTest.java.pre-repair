/****************************************************************************************
 *  JUnit‑4 test suite for {@code com.cedarsoftware.util.InetAddressUtilities}
 *
 *  The test class follows the requested steps:
 *   1. Lists all public static methods of the target class.
 *   2. Provides a basic test for each method that verifies the normal contract.
 *   3. Describes edge‑case / exception scenarios that could arise.
 *   4. Supplies additional test methods that deliberately trigger those edge cases
 *      (where possible without using mocks or external libraries).
 *
 *  Because the implementation directly calls {@code InetAddress.getLocalHost()},
 *  we cannot reliably force an {@code UnknownHostException} or other runtime
 *  failures without mocking.  The Java standard library does not expose a way to
 *  inject a failing implementation, and the exercise forbids the use of mocks.
 *  Consequently the edge‑case tests are limited to verifying the *fallback*
 *  behaviour when the call *does* fail – this is demonstrated by temporarily
 *  installing a custom {@code SecurityManager} that denies the permission required
 *  by {@code InetAddress.getLocalHost()}.  When the permission is denied, the
 *  method throws a {@code SecurityException}, which is caught by the utility
 *  methods and results in the default values (0.0.0.0 or “localhost”).
 *
 *  The test suite can be executed on any JVM (Java 8+).  It does not depend on
 *  any external libraries other than JUnit‑4.
 ****************************************************************************************/

package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.SecurityPermission;

import static org.junit.Assert.*;

public class InetAddressUtilitiesTest {

    /* -------------------------------------------------------------------------
     *  1. LIST OF PUBLIC METHODS (signatures)
     * -------------------------------------------------------------------------
     *
     *  public static InetAddress getLocalHost() throws UnknownHostException
     *  public static byte[]      getIpAddress()
     *  public static String      getHostName()
     *
     * -------------------------------------------------------------------------
     */

    /* -------------------------------------------------------------------------
     *  2. BASIC FUNCTIONALITY TESTS
     * -------------------------------------------------------------------------
     */

    /** Verify that {@code getLocalHost()} returns a non‑null InetAddress and
     *  that the address is reachable (i.e. does not throw an exception when
     *  calling {@code getHostAddress()}).
     */
    @Test
    public void testGetLocalHost_Normal() throws UnknownHostException {
        InetAddress address = InetAddressUtilities.getLocalHost();
        assertNotNull("getLocalHost() should never return null", address);
        // The string representation must be non‑empty
        assertFalse("Host address should not be empty",
                address.getHostAddress().isEmpty());
    }

    /** Verify that {@code getIpAddress()} returns a 4‑byte array for typical
     *  IPv4 hosts and that the bytes correspond to the address returned by
     *  {@code getLocalHost()}.
     */
    @Test
    public void testGetIpAddress_Normal() throws UnknownHostException {
        byte[] ip = InetAddressUtilities.getIpAddress();
        assertNotNull("IP byte array must not be null", ip);
        // Most environments resolve to an IPv4 address; if IPv6 is used the length is 16.
        // The utility does not differentiate, so we only assert that the array is non‑empty.
        assertTrue("IP byte array should contain at least one byte", ip.length > 0);

        // Cross‑check with the address from getLocalHost()
        byte[] expected = InetAddressUtilities.getLocalHost().getAddress();
        assertArrayEquals("IP byte array should match InetAddress.getAddress()", expected, ip);
    }

    /** Verify that {@code getHostName()} returns a non‑null, non‑empty string. */
    @Test
    public void testGetHostName_Normal() {
        String hostName = InetAddressUtilities.getHostName();
        assertNotNull("Host name must not be null", hostName);
        assertFalse("Host name must not be empty", hostName.isEmpty());
    }

    /* -------------------------------------------------------------------------
     *  3. EDGE‑CASE / EXCEPTION SCENARIOS
     * -------------------------------------------------------------------------
     *
     *  The utility methods catch *any* Exception thrown by
     *  InetAddress.getLocalHost() and substitute a default value:
     *
     *      - getIpAddress()  → returns {0,0,0,0}
     *      - getHostName()   → returns "localhost"
     *
     *  To trigger this path without mocks we temporarily install a SecurityManager
     *  that denies the permission required to perform the DNS lookup performed by
     *  InetAddress.getLocalHost().  The denial results in a SecurityException,
     *  which is caught by the utility methods.
     *
     *  After the test we restore the original SecurityManager.
     * -------------------------------------------------------------------------
     */

    private SecurityManager originalSecurityManager;

    @Before
    public void setUp() {
        originalSecurityManager = System.getSecurityManager();
    }

    @After
    public void tearDown() {
        System.setSecurityManager(originalSecurityManager);
    }

    /** Helper SecurityManager that throws SecurityException for any
     *  attempt to access the network (i.e., for the "resolve" permission). */
    private static class DenyNetworkSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            // Allow everything except network resolve permission
            if ("java.net.SocketPermission".equals(perm.getClass().getName())
                    && perm.getName().contains("resolve")) {
                throw new SecurityException("Network resolve denied for testing");
            }
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            checkPermission(perm);
        }
    }

    /** When InetAddress.getLocalHost() throws an exception, getIpAddress()
     *  must return the default 0.0.0.0 byte array. */
    @Test
    public void testGetIpAddress_FallbackOnException() {
        System.setSecurityManager(new DenyNetworkSecurityManager());

        byte[] ip = InetAddressUtilities.getIpAddress();
        assertNotNull("Fallback IP must not be null", ip);
        assertArrayEquals("Fallback IP should be {0,0,0,0}",
                new byte[]{0, 0, 0, 0}, ip);
    }

    /** When InetAddress.getLocalHost() throws an exception, getHostName()
     *  must return the literal string "localhost". */
    @Test
    public void testGetHostName_FallbackOnException() {
        System.setSecurityManager(new DenyNetworkSecurityManager());

        String host = InetAddressUtilities.getHostName();
        assertNotNull("Fallback host name must not be null", host);
        assertEquals("Fallback host name should be \"localhost\"",
                "localhost", host);
    }

    /** getLocalHost() propagates UnknownHostException.  This test verifies that
     *  the exception type is preserved when the underlying call fails.  We achieve
     *  the failure by using an invalid hostname via the system property
     *  "java.rmi.server.hostname".  InetAddress.getLocalHost() prefers this
     *  property; setting it to an unresolvable name forces an UnknownHostException.
     */
    @Test(expected = UnknownHostException.class)
    public void testGetLocalHost_ThrowsUnknownHostException() throws UnknownHostException {
        // Preserve original property value
        String original = System.getProperty("java.rmi.server.hostname");
        try {
            System.setProperty("java.rmi.server.hostname", "nonexistent.invalid.host.name");
            // The call should now throw UnknownHostException
            InetAddressUtilities.getLocalHost();
        } finally {
            // Restore original property (or clear if it was null)
            if (original != null) {
                System.setProperty("java.rmi.server.hostname", original);
            } else {
                System.clearProperty("java.rmi.server.hostname");
            }
        }
    }
}
