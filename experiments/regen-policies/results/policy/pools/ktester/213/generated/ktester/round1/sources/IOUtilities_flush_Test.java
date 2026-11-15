package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.Flushable;
import java.io.IOException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_flush_Test {

    @Mock
    private Flushable mockFlushable;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources.
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * Verifies that a non‑null {@link Flushable} has its {@code flush()} method invoked exactly once.
     */
    @Test
    void flushInvokesFlushOnNonNullFlushable() throws IOException {
        // Given: a mock Flushable that does nothing on flush()
        doNothing().when(mockFlushable).flush();
        // When
        IOUtilities.flush(mockFlushable);
        // Then
        verify(mockFlushable, times(1)).flush();
    }

    /**
     * Verifies that an {@link IOException} thrown by {@code flush()} is silently ignored.
     */
    @Test
    void flushIgnoresIOException() throws IOException {
        // Given: a mock Flushable that throws IOException when flush() is called
        doThrow(new IOException("simulated IO error")).when(mockFlushable).flush();
        // When / Then: the utility method must not propagate the exception
        assertDoesNotThrow(() -> IOUtilities.flush(mockFlushable));
        // And the flush method was still invoked once
        verify(mockFlushable, times(1)).flush();
    }

    /**
     * Verifies that passing {@code null} to {@code flush} results in a no‑op and does not throw.
     */
    @Test
    void flushDoesNothingWhenArgumentIsNull() {
        // When / Then: no exception should be thrown for a null argument
        assertDoesNotThrow(() -> IOUtilities.flush(null));
        // No interaction with the mock is expected
        verifyNoInteractions(mockFlushable);
    }

    /**
     * Verifies that a {@link RuntimeException} (or any unchecked exception) thrown by {@code flush()}
     * is **not** caught by the utility method and therefore propagates to the caller.
     */
    @Test
    void flushPropagatesRuntimeException() throws IOException {
        // Given: a mock Flushable that throws a RuntimeException on flush()
        RuntimeException runtimeEx = new RuntimeException("simulated runtime error");
        doThrow(runtimeEx).when(mockFlushable).flush();
        // When / Then: the RuntimeException should bubble up
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> IOUtilities.flush(mockFlushable), "Expected RuntimeException to propagate from flush()");
        // Verify that the thrown exception is the same instance we configured
        assertSame(runtimeEx, thrown, "The propagated exception should be the original one");
        // And ensure flush() was invoked exactly once before the exception escaped
        verify(mockFlushable, times(1)).flush();
    }
}
