package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.Flushable;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_flush_Test {

    /**
     * A mock {@link Flushable} that behaves normally (no exception).
     */
    @Mock
    Flushable normalFlushable;

    /**
     * A mock {@link Flushable} that throws an {@link IOException} when {@code flush()} is called.
     */
    @Mock
    Flushable throwingFlushable;

    /**
     * A mock {@link Flushable} that throws a {@link RuntimeException} when {@code flush()} is called.
     */
    @Mock
    Flushable runtimeExceptionFlushable;

    @BeforeEach
    void setupBeforeEach() {
        // Reset mocks before each test to avoid cross‑test interference.
        reset(normalFlushable, throwingFlushable, runtimeExceptionFlushable);
    }

    @Test
    void flush_NullArgument_DoesNothing() {
        // Act & Assert – the method should simply return without throwing.
        assertDoesNotThrow(() -> IOUtilities.flush(null));
    }

    @Test
    void flush_NonNullFlushable_InvokesFlushOnce() throws IOException {
        // Arrange – normalFlushable does not throw.
        doNothing().when(normalFlushable).flush();
        // Act
        IOUtilities.flush(normalFlushable);
        // Assert – verify that flush() was called exactly once.
        verify(normalFlushable, times(1)).flush();
        verifyNoMoreInteractions(normalFlushable);
    }

    @Test
    void flush_FlushableThrowsIOException_ExceptionIsSwallowed() throws IOException {
        // Arrange – configure the mock to throw IOException.
        doThrow(new IOException("simulated")).when(throwingFlushable).flush();
        // Act & Assert – the method must not propagate the exception.
        assertDoesNotThrow(() -> IOUtilities.flush(throwingFlushable));
        // Verify that flush() was still invoked.
        verify(throwingFlushable, times(1)).flush();
        verifyNoMoreInteractions(throwingFlushable);
    }

    @Test
    void flush_FlushableThrowsRuntimeException_ExceptionPropagates() {
        // Arrange – configure the mock to throw a RuntimeException.
        RuntimeException runtimeEx = new RuntimeException("runtime failure");
        doThrow(runtimeEx).when(runtimeExceptionFlushable).flush();
        // Act & Assert – the RuntimeException must be propagated.
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> IOUtilities.flush(runtimeExceptionFlushable), "Expected RuntimeException to be propagated");
        assertSame(runtimeEx, thrown, "The propagated exception should be the original one");
        // Verify that flush() was called exactly once before the exception bubbled up.
        verify(runtimeExceptionFlushable, times(1)).flush();
        verifyNoMoreInteractions(runtimeExceptionFlushable);
    }
}
