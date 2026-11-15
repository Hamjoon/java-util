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
class IOUtilities_flush_2_Test {

    @Mock
    XMLStreamWriter // injected by Mockito
    mockWriter;

    /**
     * Verify that calling {@code IOUtilities.flush(null)} does not throw any exception.
     */
    @Test
    void flush_nullWriter_doesNotThrow() {
        assertDoesNotThrow(() -> IOUtilities.flush(null));
    }

    /**
     * Verify that the writer's {@code flush()} method is invoked exactly once when the writer is non‑null.
     */
    @Test
    void flush_nonNullWriter_invokesFlush() throws Exception {
        // The mock is already created by the @Mock annotation.
        // No special stubbing needed because the default behavior is to do nothing.
        IOUtilities.flush(mockWriter);
        // Verify that flush() was called once and no other interactions occurred.
        verify(mockWriter, times(1)).flush();
        verifyNoMoreInteractions(mockWriter);
    }

    /**
     * Verify that an {@link XMLStreamException} thrown by the writer's {@code flush()}
     * is silently ignored by {@code IOUtilities.flush}.
     */
    @Test
    void flush_writerThrowsException_isSilentlyIgnored() throws Exception {
        // Arrange: make the mock throw an XMLStreamException when flush() is called.
        doThrow(new XMLStreamException("simulated exception")).when(mockWriter).flush();
        // Act & Assert: the method should not propagate the exception.
        assertDoesNotThrow(() -> IOUtilities.flush(mockWriter));
        // Verify that flush() was indeed called.
        verify(mockWriter, times(1)).flush();
    }

    /**
     * Verify that a checked {@link XMLStreamException} thrown by {@code writer.flush()}
     * is caught and ignored.
     */
    @Test
    void flush_writerThrowsXMLStreamException_isSilentlyIgnored() throws Exception {
        doThrow(new XMLStreamException("simulated checked exception")).when(mockWriter).flush();
        assertDoesNotThrow(() -> IOUtilities.flush(mockWriter));
        verify(mockWriter, times(1)).flush();
    }

    /**
     * Verify that an unchecked {@link IllegalStateException} propagates out of the method.
     */
    @Test
    void flush_writerThrowsIllegalStateException_isPropagated() throws Exception {
        doThrow(new IllegalStateException("simulated unchecked exception")).when(mockWriter).flush();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> IOUtilities.flush(mockWriter));
        assertEquals("simulated unchecked exception", thrown.getMessage());
        verify(mockWriter, times(1)).flush();
    }

    /**
     * Verify that an {@link AssertionError} propagates out of the method.
     */
    @Test
    void flush_writerThrowsAssertionError_isPropagated() throws Exception {
        doThrow(new AssertionError("simulated error")).when(mockWriter).flush();
        AssertionError thrown = assertThrows(AssertionError.class, () -> IOUtilities.flush(mockWriter));
        assertEquals("simulated error", thrown.getMessage());
        verify(mockWriter, times(1)).flush();
    }

    /**
     * Verify that a {@link NullPointerException} (runtime) propagates out of the method.
     */
    @Test
    void flush_writerThrowsNullPointerException_isPropagated() throws Exception {
        doThrow(new NullPointerException("simulated NPE")).when(mockWriter).flush();
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> IOUtilities.flush(mockWriter));
        assertEquals("simulated NPE", thrown.getMessage());
        verify(mockWriter, times(1)).flush();
    }

    /**
     * Verify that an {@link IllegalArgumentException} propagates out of the method.
     */
    @Test
    void flush_writerThrowsIllegalArgumentException_isPropagated() throws Exception {
        doThrow(new IllegalArgumentException("simulated IAE")).when(mockWriter).flush();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> IOUtilities.flush(mockWriter));
        assertEquals("simulated IAE", thrown.getMessage());
        verify(mockWriter, times(1)).flush();
    }
}
