package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Writer;
import static org.mockito.Mockito.*;
import java.io.StringWriter;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FastWriter_close_Test {

    // ----------------------------------------------------------------------
    @Mock
    private Writer // the underlying Writer that FastWriter delegates to
    mockOut;

    // instance under test
    private FastWriter fastWriter;

    @BeforeEach
    void setupBeforeEach() {
        // FastWriter has two constructors; we use the one that lets us specify a buffer size
        // to keep the test deterministic.
        fastWriter = new FastWriter(mockOut, 16);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the writer is closed after each test to avoid side‑effects.
        // If the writer is already closed, close() is a no‑op.
        try {
            fastWriter.close();
        } catch (IOException ignored) {
            // Ignored – the purpose of the test is not to verify exception handling here.
        }
    }

    /**
     * Retrieves a private field value via reflection.
     */
    private static Object getPrivateField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Sets a private field value via reflection.
     */
    private static void setPrivateField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // ----------------------------------------------------------------------
    @Test
    void close_ShouldFlushBufferAndCloseUnderlyingWriter() throws IOException {
        // GIVEN – fastWriter is constructed with a mock Writer (set up in @BeforeEach)
        // WHEN
        fastWriter.close();
        // THEN
        // The underlying Writer must be closed exactly once.
        verify(mockOut, times(1)).close();
        // The internal buffer (cb) is nulled; we cannot access it directly,
        // but a second call to close() must be a no‑op (see next test).
    }

    @Test
    void close_WhenCalledTwice_ShouldInvokeUnderlyingCloseOnlyOnce() throws IOException {
        // GIVEN – fastWriter already closed once
        fastWriter.close();
        // Reset interaction history so we can verify calls made by the second close()
        reset(mockOut);
        // WHEN – invoke close a second time
        fastWriter.close();
        // THEN – the underlying Writer must not be touched again
        verifyNoInteractions(mockOut);
    }

    @Test
    void close_WhenUnderlyingWriterThrowsIOException_ShouldStillAttemptClose() throws IOException {
        // GIVEN – configure the mock to throw from flush (which is indirectly called via flushBuffer)
        // FastWriter's close() calls flushBuffer() before closing the writer.
        // We simulate an IOException by making the mock throw when flush() is invoked.
        doThrow(new IOException("simulated flush failure")).when(mockOut).flush();
        // WHEN – close() is invoked; the exception from flushBuffer should be propagated,
        // but the finally block must still call out.close().
        IOException thrown = Assertions.assertThrows(IOException.class, fastWriter::close);
        // THEN – verify the exception message and that close() was still called.
        Assertions.assertEquals("simulated flush failure", thrown.getMessage());
        verify(mockOut, times(1)).close();
    }

    // ----------------------------------------------------------------------
    @Test
    void close_WhenOutIsNull_NoExceptionAndFieldsRemainNull() throws Exception {
        // GIVEN – a FastWriter created with the no‑arg constructor (out == null)
        // assumes such a constructor exists
        FastWriter writer = new FastWriter();
        // Ensure internal fields are null before the call
        assertNull(getPrivateField(writer, "out"));
        assertNull(getPrivateField(writer, "cb"));
        // WHEN – close() is invoked
        writer.close();
        // THEN – no exception and fields stay null
        assertNull(getPrivateField(writer, "out"));
        assertNull(getPrivateField(writer, "cb"));
    }

    @Test
    void close_WithRealWriter_FlushAndCloseAndFieldsNull() throws Exception {
        // GIVEN – a real StringWriter wrapped in a Mockito spy so we can verify close()
        StringWriter realWriter = new StringWriter();
        Writer spyWriter = spy(realWriter);
        FastWriter writer = new FastWriter(spyWriter, 16);
        // Write something to fill the internal buffer (implementation detail)
        writer.write("hello".toCharArray(), 0, 5);
        // WHEN – close() is called
        writer.close();
        // THEN – underlying writer's close() invoked and internal fields cleared
        verify(spyWriter, times(1)).close();
        assertNull(getPrivateField(writer, "out"));
        assertNull(getPrivateField(writer, "cb"));
    }

    @Test
    void close_WhenFlushBufferThrowsIOException_OutCloseStillInvoked() throws Exception {
        // GIVEN – mockOut throws IOException when write() is called (flushBuffer uses write)
        doThrow(new IOException("flushBuffer failure")).when(mockOut).write(any(char[].class), anyInt(), anyInt());
        // WHEN – close() is invoked
        IOException thrown = assertThrows(IOException.class, fastWriter::close);
        // THEN – the exception originates from flushBuffer and close() still called
        assertEquals("flushBuffer failure", thrown.getMessage());
        verify(mockOut, times(1)).close();
        // fields should be cleared despite the exception (finally block runs)
        assertNull(getPrivateField(fastWriter, "out"));
        assertNull(getPrivateField(fastWriter, "cb"));
    }

    @Test
    void close_WhenUnderlyingCloseThrowsIOException_FlushSucceedsFieldsStayNonNull() throws Exception {
        // GIVEN – write succeeds, but close() throws IOException
        doNothing().when(mockOut).write(any(char[].class), anyInt(), anyInt());
        doThrow(new IOException("close failure")).when(mockOut).close();
        // WHEN – close() is invoked
        IOException thrown = assertThrows(IOException.class, fastWriter::close);
        // THEN – exception from close() propagates and internal fields are NOT cleared
        assertEquals("close failure", thrown.getMessage());
        assertNotNull(getPrivateField(fastWriter, "out"));
        assertNotNull(getPrivateField(fastWriter, "cb"));
    }

    @Test
    void close_AfterAlreadyClosed_OutIsNull_NoException() throws Exception {
        // GIVEN – fastWriter already closed once (handled by @AfterEach as well)
        // first close – fields become null
        fastWriter.close();
        // WHEN – close() called again
        assertDoesNotThrow(() -> fastWriter.close());
        // THEN – still no interaction with the underlying writer
        verifyNoMoreInteractions(mockOut);
        assertNull(getPrivateField(fastWriter, "out"));
        assertNull(getPrivateField(fastWriter, "cb"));
    }

    @Test
    void close_ClearsInternalCharBuffer() throws Exception {
        // GIVEN – ensure the buffer is non‑null before closing
        assertNotNull(getPrivateField(fastWriter, "cb"));
        // WHEN – close() is called
        fastWriter.close();
        // THEN – internal char buffer reference cleared
        assertNull(getPrivateField(fastWriter, "cb"));
    }

    @Test
    void close_WhenFlushBufferThrowsRuntimeException_OutCloseStillInvoked() throws Exception {
        // GIVEN – mockOut throws RuntimeException during write()
        doThrow(new RuntimeException("runtime from write")).when(mockOut).write(any(char[].class), anyInt(), anyInt());
        // WHEN – close() is invoked
        RuntimeException thrown = assertThrows(RuntimeException.class, fastWriter::close);
        // THEN – exception propagates, close() still called, fields cleared
        assertEquals("runtime from write", thrown.getMessage());
        verify(mockOut, times(1)).close();
        assertNull(getPrivateField(fastWriter, "out"));
        assertNull(getPrivateField(fastWriter, "cb"));
    }

    @Test
    void close_FlushBufferAndCloseThrowIO_BothExceptionsHandled() throws Exception {
        // GIVEN – write throws IOException (flushBuffer) and close also throws IOException
        doThrow(new IOException("flush failure")).when(mockOut).write(any(char[].class), anyInt(), anyInt());
        doThrow(new IOException("close failure")).when(mockOut).close();
        // WHEN – close() is invoked
        IOException thrown = assertThrows(IOException.class, fastWriter::close);
        // THEN – the exception from close() is observed; the flush exception is suppressed
        assertEquals("close failure", thrown.getMessage());
        assertEquals(1, thrown.getSuppressed().length);
        assertEquals("flush failure", thrown.getSuppressed()[0].getMessage());
        // Because close() threw, the finally block did not reach the null‑assignments:
        assertNotNull(getPrivateField(fastWriter, "out"));
        assertNotNull(getPrivateField(fastWriter, "cb"));
    }

    @Test
    void close_UnderlyingCloseThrowsRuntimeException_FlushSucceedsFieldsUnchanged() throws Exception {
        // GIVEN – write succeeds, close() throws RuntimeException
        doNothing().when(mockOut).write(any(char[].class), anyInt(), anyInt());
        doThrow(new RuntimeException("runtime close")).when(mockOut).close();
        // WHEN – close() is invoked
        RuntimeException thrown = assertThrows(RuntimeException.class, fastWriter::close);
        // THEN – exception propagates and fields remain unchanged (null‑assignments not reached)
        assertEquals("runtime close", thrown.getMessage());
        assertNotNull(getPrivateField(fastWriter, "out"));
        assertNotNull(getPrivateField(fastWriter, "cb"));
    }
}
