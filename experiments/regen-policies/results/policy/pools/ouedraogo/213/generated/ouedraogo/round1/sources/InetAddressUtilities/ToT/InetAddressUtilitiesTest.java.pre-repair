package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Permission;

import static org.junit.Assert.*;

public class InetAddressUtilitiesTest
{
    private SecurityManager originalSecurityManager;

    /**
     * Simple SecurityManager that denies every permission.
     * This forces {@link InetAddress#getLocalHost()} to throw a
     * {@link SecurityException}, which is caught by the utility methods
     * and causes them to return their fallback values.
     */
    private static class DenyAllSecurityManager extends SecurityManager
    {
        @Override public void checkPermission(Permission perm)
        {
            throw new SecurityException("All permissions denied for test");
        }

        @Override public void checkPermission(Permission perm, Object context)
        {
            throw new SecurityException("All permissions denied for test");
        }
    }

    @Before
    public void saveOriginalSecurityManager()
    {
        originalSecurityManager = System.getSecurityManager();
    }

    @After
    public void restoreOriginalSecurityManager()
    {
        System.setSecurityManager(originalSecurityManager);
    }

    /**
     * Happy‑path test for {@link InetAddressUtilities#getLocalHost()}.
     * Verifies that the method simply forwards the call to
     * {@link InetAddress#getLocalHost()} and never returns {@code null}.
     */
    @Test
    public void testGetLocalHost_success() throws UnknownHostException
    {
        InetAddress expected = InetAddress.getLocalHost();
        InetAddress actual   = InetAddressUtilities.getLocalHost();

        assertNotNull("Local host should not be null", actual);
        assertEquals("Utility should return the same InetAddress as InetAddress.getLocalHost()",
                     expected, actual);
    }

    /**
     * Tests the normal behaviour of {@link InetAddressUtilities#getIpAddress()}.
     * The returned byte array must be equal to the address of the current host.
     */
    @Test
    public void testGetIpAddress_success()
    {
        byte[] expected = InetAddressUtilities.getLocalHost().getAddress();
        byte[] actual   = InetAddressUtilities.getIpAddress();

        assertNotNull("Returned IP byte array must not be null", actual);
        assertArrayEquals("IP address should match the host's address", expected, actual);
        // sanity check – IPv4 is 4 bytes, IPv6 is 16 bytes
        assertTrue("Address length should be either 4 (IPv4) or 16 (IPv6)",
                   actual.length == 4 || actual.length == 16);
    }

    /**
     * Tests the normal behaviour of {@link InetAddressUtilities#getHostName()}.
     * The hostname must be the same as returned by {@link InetAddress#getHostName()}.
     */
    @Test
    public void testGetHostName_success()
    {
        String expected = InetAddressUtilities.getLocalHost().getHostName();
        String actual   = InetAddressUtilities.getHostName();

        assertNotNull("Hostname must not be null", actual);
        assertEquals("Hostname should match the host's name", expected, actual);
    }

    /**
     * Forces a failure in {@code getLocalHost()} by installing a SecurityManager
     * that throws {@link SecurityException}.  The utility methods must then
     * return their documented fallback values.
     */
    @Test
    public void testErrorScenarios_fallbackValues()
    {
        // Install a SecurityManager that denies all permissions.
        System.setSecurityManager(new DenyAllSecurityManager());

        // getIpAddress() should return the 0.0.0.0 placeholder
        byte[] ipFallback = InetAddressUtilities.getIpAddress();
        assertArrayEquals("When InetAddress.getLocalHost() fails, IP fallback must be 0.0.0.0",
                          new byte[] {0, 0, 0, 0}, ipFallback);

        // getHostName() should return the literal "localhost"
        String hostFallback = InetAddressUtilities.getHostName();
        assertEquals("When InetAddress.getLocalHost() fails, hostname fallback must be 'localhost'",
                     "localhost", hostFallback);
    }

    /**
     * Verifies that the utility class cannot be instantiated (private constructor).
     * Reflection is used to attempt instantiation and the expected result is an
     * {@link IllegalAccessException}.
     */
    @Test(expected = IllegalAccessException.class)
    public void testConstructor_isPrivate() throws Exception
    {
        java.lang.reflect.Constructor<InetAddressUtilities> ctor =
                InetAddressUtilities.class.getDeclaredConstructor();
        // The constructor is private; trying to make it accessible should fail.
        ctor.setAccessible(true);
        ctor.newInstance();
    }
}
