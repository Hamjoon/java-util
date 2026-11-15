package com.cedarsoftware.util;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_flush_2_Test {

    /**
     * Mocked writer used by the tests.
     */
    @Mock
    private XMLStreamWriter mockWriter;

    /**
     * Verify that a non‑null writer has its {@code flush()} method invoked exactly once.
     */
    @Test
    void flushInvokesWriterFlush() throws Exception {
        // Act
        IOUtilities.flush(mockWriter);
        // Assert
        verify(mockWriter, times(1)).flush();
        // No other interactions should happen.
        verifyNoMoreInteractions(mockWriter);
    }

    /**
     * Verify that an {@link XMLStreamException} thrown by {@code writer.flush()}
     * is silently ignored.
     */
    @Test
    void flushIgnoresXmlStreamException() throws Exception {
        // Arrange: make the mock throw an exception when flush() is called.
        doThrow(new XMLStreamException("simulated")).when(mockWriter).flush();
        // Act & Assert: the method must not propagate the exception.
        assertDoesNotThrow(() -> IOUtilities.flush(mockWriter));
        // Verify that flush was still attempted once.
        verify(mockWriter, times(1)).flush();
        verifyNoMoreInteractions(mockWriter);
    }

    /**
     * Verify that passing {@code null} does not cause a {@link NullPointerException}
     * and results in no interaction with any writer.
     */
    @Test
    void flushWithNullWriterDoesNothing() {
        // Act & Assert: should not throw.
        assertDoesNotThrow(() -> IOUtilities.flush(null));
        // Since the writer is null, there is nothing to verify.
        // The test simply ensures no exception bubbles up.
    }

    /**
     * Verify that an unchecked {@link RuntimeException} thrown by {@code writer.flush()}
     * propagates out of the method.
     */
    @Test
    void flushPropagatesRuntimeException() throws Exception {
        // Arrange: make the mock throw a RuntimeException.
        doThrow(new RuntimeException("boom")).when(mockWriter).flush();
        // Act & Assert: the RuntimeException must be re‑thrown.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.flush(mockWriter));
        assertEquals("boom", ex.getMessage());
        // Even though the exception propagates, flush() should have been called exactly once.
        verify(mockWriter, times(1)).flush();
        verifyNoMoreInteractions(mockWriter);
    }

    /**
     * Verify that an {@link Error} (e.g., {@link AssertionError}) thrown by {@code writer.flush()}
     * propagates out of the method.
     */
    @Test
    void flushPropagatesError() throws Exception {
        // Arrange: make the mock throw an AssertionError.
        doThrow(new AssertionError("assert fail")).when(mockWriter).flush();
        // Act & Assert: the AssertionError must be re‑thrown.
        AssertionError err = assertThrows(AssertionError.class, () -> IOUtilities.flush(mockWriter));
        assertEquals("assert fail", err.getMessage());
        // Verify the interaction count before the error propagates.
        verify(mockWriter, times(1)).flush();
        verifyNoMoreInteractions(mockWriter);
    }
}
