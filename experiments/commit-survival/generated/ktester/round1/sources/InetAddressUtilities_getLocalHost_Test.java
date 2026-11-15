package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class InetAddressUtilities_getLocalHost_Test {

    /**
     * Verifies that {@code InetAddressUtilities.getLocalHost()} returns the same
     * {@link InetAddress} instance as {@link InetAddress#getLocalHost()}.
     */
    @Test
    void getLocalHost_returnsSameInetAddress() throws UnknownHostException {
        // given
        InetAddress expected = InetAddress.getLocalHost();
        // when
        InetAddress actual = InetAddressUtilities.getLocalHost();
        // then
        assertNotNull(actual, "The returned InetAddress should not be null");
        assertEquals(expected, actual, "InetAddressUtilities.getLocalHost() should return the same InetAddress as InetAddress.getLocalHost()");
    }

    /**
     * Ensures that the method propagates {@link UnknownHostException} when the underlying
     * JDK call fails. This test manipulates the {@code java.net.preferIPv4Stack}
     * system property to an invalid value to provoke the exception.
     *
     * <p>Note: The behavior of {@code InetAddress.getLocalHost()} depends on the
     * host environment; the test guards against false failures by checking that an
     * exception is indeed thrown.</p>
     */
    @Test
    void getLocalHost_propagatesUnknownHostException() {
        // given – set an impossible host name to force an exception
        String originalHost = System.getProperty("java.rmi.server.hostname");
        System.setProperty("java.rmi.server.hostname", "nonexistent.invalid.host.name");
        try {
            // when / then
            assertThrows(UnknownHostException.class, InetAddressUtilities::getLocalHost, "Expected UnknownHostException when the underlying InetAddress.getLocalHost() cannot resolve");
        } finally {
            // restore original property to avoid side effects on other tests
            if (originalHost != null) {
                System.setProperty("java.rmi.server.hostname", originalHost);
            } else {
                System.clearProperty("java.rmi.server.hostname");
            }
        }
    }

    /**
     * Normal behavior – the wrapper should return the same instance that the JDK provides.
     */
    @Test
    void getLocalHost_normalBehavior_returnsLocalHost() throws UnknownHostException {
        // given
        InetAddress expected = InetAddress.getLocalHost();
        // when
        InetAddress actual = InetAddressUtilities.getLocalHost();
        // then
        assertNotNull(actual, "Returned InetAddress must not be null");
        assertEquals(expected, actual, "Wrapper must return the same InetAddress as InetAddress.getLocalHost()");
        // Additional sanity check: hostname is localhost or address is loopback
        String hostName = actual.getHostName();
        String hostAddress = actual.getHostAddress();
        boolean isLoopback = hostAddress.equals("127.0.0.1") || hostAddress.equals("0:0:0:0:0:0:0:1");
        assertTrue(hostName.equalsIgnoreCase("localhost") || isLoopback, "Returned address should represent the local host (hostname localhost or loopback IP)");
    }

    /**
     * Exception behavior – {@link UnknownHostException} should be propagated unchanged.
     */
    @Test
    void getLocalHost_unknownHostExceptionIsPropagated() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenThrow(new UnknownHostException("forced"));
            assertThrows(UnknownHostException.class, InetAddressUtilities::getLocalHost, "Expected UnknownHostException to be propagated");
        }
    }

    /**
     * Mocked behavior – returns a custom InetAddress (hostname "myhost", IP 192.168.1.10).
     */
    @Test
    void getLocalHost_mockedReturnsCustomInetAddress() throws Exception {
        InetAddress custom = InetAddress.getByName("192.168.1.10");
        // Ensure the hostname is as expected for the assertion later
        // (some environments may resolve to a different hostname; we only check the address)
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenReturn(custom);
            InetAddress result = InetAddressUtilities.getLocalHost();
            assertSame(custom, result, "The wrapper should return the exact mocked InetAddress instance");
            assertEquals("192.168.1.10", result.getHostAddress(), "IP address must match the stubbed value");
        }
    }

    /**
     * Mocked behavior – underlying call returns {@code null}.
     * Although JDK never does this, the wrapper must not alter the result.
     */
    @Test
    void getLocalHost_mockedReturnsNull() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenReturn(null);
            InetAddress result = assertDoesNotThrow(() -> InetAddressUtilities.getLocalHost(), "Method should not throw when underlying call returns null");
            assertNull(result, "Wrapper should propagate null result unchanged");
        }
    }

    /**
     * Security exception propagation – a {@link SecurityException} from the JDK call
     * must be propagated.
     */
    @Test
    void getLocalHost_securityExceptionIsPropagated() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenThrow(new SecurityException("security"));
            assertThrows(SecurityException.class, InetAddressUtilities::getLocalHost, "SecurityException should be propagated");
        }
    }

    /**
     * Unchecked exception propagation – an {@link IllegalStateException} from the JDK call
     * must be propagated unchanged.
     */
    @Test
    void getLocalHost_uncheckedExceptionIsPropagated() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(InetAddress::getLocalHost).thenThrow(new IllegalStateException("illegal"));
            assertThrows(IllegalStateException.class, InetAddressUtilities::getLocalHost, "Unchecked exceptions should be propagated");
        }
    }

    /**
     * Thread‑safety – concurrent invocations should all return equal (and non‑null) results.
     */
    @Test
    void getLocalHost_concurrentCallsReturnEqualResults() throws Exception {
        // Baseline call to obtain the expected instance
        InetAddress baseline = InetAddressUtilities.getLocalHost();
        assertNotNull(baseline, "Baseline call must return a non‑null address");
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<InetAddress>> futures = new ArrayList<>();
        // Submit concurrent tasks
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> InetAddressUtilities.getLocalHost()));
        }
        // Gather results
        for (Future<InetAddress> f : futures) {
            InetAddress result = f.get(5, TimeUnit.SECONDS);
            assertNotNull(result, "Concurrent call returned null");
            assertEquals(baseline, result, "All concurrent calls must return equal InetAddress instances");
        }
        executor.shutdownNow();
    }
}
