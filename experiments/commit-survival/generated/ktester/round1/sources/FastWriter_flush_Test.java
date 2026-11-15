package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Writer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class FastWriter_flush_Test {

    @Mock
    private Writer // dependency of FastWriter
    mockWriter;

    // instance under test
    private FastWriter fastWriter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default buffer size constructor – the exact size is irrelevant for flush()
        fastWriter = new FastWriter(mockWriter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC and avoid accidental reuse
        fastWriter = null;
    }

    @Test
    void flush_ShouldInvokeUnderlyingWriterFlush() throws IOException {
        // GIVEN – fastWriter is already constructed with mockWriter (setupBeforeEach)
        // WHEN
        fastWriter.flush();
        // THEN – the wrapped Writer's flush() must be called exactly once
        verify(mockWriter, times(1)).flush();
        // No other interactions with the mock are expected
        verifyNoMoreInteractions(mockWriter);
    }

    @Test
    void flush_WhenUnderlyingWriterThrowsIOException_ShouldPropagateException() throws IOException {
        // GIVEN
        doThrow(new IOException("boom")).when(mockWriter).flush();
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> fastWriter.flush());
        assertEquals("boom", ex.getMessage());
        // Verify that the flush() on the underlying writer was indeed attempted
        verify(mockWriter, times(1)).flush();
    }

    @Test
    void flush_WithCustomBufferSize_ShouldStillInvokeUnderlyingWriterFlush() throws IOException {
        // GIVEN – construct with an explicit buffer size (the constructor exists in the real class)
        fastWriter = new FastWriter(mockWriter, 256);
        // WHEN
        fastWriter.flush();
        // THEN
        verify(mockWriter, times(1)).flush();
        verifyNoMoreInteractions(mockWriter);
    }

    @Test
    void flush_WhenFlushBufferThrowsIOException_ShouldPropagateAndNotCallOutFlush() throws Exception {
        // GIVEN – create a spy so we can replace the private flushBuffer() method.
        FastWriter spyWriter = spy(new FastWriter(mockWriter));
        // Use reflection to replace the private method with a stub that throws.
        // The stub is created by a lambda that simply throws the exception.
        // This works because Mockito (with mockito‑inline) can intercept calls
        // to private methods when they are made accessible via reflection.
        java.lang.reflect.Method flushBufferMethod = FastWriter.class.getDeclaredMethod("flushBuffer");
        flushBufferMethod.setAccessible(true);
        // Replace the implementation by delegating to a lambda that throws.
        // We achieve this by using a mock answer on the spy.
        doAnswer(invocation -> {
            throw new IOException("buffer error");
        }).when(spyWriter, // Mockito's private‑method stubbing
        "flushBuffer");
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, spyWriter::flush);
        assertEquals("buffer error", ex.getMessage());
        // Verify that the underlying writer's flush() was never invoked.
        verifyNoInteractions(mockWriter);
    }

    @Test
    void flush_WhenFlushBufferThrowsUncheckedException_ShouldPropagateUnchanged() throws Exception {
        // GIVEN – spy and make flushBuffer throw an unchecked exception
        FastWriter spyWriter = spy(new FastWriter(mockWriter));
        doAnswer(invocation -> {
            throw new IllegalStateException("illegal state");
        }).when(spyWriter, // private method stubbing
        "flushBuffer");
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, spyWriter::flush);
        assertEquals("illegal state", ex.getMessage());
        // No interaction with the underlying writer should have happened.
        verifyNoInteractions(mockWriter);
    }

    @Test
    void flush_WhenUnderlyingWriterThrowsRuntimeException_ShouldPropagateRuntimeException() throws IOException {
        // GIVEN – underlying writer throws a RuntimeException
        doThrow(new RuntimeException("runtime boom")).when(mockWriter).flush();
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> fastWriter.flush());
        assertEquals("runtime boom", ex.getMessage());
        // Verify that flush() was attempted exactly once.
        verify(mockWriter, times(1)).flush();
    }

    @Test
    void flush_WhenBothFlushBufferAndOutFlushThrow_OnlyFirstExceptionPropagated() throws Exception {
        // GIVEN – spy that makes flushBuffer throw IOException
        FastWriter spyWriter = spy(new FastWriter(mockWriter));
        doAnswer(invocation -> {
            throw new IOException("first");
        }).when(spyWriter, // private method stubbing
        "flushBuffer");
        // Also make the underlying writer throw a different exception (should never be reached)
        doThrow(new IOException("second")).when(mockWriter).flush();
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, spyWriter::flush);
        assertEquals("first", ex.getMessage());
        // Verify that out.flush() was never called because the first exception stopped execution.
        verify(mockWriter, never()).flush();
    }

    @Test
    void flush_WhenUnderlyingWriterIsNull_ShouldThrowNullPointerException() throws Exception {
        // GIVEN – set the private field 'out' to null via reflection
        Field outField = FastWriter.class.getDeclaredField("out");
        outField.setAccessible(true);
        outField.set(fastWriter, null);
        // WHEN / THEN
        NullPointerException ex = assertThrows(NullPointerException.class, fastWriter::flush);
        // message is implementation‑dependent
        assertNotNull(ex.getMessage());
        // No interaction with the mock (it is no longer referenced)
        verifyNoInteractions(mockWriter);
    }
}
