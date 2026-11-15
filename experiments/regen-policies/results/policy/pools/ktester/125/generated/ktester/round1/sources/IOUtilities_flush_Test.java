package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.Flushable;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.FileNotFoundException;

@ExtendWith(MockitoExtension.class)
class IOUtilities_flush_Test {

    /**
     * A mock Flushable that will behave normally (no exception).
     */
    @Mock
    private Flushable normalFlushable;

    /**
     * A mock Flushable that will throw an IOException when flushed.
     */
    @Mock
    private Flushable throwingFlushable;

    /**
     * A mock Flushable that throws a runtime exception when flushed.
     */
    @Mock
    private Flushable runtimeExceptionFlushable;

    /**
     * A mock Flushable that throws a subclass of {@link IOException}.
     */
    @Mock
    private Flushable subclassIOExceptionFlushable;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state for each mock before every test.
        reset(normalFlushable, throwingFlushable, runtimeExceptionFlushable, subclassIOExceptionFlushable);
    }

    @Test
    void flush_NullArgument_DoesNothing() {
        // The method should simply return without throwing any exception.
        assertDoesNotThrow(() -> IOUtilities.flush(null));
    }

    @Test
    void flush_NormalFlushable_InvokesFlushOnce() throws IOException {
        // Arrange: normalFlushable does nothing on flush().
        // Act
        IOUtilities.flush(normalFlushable);
        // Assert: flush() was called exactly once and no exception propagated.
        verify(normalFlushable, times(1)).flush();
    }

    @Test
    void flush_FlushableThrowsIOException_ExceptionIsSwallowed() throws IOException {
        // Arrange: configure the mock to throw IOException when flush() is called.
        doThrow(new IOException("simulated")).when(throwingFlushable).flush();
        // Act & Assert: the method must not propagate the exception.
        assertDoesNotThrow(() -> IOUtilities.flush(throwingFlushable));
        // Verify that flush() was still attempted exactly once.
        verify(throwingFlushable, times(1)).flush();
    }

    @Test
    void flush_FlushableThrowsRuntimeException_Propagates() throws IOException {
        // Arrange: mock throws a runtime exception (IllegalStateException) on flush().
        doThrow(new IllegalStateException("illegal state")).when(runtimeExceptionFlushable).flush();
        // Act & Assert: the runtime exception should NOT be caught and must propagate.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> IOUtilities.flush(runtimeExceptionFlushable), "IllegalStateException should be propagated by flush()");
        assertEquals("illegal state", ex.getMessage());
        // Verify that flush() was indeed invoked.
        verify(runtimeExceptionFlushable, times(1)).flush();
    }

    @Test
    void flush_FlushableThrowsSubclassOfIOException_ExceptionIsIgnored() throws IOException {
        // Arrange: mock throws a subclass of IOException (FileNotFoundException).
        doThrow(new FileNotFoundException("file not found")).when(subclassIOExceptionFlushable).flush();
        // Act & Assert: the method must swallow the exception just like a plain IOException.
        assertDoesNotThrow(() -> IOUtilities.flush(subclassIOExceptionFlushable));
        // Verify that flush() was called exactly once.
        verify(subclassIOExceptionFlushable, times(1)).flush();
    }
}
