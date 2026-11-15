package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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

    /**
     * Mocked static context for {@link InetAddressUtilities}.
     */
    private MockedStatic<InetAddressUtilities> inetUtilMock;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh static mock for each test so expectations do not leak.
        inetUtilMock = Mockito.mockStatic(InetAddressUtilities.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock to avoid side‑effects on other tests.
        if (inetUtilMock != null) {
            inetUtilMock.close();
        }
    }

    private static Stream<Arguments> ipv4Provider() {
        return Stream.of(Arguments.of(new byte[] { 127, 0, 0, 1 }, "IPv4 loopback address (127.0.0.1)"), Arguments.of(new byte[] { (byte) 192, (byte) 168, 1, 100 }, "Typical private IPv4 address (192.168.1.100)"));
    }

    private static Stream<Arguments> ipv6Provider() {
        return Stream.of(Arguments.of(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }, "IPv6 loopback address (::1)"), Arguments.of(new byte[] { 32, 1, 13, (byte) 184, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }, "Non‑loopback IPv6 address (2001:0db8::1)"));
    }

    @Test
    void getIpAddress_returnsLocalHostAddress_whenNoException() throws UnknownHostException {
        // GIVEN
        InetAddress fakeInet = mock(InetAddress.class);
        byte[] expected = new byte[] { 1, 2, 3, 4 };
        when(fakeInet.getAddress()).thenReturn(expected);
        // Mock the static call InetAddressUtilities.getLocalHost()
        inetUtilMock.when(InetAddressUtilities::getLocalHost).thenReturn(fakeInet);
        // WHEN
        byte[] actual = InetAddressUtilities.getIpAddress();
        // THEN
        assertArrayEquals(expected, actual, "The IP address should be the one provided by the mocked InetAddress.");
    }

    @Test
    void getIpAddress_returnsZeroedArray_whenGetLocalHostThrows() throws UnknownHostException {
        // GIVEN a getLocalHost() that throws an exception
        inetUtilMock.when(InetAddressUtilities::getLocalHost).thenThrow(new UnknownHostException("simulated failure"));
        // WHEN
        byte[] actual = InetAddressUtilities.getIpAddress();
        // THEN
        byte[] expected = new byte[] { 0, 0, 0, 0 };
        assertArrayEquals(expected, actual, "When getLocalHost throws, the method should fall back to a zeroed IP address.");
    }

    @ParameterizedTest(name = "[{index}] IPv4 address {1}")
    @MethodSource("ipv4Provider")
    void getIpAddress_returnsExpectedIpv4(byte[] addressBytes, String description) throws UnknownHostException {
        // GIVEN a mocked InetAddress that returns the supplied IPv4 byte array
        InetAddress fakeInet = mock(InetAddress.class);
        when(fakeInet.getAddress()).thenReturn(addressBytes);
        inetUtilMock.when(InetAddressUtilities::getLocalHost).thenReturn(fakeInet);
        // WHEN
        byte[] actual = InetAddressUtilities.getIpAddress();
        // THEN
        assertArrayEquals(addressBytes, actual, "IPv4 test failed: " + description);
    }

    @ParameterizedTest(name = "[{index}] IPv6 address {1}")
    @MethodSource("ipv6Provider")
    void getIpAddress_returnsExpectedIpv6(byte[] addressBytes, String description) throws UnknownHostException {
        // GIVEN a mocked InetAddress that returns the supplied IPv6 byte array
        InetAddress fakeInet = mock(InetAddress.class);
        when(fakeInet.getAddress()).thenReturn(addressBytes);
        inetUtilMock.when(InetAddressUtilities::getLocalHost).thenReturn(fakeInet);
        // WHEN
        byte[] actual = InetAddressUtilities.getIpAddress();
        // THEN
        assertArrayEquals(addressBytes, actual, "IPv6 test failed: " + description);
    }

    @Test
    void getIpAddress_returnsZeroedArray_whenLocalHostIsNull() throws UnknownHostException {
        // GIVEN getLocalHost() returns null – this will cause a NullPointerException
        // inside getIpAddress() when it tries to call .getAddress()
        inetUtilMock.when(InetAddressUtilities::getLocalHost).thenReturn(null);
        // WHEN
        byte[] actual = InetAddressUtilities.getIpAddress();
        // THEN
        byte[] expected = new byte[] { 0, 0, 0, 0 };
        assertArrayEquals(expected, actual, "When getLocalHost() returns null the method should catch the NPE and return a zeroed array.");
    }
}
