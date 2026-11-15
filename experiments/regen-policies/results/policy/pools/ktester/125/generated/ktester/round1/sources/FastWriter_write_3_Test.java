package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class FastWriter_write_3_Test {

    /**
     * objects used by the *mock‑based* tests **
     */
    @Mock
    private Writer // injected by Mockito
    mockWriter;

    // FastWriter that delegates to mockWriter
    private FastWriter fastWriterWithMock;

    // collects the characters actually written
    private StringWriter realWriter;

    // FastWriter that delegates to realWriter
    private FastWriter fastWriterWithReal;

    // deliberately tiny to force flushes
    private static final int SMALL_BUFFER = 4;

    // used for a few larger‑buffer scenarios
    private static final int DEFAULT_BUFFER = 10;

    @BeforeEach
    void setupBeforeEach() {
        // --- mock based instance -------------------------------------------------
        fastWriterWithMock = new FastWriter(mockWriter, SMALL_BUFFER);
        // --- real writer instance ------------------------------------------------
        realWriter = new StringWriter();
        fastWriterWithReal = new FastWriter(realWriter, SMALL_BUFFER);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the writer is closed after each test to avoid resource leaks.
        // Any IOException is ignored because the test framework will already have
        // reported a failure if one occurs.
        try {
            fastWriterWithMock.close();
        } catch (Exception ignored) {
        }
        try {
            fastWriterWithReal.close();
        } catch (Exception ignored) {
        }
    }

    /* --------------------------------------------------------------
     *  Utility methods for reflection access.
     * -------------------------------------------------------------- */
    private static Object getPrivateField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void setPrivateField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /* --------------------------------------------------------------
     *  1. Zero‑length write – nothing should be delegated to the
     *     underlying Writer.
     * -------------------------------------------------------------- */
    @Test
    void writeZeroLengthDoesNothing() throws IOException {
        char[] data = "abc".toCharArray();
        // len == 0
        fastWriterWithMock.write(data, 1, 0);
        // No interaction with the mock Writer at all
        verifyNoInteractions(mockWriter);
    }

    /* --------------------------------------------------------------
     *  2. Invalid offset / length combinations – should throw
     *     IndexOutOfBoundsException.
     * -------------------------------------------------------------- */
    @Test
    void writeInvalidParametersThrowIndexOutOfBounds() {
        char[] data = "hello".toCharArray();
        // off < 0
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriterWithMock.write(data, -1, 2));
        // off > array length
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriterWithMock.write(data, data.length + 1, 1));
        // len < 0
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriterWithMock.write(data, 0, -3));
        // off + len exceeds array length
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriterWithMock.write(data, 3, 3));
        // off + len overflows (negative after addition)
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriterWithMock.write(data, Integer.MAX_VALUE, 2));
    }

    /* --------------------------------------------------------------
     *  3. Length greater than or equal to the internal buffer size.
     *     The implementation should bypass the buffer and write
     *     directly to the underlying Writer.
     * -------------------------------------------------------------- */
    @Test
    void writeLengthExceedsBufferWritesDirectly() throws IOException {
        // Buffer size is 4, we ask for 5 characters → direct write path
        char[] data = "12345".toCharArray();
        fastWriterWithMock.write(data, 0, data.length);
        // Verify that the underlying Writer's write(char[],int,int) method
        // was called exactly once with the same arguments.
        verify(mockWriter, times(1)).write(data, 0, data.length);
        // No flushBuffer interaction can be verified because it is private,
        // but the direct delegation proves the correct branch was taken.
    }

    /* --------------------------------------------------------------
     *  4. Write that first fills part of the buffer and then exceeds the
     *     remaining space. The first write should be buffered, the second
     *     write must cause a flush of the buffer before the remaining data
     *     are copied into the (now empty) buffer.
     * -------------------------------------------------------------- */
    @Test
    void writeWhenBufferNeedsFlush() throws IOException {
        // Buffer size = 4
        // First write 3 chars → fits into buffer, no flush yet.
        char[] first = "abc".toCharArray();
        fastWriterWithReal.write(first, 0, first.length);
        // Second write 3 chars → only 1 slot left, so buffer must flush,
        // then the 3 chars are copied into the (empty) buffer.
        char[] second = "def".toCharArray();
        fastWriterWithReal.write(second, 0, second.length);
        // At this point nothing has been forced out to the underlying writer
        // because the buffer still holds the last three characters ("def").
        // Calling flush() forces the pending buffer to be written.
        fastWriterWithReal.flush();
        // The underlying StringWriter should now contain the concatenated text:
        // first three chars ("abc") were flushed before the second write,
        // then the three chars from the second write ("def").
        assertEquals("abcdef", realWriter.toString());
    }

    /* --------------------------------------------------------------
     *  5. Write that exactly matches the remaining buffer capacity.
     *     No explicit flush should happen; the data stays in the buffer
     *     until an explicit flush() or close().
     * -------------------------------------------------------------- */
    @Test
    void writeExactRemainingCapacityStaysBuffered() throws IOException {
        // Buffer size = 4
        // occupies 2 slots
        char[] first = "ab".toCharArray();
        fastWriterWithReal.write(first, 0, first.length);
        // exactly fills the remaining 2 slots
        char[] second = "cd".toCharArray();
        fastWriterWithReal.write(second, 0, second.length);
        // No flush yet → underlying writer should still be empty
        assertEquals("", realWriter.toString());
        // Flush now and verify the whole content appears.
        fastWriterWithReal.flush();
        assertEquals("abcd", realWriter.toString());
    }

    /* --------------------------------------------------------------
     *  6. Stream closed (out == null) → IOException with proper message.
     * -------------------------------------------------------------- */
    @Test
    void writeWhenOutIsNullThrowsIOException() throws Exception {
        // Create a normal instance then null‑out the internal writer via reflection.
        FastWriter writer = new FastWriter(mockWriter, SMALL_BUFFER);
        setPrivateField(writer, "out", null);
        char[] data = { 'x', 'y', 'z' };
        IOException ex = assertThrows(IOException.class, () -> writer.write(data, 0, 1));
        assertEquals("FastWriter stream is closed.", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  7. Null character buffer → NullPointerException.
     * -------------------------------------------------------------- */
    @Test
    void writeNullCbufThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> fastWriterWithMock.write(null, 0, 1));
    }

    /* --------------------------------------------------------------
     *  8. Normal buffering – characters are copied into the internal
     *     buffer and no flush occurs.
     * -------------------------------------------------------------- */
    @Test
    void normalBufferingCopiesIntoInternalBuffer() throws IOException, Exception {
        FastWriter writer = new FastWriter(realWriter, DEFAULT_BUFFER);
        char[] data = { 'a', 'b', 'c' };
        writer.write(data, 0, data.length);
        // Underlying writer must still be empty.
        assertEquals("", realWriter.toString());
        // Verify internal state via reflection.
        int next = (int) getPrivateField(writer, "nextChar");
        assertEquals(3, next);
        char[] internal = (char[]) getPrivateField(writer, "cb");
        assertArrayEquals(data, new char[] { internal[0], internal[1], internal[2] });
    }

    /* --------------------------------------------------------------
     *  9. Direct write when length == buffer size (bypass buffer).
     * -------------------------------------------------------------- */
    @Test
    void writeLengthEqualsBufferSizeDirectWrite() throws IOException, Exception {
        FastWriter writer = new FastWriter(mockWriter, DEFAULT_BUFFER);
        // length 10 == buffer size
        char[] data = "abcdefghij".toCharArray();
        writer.write(data, 0, data.length);
        // Should delegate directly.
        verify(mockWriter, times(1)).write(data, 0, data.length);
        // Buffer must be untouched (nextChar == 0).
        int next = (int) getPrivateField(writer, "nextChar");
        assertEquals(0, next);
    }

    /* --------------------------------------------------------------
     * 10. Flush because not enough space left in the buffer.
     * -------------------------------------------------------------- */
    @Test
    void flushBufferWhenNotEnoughSpace() throws IOException, Exception {
        // buffer size 8
        FastWriter writer = new FastWriter(realWriter, 8);
        // Fill 6 characters → 2 slots left.
        writer.write("123456".toCharArray(), 0, 6);
        // Now write 4 characters, which exceeds remaining space.
        writer.write("ABCD".toCharArray(), 0, 4);
        // After the second write, the first 6 chars must have been flushed.
        assertEquals("123456", realWriter.toString());
        // Buffer now contains "ABCD".
        int next = (int) getPrivateField(writer, "nextChar");
        assertEquals(4, next);
        char[] cb = (char[]) getPrivateField(writer, "cb");
        assertArrayEquals(new char[] { 'A', 'B', 'C', 'D' }, new char[] { cb[0], cb[1], cb[2], cb[3] });
        // Final flush should write the remaining 4 chars.
        writer.flush();
        assertEquals("123456ABCD", realWriter.toString());
    }

    /* --------------------------------------------------------------
     * 11. Exact fit into remaining buffer space – no flush.
     * -------------------------------------------------------------- */
    @Test
    void exactFitIntoRemainingSpaceDoesNotFlush() throws IOException, Exception {
        FastWriter writer = new FastWriter(realWriter, 10);
        // 3 slots left
        writer.write("1234567".toCharArray(), 0, 7);
        // exact fit
        writer.write("abc".toCharArray(), 0, 3);
        // No data should have reached the underlying writer yet.
        assertEquals("", realWriter.toString());
        // Buffer should be full.
        int next = (int) getPrivateField(writer, "nextChar");
        assertEquals(10, next);
        writer.flush();
        assertEquals("1234567abc", realWriter.toString());
    }

    /* --------------------------------------------------------------
     * 12. Multiple writes causing a flush in the middle of a sequence.
     * -------------------------------------------------------------- */
    @Test
    void multipleWritesMidSequenceFlush() throws IOException, Exception {
        FastWriter writer = new FastWriter(realWriter, 10);
        // 1st write: 6 chars, buffer not full.
        writer.write("abcdef".toCharArray(), 0, 6);
        assertEquals(6, getPrivateField(writer, "nextChar"));
        // 2nd write: 4 chars, exactly fills buffer – still no flush.
        writer.write("klmn".toCharArray(), 0, 4);
        assertEquals(10, getPrivateField(writer, "nextChar"));
        assertEquals("", realWriter.toString());
        // 3rd write: 3 chars, buffer full → flush first 10 chars, then buffer 3 chars.
        writer.write("xyz".toCharArray(), 0, 3);
        assertEquals(3, getPrivateField(writer, "nextChar"));
        assertEquals("abcdefklmn", realWriter.toString());
        // Final flush writes the remaining three characters.
        writer.flush();
        assertEquals("abcdefklmnyxz", realWriter.toString());
    }
}
