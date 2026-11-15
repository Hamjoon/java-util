package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.Closeable;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.FileNotFoundException;

@ExtendWith(MockitoExtension.class)
class IOUtilities_close_2_Test {

    /**
     * Simple Closeable that throws a RuntimeException.
     */
    private static class RuntimeExceptionCloseable implements Closeable {

        @Override
        public void close() {
            throw new RuntimeException("runtime");
        }
    }

    /**
     * Closeable that throws a FileNotFoundException (subclass of IOException).
     */
    private static class SubclassIOExceptionCloseable implements Closeable {

        private boolean closed = false;

        @Override
        public void close() throws FileNotFoundException {
            closed = true;
            throw new FileNotFoundException("file not found");
        }

        boolean wasClosed() {
            return closed;
        }
    }

    /**
     * Closeable that sets a flag before throwing an IOException.
     */
    private static class SideEffectExceptionCloseable implements Closeable {

        private boolean closed = false;

        @Override
        public void close() throws IOException {
            closed = true;
            throw new IOException("side effect IO");
        }

        boolean isClosed() {
            return closed;
        }
    }

    /**
     * Closeable whose close() method internally attempts to call close()
     * multiple times. For the test we simply count the invocations; the
     * method itself does not recurse to avoid StackOverflow.
     */
    private static class MultipleCloseInvocationCloseable implements Closeable {

        private int closeCount = 0;

        @Override
        public void close() {
            closeCount++;
            // Simulate internal logic that *might* try to close again
            // (but we deliberately do not call close() again to keep it safe)
        }

        int getCloseCount() {
            return closeCount;
        }
    }

    /**
     * Closeable that throws a NullPointerException.
     */
    private static class NullPointerExceptionCloseable implements Closeable {

        @Override
        public void close() {
            throw new NullPointerException("null pointer");
        }
    }

    /**
     * Closeable that throws an AssertionError (subclass of Error).
     */
    private static class AssertionErrorCloseable implements Closeable {

        @Override
        public void close() {
            throw new AssertionError("assertion error");
        }
    }

    @Test
    void close_NullArgument_DoesNothing() {
        // Act & Assert – the method should simply return without throwing
        assertDoesNotThrow(() -> IOUtilities.close(null));
    }

    @Test
    void close_ValidCloseable_InvokesClose() throws IOException {
        // Arrange
        Closeable mockCloseable = mock(Closeable.class);
        // Act
        IOUtilities.close(mockCloseable);
        // Assert
        verify(mockCloseable, times(1)).close();
    }

    @Test
    void close_CloseableThrowsIOException_ExceptionIsSwallowed() throws IOException {
        // Arrange
        Closeable mockCloseable = mock(Closeable.class);
        doThrow(new IOException("simulated")).when(mockCloseable).close();
        // Act & Assert – the IOException must be ignored, not propagated
        assertDoesNotThrow(() -> IOUtilities.close(mockCloseable));
        // Verify that close() was still attempted
        verify(mockCloseable, times(1)).close();
    }

    @Test
    void close_CloseableThrowsRuntimeException_ExceptionPropagates() {
        // Arrange
        Closeable runtimeCloseable = new RuntimeExceptionCloseable();
        // Act & Assert – unchecked RuntimeException should NOT be swallowed
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.close(runtimeCloseable));
        assertEquals("runtime", ex.getMessage());
    }

    @Test
    void close_CloseableThrowsSubclassOfIOException_ExceptionIsSwallowed() {
        // Arrange
        Closeable subclassCloseable = new SubclassIOExceptionCloseable();
        // Act & Assert – FileNotFoundException (subclass of IOException) is ignored
        assertDoesNotThrow(() -> IOUtilities.close(subclassCloseable));
        assertTrue(((SubclassIOExceptionCloseable) subclassCloseable).wasClosed(), "close() should have been invoked");
    }

    @Test
    void close_CloseableThrowsIOExceptionWithSideEffect_ExceptionIsSwallowedAndSideEffectObserved() {
        // Arrange
        SideEffectExceptionCloseable sideEffectCloseable = new SideEffectExceptionCloseable();
        // Act & Assert – IOException is swallowed but side‑effect flag must be true
        assertDoesNotThrow(() -> IOUtilities.close(sideEffectCloseable));
        assertTrue(sideEffectCloseable.isClosed(), "Side‑effect flag should be true indicating close() was executed");
    }

    @Test
    void close_CloseableInvokesCloseOnlyOnce_EvenIfImplementationCallsItMultipleTimes() {
        // Arrange
        MultipleCloseInvocationCloseable multiClose = new MultipleCloseInvocationCloseable();
        // Act
        IOUtilities.close(multiClose);
        // Assert – only a single invocation should have been recorded
        assertEquals(1, multiClose.getCloseCount(), "IOUtilities.close should call close() exactly once");
    }

    @Test
    void close_CloseableThrowsNullPointerException_ExceptionPropagates() {
        // Arrange
        Closeable npeCloseable = new NullPointerExceptionCloseable();
        // Act & Assert – NullPointerException is unchecked and must propagate
        NullPointerException ex = assertThrows(NullPointerException.class, () -> IOUtilities.close(npeCloseable));
        assertEquals("null pointer", ex.getMessage());
    }

    @Test
    void close_CloseableThrowsError_ExceptionPropagates() {
        // Arrange
        Closeable errorCloseable = new AssertionErrorCloseable();
        // Act & Assert – Errors are not caught by the method
        AssertionError err = assertThrows(AssertionError.class, () -> IOUtilities.close(errorCloseable));
        assertEquals("assertion error", err.getMessage());
    }
}
