package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_close_3_Test {

    /**
     * Mocked XMLStreamWriter used in the tests.
     */
    @Mock
    private XMLStreamWriter mockWriter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us via the @Mock annotation.
        // Resetting ensures a clean state for each test.
        reset(mockWriter);
    }

    @Test
    void close_nonNullWriter_callsClose() {
        // Arrange – mockWriter is already a mock; no special stubbing needed.
        // Act
        IOUtilities.close(mockWriter);
        // Assert – verify that close() was invoked exactly once.
        verify(mockWriter, times(1)).close();
    }

    @Test
    void close_nullWriter_doesNothing() {
        // Act & Assert – method must not throw and must not interact with any mock.
        assertDoesNotThrow(() -> IOUtilities.close((XMLStreamWriter) null));
        // No verification needed because there is no mock to interact with.
    }

    @Test
    void close_writerThrowsXMLStreamException_isSilentlyIgnored() throws XMLStreamException {
        // Arrange – configure the mock to throw an XMLStreamException when close() is called.
        doThrow(new XMLStreamException("simulated")).when(mockWriter).close();
        // Act & Assert – the utility method must swallow the exception and not re‑throw it.
        assertDoesNotThrow(() -> IOUtilities.close(mockWriter));
        // Verify that close() was indeed invoked despite the exception.
        verify(mockWriter, times(1)).close();
    }

    @Test
    void close_writerThrowsRuntimeException_isPropagated() throws XMLStreamException {
        // Arrange – mock throws a RuntimeException (IllegalStateException) on close().
        doThrow(new IllegalStateException("runtime failure")).when(mockWriter).close();
        // Act & Assert – the RuntimeException should propagate out of the utility method.
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> IOUtilities.close(mockWriter));
        // Verify the exception message to ensure it is the one we simulated.
        assertEquals("runtime failure", thrown.getMessage());
        // Verify that close() was still called exactly once before the exception escaped.
        verify(mockWriter, times(1)).close();
    }

    @Test
    void close_writerThrowsError_isPropagated() throws XMLStreamException {
        // Arrange – mock throws an Error (AssertionError) on close().
        doThrow(new AssertionError("assertion error")).when(mockWriter).close();
        // Act & Assert – the Error should propagate unchanged.
        AssertionError thrown = assertThrows(AssertionError.class, () -> IOUtilities.close(mockWriter));
        // Verify the error message.
        assertEquals("assertion error", thrown.getMessage());
        // Verify that close() was invoked exactly once before the error escaped.
        verify(mockWriter, times(1)).close();
    }
}
