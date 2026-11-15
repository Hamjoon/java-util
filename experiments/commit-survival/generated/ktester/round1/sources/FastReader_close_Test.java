package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Reader;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FastReader_close_Test {

    // Mocked underlying Reader that FastReader delegates to
    @Mock
    private Reader mockReader;

    // Instance under test
    private FastReader fastReader;

    /**
     * Helper to access the private {@code in} field of {@link FastReader}.
     */
    private static Field inField;

    class FastReaderSubclass extends FastReader {

        FastReaderSubclass(Reader r) {
            super(r, 16, 0);
        }

        @Override
        public void close() throws IOException {
            // Additional behaviour could be added here; for the test we just call super.
            super.close();
        }
    }

    class FastReaderSubclass extends FastReader {

        FastReaderSubclass(Reader r) {
            super(r, 16, 0);
        }

        @Override
        public void close() throws IOException {
            // Additional behaviour could be added here; for the test we just call super.
            super.close();
        }
    }

    @BeforeAll
    static void initReflection() throws NoSuchFieldException {
        // The field is private; make it accessible once for all tests.
        inField = FastReader.class.getDeclaredField("in");
        inField.setAccessible(true);
    }

    /**
     * Initialise a fresh FastReader (with a mocked underlying Reader) before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // MockitoAnnotations.openMocks(this) is handled by the MockitoExtension,
        // so we can directly use the @Mock field.
        // Use a reasonable buffer size; push‑back buffer can be zero.
        fastReader = new FastReader(mockReader, 16, 0);
        // The exact values are not important for the close() tests.
        fastReader = new FastReader(mockReader, 16, 0);
    }

    /**
     * Verify that no unexpected interactions remain after each test.
     */
    @AfterEach
    void teardownAfterEach() {
        // Ensure that the mock has no unexpected calls left.
        verifyNoMoreInteractions(mockReader);
        // Ensure the mock was used only as declared in each test.
        verifyNoMoreInteractions(mockReader);
    }

    /**
     * Returns the current value of the private {@code in} field.
     */
    private Reader getInternalReader() throws IllegalAccessException {
        return (Reader) inField.get(fastReader);
    }

    /**
     * Sets the private {@code in} field to the supplied value via reflection.
     */
    private void setInternalReader(Reader r) throws IllegalAccessException {
        inField.set(fastReader, r);
    }

    /**
     * The primary behaviour of {@code FastReader.close()} is to delegate the close
     * operation to the underlying {@link Reader} and then clear its reference.
     * This test verifies that the delegate is called exactly once.
     */
    @Test
    void close_DelegatesToUnderlyingReader() throws IOException {
        // When
        fastReader.close();
        // Then
        verify(mockReader, times(1)).close();
    }

    /**
     * After the first close, the internal reference to the underlying reader is set
     * to {@code null}. Subsequent calls to {@code close()} must therefore not attempt
     * to close the delegate again. This test calls {@code close()} twice and asserts
     * that the underlying {@link Reader} is closed only once.
     */
    @Test
    void close_CalledTwice_ClosesUnderlyingReaderOnlyOnce() throws IOException {
        // When
        // first close – should invoke mockReader.close()
        fastReader.close();
        // second close – should be a no‑op
        fastReader.close();
        // Then
        verify(mockReader, times(1)).close();
    }

    /**
     * If the underlying reader throws an {@link IOException} during close, the same
     * exception should propagate to the caller. This test configures the mock to
     * throw and asserts that the exception is re‑thrown.
     */
    @Test
    void close_WhenUnderlyingReaderThrows_PropagatesException() throws IOException {
        // Given
        IOException ioException = new IOException("underlying close failed");
        doThrow(ioException).when(mockReader).close();
        // When / Then
        IOException thrown = Assertions.assertThrows(IOException.class, () -> fastReader.close());
        Assertions.assertSame(ioException, thrown);
    }

    /**
     * Verify that calling {@code close()} when the internal {@code in} reference is
     * {@code null} does nothing and does not throw.
     */
    @Test
    void close_WhenInternalReaderIsNull_DoesNothing() throws Exception {
        // Simulate a FastReader that was constructed without an underlying Reader.
        setInternalReader(null);
        // Should not throw any exception.
        assertDoesNotThrow(() -> fastReader.close());
        // Verify that the mock was never touched.
        verifyZeroInteractions(mockReader);
        // The internal field must still be null.
        assertNull(getInternalReader(), "Internal 'in' should remain null after close()");
    }

    /**
     * After a successful close the internal reference must be cleared.
     * This test also confirms that a subsequent close is a no‑op.
     */
    @Test
    void close_SuccessfulFirstClose_ClearsInternalReferenceAndIsIdempotent() throws Exception {
        // First close – should delegate and clear the field.
        fastReader.close();
        verify(mockReader, times(1)).close();
        assertNull(getInternalReader(), "Internal 'in' should be null after first close()");
        // Second close – should not interact with the mock again.
        fastReader.close();
        // No additional interactions allowed (verified by @AfterEach).
    }

    /**
     * If the underlying {@link Reader} throws a {@link RuntimeException}
     * (e.g., {@link NullPointerException}) during {@code close()}, the exception
     * must propagate unchanged and the internal reference must stay intact.
     */
    @Test
    void close_UnderlyingReaderThrowsRuntimeException_PropagatesAndKeepsReference() throws Exception {
        RuntimeException runtimeEx = new NullPointerException("NPE on close");
        doThrow(runtimeEx).when(mockReader).close();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> fastReader.close());
        assertSame(runtimeEx, thrown, "The same RuntimeException should be re‑thrown");
        // The internal reference must still point to the mock (not cleared).
        assertNotNull(getInternalReader(), "Internal 'in' should remain non‑null after RuntimeException");
    }

    /**
     * Simulate concurrent invocations of {@code close()} from multiple threads.
     * The underlying {@link Reader} must be closed exactly once and the internal
     * reference must end up {@code null}.
     */
    @Test
    void close_ConcurrentCalls_CloseInvokedExactlyOnce() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                // signal that this thread is ready
                ready.countDown();
                try {
                    // wait for the go‑signal
                    start.await();
                    // invoke close()
                    fastReader.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            }));
        }
        // Wait until all threads are ready, then fire the start signal.
        ready.await();
        start.countDown();
        // Wait for all tasks to finish, propagating any exception.
        for (Future<?> f : futures) {
            // will re‑throw if the task failed
            f.get();
        }
        executor.shutdownNow();
        // Underlying reader must have been closed exactly once.
        verify(mockReader, times(1)).close();
        // Internal reference must be null after the operation.
        assertNull(getInternalReader(), "Internal 'in' should be null after concurrent close()");
    }

    /**
     * Ensure that subclassing does not interfere with the base {@code close()}
     * implementation. The subclass overrides {@code close()} and calls {@code super.close()}.
     */
    @Test
    void close_SubclassOverridesAndCallsSuper_CloseBehavesCorrectly() throws Exception {
        // Subclass that simply forwards to super.close()
        class FastReaderSubclass extends FastReader {

            FastReaderSubclass(Reader r) {
                super(r, 16, 0);
            }

            @Override
            public void close() throws IOException {
                // Additional behaviour could be added here; for the test we just call super.
                super.close();
            }
        }
        FastReaderSubclass sub = new FastReaderSubclass(mockReader);
        sub.close();
        // Verify that the underlying mock was closed once.
        verify(mockReader, times(1)).close();
        // Using reflection to ensure the private field in the subclass instance is null.
        Field subclassIn = FastReader.class.getDeclaredField("in");
        subclassIn.setAccessible(true);
        assertNull(subclassIn.get(sub), "Subclass internal 'in' should be null after close()");
    }
}
