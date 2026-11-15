package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.Closeable;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class IOUtilities_close_2_Test {

    /**
     * Mock used in several tests – created per‑test to avoid cross‑test interference.
     */
    @Mock
    private Closeable mockCloseable;

    class MultiClose implements Closeable {

        int callCount = 0;

        @Override
        public void close() throws IOException {
            callCount++;
            // internal second call – should not affect the utility method's behavior
            callCount++;
        }
    }

    class DelayedClose implements Closeable {

        @Override
        public void close() throws IOException {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    class ConditionalClose implements Closeable {

        @Override
        public void close() throws IOException {
            if (System.nanoTime() < 0) {
                throw new IOException("impossible");
            }
            // normal completion
        }
    }

    /**
     * Custom {@link IOException} subclass to verify that the catch block
     * handles all subclasses of {@code IOException}.
     */
    static class MyIOException extends IOException {

        MyIOException(String msg) {
            super(msg);
        }
    }

    class MultiClose implements Closeable {

        int callCount = 0;

        @Override
        public void close() throws IOException {
            callCount++;
            // internal second call – should not affect the utility method's behavior
            callCount++;
        }
    }

    class DelayedClose implements Closeable {

        @Override
        public void close() throws IOException {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    class ConditionalClose implements Closeable {

        @Override
        public void close() throws IOException {
            if (System.nanoTime() < 0) {
                throw new IOException("impossible");
            }
            // normal completion
        }
    }

    /**
     * Initialise Mockito annotations before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Verify that passing {@code null} to {@code IOUtilities.close} does not throw
     * any exception and does not attempt to invoke any method on a mock.
     */
    @Test
    void close_NullArgument_NoException() {
        assertDoesNotThrow(() -> IOUtilities.close(null));
    }

    /**
     * Verify that a normal {@link Closeable} has its {@code close()} method invoked exactly once.
     */
    @Test
    void close_ValidCloseable_InvokesCloseOnce() throws IOException {
        // Arrange – mock already created by @Mock
        // Act
        IOUtilities.close(mockCloseable);
        // Assert
        verify(mockCloseable, times(1)).close();
    }

    /**
     * Verify that an {@link IOException} thrown by {@code close()} is silently ignored.
     */
    @Test
    void close_CloseableThrowsIOException_ExceptionIsIgnored() throws IOException {
        // Arrange
        doThrow(new IOException("simulated IO problem")).when(mockCloseable).close();
        // Act & Assert – the method must not propagate the exception
        assertDoesNotThrow(() -> IOUtilities.close(mockCloseable));
        // Verify that close was still called once despite the exception
        verify(mockCloseable, times(1)).close();
    }

    /**
     * Validate that a {@link RuntimeException} thrown by {@code close()}
     * propagates out of {@code IOUtilities.close}.
     */
    @Test
    void close_CloseableThrowsRuntimeException_Propagates() {
        Closeable runtimeThrower = () -> {
            throw new IllegalStateException("runtime problem");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> IOUtilities.close(runtimeThrower));
        assertEquals("runtime problem", ex.getMessage());
    }

    /**
     * Ensure that an {@link Error} thrown by {@code close()} propagates.
     */
    @Test
    void close_CloseableThrowsError_Propagates() {
        Closeable errorThrower = () -> {
            throw new AssertionError("serious error");
        };
        AssertionError err = assertThrows(AssertionError.class, () -> IOUtilities.close(errorThrower));
        assertEquals("serious error", err.getMessage());
    }

    /**
     * Confirm that {@code IOUtilities.close} calls {@code close()} exactly once,
     * even if the implementation itself calls {@code close()} multiple times.
     */
    @Test
    void close_CloseableInvokesCloseMultipleTimes_InternalCalls() {
        class MultiClose implements Closeable {

            int callCount = 0;

            @Override
            public void close() throws IOException {
                callCount++;
                // internal second call – should not affect the utility method's behavior
                callCount++;
            }
        }
        MultiClose multi = new MultiClose();
        IOUtilities.close(multi);
        // The utility method invoked close() once; the implementation incremented twice.
        assertEquals(2, multi.callCount, "Implementation should have incremented twice");
    }

    /**
     * Verify that a short pause inside {@code close()} does not cause the utility
     * method to time‑out or be interrupted.
     */
    @Test
    void close_CloseableWithDelay_CompletesNormally() {
        class DelayedClose implements Closeable {

            @Override
            public void close() throws IOException {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        assertDoesNotThrow(() -> IOUtilities.close(new DelayedClose()));
    }

    /**
     * Test a {@code close()} implementation that contains a conditional branch
     * which never triggers an exception.
     */
    @Test
    void close_CloseableConditionalNeverThrows_NoException() {
        class ConditionalClose implements Closeable {

            @Override
            public void close() throws IOException {
                if (System.nanoTime() < 0) {
                    throw new IOException("impossible");
                }
                // normal completion
            }
        }
        assertDoesNotThrow(() -> IOUtilities.close(new ConditionalClose()));
    }

    /**
     * Provide a {@link Closeable} whose {@code close()} throws a custom
     * {@link IOException} subclass; the utility method must suppress it.
     */
    @Test
    void close_CloseableThrowsCustomIOException_SubclassIsSuppressed() {
        Closeable customIOExceptionThrower = () -> {
            throw new MyIOException("custom IO problem");
        };
        assertDoesNotThrow(() -> IOUtilities.close(customIOExceptionThrower));
    }
}
