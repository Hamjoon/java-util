package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InetAddressUtilities_getIpAddress_Test {

    private static Stream<Arguments> successfulIpProvider() {
        return Stream.of(// 127.0.0.1 (IPv4 loop‑back)
        Arguments.of(new byte[] { 127, 0, 0, 1 }), // 192.168.1.100 (non‑loopback IPv4)
        Arguments.of(new byte[] { (byte) 192, (byte) 168, 1, 100 }), // ::1 (IPv6 loop‑back, 16 bytes)
        Arguments.of(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }), // 2001:0db8::1 (non‑loopback IPv6 – using the expected signed values from the spec)
        Arguments.of(new byte[] { 32, 1, 13, -84, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));
    }

    private static Stream<Arguments> signedByteProvider() {
        return Stream.of(// 255.255.255.255 – each octet becomes -1 in a signed byte array
        Arguments.of(new byte[] { -1, -1, -1, -1 }), // 127.128.0.1 – the second octet (128) is represented as -128
        Arguments.of(new byte[] { 127, -128, 0, 1 }));
    }

    @Test
    void testGetIpAddressWhenLocalHostIsAvailable() {
        // GIVEN – a mock InetAddress that returns the supplied byte array
        InetAddress mockInet = mock(InetAddress.class);
        when(mockInet.getAddress()).thenReturn(expected);
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenReturn(mockInet);
            // WHEN
            byte[] actual = InetAddressUtilities.getIpAddress();
            // THEN
            assertArrayEquals(expected, actual, "The IP address should be exactly the byte array returned by the mocked InetAddress");
        }
    }

    @Test
    void testGetIpAddressWhenLocalHostThrowsException() {
        // GIVEN
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenThrow(new UnknownHostException("Simulated failure"));
            // WHEN
            byte[] actual = InetAddressUtilities.getIpAddress();
            // THEN
            byte[] expectedFallback = new byte[] { 0, 0, 0, 0 };
            assertArrayEquals(expectedFallback, actual, "When getLocalHost throws an exception, getIpAddress should return a zeroed byte array");
        }
    }

    /* -------------------------------------------------------------
     *  Exception handling – UnknownHostException
     * ------------------------------------------------------------- */
    @Test
    void testGetIpAddressWhenLocalHostThrowsUnknownHostException() {
        // GIVEN – getLocalHost() throws a checked exception
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenThrow(new UnknownHostException("Simulated failure"));
            // WHEN
            byte[] actual = InetAddressUtilities.getIpAddress();
            // THEN – fallback to 0.0.0.0
            byte[] expectedFallback = new byte[] { 0, 0, 0, 0 };
            assertArrayEquals(expectedFallback, actual, "When getLocalHost throws UnknownHostException, getIpAddress should return a zeroed byte array");
        }
    }

    /* -------------------------------------------------------------
     *  Exception handling – RuntimeException (e.g., SecurityException)
     * ------------------------------------------------------------- */
    @Test
    void testGetIpAddressWhenLocalHostThrowsRuntimeException() {
        // GIVEN – getLocalHost() throws an unchecked exception
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenThrow(new RuntimeException("Simulated runtime failure"));
            // WHEN
            byte[] actual = InetAddressUtilities.getIpAddress();
            // THEN – fallback to 0.0.0.0
            byte[] expectedFallback = new byte[] { 0, 0, 0, 0 };
            assertArrayEquals(expectedFallback, actual, "When getLocalHost throws a RuntimeException, getIpAddress should return a zeroed byte array");
        }
    }

    /* -------------------------------------------------------------
     *  Error propagation – AssertionError must NOT be caught
     * ------------------------------------------------------------- */
    @Test
    void testGetIpAddressWhenLocalHostThrowsError() {
        // GIVEN – getLocalHost() throws an Error (not an Exception)
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenThrow(new AssertionError("Simulated assertion error"));
            // WHEN / THEN – the Error propagates out of the method
            assertThrows(AssertionError.class, InetAddressUtilities::getIpAddress, "Errors should not be caught; they must propagate to the caller");
        }
    }

    /* -------------------------------------------------------------
     *  NullPointerException handling – getLocalHost returns null
     * ------------------------------------------------------------- */
    @Test
    void testGetIpAddressWhenLocalHostReturnsNull() {
        // GIVEN – getLocalHost() returns null, causing a NPE when .getAddress() is invoked
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenReturn(null);
            // WHEN
            byte[] actual = InetAddressUtilities.getIpAddress();
            // THEN – fallback to 0.0.0.0
            byte[] expectedFallback = new byte[] { 0, 0, 0, 0 };
            assertArrayEquals(expectedFallback, actual, "When getLocalHost returns null, the resulting NullPointerException should be caught and a zeroed array returned");
        }
    }

    /* -------------------------------------------------------------
     *  Signed byte values handling – broadcast and high‑order octet cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "Signed byte handling – expected = {0}")
    @MethodSource("signedByteProvider")
    void testGetIpAddressWithSignedByteValues(byte[] expected) {
        // GIVEN – mock InetAddress returning a byte array that contains signed values
        InetAddress mockInet = mock(InetAddress.class);
        when(mockInet.getAddress()).thenReturn(expected);
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenReturn(mockInet);
            // WHEN
            byte[] actual = InetAddressUtilities.getIpAddress();
            // THEN
            assertArrayEquals(expected, actual, "The method must return the exact signed byte representation supplied by InetAddress");
        }
    }
}
