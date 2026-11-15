package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InetAddressUtilities_getLocalHost_Test {

    /**
     * Verifies that {@code InetAddressUtilities.getLocalHost()} returns the same
     * {@link InetAddress} instance that {@link InetAddress#getLocalHost()} provides.
     */
    @Test
    void testGetLocalHostReturnsSystemLocalHost() throws UnknownHostException {
        // GIVEN – the system's local host address
        InetAddress expected = InetAddress.getLocalHost();
        // WHEN – invoking the utility method
        InetAddress actual = InetAddressUtilities.getLocalHost();
        // THEN – the two addresses should be equal (same host name and IP)
        assertNotNull(actual, "The returned InetAddress must not be null");
        assertEquals(expected.getHostName(), actual.getHostName(), "Host names should match");
        assertArrayEquals(expected.getAddress(), actual.getAddress(), "IP address byte arrays should match");
    }

    /**
     * Simulates a failure of {@link InetAddress#getLocalHost()} by throwing
     * {@link UnknownHostException}. The utility method must propagate the exception.
     */
    @Test
    void testGetLocalHostPropagatesUnknownHostException() {
        // GIVEN – static mock that throws UnknownHostException
        try (MockedStatic<InetAddress> mocked = Mockito.mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenThrow(new UnknownHostException("simulated failure"));
            // WHEN / THEN – the utility method should throw the same exception
            UnknownHostException thrown = assertThrows(UnknownHostException.class, InetAddressUtilities::getLocalHost, "Expected getLocalHost() to propagate UnknownHostException");
            assertEquals("simulated failure", thrown.getMessage(), "Exception message should be preserved");
        }
    }

    /**
     * Verifies that the utility method delegates to {@link InetAddress#getLocalHost()}
     * and returns the exact instance supplied by the mocked static call.
     */
    @Test
    void testGetLocalHostDelegatesToInetAddressGetLocalHost() throws Exception {
        // GIVEN – a predefined InetAddress (e.g., 192.0.2.1)
        InetAddress predefined = InetAddress.getByName("192.0.2.1");
        try (MockedStatic<InetAddress> mocked = Mockito.mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenReturn(predefined);
            // WHEN – invoking the utility method
            InetAddress actual = InetAddressUtilities.getLocalHost();
            // THEN – the returned instance must be the same object
            assertSame(predefined, actual, "Utility should return the exact InetAddress instance from InetAddress.getLocalHost()");
        }
    }

    /**
     * Ensures that each call to the utility method reflects the current result of the
     * underlying static call – i.e., no caching or transformation occurs.
     */
    @Test
    void testGetLocalHostReturnsDifferentInstancesOnSuccessiveCalls() throws Exception {
        // GIVEN – two distinct InetAddress objects
        InetAddress first = InetAddress.getByName("192.0.2.1");
        InetAddress second = InetAddress.getByName("192.0.2.2");
        try (MockedStatic<InetAddress> mocked = Mockito.mockStatic(InetAddress.class)) {
            // Configure the mock to return first then second
            mocked.when(InetAddress::getLocalHost).thenReturn(first).thenReturn(second);
            // WHEN – first invocation
            InetAddress actualFirst = InetAddressUtilities.getLocalHost();
            // AND – second invocation
            InetAddress actualSecond = InetAddressUtilities.getLocalHost();
            // THEN – each result matches the corresponding mock return value
            assertSame(first, actualFirst, "First call should return the first mocked instance");
            assertSame(second, actualSecond, "Second call should return the second mocked instance");
        }
    }

    /**
     * Although the JDK never returns {@code null}, this test verifies that the wrapper
     * does not introduce its own null‑check and simply propagates a {@code null} result.
     */
    @Test
    void testGetLocalHostPropagatesNullResult() {
        try (MockedStatic<InetAddress> mocked = Mockito.mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenReturn(null);
            // WHEN – invoking the utility method
            InetAddress actual = InetAddressUtilities.getLocalHost();
            // THEN – the result should be null
            assertNull(actual, "Utility should return null when the underlying call returns null");
        }
    }

    /**
     * Verifies that a {@link SecurityException} thrown by the underlying static call
     * propagates unchanged through the utility method.
     */
    @Test
    void testGetLocalHostPropagatesSecurityException() {
        try (MockedStatic<InetAddress> mocked = Mockito.mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenThrow(new SecurityException("access denied"));
            SecurityException thrown = assertThrows(SecurityException.class, InetAddressUtilities::getLocalHost, "Expected SecurityException to propagate");
            assertEquals("access denied", thrown.getMessage(), "Exception message should be preserved");
        }
    }

    /**
     * Verifies that unchecked {@link RuntimeException}s thrown by the underlying static call
     * propagate without being wrapped or swallowed.
     */
    @Test
    void testGetLocalHostPropagatesRuntimeException() {
        try (MockedStatic<InetAddress> mocked = Mockito.mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenThrow(new RuntimeException("unexpected error"));
            RuntimeException thrown = assertThrows(RuntimeException.class, InetAddressUtilities::getLocalHost, "Expected RuntimeException to propagate");
            assertEquals("unexpected error", thrown.getMessage(), "Exception message should be preserved");
        }
    }
}
