package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Permission;

import static org.junit.Assert.*;

public class InetAddressUtilitiesTest {

    private SecurityManager originalSecurityManager;

    @Before
    public void saveOriginalSecurityManager() {
        originalSecurityManager = System.getSecurityManager();
    }

    @After
    public void restoreSecurityManager() {
        System.setSecurityManager(originalSecurityManager);
    }

    /**
     * Verify that {@link InetAddressUtilities#getLocalHost()} returns a non‑null
     * {@link InetAddress} and that the address is the same as Java's standard
     * {@link InetAddress#getLocalHost()}.
     */
    @Test
    public void testGetLocalHostNotNull() throws UnknownHostException {
        InetAddress expected = InetAddress.getLocalHost();
        InetAddress actual = InetAddressUtilities.getLocalHost();

        assertNotNull("InetAddressUtilities.getLocalHost() should never return null", actual);
        assertEquals("Returned InetAddress should match InetAddress.getLocalHost()", expected, actual);
    }

    /**
     * Verify that {@link InetAddressUtilities#getIpAddress()} returns the byte
     * representation of the address obtained from {@link InetAddressUtilities#getLocalHost()}.
     */
    @Test
    public void testGetIpAddressMatchesLocalHost() throws UnknownHostException {
        byte[] expected = InetAddressUtilities.getLocalHost().getAddress();
        byte[] actual = InetAddressUtilities.getIpAddress();

        assertNotNull("IP address byte array must not be null", actual);
        assertArrayEquals("IP address from getIpAddress() must match getLocalHost().getAddress()", expected, actual);
    }

    /**
     * Verify that {@link InetAddressUtilities#getHostName()} returns the hostname
     * reported by {@link InetAddressUtilities#getLocalHost()}.
     */
    @Test
    public void testGetHostNameMatchesLocalHost() throws UnknownHostException {
        String expected = InetAddressUtilities.getLocalHost().getHostName();
        String actual = InetAddressUtilities.getHostName();

        assertNotNull("Hostname must not be null", actual);
        assertEquals("Hostname from getHostName() must match getLocalHost().getHostName()", expected, actual);
    }

    /**
     * Ensure that the byte array returned by {@code getIpAddress()} is a defensive copy.
     * Mutating the returned array must not affect subsequent calls.
     */
    @Test
    public void testGetIpAddressIsDefensiveCopy() {
        byte[] firstCall = InetAddressUtilities.getIpAddress();
        assertNotNull(firstCall);
        // Corrupt the returned array
        for (int i = 0; i < firstCall.length; i++) {
            firstCall[i] = (byte) ~firstCall[i];
        }

        byte[] secondCall = InetAddressUtilities.getIpAddress();
        // The second call should return the original, unmodified address
        assertArrayEquals("Subsequent calls must return the original address, not the mutated copy",
                secondCall, InetAddressUtilities.getLocalHost().getAddress());
    }

    /**
     * Simulate a failure inside {@code InetAddressUtilities#getIpAddress()} by installing a
     * restrictive {@link SecurityManager} that throws a {@link SecurityException} when
     * {@code InetAddress.getLocalHost()} attempts any privileged operation.
     * The method should then fall back to the default {@code {0,0,0,0}} address.
     */
    @Test
    public void testFallbackIpAddressWhenException() {
        System.setSecurityManager(new RestrictiveSecurityManager());

        byte[] ip = InetAddressUtilities.getIpAddress();

        assertNotNull("Fallback IP address must not be null", ip);
        assertArrayEquals("When an exception occurs, getIpAddress() should return {0,0,0,0}",
                new byte[]{0, 0, 0, 0}, ip);
    }

    /**
     * Simulate a failure inside {@code InetAddressUtilities#getHostName()} using the same
     * restrictive {@link SecurityManager}. The method should fall back to the string
     * {@code "localhost"}.
     */
    @Test
    public void testFallbackHostNameWhenException() {
        System.setSecurityManager(new RestrictiveSecurityManager());

        String hostName = InetAddressUtilities.getHostName();

        assertNotNull("Fallback hostname must not be null", hostName);
        assertEquals("When an exception occurs, getHostName() should return \"localhost\"",
                "localhost", hostName);
    }

    /**
     * A minimal SecurityManager that denies all permission checks, forcing
     * {@code InetAddress.getLocalHost()} to throw a {@link SecurityException}.
     */
    private static class RestrictiveSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            // Deny everything by throwing a SecurityException
            throw new SecurityException("Permission denied by test SecurityManager");
        }

        // The following overrides are required for Java 17+ where the default
        // implementation of checkPermission(Permission, Object) delegates to the
        // single‑argument version.
        @Override
        public void checkPermission(Permission perm, Object context) {
            throw new SecurityException("Permission denied by test SecurityManager");
        }
    }
}
