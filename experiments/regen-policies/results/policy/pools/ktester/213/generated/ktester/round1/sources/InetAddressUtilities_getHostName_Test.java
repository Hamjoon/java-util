package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

@ExtendWith(MockitoExtension.class)
class InetAddressUtilities_getHostName_Test {

    @Test
    void testGetHostNameReturnsHostNameFromLocalHost() throws UnknownHostException {
        // Arrange
        InetAddress mockInet = mock(InetAddress.class);
        when(mockInet.getHostName()).thenReturn("my-test-host");
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenReturn(mockInet);
            // Act
            String result = InetAddressUtilities.getHostName();
            // Assert
            assertEquals("my-test-host", result);
            utilitiesMock.verify(InetAddressUtilities::getLocalHost);
        }
    }

    @Test
    void testGetHostNameHandlesExceptionAndReturnsDefault() throws UnknownHostException {
        // Arrange
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenThrow(new UnknownHostException("forced exception"));
            // Act
            String result = InetAddressUtilities.getHostName();
            // Assert
            assertEquals("localhost", result);
            utilitiesMock.verify(InetAddressUtilities::getLocalHost);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetHostNameHandlesUnknownHostExceptionAndReturnsDefault() throws UnknownHostException {
        // Arrange
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenThrow(new UnknownHostException("forced exception"));
            // Capture System.err output
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errContent));
            // Act
            String result = InetAddressUtilities.getHostName();
            // Restore original System.err
            System.setErr(originalErr);
            // Assert
            assertEquals("localhost", result);
            assertTrue(errContent.toString().contains("Unable to fetch 'hostname'"));
            utilitiesMock.verify(InetAddressUtilities::getLocalHost);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetHostNameHandlesSecurityExceptionAndReturnsDefault() throws UnknownHostException {
        // Arrange
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenThrow(new SecurityException("no permission"));
            // Capture System.err output
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errContent));
            // Act
            String result = InetAddressUtilities.getHostName();
            // Restore System.err
            System.setErr(originalErr);
            // Assert
            assertEquals("localhost", result);
            assertTrue(errContent.toString().contains("Unable to fetch 'hostname'"));
            utilitiesMock.verify(InetAddressUtilities::getLocalHost);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetHostNameHandlesRuntimeExceptionFromInetAddressAndReturnsDefault() throws UnknownHostException {
        // Arrange
        InetAddress mockInet = mock(InetAddress.class);
        when(mockInet.getHostName()).thenThrow(new IllegalStateException("boom"));
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenReturn(mockInet);
            // Capture System.err output
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errContent));
            // Act
            String result = InetAddressUtilities.getHostName();
            // Restore System.err
            System.setErr(originalErr);
            // Assert
            assertEquals("localhost", result);
            assertTrue(errContent.toString().contains("Unable to fetch 'hostname'"));
            utilitiesMock.verify(InetAddressUtilities::getLocalHost);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetHostNamePropagatesNullReturnedByInetAddress() throws UnknownHostException {
        // Arrange
        InetAddress mockInet = mock(InetAddress.class);
        when(mockInet.getHostName()).thenReturn(null);
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenReturn(mockInet);
            // Act
            String result = InetAddressUtilities.getHostName();
            // Assert
            assertNull(result);
            utilitiesMock.verify(InetAddressUtilities::getLocalHost);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetHostNamePropagatesEmptyStringReturnedByInetAddress() throws UnknownHostException {
        // Arrange
        InetAddress mockInet = mock(InetAddress.class);
        when(mockInet.getHostName()).thenReturn("");
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenReturn(mockInet);
            // Act
            String result = InetAddressUtilities.getHostName();
            // Assert
            assertEquals("", result);
            utilitiesMock.verify(InetAddressUtilities::getLocalHost);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetHostNameHandlesRuntimeExceptionFromGetLocalHostAndReturnsDefault() throws UnknownHostException {
        // Arrange
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenThrow(new IllegalStateException("unexpected state"));
            // Capture System.err output
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errContent));
            // Act
            String result = InetAddressUtilities.getHostName();
            // Restore System.err
            System.setErr(originalErr);
            // Assert
            assertEquals("localhost", result);
            assertTrue(errContent.toString().contains("Unable to fetch 'hostname'"));
            utilitiesMock.verify(InetAddressUtilities::getLocalHost);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetHostNameHandlesNullInetAddressFromGetLocalHostAndReturnsDefault() throws UnknownHostException {
        // Arrange
        try (MockedStatic<InetAddressUtilities> utilitiesMock = mockStatic(InetAddressUtilities.class)) {
            utilitiesMock.when(InetAddressUtilities::getLocalHost).thenReturn(null);
            // Capture System.err output
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errContent));
            // Act
            String result = InetAddressUtilities.getHostName();
            // Restore System.err
            System.setErr(originalErr);
            // Assert
            assertEquals("localhost", result);
            assertTrue(errContent.toString().contains("Unable to fetch 'hostname'"));
            utilitiesMock.verify(InetAddressUtilities::getLocalHost);
        }
    }
}
