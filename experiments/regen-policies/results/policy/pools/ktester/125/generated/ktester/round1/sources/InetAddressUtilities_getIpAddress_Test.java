package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class InetAddressUtilities_getIpAddress_Test {

    /**
     * Supplies a behaviour for the mocked {@code getLocalHost()} call.
     */
    @FunctionalInterface
    private interface HostSupplier {

        /**
         * Configures the static mock to either return a value or throw an exception.
         */
        void configure(MockedStatic<InetAddressUtilities> mock);
    }

    /**
     * Provides IPv4 and IPv6 test data.
     */
    private static Stream<Arguments> successfulCases() throws UnknownHostException {
        // IPv4 example: 192.168.1.10
        InetAddress ipv4 = InetAddress.getByName("192.168.1.10");
        // length 4
        byte[] ipv4Bytes = ipv4.getAddress();
        // IPv6 example: 2001:db8::1
        InetAddress ipv6 = InetAddress.getByName("2001:db8::1");
        // length 16
        byte[] ipv6Bytes = ipv6.getAddress();
        return Stream.of(arguments(ipv4, ipv4Bytes, "IPv4 address should be returned unchanged"), arguments(ipv6, ipv6Bytes, "IPv6 address should be returned unchanged"));
    }

    /**
     * Provides the different failure scenarios.
     */
    private static Stream<Arguments> exceptionCases() throws UnknownHostException {
        byte[] fallback = new byte[] { 0, 0, 0, 0 };
        // 1. UnknownHostException
        HostSupplier unknownHost = mock -> mock.when(InetAddressUtilities::getLocalHost).thenThrow(new UnknownHostException("forced UnknownHostException"));
        // 2. SecurityException
        HostSupplier security = mock -> mock.when(InetAddressUtilities::getLocalHost).thenThrow(new SecurityException("forced SecurityException"));
        // 3. getLocalHost() returns null (causes NullPointerException inside getIpAddress)
        HostSupplier nullHost = mock -> mock.when(InetAddressUtilities::getLocalHost).thenReturn(null);
        // 4. getAddress() throws RuntimeException
        HostSupplier addressThrows = mock -> {
            InetAddress badInet = Mockito.mock(InetAddress.class);
            Mockito.when(badInet.getAddress()).thenThrow(new RuntimeException("forced RuntimeException"));
            mock.when(InetAddressUtilities::getLocalHost).thenReturn(badInet);
        };
        return Stream.of(arguments(unknownHost, fallback, "UnknownHostException should lead to fallback array"), arguments(security, fallback, "SecurityException should lead to fallback array"), arguments(nullHost, fallback, "Null return from getLocalHost should lead to fallback array"), arguments(addressThrows, fallback, "RuntimeException from getAddress should lead to fallback array"));
    }

    @Test
    void shouldReturnLocalHostAddressWhenNoException() throws UnknownHostException {
        // given – the real InetAddress for the current machine
        byte[] expected = InetAddress.getLocalHost().getAddress();
        // when
        byte[] actual = InetAddressUtilities.getIpAddress();
        // then
        assertArrayEquals(expected, actual, "The utility should return the same IP bytes as InetAddress.getLocalHost()");
    }

    @Test
    void shouldReturnZeroedArrayWhenGetLocalHostThrowsException() {
        // given – mock the static method getLocalHost() to throw an exception
        try (MockedStatic<InetAddressUtilities> utilitiesMock = Mockito.mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getIpAddress).thenCallRealMethod();
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenThrow(new UnknownHostException("forced failure"));
            // when
            byte[] result = InetAddressUtilities.getIpAddress();
            // then – the fallback array of four zero bytes is returned
            assertNotNull(result, "Result must not be null");
            assertEquals(4, result.length, "Result array must have length 4");
            assertArrayEquals(new byte[] { 0, 0, 0, 0 }, result, "When an exception occurs the method should return a zeroed byte array");
        }
    }

    /**
     * Parameterized test covering normal IPv4 and IPv6 results.
     *
     * @param host   the {@link InetAddress} that {@code getLocalHost()} should return
     * @param expected the exact byte array that {@code getIpAddress()} must return
     * @param description description of the test case (used in the test name)
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successfulCases")
    void shouldReturnExactAddressWhenGetLocalHostSucceeds(InetAddress host, byte[] expected, String description) throws UnknownHostException {
        // Mock the static methods of InetAddressUtilities.
        try (MockedStatic<InetAddressUtilities> utilitiesMock = Mockito.mockStatic(InetAddressUtilities.class)) {
            // getIpAddress() must execute its real code.
            utilitiesMock.when(InetAddressUtilities::getIpAddress).thenCallRealMethod();
            // getLocalHost() returns the supplied host (could be IPv4 or IPv6).
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenReturn(host);
            // When
            byte[] actual = InetAddressUtilities.getIpAddress();
            // Then
            assertArrayEquals(expected, actual, "The returned byte array should match the address supplied by getLocalHost()");
        }
    }

    /**
     * Parameterized test covering all exception / error paths.
     *
     * @param hostSupplier supplies the value (or exception) that {@code getLocalHost()}
     *                     should produce when invoked.
     * @param expected    the fallback array that must be returned.
     * @param description description of the scenario.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionCases")
    void shouldReturnFallbackArrayWhenExceptionOccurs(HostSupplier hostSupplier, byte[] expected, String description) {
        try (MockedStatic<InetAddressUtilities> utilitiesMock = Mockito.mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getIpAddress).thenCallRealMethod();
            // Configure getLocalHost() according to the scenario.
            hostSupplier.configure(utilitiesMock);
            // When
            byte[] actual = InetAddressUtilities.getIpAddress();
            // Then
            assertNotNull(actual, "Result must not be null");
            assertArrayEquals(expected, actual, "When an exception occurs the method should return the fallback zeroed array");
        }
    }
}
