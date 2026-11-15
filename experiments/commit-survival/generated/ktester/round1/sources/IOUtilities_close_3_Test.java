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

    @Mock
    private XMLStreamWriter // injected by Mockito
    mockWriter;

    @Test
    void close_ShouldDoNothing_WhenWriterIsNull() {
        // given: a null writer
        XMLStreamWriter writer = null;
        // when / then: no exception is thrown
        assertDoesNotThrow(() -> IOUtilities.close(writer));
    }

    @Test
    void close_ShouldInvokeClose_WhenWriterIsNonNullAndNoException() throws Exception {
        // given: a mock writer that does not throw
        // (mockWriter is already injected by @Mock)
        // when
        IOUtilities.close(mockWriter);
        // then
        verify(mockWriter, times(1)).close();
    }

    @Test
    void close_ShouldSuppressXMLStreamException_WhenWriterThrows() throws Exception {
        // given: a mock writer that throws XMLStreamException on close()
        doThrow(new XMLStreamException("simulated")).when(mockWriter).close();
        // when / then: the method should not propagate the exception
        assertDoesNotThrow(() -> IOUtilities.close(mockWriter));
        // and: close() should still have been called exactly once
        verify(mockWriter, times(1)).close();
    }

    @Test
    void close_ShouldPropagateRuntimeException_WhenWriterThrowsRuntimeException() throws Exception {
        // given: a mock writer that throws a RuntimeException (IllegalStateException) on close()
        doThrow(new IllegalStateException("runtime failure")).when(mockWriter).close();
        // when / then: the RuntimeException must bubble up
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> IOUtilities.close(mockWriter));
        assertEquals("runtime failure", thrown.getMessage());
        // and: verify that close() was indeed invoked before the exception escaped
        verify(mockWriter, times(1)).close();
    }

    @Test
    void close_ShouldPropagateError_WhenWriterThrowsError() throws Exception {
        // given: a mock writer that throws an Error (AssertionError) on close()
        doThrow(new AssertionError("serious error")).when(mockWriter).close();
        // when / then: the Error must bubble up unchanged
        AssertionError thrown = assertThrows(AssertionError.class, () -> IOUtilities.close(mockWriter));
        assertEquals("serious error", thrown.getMessage());
        // and: verify that close() was called
        verify(mockWriter, times(1)).close();
    }
}
