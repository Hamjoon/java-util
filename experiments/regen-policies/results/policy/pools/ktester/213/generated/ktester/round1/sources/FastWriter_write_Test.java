package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Writer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FastWriter_write_Test {

    /**
     * Small buffer size used to force automatic flushing.
     */
    private static final int SMALL_BUFFER = 3;

    private MockWriter mockWriter;

    private FastWriter fastWriter;

    /* --------------------------------------------------------------------- */
    private static class MockWriter extends Writer {

        private final StringBuilder sb = new StringBuilder();

        private boolean closed = false;

        @Override
        public void write(char[] cbuf, int off, int len) {
            sb.append(cbuf, off, len);
        }

        @Override
        public void flush() {
            // No-op – FastWriter handles flushing of its internal buffer.
        }

        @Override
        public void close() {
            closed = true;
        }

        String getOutput() {
            return sb.toString();
        }

        boolean isClosed() {
            return closed;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        mockWriter = new MockWriter();
        // Use the small buffer to make the flush‑on‑overflow behaviour easy to test.
        fastWriter = new FastWriter(mockWriter, SMALL_BUFFER);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        if (fastWriter != null) {
            fastWriter.close();
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
     * Helper to set a private field via reflection.
     */
    private static void setPrivateField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void writeSingleCharacter_isBufferedUntilFlush() throws IOException {
        fastWriter.write('A');
        // Not flushed yet – underlying writer should still be empty.
        assertEquals("", mockWriter.getOutput());
        // Explicit flush writes the buffered data.
        fastWriter.flush();
        assertEquals("A", mockWriter.getOutput());
    }

    @Test
    void writeMultipleCharacters_flushesWhenBufferIsFull() throws IOException {
        // Buffer size = 3. After two writes the buffer is not full,
        // the third write triggers a flush before the character is stored.
        // buffer: a
        fastWriter.write('a');
        // buffer: a b
        fastWriter.write('b');
        // At this point the next write will cause a flush.
        // flushes "ab", then stores 'c' in fresh buffer
        fastWriter.write('c');
        // After the third write the underlying writer should contain the first two chars.
        assertEquals("ab", mockWriter.getOutput(), "The buffer should have been flushed when it became full.");
        // Write another character – buffer now holds "c" and "d".
        fastWriter.write('d');
        // Still only "ab" has been written to the underlying writer.
        assertEquals("ab", mockWriter.getOutput());
        // Final flush writes the remaining buffered characters.
        fastWriter.flush();
        assertEquals("abcd", mockWriter.getOutput(), "After explicit flush the remaining buffered characters must be written.");
    }

    @Test
    void writeAfterClose_throwsIOException() throws IOException {
        fastWriter.close();
        assertThrows(IOException.class, () -> fastWriter.write('x'), "Writing after the FastWriter has been closed must throw IOException.");
    }

    /* ---------- write - stream closed ---------- */
    @Test
    void writeWhenInternalWriterIsNull_throwsIOException() throws Exception {
        // Simulate a closed writer by setting the internal `out` to null via reflection.
        setPrivateField(fastWriter, "out", null);
        IOException ex = assertThrows(IOException.class, () -> fastWriter.write(65), "Calling write(int) when the internal Writer `out` is null should throw IOException.");
        assertEquals("FastWriter stream is closed.", ex.getMessage());
    }

    /* ---------- write - buffer has space (no flush) ---------- */
    @ParameterizedTest(name = "write({0}) stores char without flushing")
    @CsvSource({ "97, a", "98, b" })
    void writeWhenBufferHasSpace_noFlush(int intValue, char expectedChar) throws Exception {
        // Buffer is initially empty, size = SMALL_BUFFER (3)
        fastWriter.write(intValue);
        // Verify underlying writer still empty (no flush)
        assertEquals("", mockWriter.getOutput());
        // Verify internal buffer state
        char[] cb = (char[]) getPrivateField(fastWriter, "cb");
        int nextChar = (int) getPrivateField(fastWriter, "nextChar");
        assertEquals(1, nextChar, "nextChar should be incremented to 1");
        assertEquals(expectedChar, cb[0], "Character should be stored at position 0");
    }

    /* ---------- write - buffer full triggers flush ---------- */
    @Test
    void writeWhenBufferIsFull_triggersFlush() throws Exception {
        // Create a writer with buffer size 2 to hit the edge quickly.
        FastWriter fw = new FastWriter(mockWriter, 2);
        // Fill the buffer (no flush yet)
        // nextChar = 1
        fw.write('x');
        // nextChar = 2, still no flush because check is (nextChar+1)>=len
        fw.write('y');
        // The third write should cause a flush of "xy" before storing 'z'.
        fw.write('z');
        // Underlying writer should have received "xy"
        assertEquals("xy", mockWriter.getOutput());
        // Internal buffer should now contain only 'z' at position 0
        char[] cb = (char[]) getPrivateField(fw, "cb");
        int nextChar = (int) getPrivateField(fw, "nextChar");
        assertEquals(1, nextChar);
        assertEquals('z', cb[0]);
    }

    /* ---------- write - store null character ---------- */
    @Test
    void writeNullCharacter_isStoredCorrectly() throws Exception {
        fastWriter.write(0);
        assertEquals("", mockWriter.getOutput());
        char[] cb = (char[]) getPrivateField(fastWriter, "cb");
        int nextChar = (int) getPrivateField(fastWriter, "nextChar");
        assertEquals(1, nextChar);
        assertEquals('\u0000', cb[0]);
    }

    /* ---------- write - store maximum char value ---------- */
    @Test
    void writeMaximumCharValue_isStoredCorrectly() throws Exception {
        fastWriter.write(65535);
        assertEquals("", mockWriter.getOutput());
        char[] cb = (char[]) getPrivateField(fastWriter, "cb");
        int nextChar = (int) getPrivateField(fastWriter, "nextChar");
        assertEquals(1, nextChar);
        assertEquals('\uFFFF', cb[0]);
    }

    /* ---------- write - int value wraps to char (value > 0xFFFF) ---------- */
    @Test
    void writeIntGreaterThanCharRange_wrapsToLowBits() throws Exception {
        // 0x10000 -> low 16 bits = 0
        fastWriter.write(65536);
        assertEquals("", mockWriter.getOutput());
        char[] cb = (char[]) getPrivateField(fastWriter, "cb");
        int nextChar = (int) getPrivateField(fastWriter, "nextChar");
        assertEquals(1, nextChar);
        assertEquals('\u0000', cb[0]);
    }

    /* ---------- write - negative int value wraps to char ---------- */
    @Test
    void writeNegativeInt_wrapsToChar() throws Exception {
        // low 16 bits = 0xFFFF
        fastWriter.write(-1);
        assertEquals("", mockWriter.getOutput());
        char[] cb = (char[]) getPrivateField(fastWriter, "cb");
        int nextChar = (int) getPrivateField(fastWriter, "nextChar");
        assertEquals(1, nextChar);
        assertEquals('\uFFFF', cb[0]);
    }

    /* ---------- write - buffer size 1 edge case ---------- */
    @Test
    void writeWithBufferSizeOne_flushesBeforeStoring() throws Exception {
        FastWriter fw = new FastWriter(mockWriter, 1);
        // First write: buffer length = 1, condition true, flushBuffer() (nothing to flush)
        fw.write('x');
        // No data should have been written yet because buffer was empty before flush
        assertEquals("", mockWriter.getOutput());
        // Buffer now holds 'x'
        char[] cb = (char[]) getPrivateField(fw, "cb");
        int nextChar = (int) getPrivateField(fw, "nextChar");
        assertEquals(1, nextChar);
        assertEquals('x', cb[0]);
    }

    /* ---------- write - flushBuffer propagates IOException ---------- */
    @Test
    void writeWhenFlushBufferThrows_propagatesIOException() throws Exception {
        // Mock a Writer that throws IOException on write(char[],int,int)
        Writer throwingWriter = mock(Writer.class);
        doThrow(new IOException("boom")).when(throwingWriter).write(any(char[].class), anyInt(), anyInt());
        // Buffer size 1 forces flush on the first write
        FastWriter fw = new FastWriter(throwingWriter, 1);
        IOException ex = assertThrows(IOException.class, () -> fw.write('a'), "IOException from flushBuffer() must be propagated.");
        assertEquals("boom", ex.getMessage());
    }

    /* ---------- write - fill buffer exactly then next write triggers flush ---------- */
    @Test
    void writeFillExactThenFlushOnNextWrite() throws Exception {
        FastWriter fw = new FastWriter(mockWriter, 2);
        // First write – no flush
        fw.write('A');
        // Second write – still no flush (condition checks nextChar+1)
        fw.write('B');
        // Verify internal state before third write
        assertEquals("", mockWriter.getOutput());
        int nextCharBefore = (int) getPrivateField(fw, "nextChar");
        assertEquals(2, nextCharBefore);
        // Third write – should flush "AB" then store 'C'
        fw.write('C');
        // Underlying writer should now contain "AB"
        assertEquals("AB", mockWriter.getOutput());
        // Buffer should contain only 'C'
        char[] cb = (char[]) getPrivateField(fw, "cb");
        int nextCharAfter = (int) getPrivateField(fw, "nextChar");
        assertEquals(1, nextCharAfter);
        assertEquals('C', cb[0]);
    }

    /* ---------- write - null internal buffer ---------- */
    @Test
    void writeWithNullInternalBuffer_throwsNullPointerException() throws Exception {
        // Set internal buffer to null via reflection
        setPrivateField(fastWriter, "cb", null);
        assertThrows(NullPointerException.class, () -> fastWriter.write('A'), "Null internal buffer should cause NullPointerException.");
    }

    /* ---------- write - zero-length buffer ---------- */
    @Test
    void writeWithZeroLengthBuffer_throwsArrayIndexOutOfBoundsException() throws Exception {
        // Replace internal buffer with zero‑length array
        setPrivateField(fastWriter, "cb", new char[0]);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> fastWriter.write('A'), "Writing to a zero‑length buffer should throw ArrayIndexOutOfBoundsException.");
    }
}
