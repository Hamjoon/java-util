package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_close_Test {

    @Mock
    XMLStreamReader mockReader;

    @AfterEach
    void teardownAfterEach() {
        // Verify that no unexpected interactions occurred.
        // This is a safety net; individual tests may add their own verifications.
        verifyNoMoreInteractions(mockReader);
    }

    @Test
    void close_NonNullReader_InvokesClose() {
        // Arrange
        // mockReader is already a Mockito mock; by default close() does nothing.
        // Act & Assert
        assertDoesNotThrow(() -> IOUtilities.close(mockReader));
        // Verify that close() was called exactly once.
        verify(mockReader, times(1)).close();
    }

    @Test
    void close_ReaderThrowsXMLStreamException_ExceptionIsSwallowed() throws XMLStreamException {
        // Arrange
        doThrow(new XMLStreamException("simulated")).when(mockReader).close();
        // Act & Assert
        assertDoesNotThrow(() -> IOUtilities.close(mockReader));
        // Verify that close() was still called despite the exception.
        verify(mockReader, times(1)).close();
    }

    @Test
    void close_NullReader_DoesNothing() {
        // Act & Assert – simply ensure no exception is thrown.
        assertDoesNotThrow(() -> IOUtilities.close((XMLStreamReader) null));
        // No interaction verification needed because the argument is null.
    }

    /**
     * 4. Unchecked exception propagates – IllegalStateException should bubble up. **
     */
    @Test
    void close_ReaderThrowsRuntimeException_ExceptionPropagates() {
        doThrow(new IllegalStateException("runtime")).when(mockReader).close();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> IOUtilities.close(mockReader));
        assertEquals("runtime", thrown.getMessage());
        verify(mockReader, times(1)).close();
    }

    /**
     * 5. No‑op close (close() does nothing) – still verified exactly once. **
     */
    @Test
    void close_ReaderNoOp_CloseCalledExactlyOnce() {
        // No special stubbing – the mock's close() is a no‑op.
        assertDoesNotThrow(() -> IOUtilities.close(mockReader));
        verify(mockReader, times(1)).close();
    }
}
