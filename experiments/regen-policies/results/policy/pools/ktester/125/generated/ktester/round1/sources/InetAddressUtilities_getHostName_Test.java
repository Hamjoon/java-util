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
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InetAddressUtilities_getHostName_Test {

    /**
     * Provides a stream of different exception instances to be used by the
     * {@link #testGetHostName_FallbackOnException(Exception)} test.
     */
    private static Stream<Exception> exceptionProvider() {
        return Stream.of(new UnknownHostException("forced UnknownHostException"), new RuntimeException("forced RuntimeException"), // non‑UnknownHost checked exception
        new java.io.IOException("forced IOException"));
    }

    /**
     * Verify that {@code getHostName()} returns the host name supplied by {@code InetAddress.getLocalHost()}
     * when no exception is thrown.
     */
    @Test
    void testGetHostName_Success() throws Exception {
        // Given – the real environment
        String expected = InetAddress.getLocalHost().getHostName();
        // When
        String actual = InetAddressUtilities.getHostName();
        // Then
        assertEquals(expected, actual, "When getLocalHost() works, getHostName() should return the same host name");
    }

    /**
     * Verify that {@code getHostName()} falls back to the literal {@code "localhost"} when
     * {@code getLocalHost()} throws an exception.
     */
    @Test
    void testGetHostName_FallbackOnException() throws Exception {
        // Given – mock the static method getLocalHost() to throw an exception
        try (MockedStatic<InetAddressUtilities> mocked = mockStatic(InetAddressUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(InetAddressUtilities::getLocalHost).thenThrow(new UnknownHostException("forced failure for test"));
            // When
            String result = InetAddressUtilities.getHostName();
            // Then
            assertEquals("localhost", result, "When getLocalHost() throws, getHostName() should return the fallback value \"localhost\"");
        }
    }

    /**
     * Verify that {@code getHostName()} returns the host name supplied by the mocked
     * {@code InetAddress.getLocalHost()} when no exception is thrown.
     */
    @Test
    void testGetHostName_SuccessfulRetrieval() throws Exception {
        // Arrange – mock a InetAddress whose getHostName() returns "my-host"
        InetAddress mockAddress = mock(InetAddress.class);
        when(mockAddress.getHostName()).thenReturn("my-host");
        try (MockedStatic<InetAddressUtilities> mocked = mockStatic(InetAddressUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(InetAddressUtilities::getLocalHost).thenReturn(mockAddress);
            // Act
            String actual = InetAddressUtilities.getHostName();
            // Assert
            assertEquals("my-host", actual, "When getLocalHost() returns a valid InetAddress, getHostName() should return its host name");
        }
    }

    /**
     * Verify that {@code getHostName()} returns {@code "localhost"} when
     * {@code getLocalHost()} returns {@code null}. The subsequent call to
     * {@code null.getHostName()} throws a {@link NullPointerException},
     * which is caught by the method.
     */
    @Test
    void testGetHostName_NullInetAddress() throws Exception {
        // Arrange – mock getLocalHost() to return null
        try (MockedStatic<InetAddressUtilities> mocked = mockStatic(InetAddressUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(InetAddressUtilities::getLocalHost).thenReturn(null);
            // Act
            String result = InetAddressUtilities.getHostName();
            // Assert
            assertEquals("localhost", result, "When getLocalHost() returns null, getHostName() should fall back to \"localhost\"");
        }
    }

    /**
     * Verify that {@code getHostName()} returns {@code "localhost"} when the
     * {@code InetAddress} instance returned by {@code getLocalHost()} throws a
     * {@link SecurityException} (or any other runtime exception) from its
     * {@code getHostName()} method.
     */
    @Test
    void testGetHostName_InetAddressThrows() throws Exception {
        // Arrange – mock an InetAddress whose getHostName() throws SecurityException
        InetAddress mockAddress = mock(InetAddress.class);
        when(mockAddress.getHostName()).thenThrow(new SecurityException("forced SecurityException"));
        try (MockedStatic<InetAddressUtilities> mocked = mockStatic(InetAddressUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(InetAddressUtilities::getLocalHost).thenReturn(mockAddress);
            // Act
            String result = InetAddressUtilities.getHostName();
            // Assert
            assertEquals("localhost", result, "When InetAddress.getHostName() throws, getHostName() should fall back to \"localhost\"");
        }
    }
}
