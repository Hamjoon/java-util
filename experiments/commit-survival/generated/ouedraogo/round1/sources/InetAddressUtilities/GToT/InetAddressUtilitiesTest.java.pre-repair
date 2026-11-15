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
 * <p>This test class covers:
 * <ul>
 *   <li>normal execution of each public method</li>
 *   <li>fallback behaviour when {@code InetAddress.getLocalHost()} throws an exception</li>
 *   <li>basic edge‑case validation (null checks, array length, default values)</li>
 * </ul>
 *
 * <p>Because the original class does not expose any injection points,
 * the only way to force an exception from {@code getLocalHost()} is to install a
 * {@link SecurityManager} that denies the required permission. The test suite
 * temporarily installs such a manager, runs the method, and then restores the
 * original manager.</p>
 *
 * @author  ChatGPT – test author
 */
public class InetAddressUtilitiesTest
{
    /** Keep a reference to the original SecurityManager so we can restore it after the test. */
    private SecurityManager originalSecurityManager;

    /** Simple SecurityManager that throws a {@link SecurityException} for any permission check. */
    private static class DenyAllSecurityManager extends SecurityManager
    {
        @Override
        public void checkPermission(Permission perm)
        {
            // Deny everything – this will cause InetAddress.getLocalHost() to fail.
            throw new SecurityException("Permission denied for testing fallback behaviour");
        }

        // JDK 1.8 also calls checkPermission with a second argument; override it as well.
        @Override
        public void checkPermission(Permission perm, Object context)
        {
            throw new SecurityException("Permission denied for testing fallback behaviour");
        }
    }

    @Before
    public void setUp()
    {
        // Record the current SecurityManager (may be null) so we can restore it later.
        originalSecurityManager = System.getSecurityManager();
    }

    @After
    public void tearDown()
    {
        // Restore the original SecurityManager regardless of test outcome.
        System.setSecurityManager(originalSecurityManager);
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Tests for public static InetAddress getLocalHost()                     */
    /* ---------------------------------------------------------------------- */

    /**
     * Verify that {@code getLocalHost()} returns a non‑null {@link InetAddress}
     * and that the address is reachable (i.e., does not throw when we ask for its
     * textual representation). The exact value depends on the test environment,
     * but the contract is that it must never be {@code null}.
     */
    @Test
    public void testGetLocalHost_NormalPath() throws UnknownHostException
    {
        InetAddress address = InetAddressUtilities.getLocalHost();
        assertNotNull("getLocalHost() should never return null", address);
        // The host name and address string should be non‑empty.
        assertNotNull("Host name must not be null", address.getHostName());
        assertFalse("Host name must not be empty", address.getHostName().isEmpty());
        assertNotNull("Host address must not be null", address.getHostAddress());
        assertFalse("Host address must not be empty", address.getHostAddress().isEmpty());
    }

    /**
     * Force {@code InetAddress.getLocalHost()} to throw an exception by installing
     * a restrictive SecurityManager and verify that the exception propagates
     * (the method does not catch it – it declares {@code throws UnknownHostException}).
     */
    @Test(expected = SecurityException.class)
    public void testGetLocalHost_ExceptionPath()
    {
        // Install the denying SecurityManager.
        System.setSecurityManager(new DenyAllSecurityManager());

        // The call should now throw SecurityException, which is a subclass of
        // RuntimeException, causing the test to succeed because of the @Test(expected) clause.
        InetAddressUtilities.getLocalHost();
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Tests for public static byte[] getIpAddress()                         */
    /* ---------------------------------------------------------------------- */

    /**
     * Normal path – the method should return the byte representation of the
     * local host address. The length is either 4 (IPv4) or 16 (IPv6). The
     * content is not asserted because it varies per environment.
     */
    @Test
    public void testGetIpAddress_NormalPath()
    {
        byte[] ip = InetAddressUtilities.getIpAddress();
        assertNotNull("IP address byte array must not be null", ip);
        // IPv4 = 4 bytes, IPv6 = 16 bytes. Anything else would be unexpected.
        assertTrue("IP address length should be 4 (IPv4) or 16 (IPv6)",
                ip.length == 4 || ip.length == 16);
    }

    /**
     * Fallback path – when {@code getLocalHost()} throws an exception,
     * the method should return the default array {@code {0,0,0,0}}.
     */
    @Test
    public void testGetIpAddress_Fallback()
    {
        // Install the denying SecurityManager to force an exception inside getIpAddress().
        System.setSecurityManager(new DenyAllSecurityManager());

        byte[] ip = InetAddressUtilities.getIpAddress();

        assertNotNull("Fallback IP address must not be null", ip);
        assertArrayEquals("Fallback IP address should be {0,0,0,0}",
                new byte[] {0, 0, 0, 0}, ip);
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Tests for public static String getHostName()                           */
    /* ---------------------------------------------------------------------- */

    /**
     * Normal path – should return the actual host name (non‑null, non‑empty).
     */
    @Test
    public void testGetHostName_NormalPath()
    {
        String hostName = InetAddressUtilities.getHostName();
        assertNotNull("Host name must not be null", hostName);
        assertFalse("Host name must not be empty", hostName.trim().isEmpty());
    }

    /**
     * Fallback path – when {@code getLocalHost()} throws an exception,
     * the method must return the literal string {@code "localhost"}.
     */
    @Test
    public void testGetHostName_Fallback()
    {
        // Install the denying SecurityManager to trigger the catch block.
        System.setSecurityManager(new DenyAllSecurityManager());

        String hostName = InetAddressUtilities.getHostName();

        assertEquals("Fallback host name should be \"localhost\"",
                "localhost", hostName);
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Additional sanity checks (optional)                                   */
    /* ---------------------------------------------------------------------- */

    /**
     * Verify that the utility class cannot be instantiated (private constructor).
     * Reflection is used to attempt instantiation and should result in an
     * {@link IllegalAccessException} or {@link java.lang.reflect.InvocationTargetException}
     * caused by the private access modifier.
     */
    @Test
    public void testConstructorIsPrivate()
    {
        try
        {
            java.lang.reflect.Constructor<InetAddressUtilities> ctor =
                    InetAddressUtilities.class.getDeclaredConstructor();
            assertTrue("Constructor must be private", java.lang.reflect.Modifier.isPrivate(ctor.getModifiers()));
            // Attempt to make it accessible – should throw a security exception in normal
            // environments, but we just verify that it is indeed private.
            ctor.setAccessible(true);
            ctor.newInstance();
            fail("Instantiating InetAddressUtilities should not be allowed");
        }
        catch (NoSuchMethodException e)
        {
            fail("Expected a no‑arg constructor to exist: " + e);
        }
        catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e)
        {
            // Expected – the constructor is private.
        }
        catch (Exception e)
        {
            // Any other exception means the test failed.
            fail("Unexpected exception while testing private constructor: " + e);
        }
    }
}
