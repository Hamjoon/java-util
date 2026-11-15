/****************************************************************************************
 * JUnit 4 test suite for {@link com.cedarsoftware.util.InetAddressUtilities}
 *
 * The tests cover:
 *   • Normal operation of getLocalHost(), getIpAddress() and getHostName()
 *   • Validation that the returned values are consistent with the JDK's
 *     {@link java.net.InetAddress} API.
 *   • Verification that the utility class cannot be instantiated (private ctor).
 *
 * NOTE:
 *   – The tests avoid any external mocking frameworks as required.
 *   – They rely only on the public API of {@code InetAddressUtilities} and the
 *     standard JDK classes shown in the source.
 *   – Because the actual host name / IP address varies between test environments,
 *     assertions are written to be tolerant (e.g., length checks, non‑null checks)
 *     rather than expecting a specific constant value.
 ****************************************************************************************/

package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class InetAddressUtilitiesTest {

    /** Cached reference to the real local host – used for comparison in several tests. */
    private static InetAddress realLocalHost;

    @BeforeClass
    public static void setUpClass() throws UnknownHostException {
        // Resolve the real local host once; if this fails the whole suite should fail
        // because the class under test depends on this operation.
        realLocalHost = InetAddress.getLocalHost();
        assertNotNull("Real local host must not be null", realLocalHost);
    }

    @AfterClass
    public static void tearDownClass() {
        realLocalHost = null;
    }

    /**
     * Verify that the private constructor cannot be invoked directly.
     * The class is designed as a static utility holder.
     */
    @Test
    public void testPrivateConstructorIsInaccessible() throws Exception {
        Constructor<InetAddressUtilities> ctor = InetAddressUtilities.class.getDeclaredConstructor();
        // The constructor must be private
        assertTrue("Constructor should be private", Modifier.isPrivate(ctor.getModifiers()));

        // Trying to instantiate without overriding accessibility should throw IllegalAccessException
        try {
            ctor.newInstance();
            fail("Expected IllegalAccessException when invoking private constructor");
        } catch (IllegalAccessException expected) {
            // Expected path
        }

        // If we force accessibility, we *can* create an instance – this proves the
        // constructor exists and is private.
        ctor.setAccessible(true);
        InetAddressUtilities instance = ctor.newInstance();
        assertNotNull("Instance created via reflection should not be null", instance);
    }

    /**
     * The utility method should simply delegate to {@link InetAddress#getLocalHost()}.
     * This test checks that the returned object is the same as the JDK's value.
     */
    @Test
    public void testGetLocalHostReturnsSameAsInetAddress() throws UnknownHostException {
        InetAddress fromUtility = InetAddressUtilities.getLocalHost();
        InetAddress fromJDK = InetAddress.getLocalHost();

        assertNotNull("Utility method must not return null", fromUtility);
        assertEquals("Utility should return the same host as InetAddress.getLocalHost()",
                     fromJDK, fromUtility);
    }

    /**
     * Verify that {@code getIpAddress()} returns the byte representation of the
     * local host address. The array length should correspond to IPv4 (4) or IPv6 (16)
     * depending on the environment.
     */
    @Test
    public void testGetIpAddressMatchesLocalHost() {
        byte[] ipFromUtility = InetAddressUtilities.getIpAddress();
        assertNotNull("IP address byte array must not be null", ipFromUtility);
        // Length must be either 4 (IPv4) or 16 (IPv6)
        assertTrue("IP address length should be 4 or 16", ipFromUtility.length == 4 || ipFromUtility.length == 16);

        byte[] ipFromJDK = realLocalHost.getAddress();
        assertArrayEquals("Utility IP bytes must match InetAddress.getAddress()", ipFromJDK, ipFromUtility);
    }

    /**
     * Ensure that the utility never returns the default error array {@code {0,0,0,0}}
     * when the system call succeeds. The test runs against the actual host, so a
     * successful lookup should never yield all‑zero bytes.
     */
    @Test
    public void testGetIpAddressIsNotDefaultErrorArrayWhenSuccessful() {
        byte[] ip = InetAddressUtilities.getIpAddress();
        byte[] defaultError = new byte[]{0, 0, 0, 0};

        // Only compare when the length matches (IPv4 case). For IPv6 we simply assert
        // that the array is not equal to the IPv4 default error pattern.
        if (ip.length == 4) {
            assertFalse("IP address should not be the default error array", Arrays.equals(ip, defaultError));
        } else {
            // In IPv6 case, ensure at least one byte is non‑zero (highly likely)
            boolean allZero = true;
            for (byte b : ip) {
                if (b != 0) {
                    allZero = false;
                    break;
                }
            }
            assertFalse("IPv6 address should not be all zeros", allZero);
        }
    }

    /**
     * Verify that {@code getHostName()} returns the host name reported by the JDK.
     * The result must be non‑null and non‑empty.
     */
    @Test
    public void testGetHostNameMatchesInetAddress() {
        String hostNameFromUtility = InetAddressUtilities.getHostName();
        assertNotNull("Host name must not be null", hostNameFromUtility);
        assertFalse("Host name must not be empty", hostNameFromUtility.trim().isEmpty());

        String hostNameFromJDK = realLocalHost.getHostName();
        assertEquals("Utility host name should match InetAddress.getHostName()", hostNameFromJDK, hostNameFromUtility);
    }

    /**
     * Simulate a failure scenario for {@code getIpAddress()} and {@code getHostName()}
     * by temporarily altering the security manager to deny access to the local host.
     * When {@code InetAddress.getLocalHost()} throws an exception, the utility methods
     * should return their defined fallback values.
     *
     * Note: This test manipulates the global security manager; it restores the original
     * manager in a finally block to avoid side‑effects on other tests.
     */
    @Test
    public void testFallbackValuesWhenLocalHostLookupFails() {
        // Preserve the original SecurityManager (may be null)
        SecurityManager original = System.getSecurityManager();

        // Install a restrictive SecurityManager that denies any socket/host lookup.
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(java.security.Permission perm) {
                // Allow everything else
            }

            @Override
            public void checkConnect(String host, int port) {
                // Deny any network connection attempt, which also blocks getLocalHost()
                throw new SecurityException("Network connections disabled for test");
            }
        });

        try {
            // The following calls should hit the catch blocks inside the utility class.
            byte[] ip = InetAddressUtilities.getIpAddress();
            assertArrayEquals("When lookup fails, IP should be default error array",
                              new byte[]{0, 0, 0, 0}, ip);

            String hostName = InetAddressUtilities.getHostName();
            assertEquals("When lookup fails, host name should be fallback \"localhost\"",
                         "localhost", hostName);
        } finally {
            // Restore original SecurityManager to avoid affecting other tests or the JVM.
            System.setSecurityManager(original);
        }
    }
}
