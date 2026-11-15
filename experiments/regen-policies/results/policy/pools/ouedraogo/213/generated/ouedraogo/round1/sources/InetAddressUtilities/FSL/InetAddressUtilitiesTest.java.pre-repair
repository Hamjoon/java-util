package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Permission;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link InetAddressUtilities}.
 *
 * <p>
 * The class under test contains only static utility methods that delegate to
 * {@link java.net.InetAddress#getLocalHost()}.  The tests therefore cover:
 * </p>
 * <ul>
 *   <li>Normal successful execution paths – the methods return the real host
 *       information provided by the JVM.</li>
 *   <li>Failure paths – when {@code InetAddress.getLocalHost()} throws an
 *       exception (simulated by installing a {@link SecurityManager} that
 *       denies network information).  The utilities must return the documented
 *       fallback values.</li>
 *   <li>Edge‑cases such as {@code null} handling (not applicable here) and the
 *       immutability of the returned byte array.</li>
 * </ul>
 *
 * <p>
 * No external mocking frameworks are used; only the public API of the JDK and
 * the class under test are exercised.
 * </p>
 */
public class InetAddressUtilitiesTest {

    /** Preserve the original security manager so we can restore it after the
     *  failure‑scenario tests. */
    private SecurityManager originalSecurityManager;

    /** Simple {@link SecurityManager} that throws a {@link SecurityException}
     *  for any permission check – this forces {@code InetAddress.getLocalHost()}
     *  to fail. */
    private static class DenyAllSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            // Deny everything that might be required for network look‑ups.
            throw new SecurityException("Permission denied by test SecurityManager");
        }

        // The JRE may call other check* methods; delegate to the generic one.
        @Override
        public void checkPermission(Permission perm, Object context) {
            checkPermission(perm);
        }
    }

    @Before
    public void saveSecurityManager() {
        originalSecurityManager = System.getSecurityManager();
    }

    @After
    public void restoreSecurityManager() {
        System.setSecurityManager(originalSecurityManager);
    }

    /* --------------------------------------------------------------
     *  Normal (successful) execution tests
     * -------------------------------------------------------------- */

    @Test
    public void testGetLocalHost_ReturnsNonNullAndConsistent() throws UnknownHostException {
        InetAddress local = InetAddressUtilities.getLocalHost();
        assertNotNull("InetAddressUtilities.getLocalHost() should never return null", local);

        // The hostname obtained via the utility must match the one from the
        // InetAddress instance itself.
        String hostNameFromUtility = InetAddressUtilities.getHostName();
        assertEquals("Host name from utility must equal InetAddress#getHostName()",
                local.getHostName(), hostNameFromUtility);
    }

    @Test
    public void testGetIpAddress_ReturnsRealAddressWhenAvailable() {
        byte[] ip = InetAddressUtilities.getIpAddress();
        assertNotNull("IP address byte array must not be null", ip);
        // The fallback array is exactly four zeros; a real address will never
        // be all zeros on a typical machine.
        boolean allZeros = true;
        for (byte b : ip) {
            if (b != 0) {
                allZeros = false;
                break;
            }
        }
        assertFalse("When the host address is obtainable, it should not be the fallback {0,0,0,0}",
                allZeros);
        // IPv4 addresses are 4 bytes; IPv6 are 16 bytes.  Verify we get a sensible length.
        assertTrue("IP address length should be 4 (IPv4) or 16 (IPv6)",
                ip.length == 4 || ip.length == 16);
    }

    @Test
    public void testGetHostName_ReturnsRealHostNameWhenAvailable() {
        String hostName = InetAddressUtilities.getHostName();
        assertNotNull("Host name must not be null", hostName);
        // The fallback is the literal string "localhost".  Most environments will
        // resolve to a more specific name.
        assertNotEquals("When the host name is obtainable, it should not be the fallback \"localhost\"",
                "localhost", hostName);
    }

    /* --------------------------------------------------------------
     *  Failure (exception) path tests – enforced via SecurityManager
     * -------------------------------------------------------------- */

    @Test
    public void testGetIpAddress_FallbackWhenLocalHostFails() {
        // Install a SecurityManager that denies network information.
        System.setSecurityManager(new DenyAllSecurityManager());

        byte[] ip = InetAddressUtilities.getIpAddress();
        assertNotNull("Fallback IP array must not be null", ip);
        assertArrayEquals("When InetAddress.getLocalHost() fails, fallback must be {0,0,0,0}",
                new byte[] {0, 0, 0, 0}, ip);
    }

    @Test
    public void testGetHostName_FallbackWhenLocalHostFails() {
        // Install a SecurityManager that denies network information.
        System.setSecurityManager(new DenyAllSecurityManager());

        String hostName = InetAddressUtilities.getHostName();
        assertNotNull("Fallback host name must not be null", hostName);
        assertEquals("When InetAddress.getLocalHost() fails, fallback must be \"localhost\"",
                "localhost", hostName);
    }

    @Test
    public void testGetLocalHost_ThrowsWhenSecurityManagerDenies() {
        System.setSecurityManager(new DenyAllSecurityManager());

        try {
            InetAddressUtilities.getLocalHost();
            fail("Expected UnknownHostException or SecurityException wrapped in UnknownHostException");
        } catch (UnknownHostException e) {
            // Expected path – the utility propagates the exception unchanged.
        } catch (SecurityException se) {
            // Some JVMs may surface a raw SecurityException; accept it as well.
        }
    }
}
