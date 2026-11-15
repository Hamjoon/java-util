package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Permission;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link InetAddressUtilities}.
 *
 * The suite covers:
 * 1. Normal (happy‑path) behaviour of all public static methods.
 * 2. Edge‑case / exception handling when {@code InetAddress.getLocalHost()}
 *    is prevented from succeeding (simulated via a restrictive SecurityManager).
 *
 * No external mocking frameworks are used – only real JDK classes and the
 * public API provided by {@code InetAddressUtilities}.
 */
public class InetAddressUtilitiesTest {

    /** Keep a reference to the original SecurityManager so we can restore it after each test. */
    private SecurityManager originalSecurityManager;

    @Before
    public void saveOriginalSecurityManager() {
        originalSecurityManager = System.getSecurityManager();
    }

    @After
    public void restoreSecurityManager() {
        System.setSecurityManager(originalSecurityManager);
    }

    /* --------------------------------------------------------------------- */
    /* 1. HAPPY‑PATH TESTS                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetLocalHostReturnsNonNull() throws UnknownHostException {
        InetAddress addr = InetAddressUtilities.getLocalHost();
        assertNotNull("getLocalHost() should never return null", addr);
        // The address should be a real host address; loopback is acceptable on CI servers.
        assertTrue("Returned address must be an InetAddress instance",
                addr instanceof InetAddress);
    }

    @Test
    public void testGetIpAddressReturnsValidLength() {
        byte[] ip = InetAddressUtilities.getIpAddress();
        assertNotNull("IP byte array must not be null", ip);
        // IPv4 => 4 bytes, IPv6 => 16 bytes.  No other length is expected.
        assertTrue("IP byte array should be length 4 (IPv4) or 16 (IPv6)",
                ip.length == 4 || ip.length == 16);
    }

    @Test
    public void testGetHostNameReturnsNonEmptyString() {
        String host = InetAddressUtilities.getHostName();
        assertNotNull("Host name must not be null", host);
        assertFalse("Host name should not be empty", host.trim().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 2. EDGE‑CASE / EXCEPTION HANDLING TESTS                               */
    /* --------------------------------------------------------------------- */

    /**
     * A SecurityManager that denies *all* permission checks by throwing
     * a SecurityException.  This forces {@code InetAddress.getLocalHost()}
     * to fail, exercising the catch‑block in the utility methods.
     */
    private static class DenyAllSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            throw new SecurityException("Permission denied by test SecurityManager");
        }

        // The deprecated overload is also overridden for completeness.
        @Override
        public void checkPermission(Permission perm, Object context) {
            throw new SecurityException("Permission denied by test SecurityManager");
        }
    }

    @Test
    public void testGetIpAddressFallbackWhenLocalHostFails() {
        System.setSecurityManager(new DenyAllSecurityManager());

        byte[] ip = InetAddressUtilities.getIpAddress();

        assertNotNull("Fallback IP array must not be null", ip);
        assertEquals("Fallback IP must contain exactly 4 bytes", 4, ip.length);
        assertArrayEquals("Fallback IP must be all zeros",
                new byte[]{0, 0, 0, 0}, ip);
    }

    @Test
    public void testGetHostNameFallbackWhenLocalHostFails() {
        System.setSecurityManager(new DenyAllSecurityManager());

        String host = InetAddressUtilities.getHostName();

        assertNotNull("Fallback host name must not be null", host);
        assertEquals("Fallback host name must be the literal \"localhost\"",
                "localhost", host);
    }
}
