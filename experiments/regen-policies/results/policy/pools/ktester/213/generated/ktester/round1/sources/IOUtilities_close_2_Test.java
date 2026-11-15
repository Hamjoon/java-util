package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.Closeable;
import java.io.IOException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_close_2_Test {

    /**
     * A mock Closeable that will be used in several tests.
     */
    @Mock
    private Closeable mockCloseable;

    /**
     * Verifies that calling {@code IOUtilities.close(null)} does not throw any exception.
     */
    @Test
    void close_nullReference_isIgnored() {
        assertDoesNotThrow(() -> IOUtilities.close((Closeable) null));
    }

    /**
     * Verifies that a non‑null {@link Closeable} has its {@code close()} method invoked exactly once.
     */
    @Test
    void close_validCloseable_invokesClose() throws IOException {
        // Arrange – the mock is already created by @Mock
        // Act
        IOUtilities.close(mockCloseable);
        // Assert
        verify(mockCloseable, times(1)).close();
    }

    /**
     * Verifies that an {@link IOException} thrown by {@code close()} is silently ignored.
     */
    @Test
    void close_closeThrowsIOException_isSilentlyIgnored() throws IOException {
        // Arrange
        doThrow(new IOException("simulated IO failure")).when(mockCloseable).close();
        // Act / Assert – the method must not propagate the exception.
        assertDoesNotThrow(() -> IOUtilities.close(mockCloseable));
        // Verify that close() was still attempted.
        verify(mockCloseable, times(1)).close();
    }

    /**
     * A sanity check using a real {@link java.io.ByteArrayOutputStream}
     * (which implements {@link Closeable}) to ensure the method works with concrete classes.
     */
    @Test
    void close_realCloseable_doesNotThrow() {
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            // The stream is still open here; we close it via the utility.
            assertDoesNotThrow(() -> IOUtilities.close(baos));
        }
    }

    /**
     * Verifies that a {@link RuntimeException} (unchecked) thrown by {@code close()}
     * propagates out of {@link IOUtilities#close(Closeable)}.
     */
    @Test
    void close_closeThrowsRuntimeException_propagates() throws IOException {
        doThrow(new IllegalStateException("boom")).when(mockCloseable).close();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> IOUtilities.close(mockCloseable));
        assertEquals("boom", thrown.getMessage());
        verify(mockCloseable, times(1)).close();
    }

    /**
     * Verifies that a {@link NullPointerException} thrown by {@code close()}
     * propagates out of the utility method.
     */
    @Test
    void close_closeThrowsNullPointerException_propagates() throws IOException {
        doThrow(new NullPointerException("null")).when(mockCloseable).close();
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> IOUtilities.close(mockCloseable));
        assertEquals("null", thrown.getMessage());
        verify(mockCloseable, times(1)).close();
    }

    /**
     * Verifies that an {@link AssertionError} (subclass of {@link Error}) thrown by {@code close()}
     * propagates out of the utility method.
     */
    @Test
    void close_closeThrowsAssertionError_propagates() throws IOException {
        doThrow(new AssertionError("fatal")).when(mockCloseable).close();
        AssertionError thrown = assertThrows(AssertionError.class, () -> IOUtilities.close(mockCloseable));
        assertEquals("fatal", thrown.getMessage());
        verify(mockCloseable, times(1)).close();
    }

    /**
     * Verifies that an {@link OutOfMemoryError} thrown by {@code close()}
     * propagates out of the utility method.
     */
    @Test
    void close_closeThrowsOutOfMemoryError_propagates() throws IOException {
        doThrow(new OutOfMemoryError("oom")).when(mockCloseable).close();
        OutOfMemoryError thrown = assertThrows(OutOfMemoryError.class, () -> IOUtilities.close(mockCloseable));
        assertEquals("oom", thrown.getMessage());
        verify(mockCloseable, times(1)).close();
    }

    /**
     * Verifies that any {@link Error} other than {@link IOException} is not swallowed.
     */
    @Test
    void close_closeThrowsGenericError_propagates() throws IOException {
        doThrow(new StackOverflowError("stack overflow")).when(mockCloseable).close();
        StackOverflowError thrown = assertThrows(StackOverflowError.class, () -> IOUtilities.close(mockCloseable));
        assertEquals("stack overflow", thrown.getMessage());
        verify(mockCloseable, times(1)).close();
    }
}
