package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InetAddressUtilities_getHostName_Test {

    /**
     * Mocked static scope for {@link InetAddressUtilities}.
     */
    private MockedStatic<InetAddressUtilities> utilitiesMock;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh static mock for each test to avoid cross‑test interference.
        utilitiesMock = mockStatic(InetAddressUtilities.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock; this also resets any stubbing performed.
        utilitiesMock.close();
    }

    @Test
    void getHostName_returnsHostNameFromInetAddress_whenLocalHostSucceeds() throws UnknownHostException {
        // GIVEN
        InetAddress mockInet = mock(InetAddress.class);
        when(mockInet.getHostName()).thenReturn("my-test-host");
        // Stub the static method getLocalHost() to return our mock.
        utilitiesMock.when(InetAddressUtilities::getLocalHost).thenReturn(mockInet);
        // WHEN
        String result = InetAddressUtilities.getHostName();
        // THEN
        assertEquals("my-test-host", result, "The hostname should be delegated from the mocked InetAddress");
        // Verify that getHostName() on the mock was indeed called.
        verify(mockInet).getHostName();
    }

    @Test
    void getHostName_returnsDefault_whenLocalHostThrowsException() throws UnknownHostException {
        // GIVEN
        utilitiesMock.when(InetAddressUtilities::getLocalHost).thenThrow(new UnknownHostException("simulated failure"));
        // WHEN
        String result = InetAddressUtilities.getHostName();
        // THEN
        assertEquals("localhost", result, "When getLocalHost() throws, the method should fall back to \"localhost\"");
    }

    /**
     * Exception path 1 – {@code getLocalHost()} throws {@link UnknownHostException},
     * which should be caught and result in the fallback value {@code "localhost"}.
     */
    @Test
    void getHostName_returnsDefault_whenLocalHostThrowsUnknownHostException() throws UnknownHostException {
        // GIVEN
        utilitiesMock.when(InetAddressUtilities::getLocalHost).thenThrow(new UnknownHostException("simulated failure"));
        // WHEN
        String result = InetAddressUtilities.getHostName();
        // THEN
        assertEquals("localhost", result, "When getLocalHost() throws, the method should fall back to \"localhost\"");
    }

    /**
     * Exception path 2 – {@code getLocalHost()} returns a mock {@link InetAddress},
     * but its {@code getHostName()} throws a {@link RuntimeException}.
     * The catch block in {@code getHostName()} should handle it and return the fallback.
     */
    @Test
    void getHostName_returnsDefault_whenInetAddressHostNameThrowsRuntimeException() throws UnknownHostException {
        // GIVEN
        InetAddress mockInet = mock(InetAddress.class);
        when(mockInet.getHostName()).thenThrow(new RuntimeException("unexpected failure"));
        utilitiesMock.when(InetAddressUtilities::getLocalHost).thenReturn(mockInet);
        // WHEN
        String result = InetAddressUtilities.getHostName();
        // THEN
        assertEquals("localhost", result, "When InetAddress#getHostName() throws, the method should fall back to \"localhost\"");
        // ensure the method was indeed invoked
        verify(mockInet).getHostName();
    }
}
