package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Writer;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FastWriter_close_Test {

    @Mock
    private Writer // mock underlying Writer
    mockWriter;

    // instance under test
    private FastWriter fastWriter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh FastWriter for each test using the mocked Writer
        fastWriter = new FastWriter(mockWriter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the FastWriter is closed after each test to avoid side‑effects
        try {
            fastWriter.close();
        } catch (IOException ignored) {
            // ignored – tests verify behavior of close itself
        }
    }

    /**
     * Helper to read a private field via reflection.
     */
    private static Object getPrivateField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Helper to assert that the internal fields {@code out} and {@code cb} are null.
     */
    private static void assertInternalFieldsAreNull(FastWriter writer) throws Exception {
        assertNull(getPrivateField(writer, "out"), "Field 'out' should be null after close()");
        assertNull(getPrivateField(writer, "cb"), "Field 'cb' should be null after close()");
    }

    /**
     * Helper to assert that the internal fields {@code out} and {@code cb} are non‑null.
     */
    private static void assertInternalFieldsAreNotNull(FastWriter writer) throws Exception {
        assertNotNull(getPrivateField(writer, "out"), "Field 'out' should remain non‑null after close()");
        assertNotNull(getPrivateField(writer, "cb"), "Field 'cb' should remain non‑null after close()");
    }

    @Test
    void close_WhenUnderlyingWriterIsNotNull_ClosesWriterAndClearsResources() throws IOException {
        // Act
        fastWriter.close();
        // Assert – the underlying writer's close() must be invoked exactly once
        verify(mockWriter, times(1)).close();
        // Calling close again must not attempt to close the underlying writer a second time
        fastWriter.close();
        // still only one invocation
        verify(mockWriter, times(1)).close();
    }

    @Test
    void close_WhenUnderlyingWriterIsNull_DoesNothingAndDoesNotThrow() throws IOException {
        // Arrange – create a FastWriter with a null Writer (constructor allows it)
        FastWriter writerWithNull = new FastWriter((Writer) null);
        // Act & Assert – should return silently without throwing any exception
        Assertions.assertDoesNotThrow(writerWithNull::close);
    }

    @Test
    @DisplayName("close() – normal operation with buffered data")
    void close_WithBufferedData_FlushesAndClearsResources() throws Exception {
        // Fill the internal buffer by writing some characters
        fastWriter.write("abcd".toCharArray(), 0, 4);
        // Act
        fastWriter.close();
        // Verify that the underlying writer was flushed (via write) and then closed
        verify(mockWriter, atLeastOnce()).write(any(char[].class), anyInt(), anyInt());
        verify(mockWriter, times(1)).close();
        // Internal fields must be cleared
        assertInternalFieldsAreNull(fastWriter);
    }

    @Test
    @DisplayName("close() – normal operation with empty buffer")
    void close_WithEmptyBuffer_FlushesAndClearsResources() throws Exception {
        // No write performed – buffer is empty
        // Act
        fastWriter.close();
        // Even with an empty buffer, flushBuffer() is called (no‑op) and writer is closed
        verify(mockWriter, never()).write(any(char[].class), anyInt(), anyInt());
        verify(mockWriter, times(1)).close();
        // Internal fields must be cleared
        assertInternalFieldsAreNull(fastWriter);
    }

    @Test
    @DisplayName("close() – flushBuffer throws IOException, out.close succeeds")
    void close_FlushBufferThrowsIOException_PropagatesAndClearsResources() throws Exception {
        // Arrange: write data so that flushBuffer will invoke out.write(...)
        fastWriter.write("xyz".toCharArray(), 0, 3);
        // Simulate IOException from the underlying writer during the flush
        doThrow(new IOException("flush failure")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());
        // Act & Assert
        IOException ex = assertThrows(IOException.class, fastWriter::close);
        assertEquals("flush failure", ex.getMessage());
        // out.close() should still be called despite the flush failure
        verify(mockWriter, times(1)).close();
        // After close() the internal fields must be null (finally block runs)
        assertInternalFieldsAreNull(fastWriter);
    }

    @Test
    @DisplayName("close() – flushBuffer throws IOException and out.close also throws IOException")
    void close_FlushAndCloseBothThrow_PropagatesCloseExceptionAndLeavesFieldsIntact() throws Exception {
        // Arrange: write data to trigger flushBuffer
        fastWriter.write("12345".toCharArray(), 0, 5);
        // flushBuffer (i.e., write) throws
        doThrow(new IOException("flush failure")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());
        // out.close also throws
        doThrow(new IOException("close failure")).when(mockWriter).close();
        // Act & Assert – the exception from out.close should be the one observed
        IOException ex = assertThrows(IOException.class, fastWriter::close);
        assertEquals("close failure", ex.getMessage());
        // Verify both write and close were attempted
        verify(mockWriter, atLeastOnce()).write(any(char[].class), anyInt(), anyInt());
        verify(mockWriter, times(1)).close();
        // Because out.close threw, the statements that nullify fields are not executed
        assertInternalFieldsAreNotNull(fastWriter);
    }

    @Test
    @DisplayName("close() – out.close throws IOException after successful flushBuffer")
    void close_OutCloseThrowsAfterSuccessfulFlush_PropagatesExceptionAndLeavesFieldsIntact() throws Exception {
        // Arrange: no data written, so flushBuffer is a no‑op but still called
        // Make out.close throw
        doThrow(new IOException("close failure")).when(mockWriter).close();
        // Act & Assert
        IOException ex = assertThrows(IOException.class, fastWriter::close);
        assertEquals("close failure", ex.getMessage());
        // Verify that flushBuffer (no‑op) was called – we cannot directly verify private method,
        // but we can assert that write was never called and close was attempted.
        verify(mockWriter, never()).write(any(char[].class), anyInt(), anyInt());
        verify(mockWriter, times(1)).close();
        // Since out.close threw, the fields are not cleared
        assertInternalFieldsAreNotNull(fastWriter);
    }
}
