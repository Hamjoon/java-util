package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

/**
 * JUnit 4 test cases for {@link InetAddressUtilities}.
 *
 * These tests cover:
 * <ul>
 *   <li>Verification that the utility class cannot be instantiated (private constructor).</li>
 *   <li>Correct delegation to {@link InetAddress#getLocalHost()} for {@code getLocalHost()}.</li>
 *   <li>Correct retrieval of the IP address byte array via {@code getIpAddress()}.</li>
 *   <li>Correct retrieval of the host name via {@code getHostName()}.</li>
 *   <li>Basic sanity checks on the returned values (non‑null, expected length, consistency).</li>
 * </ul>
 *
 * Note: Because the implementation does not provide a way to force the
 * {@code catch} blocks (the methods rely on {@code InetAddress.getLocalHost()}
 * which normally does not throw on a typical machine), the tests focus on the
 * successful execution paths. Attempting to provoke an exception would require
 * mocking or altering the JDK internals, which is prohibited by the task
 * constraints.
 */
public class InetAddressUtilitiesTest {

    /**
     * Ensure that the utility class cannot be instantiated.
     */
    @Test
    public void testConstructorIsPrivate() throws Exception {
        Constructor<InetAddressUtilities> ctor = InetAddressUtilities.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", Modifier.isPrivate(ctor.getModifiers()));
        // The constructor is private; make it accessible to verify it can be invoked via reflection.
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
        } catch (Exception e) {
            // The constructor does nothing but calling super(); it should not throw.
            fail("Instantiating the private constructor should not throw an exception");
        }
    }

    /**
     * Verify that {@code getLocalHost()} returns the same instance as
     * {@link InetAddress#getLocalHost()}.
     */
    @Test
    public void testGetLocalHost() throws UnknownHostException {
        InetAddress expected = InetAddress.getLocalHost();
        InetAddress actual = InetAddressUtilities.getLocalHost();
        assertNotNull("getLocalHost() must not return null", actual);
        // The two objects should be equal (they represent the same host).
        assertEquals("Utility method should delegate to InetAddress.getLocalHost()", expected, actual);
    }

    /**
     * Verify that {@code getIpAddress()} returns the byte array representation
     * of the local host address and that the array length matches the expected
     * IP version (IPv4 → 4 bytes, IPv6 → 16 bytes).
     */
    @Test
    public void testGetIpAddress() throws UnknownHostException {
        byte[] expected = InetAddress.getLocalHost().getAddress();
        byte[] actual = InetAddressUtilities.getIpAddress();

        assertNotNull("getIpAddress() must not return null", actual);
        assertArrayEquals("IP address byte array should match InetAddress.getAddress()", expected, actual);

        // Verify length is either 4 (IPv4) or 16 (IPv6) – both are valid.
        int len = actual.length;
        assertTrue("IP address length should be 4 (IPv4) or 16 (IPv6)", len == 4 || len == 16);
    }

    /**
     * Verify that {@code getHostName()} returns the same host name as
     * {@link InetAddress#getHostName()}.
     */
    @Test
    public void testGetHostName() throws UnknownHostException {
        String expected = InetAddress.getLocalHost().getHostName();
        String actual = InetAddressUtilities.getHostName();

        assertNotNull("getHostName() must not return null", actual);
        assertEquals("Utility method should delegate to InetAddress.getHostName()", expected, actual);
    }

    /**
     * Additional sanity check: ensure that the byte array returned from
     * {@code getIpAddress()} is not the default fallback {0,0,0,0}.
     * This would indicate that the method unexpectedly fell into the catch block.
     */
    @Test
    public void testIpAddressIsNotFallback() {
        byte[] ip = InetAddressUtilities.getIpAddress();
        // The fallback is exactly four zero bytes.
        boolean isFallback = ip.length == 4 && ip[0] == 0 && ip[1] == 0 && ip[2] == 0 && ip[3] == 0;
        assertFalse("IP address should not be the fallback {0,0,0,0}", isFallback);
    }

    /**
     * Additional sanity check: ensure that {@code getHostName()} never returns the
     * literal fallback value \"localhost\" under normal circumstances.
     */
    @Test
    public void testHostNameIsNotFallback() {
        String hostName = InetAddressUtilities.getHostName();
        assertFalse("Host name should not be the fallback \"localhost\" under normal execution", "localhost".equalsIgnoreCase(hostName));
    }
}
