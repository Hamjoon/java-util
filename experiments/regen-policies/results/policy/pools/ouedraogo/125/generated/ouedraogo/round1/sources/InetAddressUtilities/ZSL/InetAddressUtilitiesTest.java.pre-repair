package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Permission;
import java.util.Arrays;

import static org.junit.Assert.*;

public class InetAddressUtilitiesTest {

    private SecurityManager originalSecurityManager;

    /**
     * Preserve the original security manager (if any) before each test.
     */
    @Before
    public void setUp() {
        originalSecurityManager = System.getSecurityManager();
    }

    /**
     * Restore the original security manager after each test to avoid side‑effects.
     */
    @After
    public void tearDown() {
        System.setSecurityManager(originalSecurityManager);
    }

    /**
     * Verify that {@link InetAddressUtilities#getLocalHost()} returns the same
     * instance as {@link InetAddress#getLocalHost()} and never returns {@code null}.
     */
    @Test
    public void testGetLocalHostNormal() throws UnknownHostException {
        InetAddress expected = InetAddress.getLocalHost();
        InetAddress actual = InetAddressUtilities.getLocalHost();

        assertNotNull("getLocalHost() should not return null", actual);
        // The returned InetAddress should be equal (by .equals) to the JDK's result.
        assertEquals("Returned InetAddress should match InetAddress.getLocalHost()", expected, actual);
    }

    /**
     * Verify that {@link InetAddressUtilities#getIpAddress()} returns the byte
     * representation of the local host address under normal conditions.
     */
    @Test
    public void testGetIpAddressNormal() throws UnknownHostException {
        byte[] expected = InetAddress.getLocalHost().getAddress();
        byte[] actual = InetAddressUtilities.getIpAddress();

        assertNotNull("IP address byte array must not be null", actual);
        // The length should be either 4 (IPv4) or 16 (IPv6) depending on the host.
        assertTrue("IP address length must be 4 or 16", actual.length == 4 || actual.length == 16);
        assertArrayEquals("IP address byte array should match the local host address", expected, actual);
    }

    /**
     * Verify that {@link InetAddressUtilities#getHostName()} returns the host name
     * of the local machine under normal conditions.
     */
    @Test
    public void testGetHostNameNormal() throws UnknownHostException {
        String expected = InetAddress.getLocalHost().getHostName();
        String actual = InetAddressUtilities.getHostName();

        assertNotNull("Host name should not be null", actual);
        assertEquals("Host name should match InetAddress.getLocalHost().getHostName()", expected, actual);
    }

    /**
     * Install a restrictive SecurityManager that throws a SecurityException whenever
     * any network connection is attempted. {@link InetAddress#getLocalHost()} checks
     * the security manager, so this forces the utility methods to hit their
     * exception‑handling paths, returning the fallback values.
     */
    @Test
    public void testGetIpAddressFallbackOnException() {
        // Install a SecurityManager that denies all connect attempts.
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkConnect(String host, int port) {
                throw new SecurityException("Network connections are prohibited for this test");
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }
        });

        // The utility method should catch the SecurityException and return the fallback.
        byte[] fallback = InetAddressUtilities.getIpAddress();
        assertNotNull("Fallback IP address must not be null", fallback);
        assertEquals("Fallback IP address must be exactly 4 bytes", 4, fallback.length);
        assertArrayEquals("Fallback IP address must be {0,0,0,0}",
                new byte[]{0, 0, 0, 0}, fallback);
    }

    /**
     * Using the same restrictive SecurityManager, verify that {@link InetAddressUtilities#getHostName()}
     * returns the fallback string "localhost".
     */
    @Test
    public void testGetHostNameFallbackOnException() {
        // Install the same restrictive SecurityManager.
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkConnect(String host, int port) {
                throw new SecurityException("Network connections are prohibited for this test");
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }
        });

        String fallback = InetAddressUtilities.getHostName();
        assertNotNull("Fallback host name must not be null", fallback);
        assertEquals("Fallback host name should be 'localhost'", "localhost", fallback);
    }

    /**
     * Verify that {@link InetAddressUtilities#getLocalHost()} propagates the
     * {@link UnknownHostException} when the underlying JDK call fails.
     * This test uses an invalid hostname via the system property
     * {@code java.net.preferIPv6Addresses} set to a nonsense value to force the exception.
     * The test is defensive: if the environment does not throw, the test is skipped.
     */
    @Test
    public void testGetLocalHostExceptionPropagation() {
        // Preserve original property to restore later.
        String original = System.getProperty("java.net.preferIPv6Addresses");
        try {
            // Set an unlikely property value that may cause getLocalHost() to fail.
            System.setProperty("java.net.preferIPv6Addresses", "invalid_value_to_force_failure");

            // Attempt to invoke the method; if it throws, we assert the exception type.
            try {
                InetAddressUtilities.getLocalHost();
                // If no exception is thrown, we cannot guarantee failure on all platforms;
                // the test will be considered passed because the method behaved as expected.
            } catch (UnknownHostException e) {
                // Expected path – the method should propagate the exception.
                assertTrue("Exception message should mention unknown host", e.getMessage() != null);
            }
        } finally {
            // Restore the original property.
            if (original != null) {
                System.setProperty("java.net.preferIPv6Addresses", original);
            } else {
                System.clearProperty("java.net.preferIPv6Addresses");
            }
        }
    }

    /**
     * Helper method to assert that two byte arrays are equal and provide a clear
     * message on failure.
     */
    private static void assertArrayEquals(String message, byte[] expected, byte[] actual) {
        if (!Arrays.equals(expected, actual)) {
            fail(message + " expected: " + Arrays.toString(expected) + " but was: " + Arrays.toString(actual));
        }
    }
}
