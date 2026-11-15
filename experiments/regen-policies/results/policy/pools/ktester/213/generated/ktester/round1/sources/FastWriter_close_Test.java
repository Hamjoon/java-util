package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Writer;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.InOrder;
import org.mockito.Mockito;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class FastWriter_close_Test {

    private static final int BUFFER_SIZE = 8;

    private MockWriter mockWriter;

    private FastWriter fastWriter;

    /* -----------------------------------------------------------
     *  Helper class – a simple Writer that records actions
     * ----------------------------------------------------------- */
    private static class MockWriter extends Writer {

        private final StringBuilder sb = new StringBuilder();

        private boolean closed = false;

        private int writeCount = 0;

        @Override
        public void write(char[] cbuf, int off, int len) {
            writeCount++;
            sb.append(cbuf, off, len);
        }

        @Override
        public void flush() {
            // No side‑effects needed for these tests
        }

        @Override
        public void close() {
            closed = true;
        }

        /**
         * Returns true if close() has been invoked.
         */
        boolean isClosed() {
            return closed;
        }

        /**
         * Resets the closed flag – used to verify that a second close does not touch the writer.
         */
        void reset() {
            closed = false;
        }

        /**
         * Returns the number of write(char[],int,int) calls received.
         */
        int getWriteCount() {
            return writeCount;
        }

        /**
         * Returns the data written so far.
         */
        String getOutput() {
            return sb.toString();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        mockWriter = new MockWriter();
        fastWriter = new FastWriter(mockWriter, BUFFER_SIZE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the FastWriter is closed after each test – the method under test
        // must be safe to call multiple times, so we simply invoke it.
        if (fastWriter != null) {
            fastWriter.close();
        }
    }

    /**
     * Returns the value of a private field via reflection.
     */
    private static Object getPrivateField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Sets a private field via reflection.
     */
    private static void setPrivateField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void closeWhenUnderlyingWriterIsPresent_closesWriterAndClearsState() throws IOException {
        // Write something so that the internal buffer is populated
        fastWriter.write('a');
        fastWriter.write('b');
        // Act
        fastWriter.close();
        // Verify that the underlying writer was closed
        assertTrue(mockWriter.isClosed(), "Underlying writer should be marked as closed");
        // The FastWriter should now be in a state where a second close is a no‑op
        assertDoesNotThrow(() -> fastWriter.close(), "Second close should not throw");
    }

    @Test
    void closeWhenUnderlyingWriterIsAlreadyNull_isNoOp() throws IOException {
        // First close – puts FastWriter into the "out == null" state
        fastWriter.close();
        // Reset mock state to ensure we can detect any additional close calls
        mockWriter.reset();
        // Act – second close should simply return without touching the writer
        fastWriter.close();
        // Verify that no additional close was attempted on the underlying writer
        assertFalse(mockWriter.isClosed(), "Underlying writer should not be closed again");
    }

    @Test
    void closePropagatesIOExceptionFromUnderlyingWriter() {
        // Arrange a writer that throws on close
        Writer explodingWriter = new Writer() {

            @Override
            public void write(char[] cbuf, int off, int len) {
                /* no‑op */
            }

            @Override
            public void flush() {
                /* no‑op */
            }

            @Override
            public void close() throws IOException {
                throw new IOException("boom");
            }
        };
        FastWriter fw = new FastWriter(explodingWriter, BUFFER_SIZE);
        // Act & Assert
        IOException ex = assertThrows(IOException.class, fw::close, "FastWriter.close() should propagate IOException from the underlying writer");
        assertEquals("boom", ex.getMessage());
    }

    /**
     * Group: close when underlying writer is null
     */
    @Test
    void closeWhenOutIsNull_isNoOp() throws Exception {
        // FastWriter instantiated with a real writer, then we force out == null.
        setPrivateField(fastWriter, "out", null);
        setPrivateField(fastWriter, "cb", null);
        // Act – should return immediately without exception
        assertDoesNotThrow(() -> fastWriter.close());
        // Verify fields are still null
        assertNull(getPrivateField(fastWriter, "out"));
        assertNull(getPrivateField(fastWriter, "cb"));
    }

    /**
     * Group: close with a normal writer where flushBuffer succeeds
     */
    @Test
    void closeNormalWriter_successfulFlushAndClose() throws Exception {
        // Populate buffer so flushBuffer will be exercised (though it succeeds)
        fastWriter.write('x');
        fastWriter.write('y');
        fastWriter.close();
        // Underlying writer must be closed exactly once
        assertTrue(mockWriter.isClosed(), "Underlying writer should be closed");
        // Internal fields must be cleared
        assertNull(getPrivateField(fastWriter, "out"));
        assertNull(getPrivateField(fastWriter, "cb"));
    }

    /**
     * Group: close when flushBuffer throws IOException
     */
    @Test
    void closeFlushBufferThrowsIOException_underlyingWriterClosedAndFieldsCleared() throws Exception {
        // Fill the buffer so flushBuffer will attempt to write
        fastWriter.write('1');
        fastWriter.write('2');
        // Configure mockWriter.write to throw IOException
        doThrow(new IOException("flush error")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());
        IOException ex = assertThrows(IOException.class, () -> fastWriter.close(), "IOException from flushBuffer should be propagated");
        assertEquals("flush error", ex.getMessage());
        // Even though flushBuffer failed, the finally block must close the writer
        assertTrue(mockWriter.isClosed(), "Writer must be closed even when flushBuffer fails");
        // Fields must be cleared because close()'s finally block runs after out.close()
        assertNull(getPrivateField(fastWriter, "out"));
        assertNull(getPrivateField(fastWriter, "cb"));
    }

    /**
     * Group: close when underlying writer's close() throws IOException
     */
    @Test
    void closeWriterCloseThrowsIOException_fieldsRemainNonNull() throws Exception {
        // Populate buffer so flushBuffer runs (no exception)
        fastWriter.write('a');
        // Make out.close() throw IOException
        doThrow(new IOException("close error")).when(mockWriter).close();
        IOException ex = assertThrows(IOException.class, () -> fastWriter.close(), "IOException from out.close() should be propagated");
        assertEquals("close error", ex.getMessage());
        // Because the exception occurs in finally, assignments after it are not executed
        assertNotNull(getPrivateField(fastWriter, "out"), "out should remain non‑null");
        assertNotNull(getPrivateField(fastWriter, "cb"), "cb should remain non‑null");
    }

    /**
     * Group: close when flushBuffer throws unchecked RuntimeException
     */
    @Test
    void closeFlushBufferThrowsRuntimeException_underlyingWriterClosedAndFieldsCleared() throws Exception {
        fastWriter.write('z');
        // Make write throw RuntimeException
        doThrow(new IllegalStateException("runtime from write")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> fastWriter.close(), "RuntimeException from flushBuffer should propagate");
        assertEquals("runtime from write", ex.getMessage());
        // Writer must still be closed
        assertTrue(mockWriter.isClosed(), "Writer must be closed even when flushBuffer throws RuntimeException");
        // Fields cleared
        assertNull(getPrivateField(fastWriter, "out"));
        assertNull(getPrivateField(fastWriter, "cb"));
    }

    /**
     * Group: close when both flushBuffer and out.close throw IOException
     */
    @Test
    void closeFlushAndCloseThrowIOException_outCloseExceptionOverrides() throws Exception {
        fastWriter.write('p');
        // flushBuffer (via write) throws IOException
        doThrow(new IOException("flush IOException")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());
        // out.close also throws IOException
        doThrow(new IOException("close IOException")).when(mockWriter).close();
        IOException ex = assertThrows(IOException.class, () -> fastWriter.close(), "Exception from out.close() should be observed");
        assertEquals("close IOException", ex.getMessage());
        // Because out.close threw, fields stay non‑null
        assertNotNull(getPrivateField(fastWriter, "out"));
        assertNotNull(getPrivateField(fastWriter, "cb"));
    }

    /**
     * Group: close called twice: first successful then no‑op
     */
    @Test
    void closeCalledTwice_firstSuccessSecondNoOp() throws Exception {
        fastWriter.write('m');
        // first close – should succeed
        fastWriter.close();
        // Verify first close closed writer and cleared fields
        assertTrue(mockWriter.isClosed());
        assertNull(getPrivateField(fastWriter, "out"));
        assertNull(getPrivateField(fastWriter, "cb"));
        // Reset mock to detect any further close calls
        mockWriter.reset();
        // Second close – should be a no‑op
        assertDoesNotThrow(() -> fastWriter.close());
        assertFalse(mockWriter.isClosed(), "Second close must not invoke underlying writer");
    }

    /**
     * Group: close when out is a Writer that throws IOException on close but flushBuffer also throws RuntimeException
     */
    @Test
    void closeFlushRuntimeAndOutCloseIOException_runtimeExceptionPropagates() throws Exception {
        fastWriter.write('q');
        // flushBuffer (write) throws RuntimeException
        doThrow(new IllegalArgumentException("flush runtime")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());
        // out.close throws IOException
        doThrow(new IOException("close IOException")).when(mockWriter).close();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> fastWriter.close(), "RuntimeException from flushBuffer should be observed");
        assertEquals("flush runtime", ex.getMessage());
        // Because out.close threw, fields stay non‑null
        assertNotNull(getPrivateField(fastWriter, "out"));
        assertNotNull(getPrivateField(fastWriter, "cb"));
    }

    /**
     * Group: close when underlying writer's close() throws RuntimeException
     */
    @Test
    void closeWriterCloseThrowsRuntimeException_fieldsRemainNonNull() throws Exception {
        fastWriter.write('v');
        doThrow(new IllegalStateException("close runtime")).when(mockWriter).close();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> fastWriter.close(), "RuntimeException from out.close() should propagate");
        assertEquals("close runtime", ex.getMessage());
        // Fields remain because exception aborts after out.close()
        assertNotNull(getPrivateField(fastWriter, "out"));
        assertNotNull(getPrivateField(fastWriter, "cb"));
    }

    /**
     * Group: close when underlying writer's close() throws RuntimeException and flushBuffer throws IOException
     */
    @Test
    void closeFlushIOExceptionAndOutCloseRuntimeException_runtimeExceptionOverrides() throws Exception {
        fastWriter.write('w');
        // flushBuffer (write) throws IOException
        doThrow(new IOException("flush IOException")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());
        // out.close throws RuntimeException
        doThrow(new IllegalStateException("close runtime")).when(mockWriter).close();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> fastWriter.close(), "RuntimeException from out.close() should replace IOException");
        assertEquals("close runtime", ex.getMessage());
        // Fields stay non‑null because out.close threw
        assertNotNull(getPrivateField(fastWriter, "out"));
        assertNotNull(getPrivateField(fastWriter, "cb"));
    }

    /**
     * Group: close when underlying writer's close() throws RuntimeException and flushBuffer throws RuntimeException
     */
    @Test
    void closeBothFlushAndCloseThrowRuntimeException_outCloseExceptionObserved() throws Exception {
        fastWriter.write('x');
        // flushBuffer (write) throws RuntimeException
        doThrow(new IllegalArgumentException("flush runtime")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());
        // out.close throws a different RuntimeException
        doThrow(new IllegalStateException("close runtime")).when(mockWriter).close();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> fastWriter.close(), "RuntimeException from out.close() should replace the one from flushBuffer");
        assertEquals("close runtime", ex.getMessage());
        // Fields remain non‑null
        assertNotNull(getPrivateField(fastWriter, "out"));
        assertNotNull(getPrivateField(fastWriter, "cb"));
    }

    /**
     * Group: close when flushBuffer throws Error and out.close succeeds
     */
    @Test
    void closeFlushBufferThrowsError_outCloseSucceeds_fieldsCleared() throws Exception {
        fastWriter.write('e');
        // Make write throw an AssertionError (simulating Error from flushBuffer)
        doThrow(new AssertionError("flush error")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());
        AssertionError err = assertThrows(AssertionError.class, () -> fastWriter.close(), "Error from flushBuffer should propagate");
        assertEquals("flush error", err.getMessage());
        // out.close must have been called
        assertTrue(mockWriter.isClosed(), "Writer must be closed even when flushBuffer throws Error");
        // Fields cleared after finally block
        assertNull(getPrivateField(fastWriter, "out"));
        assertNull(getPrivateField(fastWriter, "cb"));
    }

    /**
     * Group: close when flushBuffer throws Error and out.close throws IOException
     */
    @Test
    void closeFlushErrorOutCloseIOException_outCloseExceptionObserved() throws Exception {
        fastWriter.write('f');
        // flushBuffer (write) throws Error
        doThrow(new AssertionError("flush error")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());
        // out.close throws IOException
        doThrow(new IOException("close IOException")).when(mockWriter).close();
        IOException ex = assertThrows(IOException.class, () -> fastWriter.close(), "IOException from out.close() should replace the Error");
        assertEquals("close IOException", ex.getMessage());
        // Fields remain non‑null because out.close threw
        assertNotNull(getPrivateField(fastWriter, "out"));
        assertNotNull(getPrivateField(fastWriter, "cb"));
    }

    /**
     * Group: close when flushBuffer throws Error and out.close throws RuntimeException
     */
    @Test
    void closeFlushErrorOutCloseRuntimeException_runtimeExceptionObserved() throws Exception {
        fastWriter.write('g');
        // flushBuffer (write) throws Error
        doThrow(new AssertionError("flush error")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());
        // out.close throws RuntimeException
        doThrow(new IllegalStateException("close runtime")).when(mockWriter).close();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> fastWriter.close(), "RuntimeException from out.close() should replace the Error");
        assertEquals("close runtime", ex.getMessage());
        // Fields stay non‑null
        assertNotNull(getPrivateField(fastWriter, "out"));
        assertNotNull(getPrivateField(fastWriter, "cb"));
    }
}
