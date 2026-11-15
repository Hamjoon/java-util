package com.cedarsoftware.util;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class IOUtilities_close_Test {

    @Mock
    XMLStreamReader // injected by Mockito
    mockReader;

    /**
     * Verify that a non‑null {@link XMLStreamReader} is closed exactly once and that
     * no exception propagates when the underlying {@code close()} method succeeds.
     */
    @Test
    void testCloseNonNullReader_successfulClose() throws Exception {
        // Arrange – mock already created by @Mock annotation
        // Act
        assertDoesNotThrow(() -> IOUtilities.close(mockReader));
        // Assert
        verify(mockReader, times(1)).close();
    }

    /**
     * Verify that {@link IOUtilities#close(XMLStreamReader)} silently ignores an
     * {@link XMLStreamException} thrown by the reader's {@code close()} method.
     */
    @Test
    void testCloseNonNullReader_closeThrowsExceptionIsIgnored() throws Exception {
        // Arrange
        doThrow(new XMLStreamException("simulated")).when(mockReader).close();
        // Act & Assert – the method must not throw any exception
        assertDoesNotThrow(() -> IOUtilities.close(mockReader));
        // Verify that close was still invoked
        verify(mockReader, times(1)).close();
    }

    /**
     * Verify that passing {@code null} does not result in a {@link NullPointerException}
     * or any other exception.
     */
    @Test
    void testCloseNullReader_isNoOp() {
        assertDoesNotThrow(() -> IOUtilities.close((XMLStreamReader) null));
        // No verification needed because there is no mock to interact with
    }

    /**
     * Verify that an {@link XMLStreamException} thrown by {@code reader.close()}
     * is silently ignored.
     */
    @Test
    void testCloseNonNullReader_closeThrowsXMLStreamException_isIgnored() throws Exception {
        // Arrange
        doThrow(new XMLStreamException("simulated")).when(mockReader).close();
        // Act & Assert – the method must not throw any exception
        assertDoesNotThrow(() -> IOUtilities.close(mockReader));
        // Verify that close was still invoked exactly once
        verify(mockReader, times(1)).close();
    }

    /**
     * Verify that an unchecked {@link RuntimeException} (e.g., {@link IllegalStateException})
     * thrown by {@code reader.close()} propagates out of the utility method.
     */
    @Test
    void testCloseNonNullReader_closeThrowsRuntimeException_propagates() throws Exception {
        // Arrange
        doThrow(new IllegalStateException("runtime exception")).when(mockReader).close();
        // Act & Assert
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> IOUtilities.close(mockReader));
        // Optional sanity check on the exception message
        assert (thrown.getMessage().contains("runtime exception"));
        // Verify that close was invoked exactly once before the exception bubbled up
        verify(mockReader, times(1)).close();
    }

    /**
     * Verify that an {@link Error} (e.g., {@link AssertionError}) thrown by
     * {@code reader.close()} propagates unchanged.
     */
    @Test
    void testCloseNonNullReader_closeThrowsError_propagates() throws Exception {
        // Arrange
        doThrow(new AssertionError("assertion error")).when(mockReader).close();
        // Act & Assert
        AssertionError thrown = assertThrows(AssertionError.class, () -> IOUtilities.close(mockReader));
        // Optional sanity check on the error message
        assert (thrown.getMessage().contains("assertion error"));
        // Verify that close was invoked exactly once before the error bubbled up
        verify(mockReader, times(1)).close();
    }
}
