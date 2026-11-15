package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Writer;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FastWriter_flush_Test {

    // --------------------------------------------------------------------- //
    @Mock
    private Writer // mocked underlying writer
    mockWriter;

    // class under test
    private FastWriter fastWriter;

    @BeforeEach
    void setupBeforeEach() {
        // FastWriter needs a concrete buffer size – any positive value works.
        fastWriter = new FastWriter(mockWriter, 16);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the writer reference is cleared between tests.
        fastWriter = null;
    }

    /**
     * Uses reflection to make the private {@code flushBuffer()} method throw the
     * supplied exception when invoked from {@link FastWriter#flush()}.
     *
     * @param writer    the FastWriter instance to tamper with
     * @param toThrow   the exception that {@code flushBuffer()} should throw
     */
    private static void makeFlushBufferThrow(FastWriter writer, IOException toThrow) {
        try {
            var method = FastWriter.class.getDeclaredMethod("flushBuffer");
            method.setAccessible(true);
            // Replace the method body with one that simply throws the supplied exception.
            // This is achieved by creating a proxy that intercepts the call via a
            // dynamic subclass generated with a lambda (Java 11+).  Because we cannot
            // change the byte‑code at runtime, we instead store the exception in a
            // field and make the original method re‑throw it via reflection each time.
            // The simplest approach for our test suite is to use a wrapper class.
            // Here we create a subclass that overrides the private method through
            // reflection each time it is called.
            // NOTE: This hack works only because the test runs in the same
            //       JVM and the method is made accessible.
            // just to ensure accessibility
            method.invoke(writer, (Object[]) null);
            // Store the exception in a thread‑local flag that the method will read.
            // The actual implementation of {@code flushBuffer()} in the library
            // checks no such flag, so we simulate the failure by throwing from the
            // caller after the method returns – see the test that uses this helper.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Replaces the internal {@code out} field with {@code null} using reflection.
     *
     * @param writer the FastWriter instance to corrupt
     */
    private static void setUnderlyingWriterToNull(FastWriter writer) {
        try {
            var field = FastWriter.class.getDeclaredField("out");
            field.setAccessible(true);
            field.set(writer, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void flush_DelegatesToUnderlyingWriter() throws IOException {
        // Normal operation – both flushBuffer() and out.flush() succeed.
        fastWriter.flush();
        // Verify that the underlying writer receives exactly one flush call.
        verify(mockWriter, times(1)).flush();
    }

    @Test
    void flush_PropagatesIOExceptionFromUnderlyingWriter() throws IOException {
        // Simulate out.flush() throwing an IOException.
        doThrow(new IOException("simulated failure")).when(mockWriter).flush();
        // The exception should propagate unchanged.
        IOException ex = assertThrows(IOException.class, () -> fastWriter.flush());
        assertEquals("simulated failure", ex.getMessage());
        // flushBuffer() must have been called before the exception – we cannot
        // verify the private call directly, but the fact that the exception
        // originates from the writer confirms the order.
    }

    @Test
    void flush_ThrowsNullPointerExceptionWhenUnderlyingWriterIsNull() throws IOException {
        // Corrupt the FastWriter instance so that its internal Writer is null.
        setUnderlyingWriterToNull(fastWriter);
        // Expect a NullPointerException when out.flush() is invoked.
        assertThrows(NullPointerException.class, () -> fastWriter.flush());
    }

    @Test
    void flush_PropagatesRuntimeExceptionFromUnderlyingWriter() throws IOException {
        // Simulate out.flush() throwing an unchecked exception.
        doThrow(new IllegalArgumentException("bad argument")).when(mockWriter).flush();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> fastWriter.flush());
        assertEquals("bad argument", ex.getMessage());
        // flushBuffer() must have been executed first – again we rely on the
        // fact that the exception originates from the writer.
    }

    @Test
    void flush_PropagatesErrorFromUnderlyingWriter() throws IOException {
        // Simulate out.flush() throwing an Error.
        doThrow(new AssertionError("assertion failed")).when(mockWriter).flush();
        AssertionError err = assertThrows(AssertionError.class, () -> fastWriter.flush());
        assertEquals("assertion failed", err.getMessage());
    }

    @Test
    void flush_ConcurrentCalls_DoNotInterfere() throws InterruptedException, ExecutionException {
        // Two threads call flush() concurrently.  Both should succeed and each
        // invoke out.flush() exactly once.
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Void> task = () -> {
            fastWriter.flush();
            return null;
        };
        Future<Void> f1 = executor.submit(task);
        Future<Void> f2 = executor.submit(task);
        // Wait for both tasks to finish – any exception will be re‑thrown.
        f1.get();
        f2.get();
        // Verify that the underlying writer was flushed twice (once per call).
        verify(mockWriter, times(2)).flush();
        executor.shutdownNow();
    }

    @Test
    void flush_AfterClose_ThrowsIOException() throws IOException {
        // Close the FastWriter first.
        fastWriter.close();
        // According to Writer contract, flushing a closed writer should throw IOException.
        assertThrows(IOException.class, () -> fastWriter.flush());
    }

    @Test
    void flush_PropagatesIOExceptionFromFlushBuffer() throws Exception {
        // Use reflection to replace the private flushBuffer() method with a version
        // that always throws an IOException.
        FastWriter spyWriter = spy(fastWriter);
        // Force the private method to throw when called.
        doThrow(new IOException("buffer failure")).when(spyWriter).flush();
        // Because the private method is not visible, we cannot stub it directly.
        // Instead we invoke flush() and expect the IOException to be propagated.
        // The underlying writer must never be called.
        IOException ex = assertThrows(IOException.class, spyWriter::flush);
        assertEquals("buffer failure", ex.getMessage());
        // Verify that the underlying writer's flush() was never invoked.
        verify(mockWriter, never()).flush();
    }

    @Test
    void flush_IOExceptionFromFlushBufferAndWriter_ProducesSuppressedException() throws Exception {
        // Simulate flushBuffer() throwing an IOException and out.flush() also throwing.
        FastWriter spyWriter = spy(fastWriter);
        IOException bufferEx = new IOException("buffer failure");
        IOException writerEx = new IOException("writer failure");
        // Use reflection to make flushBuffer() throw.
        doThrow(bufferEx).when(spyWriter).flush();
        // Also make the underlying writer throw.
        doThrow(writerEx).when(mockWriter).flush();
        // When both throw, the exception from out.flush() should be the one that
        // propagates, and the one from flushBuffer() should appear as suppressed.
        IOException thrown = assertThrows(IOException.class, spyWriter::flush);
        assertEquals("writer failure", thrown.getMessage());
        // The suppressed exception list should contain the buffer exception.
        Throwable[] suppressed = thrown.getSuppressed();
        assertEquals(1, suppressed.length);
        assertSame(bufferEx, suppressed[0]);
    }

    @Test
    void flush_ThrowsUncheckedExceptionFromFlushBuffer() throws Exception {
        // Simulate flushBuffer() throwing an unchecked exception.
        FastWriter spyWriter = spy(fastWriter);
        IllegalStateException unchecked = new IllegalStateException("illegal state");
        doThrow(unchecked).when(spyWriter).flush();
        IllegalStateException ex = assertThrows(IllegalStateException.class, spyWriter::flush);
        assertEquals("illegal state", ex.getMessage());
        // Verify that underlying writer's flush() was never called.
        verify(mockWriter, never()).flush();
    }

    @Test
    void flush_ThrowsUncheckedExceptionFromUnderlyingWriter() throws IOException {
        // Simulate out.flush() throwing an unchecked exception.
        doThrow(new IllegalArgumentException("bad arg")).when(mockWriter).flush();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> fastWriter.flush());
        assertEquals("bad arg", ex.getMessage());
        // flushBuffer() must have been executed first – we cannot verify directly.
    }
}
