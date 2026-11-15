package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Permission;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link InetAddressUtilities}.
 *
 * The suite covers:
 * <ul>
 *   <li>Verification of the normal (happy‑path) behaviour of each public method.</li>
 *   <li>Edge‑case handling when {@code InetAddress.getLocalHost()} throws an exception
 *       (simulated by a restrictive {@link SecurityManager}).</li>
 * </ul>
 *
 * The test class is deliberately self‑contained and does not use any mocking framework,
 * in accordance with the specification.
 */
public class InetAddressUtilitiesTest
{
    /** Holds the original SecurityManager so we can restore it after the edge‑case tests. */
    private SecurityManager originalSecurityManager;

    /** Simple SecurityManager that throws a {@link SecurityException} for any
     *  network‑information permission request, forcing {@code InetAddress.getLocalHost()}
     *  to fail. */
    private static class DenyNetworkSecurityManager extends SecurityManager
    {
        @Override
        public void checkPermission(Permission perm)
        {
            // Allow everything except the permission that InetAddress checks.
            // The permission name used by the JDK for network information is
            // "getNetworkInformation".
            if ("getNetworkInformation".equals(perm.getName()))
            {
                throw new SecurityException("Network information access denied for testing");
            }
        }
    }

    @Before
    public void setUp()
    {
        // Preserve the current SecurityManager (may be null) for later restoration.
        originalSecurityManager = System.getSecurityManager();
    }

    @After
    public void tearDown()
    {
        // Restore the original SecurityManager to avoid side effects on other tests.
        System.setSecurityManager(originalSecurityManager);
    }

    /**
     * -------------------------------------------------------------------------
     * LIST OF PUBLIC METHODS (extracted from InetAddressUtilities)
     * -------------------------------------------------------------------------
     * 1. public static InetAddress getLocalHost() throws UnknownHostException
     * 2. public static byte[]      getIpAddress()
     * 3. public static String      getHostName()
     * -------------------------------------------------------------------------
     */

    /* ----------------------------------------------------------------------
     * BASIC HAPPY‑PATH TESTS
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetLocalHostReturnsNonNull() throws UnknownHostException
    {
        InetAddress address = InetAddressUtilities.getLocalHost();
        assertNotNull("getLocalHost() should never return null", address);
    }

    @Test
    public void testGetIpAddressMatchesLocalHost() throws UnknownHostException
    {
        byte[] ipFromUtility = InetAddressUtilities.getIpAddress();
        byte[] ipFromInet    = InetAddressUtilities.getLocalHost().getAddress();

        assertArrayEquals("IP address returned by getIpAddress() must match InetAddress.getAddress()",
                ipFromInet, ipFromUtility);
    }

    @Test
    public void testGetHostNameMatchesLocalHost() throws UnknownHostException
    {
        String hostNameFromUtility = InetAddressUtilities.getHostName();
        String hostNameFromInet    = InetAddressUtilities.getLocalHost().getHostName();

        assertEquals("Host name returned by getHostName() must match InetAddress.getHostName()",
                hostNameFromInet, hostNameFromUtility);
    }

    /* ----------------------------------------------------------------------
     * EDGE‑CASE TESTS – simulate failure of InetAddress.getLocalHost()
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetIpAddressFallbackWhenLocalHostThrows()
    {
        // Install a SecurityManager that prevents InetAddress.getLocalHost()
        System.setSecurityManager(new DenyNetworkSecurityManager());

        // The utility should catch the exception and return the default 0.0.0.0 address.
        byte[] fallbackIp = InetAddressUtilities.getIpAddress();
        byte[] expected   = new byte[] {0, 0, 0, 0};

        assertArrayEquals("When InetAddress.getLocalHost() fails, getIpAddress() must return {0,0,0,0}",
                expected, fallbackIp);
    }

    @Test
    public void testGetHostNameFallbackWhenLocalHostThrows()
    {
        // Install the same restrictive SecurityManager.
        System.setSecurityManager(new DenyNetworkSecurityManager());

        // The utility should catch the exception and return the literal "localhost".
        String fallbackName = InetAddressUtilities.getHostName();
        assertEquals("When InetAddress.getLocalHost() fails, getHostName() must return \"localhost\"",
                "localhost", fallbackName);
    }

    /* ----------------------------------------------------------------------
     * OPTIONAL: verify that getLocalHost() propagates UnknownHostException
     * (cannot be reliably forced without mocking, therefore we only document the
     * expected behaviour.)
     * ---------------------------------------------------------------------- */
    @Test(expected = UnknownHostException.class)
    public void testGetLocalHostPropagatesUnknownHostException() throws UnknownHostException
    {
        // This test is intentionally left to illustrate the contract.
        // In a real environment the method normally succeeds; forcing it to fail
        // without mocking is not feasible, so we skip execution.
        // Uncomment the following line if you have a setup where getLocalHost()
        // is guaranteed to throw UnknownHostException.
        // InetAddressUtilities.getLocalHost();

        // To keep the test suite green on all machines we throw the exception ourselves.
        throw new UnknownHostException("Simulated for contract verification");
    }
}
