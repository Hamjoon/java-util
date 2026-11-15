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

    @Test
    void flush_NullWriter_DoesNothing() {
        // Given
        XMLStreamWriter writer = null;
        // When / Then
        assertDoesNotThrow(() -> IOUtilities.flush(writer), "flush(null) should not throw any exception");
        // No interaction to verify because writer is null.
    }

    @Test
    void flush_ValidWriter_CallsFlushOnce() throws XMLStreamException {
        // Given
        XMLStreamWriter writer = mock(XMLStreamWriter.class);
        // When
        IOUtilities.flush(writer);
        // Then
        verify(writer, times(1)).flush();
        verifyNoMoreInteractions(writer);
    }

    @Test
    void flush_WriterThrowsXMLStreamException_ExceptionIsSwallowed() throws XMLStreamException {
        // Given
        XMLStreamWriter writer = mock(XMLStreamWriter.class);
        doThrow(new XMLStreamException("simulated")).when(writer).flush();
        // When / Then
        assertDoesNotThrow(() -> IOUtilities.flush(writer), "flush should swallow XMLStreamException thrown by writer.flush()");
        // Even though an exception was thrown, the method should have attempted to flush once.
        verify(writer, times(1)).flush();
    }

    @Test
    void flush_WriterThrowsRuntimeException_ExceptionPropagates() throws XMLStreamException {
        // Given
        XMLStreamWriter writer = mock(XMLStreamWriter.class);
        doThrow(new IllegalStateException("runtime problem")).when(writer).flush();
        // When / Then
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> IOUtilities.flush(writer), "flush should let unchecked RuntimeException propagate");
        assertEquals("runtime problem", thrown.getMessage());
        // Verify that flush() was indeed invoked before the exception escaped.
        verify(writer, times(1)).flush();
    }

    @Test
    void flush_WriterThrowsError_ErrorPropagates() throws XMLStreamException {
        // Given
        XMLStreamWriter writer = mock(XMLStreamWriter.class);
        doThrow(new AssertionError("serious error")).when(writer).flush();
        // When / Then
        AssertionError thrown = assertThrows(AssertionError.class, () -> IOUtilities.flush(writer), "flush should let Error propagate");
        assertEquals("serious error", thrown.getMessage());
        // Verify that flush() was called once before the error escaped.
        verify(writer, times(1)).flush();
    }
}
