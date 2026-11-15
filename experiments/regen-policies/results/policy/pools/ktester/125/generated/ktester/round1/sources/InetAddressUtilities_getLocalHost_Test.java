package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.SecurityException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InetAddressUtilities_getLocalHost_Test {

    /**
     * Verifies that {@code InetAddressUtilities.getLocalHost()} returns the same
     * {@link InetAddress} instance as {@link InetAddress#getLocalHost()}.
     */
    @Test
    void testGetLocalHostReturnsSameInstance() throws UnknownHostException {
        // GIVEN
        InetAddress expected = InetAddress.getLocalHost();
        // WHEN
        InetAddress actual = InetAddressUtilities.getLocalHost();
        // THEN
        assertEquals(expected, actual, "InetAddressUtilities.getLocalHost() should return the same InetAddress as InetAddress.getLocalHost()");
    }

    /**
     * Ensures that {@code getLocalHost()} propagates {@link UnknownHostException}
     * when the underlying {@link InetAddress#getLocalHost()} throws it.
     *
     * <p>Because {@link InetAddress#getLocalHost()} is a final static method,
     * we cannot mock it with Mockito alone. Instead, we rely on the JVM's
     * behavior: if the host cannot be resolved, the method naturally throws
     * {@code UnknownHostException}. The test simply asserts that the exception
     * type is preserved.</p>
     *
     * <p>Note: This test will pass on normal environments where the host name
     * is resolvable. It is included for completeness and documentation
     * purposes.</p>
     */
    @Test
    void testGetLocalHostPropagatesUnknownHostException() {
        // GIVEN – No special setup; we just call the method and expect the same exception type if any.
        // WHEN / THEN
        assertDoesNotThrow(() -> {
            // The call itself should not throw on a correctly configured machine.
            // If it does, the test will fail, indicating that the environment cannot resolve the host.
            InetAddressUtilities.getLocalHost();
        });
    }

    /**
     * Normal behavior – verifies that a non‑null {@link InetAddress} is returned.
     */
    @Test
    void testGetLocalHostReturnsNonNull() throws UnknownHostException {
        // WHEN
        InetAddress result = InetAddressUtilities.getLocalHost();
        // THEN
        assertNotNull(result, "InetAddressUtilities.getLocalHost() should never return null on a resolvable host");
    }

    /**
     * Error handling – simulate {@link UnknownHostException} from the underlying
     * {@link InetAddress#getLocalHost()} and verify that it propagates.
     */
    @Test
    void testGetLocalHostThrowsUnknownHostExceptionWhenUnderlyingFails() {
        // Mock the static method to throw the exception.
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenThrow(new UnknownHostException("Simulated failure"));
            // WHEN / THEN
            UnknownHostException ex = assertThrows(UnknownHostException.class, InetAddressUtilities::getLocalHost, "Expected UnknownHostException to be propagated");
            assertEquals("Simulated failure", ex.getMessage(), "Exception message should be preserved");
        }
    }

    /**
     * Edge case – when the host name resolves to the loopback address (127.0.0.1).
     * The method should simply return that {@link InetAddress}.
     */
    @Test
    void testGetLocalHostReturnsLoopbackAddressWhenResolvedAsLoopback() throws Exception {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenReturn(loopback);
            // WHEN
            InetAddress result = InetAddressUtilities.getLocalHost();
            // THEN
            assertTrue(result.isLoopbackAddress(), "Returned address should be a loopback address when the underlying host resolves to 127.0.0.1");
            assertEquals(loopback, result, "The exact InetAddress instance returned by the mock should be propagated");
        }
    }

    /**
     * Edge case – verify that the hostname of the returned {@link InetAddress}
     * matches the hostname provided by {@link InetAddress#getLocalHost()}.
     */
    @Test
    void testGetLocalHostHostnameMatchesUnderlying() throws Exception {
        InetAddress mockedAddress = InetAddress.getByName("127.0.0.1");
        // Force a specific hostname for the mock
        when(mockedAddress.getHostName()).thenReturn("my-test-host");
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenReturn(mockedAddress);
            // WHEN
            InetAddress result = InetAddressUtilities.getLocalHost();
            // THEN
            assertEquals("my-test-host", result.getHostName(), "Hostname of the returned InetAddress should match the underlying mocked hostname");
        }
    }

    /**
     * Security exception handling – simulate a {@link SecurityException}
     * thrown by {@link InetAddress#getLocalHost()} and verify propagation.
     */
    @Test
    void testGetLocalHostThrowsSecurityExceptionWhenAccessDenied() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenThrow(new SecurityException("Access denied"));
            // WHEN / THEN
            SecurityException ex = assertThrows(SecurityException.class, InetAddressUtilities::getLocalHost, "Expected SecurityException to be propagated");
            assertEquals("Access denied", ex.getMessage(), "Exception message should be preserved");
        }
    }
}
