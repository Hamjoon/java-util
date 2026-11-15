package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class FastWriter_flush_Test {

    private static final int CUSTOM_BUFFER_SIZE = 10;

    private FastWriter fastWriter;

    /**
     * A real {@link StringWriter} wrapped with a Mockito spy so we can verify {@code flush()} calls.
     */
    private StringWriter realStringWriter;

    private StringWriter spyStringWriter;

    /**
     * A mock {@link Writer} used to simulate exceptional behaviour.
     */
    @Mock
    private Writer failingWriter;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Prepare a spied StringWriter for the normal‑flow tests.
        realStringWriter = new StringWriter();
        spyStringWriter = spy(realStringWriter);
        fastWriter = new FastWriter(spyStringWriter, CUSTOM_BUFFER_SIZE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        if (fastWriter != null) {
            fastWriter.close();
        }
    }

    /**
     * Verify that {@code flush()} writes any buffered characters to the underlying writer
     * and then calls {@code flush()} on that writer.
     */
    @Test
    void flushWritesBufferedDataAndFlushesUnderlyingWriter() throws IOException {
        // Write a few characters – fewer than the buffer size so nothing is flushed automatically.
        fastWriter.write('h');
        fastWriter.write('i');
        // At this point nothing should have been written to the underlying writer.
        assertEquals("", realStringWriter.toString(), "Buffer should not have been flushed before explicit flush() call.");
        // Invoke flush – this should cause the buffer to be written out and the underlying writer flushed.
        fastWriter.flush();
        // Verify that the underlying writer received the data.
        assertEquals("hi", realStringWriter.toString(), "Buffered characters must be written to the underlying writer after flush().");
        // Verify that the underlying writer's flush() method was called exactly once.
        verify(spyStringWriter, times(1)).flush();
    }

    /**
     * Verify that an {@link IOException} thrown by the underlying writer's {@code flush()}
     * method is propagated by {@code FastWriter.flush()}.
     */
    @Test
    void flushPropagatesIOExceptionFromUnderlyingWriter() throws IOException {
        // Stub the underlying writer so that write operations succeed but flush throws.
        doNothing().when(failingWriter).write(any(char[].class), anyInt(), anyInt());
        doThrow(new IOException("underlying flush failure")).when(failingWriter).flush();
        // Use a small buffer to avoid automatic flushing.
        FastWriter writerWithFailure = new FastWriter(failingWriter, CUSTOM_BUFFER_SIZE);
        // buffer something
        writerWithFailure.write('x');
        IOException ex = assertThrows(IOException.class, writerWithFailure::flush, "FastWriter.flush() must propagate IOException from the underlying writer.");
        assertEquals("underlying flush failure", ex.getMessage());
    }

    /**
     * Verify that calling {@code flush()} on a freshly created {@code FastWriter}
     * (with an empty buffer) still forwards the call to the underlying writer.
     */
    @Test
    void flushOnEmptyBufferStillCallsUnderlyingFlush() throws IOException {
        // No writes performed – buffer is empty.
        fastWriter.flush();
        // Underlying writer's flush should still be invoked.
        verify(spyStringWriter, times(1)).flush();
        assertEquals("", realStringWriter.toString(), "No data should have been written when flushing an empty buffer.");
    }

    /**
     * Group: Normal operation – both flushBuffer and underlying Writer flush succeed.
     * The test verifies that no exception is thrown and that the underlying writer's
     * {@code flush()} is invoked exactly once.  Because {@code flushBuffer()} is private,
     * we infer its execution by checking that a write to the underlying writer occurs.
     */
    @Test
    void normalOperationBothFlushesSucceed() throws IOException {
        // Buffer some data (less than the buffer size to avoid auto‑flush)
        fastWriter.write('a');
        fastWriter.write('b');
        // No exception expected
        assertDoesNotThrow(() -> fastWriter.flush(), "flush() should complete normally when both internal and external flush succeed.");
        // Verify underlying writer received data (means flushBuffer ran)
        assertEquals("ab", realStringWriter.toString(), "flushBuffer() should have written buffered characters.");
        // Verify underlying writer's flush() was called once
        verify(spyStringWriter, times(1)).flush();
    }

    /**
     * Group: Exception propagation – flushBuffer throws {@link IOException}.
     * We simulate this by making the underlying writer's {@code write} throw an
     * {@code IOException}.  When {@code flush()} calls {@code flushBuffer()},
     * the exception bubbles up and {@code out.flush()} must **not** be invoked.
     */
    @Test
    void flushBufferThrowsIOExceptionIsPropagated() throws IOException {
        // Configure the mock writer to throw on write (which flushBuffer uses)
        doThrow(new IOException("flushBuffer failure")).when(failingWriter).write(any(char[].class), anyInt(), anyInt());
        // Use a small buffer to guarantee data stays in the buffer
        FastWriter writer = new FastWriter(failingWriter, CUSTOM_BUFFER_SIZE);
        // buffer a character
        writer.write('z');
        IOException thrown = assertThrows(IOException.class, writer::flush, "IOException from flushBuffer should be propagated.");
        assertEquals("flushBuffer failure", thrown.getMessage());
        // Verify that out.flush() was never called
        verify(failingWriter, never()).flush();
    }

    /**
     * Group: Exception propagation – underlying Writer.flush() throws {@link IOException}.
     * The underlying writer's {@code flush()} is stubbed to throw after a successful
     * {@code flushBuffer()}.  The test asserts that the exception propagates and that
     * {@code flushBuffer()} was indeed executed beforehand.
     */
    @Test
    void underlyingWriterFlushThrowsIOExceptionIsPropagated() throws IOException {
        // Normal write behaviour – do nothing
        doNothing().when(failingWriter).write(any(char[].class), anyInt(), anyInt());
        // Flush throws
        doThrow(new IOException("underlying flush failure")).when(failingWriter).flush();
        FastWriter writer = new FastWriter(failingWriter, CUSTOM_BUFFER_SIZE);
        // buffer something
        writer.write('y');
        IOException ex = assertThrows(IOException.class, writer::flush, "IOException from underlying writer.flush() must be propagated.");
        assertEquals("underlying flush failure", ex.getMessage());
        // Since we wrote something, flushBuffer must have attempted to write.
        verify(failingWriter, times(1)).write(any(char[].class), anyInt(), anyInt());
    }

    /**
     * Group: Null underlying writer – {@code out} is {@code null}.
     * FastWriter's constructor accepts a {@code Writer} argument; we pass {@code null}
     * and expect a {@link NullPointerException} when {@code flush()} tries to delegate.
     */
    @Test
    void flushWithNullUnderlyingWriterThrowsNPE() {
        FastWriter writerWithNull = new FastWriter(null, CUSTOM_BUFFER_SIZE);
        NullPointerException npe = assertThrows(NullPointerException.class, writerWithNull::flush, "Calling flush() when the wrapped writer is null should throw NPE.");
        // Message is not specified by the implementation; we only assert the type.
        assertNotNull(npe);
    }

    /**
     * Group: Exception propagation – flushBuffer throws subclass of {@link IOException}
     * (specifically {@link EOFException}).  The underlying writer's {@code write}
     * is stubbed to throw an {@code EOFException}.
     */
    @Test
    void flushBufferThrowsEOFExceptionIsPropagated() throws IOException {
        EOFException eof = new EOFException("end of stream");
        doThrow(eof).when(failingWriter).write(any(char[].class), anyInt(), anyInt());
        FastWriter writer = new FastWriter(failingWriter, CUSTOM_BUFFER_SIZE);
        // buffer a character
        writer.write('e');
        EOFException thrown = assertThrows(EOFException.class, writer::flush, "EOFException from flushBuffer should be propagated unchanged.");
        assertSame(eof, thrown);
        // Underlying writer's flush must not be called.
        verify(failingWriter, never()).flush();
    }

    /**
     * Group: Unchecked exception propagation – underlying Writer.flush() throws {@link IllegalStateException}.
     */
    @Test
    void underlyingWriterFlushThrowsUncheckedExceptionIsPropagated() throws IOException {
        doNothing().when(failingWriter).write(any(char[].class), anyInt(), anyInt());
        doThrow(new IllegalStateException("illegal state")).when(failingWriter).flush();
        FastWriter writer = new FastWriter(failingWriter, CUSTOM_BUFFER_SIZE);
        // buffer something
        writer.write('u');
        IllegalStateException ex = assertThrows(IllegalStateException.class, writer::flush, "Unchecked exception from underlying writer.flush() must propagate.");
        assertEquals("illegal state", ex.getMessage());
        // Verify flushBuffer was called (write was invoked)
        verify(failingWriter, times(1)).write(any(char[].class), anyInt(), anyInt());
    }

    /**
     * Group: Repeated flush calls – verify both {@code flushBuffer()} and {@code out.flush()}
     * are invoked on each call.
     */
    @Test
    void repeatedFlushCallsInvokeBothMethodsEachTime() throws IOException {
        // Spy on the underlying writer to count flush() invocations.
        FastWriter writer = new FastWriter(spyStringWriter, CUSTOM_BUFFER_SIZE);
        // First flush (empty buffer)
        writer.flush();
        // Second flush (still empty)
        writer.flush();
        // Verify underlying writer's flush called twice.
        verify(spyStringWriter, times(2)).flush();
        // Since buffer is empty, we cannot directly verify private flushBuffer calls,
        // but the absence of exceptions and the correct flush count demonstrates
        // that flush() executes its full body each time.
    }

    /**
     * Group: Flush after close – underlying Writer throws {@link IOException} on flush.
     */
    @Test
    void flushAfterClosePropagatesIOException() throws IOException {
        // Mock a writer that throws on flush after being closed.
        Writer mockWriter = mock(Writer.class);
        // close() does nothing special; we only care about flush().
        doThrow(new IOException("Stream closed")).when(mockWriter).flush();
        FastWriter writer = new FastWriter(mockWriter, CUSTOM_BUFFER_SIZE);
        // close first
        writer.close();
        IOException ex = assertThrows(IOException.class, writer::flush, "IOException from flushing a closed writer must be propagated.");
        assertEquals("Stream closed", ex.getMessage());
        // Even after close, flushBuffer would be called first; we verify write was never attempted.
        verify(mockWriter, never()).write(any(char[].class), anyInt(), anyInt());
    }

    /**
     * Group: Underlying Writer is a NullWriter (no‑op) – ensure normal completion.
     */
    @Test
    void nullWriterNoOpFlushCompletesNormally() throws IOException {
        Writer nullWriter = new Writer() {

            @Override
            public void write(char[] cbuf, int off, int len) {
                /* no‑op */
            }

            @Override
            public void flush() {
                /* no‑op */
            }

            @Override
            public void close() {
                /* no‑op */
            }
        };
        FastWriter writer = new FastWriter(nullWriter, CUSTOM_BUFFER_SIZE);
        // No data written; just call flush()
        assertDoesNotThrow(writer::flush, "flush() should complete without exception for a no‑op writer.");
    }

    /**
     * Group: Unchecked exception propagation – flushBuffer throws {@link IllegalStateException}.
     * We simulate this by making the underlying writer's {@code write} throw the unchecked exception.
     */
    @Test
    void flushBufferThrowsUncheckedExceptionIsPropagated() throws IOException {
        IllegalStateException illegal = new IllegalStateException("buffer failure");
        doThrow(illegal).when(failingWriter).write(any(char[].class), anyInt(), anyInt());
        FastWriter writer = new FastWriter(failingWriter, CUSTOM_BUFFER_SIZE);
        // buffer a character
        writer.write('k');
        IllegalStateException ex = assertThrows(IllegalStateException.class, writer::flush, "Unchecked exception from flushBuffer must propagate.");
        assertSame(illegal, ex);
        // Verify underlying writer's flush() never runs.
        verify(failingWriter, never()).flush();
    }

    /**
     * Group: Error propagation – underlying Writer.flush() throws {@link AssertionError}.
     */
    @Test
    void underlyingWriterFlushThrowsErrorIsPropagated() throws IOException {
        doNothing().when(failingWriter).write(any(char[].class), anyInt(), anyInt());
        doThrow(new AssertionError("assertion failure")).when(failingWriter).flush();
        FastWriter writer = new FastWriter(failingWriter, CUSTOM_BUFFER_SIZE);
        // buffer something
        writer.write('e');
        AssertionError err = assertThrows(AssertionError.class, writer::flush, "Error thrown by underlying writer.flush() must propagate.");
        assertEquals("assertion failure", err.getMessage());
        // flushBuffer should have been called (write invoked)
        verify(failingWriter, times(1)).write(any(char[].class), anyInt(), anyInt());
    }

    /**
     * Group: Unchecked exception propagation – underlying Writer.flush() throws {@link NullPointerException}.
     */
    @Test
    void underlyingWriterFlushThrowsUncheckedNPEIsPropagated() throws IOException {
        doNothing().when(failingWriter).write(any(char[].class), anyInt(), anyInt());
        doThrow(new NullPointerException("null pointer")).when(failingWriter).flush();
        FastWriter writer = new FastWriter(failingWriter, CUSTOM_BUFFER_SIZE);
        // buffer something
        writer.write('n');
        NullPointerException npe = assertThrows(NullPointerException.class, writer::flush, "Unchecked NPE from underlying writer.flush() must propagate.");
        assertEquals("null pointer", npe.getMessage());
        // Verify flushBuffer (write) was called before the NPE.
        verify(failingWriter, times(1)).write(any(char[].class), anyInt(), anyInt());
    }
}
