package com.cedarsoftware.util;

import java.io.IOException;
import java.io.Writer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class FastWriter_write_3_Test {

    /**
     * Small buffer size used for most tests – makes it easy to trigger flushes.
     */
    private static final int SMALL_BUFFER_SIZE = 8;

    private MockWriter mockWriter;

    private FastWriter fastWriter;

    @Mock
    private Writer // used for exception‑propagation tests
    mockUnderlying;

    /**
     * A simple mock writer that records written characters.
     */
    private static class MockWriter extends Writer {

        private final StringBuilder sb = new StringBuilder();

        private boolean closed = false;

        private int writeCalls = 0;

        @Override
        public void write(char[] cbuf, int off, int len) {
            writeCalls++;
            sb.append(cbuf, off, len);
        }

        @Override
        public void flush() {
            // no‑op for the mock
        }

        @Override
        public void close() {
            closed = true;
        }

        String getOutput() {
            return sb.toString();
        }

        int getWriteCalls() {
            return writeCalls;
        }

        boolean isClosed() {
            return closed;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        mockWriter = new MockWriter();
        fastWriter = new FastWriter(mockWriter, SMALL_BUFFER_SIZE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        if (fastWriter != null) {
            fastWriter.close();
        }
    }

    @Test
    void write_throwsIndexOutOfBounds_whenOffsetNegative() {
        char[] buf = "abc".toCharArray();
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriter.write(buf, -1, 2));
    }

    @Test
    void write_throwsIndexOutOfBounds_whenOffsetBeyondArray() {
        char[] buf = "abc".toCharArray();
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriter.write(buf, 4, 1));
    }

    @Test
    void write_throwsIndexOutOfBounds_whenLengthNegative() {
        char[] buf = "abc".toCharArray();
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriter.write(buf, 0, -2));
    }

    @Test
    void write_throwsIndexOutOfBounds_whenOffsetPlusLengthExceedsArray() {
        char[] buf = "abc".toCharArray();
        assertThrows(IndexOutOfBoundsException.class, // 1+3 > 3
        () -> fastWriter.write(buf, 1, 3));
    }

    @Test
    void write_throwsIndexOutOfBounds_whenOffsetPlusLengthOverflows() {
        char[] buf = new char[100];
        int off = 10;
        // off + len overflows to negative
        int len = Integer.MAX_VALUE;
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriter.write(buf, off, len));
    }

    @Test
    void write_throwsIOException_whenWriterClosed() throws IOException {
        // close underlying FastWriter (and thus mockWriter)
        fastWriter.close();
        char[] data = "data".toCharArray();
        IOException ex = assertThrows(IOException.class, () -> fastWriter.write(data, 0, data.length));
        assertEquals("FastWriter stream is closed.", ex.getMessage());
    }

    @Test
    void write_zeroLengthDoesNothing() throws IOException {
        char[] data = "hello".toCharArray();
        // should be a no‑op
        fastWriter.write(data, 0, 0);
        // ensure any pending buffer is flushed
        fastWriter.flush();
        assertEquals("", mockWriter.getOutput(), "Zero‑length write must not modify the underlying writer");
    }

    @Test
    void write_directlyWhenLengthExceedsBuffer() throws IOException {
        // Buffer size = 8, we write 10 chars => direct write path
        // 10 chars
        char[] data = "ABCDEFGHIJ".toCharArray();
        fastWriter.write(data, 0, data.length);
        // No data should be in the mock yet because the internal buffer is empty;
        // the direct write writes straight to the underlying writer.
        assertEquals("ABCDEFGHIJ", mockWriter.getOutput(), "When len >= buffer size the data must be written directly");
        assertEquals(1, mockWriter.getWriteCalls(), "Direct write should result in exactly one call to the underlying writer");
    }

    @Test
    void write_flushesBufferWhenNotEnoughSpace_thenCopies() throws IOException {
        // Fill the buffer almost completely (7 of 8 chars)
        fastWriter.write("1234567".toCharArray(), 0, 7);
        // flush to make sure the 7 chars are written
        fastWriter.flush();
        assertEquals("1234567", mockWriter.getOutput());
        // Now buffer is empty again. Write 5 chars to fill part of it.
        // buffer now holds 5 chars
        fastWriter.write("abcde".toCharArray(), 0, 5);
        // Next write tries to copy 6 chars, which does NOT fit (5+6 > 8)
        // 6 chars
        char[] more = "XYZ123".toCharArray();
        // should flush the 5 chars first
        fastWriter.write(more, 0, more.length);
        // After the call, the 5 chars should have been flushed, then the 6 chars
        // are copied into the buffer (but not yet flushed).
        assertEquals("1234567abcde", mockWriter.getOutput(), "Buffer should have been flushed before copying the new array");
        // Finally flush to push the remaining 6 chars.
        fastWriter.flush();
        assertEquals("1234567abcdeXYZ123", mockWriter.getOutput(), "After final flush all data must be present");
    }

    @Test
    void write_copiesIntoBufferWhenSpaceAvailable() throws IOException {
        // Buffer size = 8, write 3 chars – they stay in the internal buffer.
        fastWriter.write("abc".toCharArray(), 0, 3);
        // No output yet because we haven't flushed.
        assertEquals("", mockWriter.getOutput());
        // Write another 4 chars – still fits (3+4 = 7 < 8).
        fastWriter.write("defgh".toCharArray(), 0, 4);
        assertEquals("", mockWriter.getOutput());
        // Flush now; all 7 chars should appear in the underlying writer.
        fastWriter.flush();
        assertEquals("abcdefg", mockWriter.getOutput(), "All characters written to the buffer must appear after flush");
    }

    @Test
    void write_throwsNullPointerException_whenCbufIsNull() {
        assertThrows(NullPointerException.class, () -> fastWriter.write(null, 0, 1));
    }

    @Test
    void write_propagatesIOException_fromUnderlyingWriter_onDirectWrite() throws IOException {
        // Setup a Mockito writer that throws on write(...)
        doThrow(new IOException("underlying failure")).when(mockUnderlying).write(any(char[].class), anyInt(), anyInt());
        // FastWriter with the mocked underlying writer and a small buffer
        FastWriter fw = new FastWriter(mockUnderlying, SMALL_BUFFER_SIZE);
        // length 6 >= buffer size (8) ? actually 6 < 8, so we need length >= buffer size.
        char[] data = "abcdef".toCharArray();
        // Use length 8 to trigger direct write path
        // exactly buffer size
        char[] big = "abcdefgh".toCharArray();
        IOException ex = assertThrows(IOException.class, () -> fw.write(big, 0, big.length));
        assertEquals("underlying failure", ex.getMessage());
        // Verify that write was attempted exactly once (no extra flush before it)
        verify(mockUnderlying, times(1)).write(any(char[].class), eq(0), eq(big.length));
    }

    @Test
    void write_propagatesIOException_fromUnderlyingWriter_duringFlush() throws IOException {
        // The mock will throw when the internal flushBuffer tries to write the buffered chars.
        doThrow(new IOException("flush failure")).when(mockUnderlying).write(any(char[].class), anyInt(), anyInt());
        // tiny buffer to force flush
        FastWriter fw = new FastWriter(mockUnderlying, 4);
        // First write fills part of the buffer (2 chars)
        fw.write("ab".toCharArray(), 0, 2);
        // Second write of length 3 does not fit (2+3 > 4) -> flushBuffer() should be invoked and throw
        IOException ex = assertThrows(IOException.class, () -> fw.write("cde".toCharArray(), 0, 3));
        assertEquals("flush failure", ex.getMessage());
        // Verify that the underlying writer's write was called once (by flushBuffer) and no further calls.
        verify(mockUnderlying, times(1)).write(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void write_flushesWhenBufferExactlyFull_thenBuffersNewChar() throws IOException {
        // buffer size 4
        FastWriter fw = new FastWriter(mockWriter, 4);
        // First call fills the buffer exactly
        fw.write("abcd".toCharArray(), 0, 4);
        // No flush yet – data is still in internal buffer
        assertEquals("", mockWriter.getOutput());
        // Second call with a single character should trigger flush of the previous 4 chars
        fw.write("e".toCharArray(), 0, 1);
        // After the second call the previous four chars must have been written
        assertEquals("abcd", mockWriter.getOutput(), "Buffer should be flushed before buffering the new character");
        // The new character is still in the buffer; flush to make it appear
        fw.flush();
        assertEquals("abcde", mockWriter.getOutput(), "After final flush the trailing character must be written");
    }

    @Test
    void write_twoWritesExactRemainingSpace_noFlushUntilFinalFlush() throws IOException {
        // buffer size 4
        FastWriter fw = new FastWriter(mockWriter, 4);
        // First write 2 chars
        fw.write("12".toCharArray(), 0, 2);
        // Second write exactly the remaining space (2 chars)
        fw.write("34".toCharArray(), 0, 2);
        // No flush should have happened yet
        assertEquals("", mockWriter.getOutput());
        // Flush now – all four chars appear
        fw.flush();
        assertEquals("1234", mockWriter.getOutput(), "Two exact‑size writes should fill the buffer without intermediate flushes");
    }

    @Test
    void write_partialThenLargeWrite_flushesBufferAndWritesDirectly() throws IOException {
        // buffer size 4
        FastWriter fw = new FastWriter(mockWriter, 4);
        // First write 2 chars – they stay in the buffer
        fw.write("xy".toCharArray(), 0, 2);
        assertEquals("", mockWriter.getOutput());
        // Second write length >= buffer size (4) triggers flush of existing buffer and direct write
        // length 4
        char[] big = "wxyz".toCharArray();
        fw.write(big, 0, big.length);
        // After the call, the first two chars must have been flushed, then the big array written directly.
        // The direct write does not affect the internal buffer, so after flush the buffer is empty.
        assertEquals("xywxyz", mockWriter.getOutput(), "Existing buffered data must be flushed before the large direct write");
    }

    @Test
    void write_directWriteFlushesBufferAndPropagatesIOException_fromFlush() throws IOException {
        // Mock writer that throws on write (used by flushBuffer)
        doThrow(new IOException("flush failure")).when(mockUnderlying).write(any(char[].class), anyInt(), anyInt());
        // small buffer
        FastWriter fw = new FastWriter(mockUnderlying, 4);
        // Fill the buffer partially
        fw.write("ab".toCharArray(), 0, 2);
        // Now request a direct write (len >= buffer size) – this should first flush the buffer,
        // encounter the IOException, and never invoke the second direct write.
        // length 4 == buffer size
        char[] big = "cdef".toCharArray();
        IOException ex = assertThrows(IOException.class, () -> fw.write(big, 0, big.length));
        assertEquals("flush failure", ex.getMessage());
        // Verify that only one write call happened (the one from flushBuffer)
        verify(mockUnderlying, times(1)).write(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void write_partialFlushThenCopy_matchesJsonSpecification() throws IOException {
        // Buffer size 4, start with nextChar = 2 (pretend two chars already buffered)
        FastWriter fw = new FastWriter(mockWriter, 4);
        // buffer now has 2 chars
        fw.write("ab".toCharArray(), 0, 2);
        // Write 3 chars starting from index 1 of source array => "2","3","4"
        char[] src = { '1', '2', '3', '4' };
        // len 3 > remaining space (4-2=2) => flush then copy
        fw.write(src, 1, 3);
        // After the call the first two chars must have been flushed
        assertEquals("ab", mockWriter.getOutput(), "Existing buffer should be flushed before copying new data");
        // The new chars should be buffered but not yet flushed
        fw.flush();
        assertEquals("ab234", mockWriter.getOutput(), "After final flush the copied characters must appear");
    }
}
