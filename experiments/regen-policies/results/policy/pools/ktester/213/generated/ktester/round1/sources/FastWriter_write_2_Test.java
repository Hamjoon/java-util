package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.StringWriter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FastWriter_write_2_Test {

    // small size to trigger buffer logic easily
    private static final int CUSTOM_BUFFER_SIZE = 8;

    private StringWriter stringWriter;

    private FastWriter fastWriter;

    @Mock
    private Writer // used in tests that need to inject failures
    mockWriter;

    @BeforeEach
    void setupBeforeEach() {
        stringWriter = new StringWriter();
        fastWriter = new FastWriter(stringWriter, CUSTOM_BUFFER_SIZE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        if (fastWriter != null) {
            fastWriter.close();
        }
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> shortFitProvider() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("AB", 2), org.junit.jupiter.params.provider.Arguments.of("XYZ", 3));
    }

    /**
     * Fast path – the whole string fits into the remaining buffer space.
     */
    @Test
    void testWriteStringFitsInBuffer() throws IOException {
        // buffer has space for 8 chars
        fastWriter.write("abc", 0, 3);
        // force write to underlying writer
        fastWriter.flush();
        assertEquals("abc", stringWriter.toString());
    }

    /**
     * Fast path – string length exactly fills the buffer, causing an automatic flush.
     */
    @Test
    void testWriteStringExactlyFillsBuffer() throws IOException {
        // fills buffer completely
        fastWriter.write("12345678", 0, 8);
        // flush happens inside write when buffer becomes full, no explicit flush needed
        assertEquals("12345678", stringWriter.toString());
    }

    /**
     * Medium path – string longer than buffer size, requiring multiple buffer fills and direct writes.
     */
    @Test
    void testWriteStringLongerThanBuffer() throws IOException {
        // Buffer size is 8, write 20 chars to exercise all branches
        String data = "ABCDEFGHIJKLMNOPQRST";
        fastWriter.write(data, 0, data.length());
        // All data should have been written to the underlying writer
        assertEquals(data, stringWriter.toString());
    }

    /**
     * Medium path – string length spans multiple full‑buffer chunks after the initial partial fill.
     */
    @Test
    void testWriteStringSpanningPartialAndFullChunks() throws IOException {
        // First write a short string to leave the buffer partially filled
        // 3 chars in buffer, 5 slots left
        fastWriter.write("XYZ", 0, 3);
        // Now write a longer string that will fill the remaining 5, flush,
        // then write two full buffer chunks (8 chars each) directly.
        // 14 chars
        String longer = "ABCDEFGHIJKLMN";
        fastWriter.write(longer, 0, longer.length());
        // Expected output: first 3 chars + the 5 that completed the first buffer,
        // then the remaining 14 chars written directly.
        String expected = "XYZ" + "ABCDE" + "FGHIJKLMN";
        assertEquals(expected, stringWriter.toString());
    }

    /**
     * Edge case – length zero should result in no write and no exception.
     */
    @Test
    void testWriteStringZeroLength() throws IOException {
        fastWriter.write("anything", 0, 0);
        fastWriter.flush();
        assertEquals("", stringWriter.toString());
    }

    /**
     * Edge case – non‑zero offset with a substring.
     */
    @Test
    void testWriteStringWithOffset() throws IOException {
        // writes "World"
        fastWriter.write("HelloWorld", 5, 5);
        fastWriter.flush();
        assertEquals("World", stringWriter.toString());
    }

    /**
     * Verify that attempting to write after the writer has been closed throws IOException.
     */
    @Test
    void testWriteAfterCloseThrows() throws IOException {
        fastWriter.close();
        IOException ex = assertThrows(IOException.class, () -> fastWriter.write("test", 0, 4));
        assertTrue(ex.getMessage().contains("FastWriter stream is closed"));
    }

    /**
     * Verify that a null underlying writer (simulated by reflection) causes IOException.
     *  This test does not use reflection to access private fields; instead it relies on
     *  the documented behaviour that a closed FastWriter throws IOException on write.
     */
    @Test
    void testWriteWhenUnderlyingWriterIsNullViaClose() throws IOException {
        // closes underlying writer and marks FastWriter as closed
        fastWriter.close();
        assertThrows(IOException.class, () -> fastWriter.write("abc", 0, 3));
    }

    /**
     * Short string fits entirely in buffer (no flush).
     */
    @ParameterizedTest(name = "{index}: write(\"{0}\",0,{1})")
    @MethodSource("shortFitProvider")
    void testShortStringFitsInBuffer_NoFlush(String str, int len) throws IOException, NoSuchFieldException, IllegalAccessException {
        fastWriter.write(str, 0, len);
        // inspect private nextChar to ensure it equals len and no flush occurred
        Field nextCharField = FastWriter.class.getDeclaredField("nextChar");
        nextCharField.setAccessible(true);
        int nextChar = (int) nextCharField.get(fastWriter);
        assertEquals(len, nextChar, "nextChar should be updated to the number of characters written");
        // underlying writer should still be empty because we haven't flushed yet
        assertEquals("", stringWriter.toString());
    }

    /**
     * Exact buffer fill triggers flush.
     */
    @Test
    void testExactBufferFillTriggersFlush() throws IOException {
        // Use a smaller buffer to make the test clearer
        FastWriter writer = new FastWriter(stringWriter, 5);
        // exactly fills buffer
        writer.write("12345", 0, 5);
        // flush occurs automatically, so underlying writer already contains data
        assertEquals("12345", stringWriter.toString());
    }

    /**
     * Final fragment handling – leftover characters after the while‑loop are stored in the buffer.
     */
    @Test
    void testFinalFragmentHandling() throws IOException, NoSuchFieldException, IllegalAccessException {
        FastWriter writer = new FastWriter(stringWriter, 5);
        // Write 7 characters: first 5 fill & flush, remaining 2 should stay in buffer
        writer.write("ABCDEFG", 0, 7);
        // Verify underlying writer received only the first 5 chars
        assertEquals("ABCDE", stringWriter.toString());
        // Use reflection to check that the buffer now holds the final fragment "FG"
        Field nextCharField = FastWriter.class.getDeclaredField("nextChar");
        nextCharField.setAccessible(true);
        int nextChar = (int) nextCharField.get(writer);
        assertEquals(2, nextChar, "After processing, nextChar should equal the leftover length");
        // Flush to ensure the final fragment is eventually written
        writer.flush();
        assertEquals("ABCDEFG", stringWriter.toString());
    }

    /**
     * Medium path – partial fill then flush with a pre‑filled buffer.
     */
    @Test
    void testMediumPartialFillThenFlush() throws IOException {
        // Buffer size 5, pre‑fill with 2 chars
        FastWriter writer = new FastWriter(stringWriter, 5);
        // buffer now contains "AB", nextChar = 2
        writer.write("AB", 0, 2);
        // Now write 7 chars; 3 will complete the buffer and cause a flush,
        // remaining 4 will be left in the buffer.
        writer.write("ABCDEFG", 0, 7);
        // Underlying writer should have received the first flush ("AB" + "CDE")
        assertEquals("ABCDE", stringWriter.toString());
        // Flush remaining fragment and verify full output
        writer.flush();
        assertEquals("ABCDEFG", stringWriter.toString());
    }

    /**
     * Large string with multiple full‑buffer writes.
     */
    @Test
    void testLargeStringMultipleFullBufferWrites() throws IOException {
        FastWriter writer = new FastWriter(stringWriter, 5);
        // 2 full buffers + 2 leftover chars
        writer.write("ABCDEFGHIJKL", 0, 12);
        // Two full buffer writes should have been flushed automatically
        assertEquals("ABCDEFGHIJ", stringWriter.toString());
        // Flush the remaining 2 chars
        writer.flush();
        assertEquals("ABCDEFGHIJKL", stringWriter.toString());
    }

    /**
     * Offset handling (substring write).
     */
    @Test
    void testOffsetHandling() throws IOException {
        // writes "3456"
        fastWriter.write("0123456789", 3, 4);
        fastWriter.flush();
        assertEquals("3456", stringWriter.toString());
    }

    /**
     * Negative offset throws StringIndexOutOfBoundsException.
     */
    @Test
    void testNegativeOffsetThrows() {
        assertThrows(StringIndexOutOfBoundsException.class, () -> fastWriter.write("HelloWorld", -1, 5));
    }

    /**
     * Negative length throws StringIndexOutOfBoundsException.
     */
    @Test
    void testNegativeLengthThrows() {
        assertThrows(StringIndexOutOfBoundsException.class, () -> fastWriter.write("HelloWorld", 0, -3));
    }

    /**
     * Offset+len exceeds string length throws StringIndexOutOfBoundsException.
     */
    @Test
    void testOffsetLenExceedsStringThrows() {
        assertThrows(StringIndexOutOfBoundsException.class, () -> fastWriter.write("ABCDEF", 4, 3));
    }

    /**
     * IOException from underlying writer during flushBuffer (exact buffer fill).
     */
    @Test
    void testIOExceptionDuringFlushBuffer() throws IOException {
        // Buffer size 5, mock writer throws on write(...)
        doThrow(new IOException("mocked write failure")).when(mockWriter).write(any(char[].class), eq(0), eq(5));
        FastWriter writer = new FastWriter(mockWriter, 5);
        IOException ex = assertThrows(IOException.class, () -> writer.write("12345", 0, 5));
        assertTrue(ex.getMessage().contains("mocked write failure"));
    }

    /**
     * IOException from underlying writer during direct buffer write (while‑loop).
     */
    @Test
    void testIOExceptionDuringDirectWriteInLoop() throws IOException {
        // Buffer size 5, first write (flush of initial fill) succeeds, second write throws
        // Configure the mock to do nothing on the first invocation and throw on the second.
        doNothing().doThrow(new IOException("direct write failure")).when(mockWriter).write(any(char[].class), eq(0), eq(5));
        FastWriter writer = new FastWriter(mockWriter, 5);
        // Write 10 chars: first 5 fill & flush (first mock call), next 5 written directly (second mock call)
        IOException ex = assertThrows(IOException.class, () -> writer.write("ABCDEFGHIJ", 0, 10));
        assertTrue(ex.getMessage().contains("direct write failure"));
        // Verify the order of interactions: first a successful write, then a failing one
        InOrder inOrder = inOrder(mockWriter);
        // first flush
        inOrder.verify(mockWriter).write(any(char[].class), eq(0), eq(5));
        // second (failed) write
        inOrder.verify(mockWriter).write(any(char[].class), eq(0), eq(5));
    }

    /**
     * Ensure that after an IOException the internal state remains consistent (no partial buffer loss).
     */
    @Test
    void testStateAfterIOExceptionDuringDirectWrite() throws IOException {
        // Buffer size 5, make the second direct write throw
        doNothing().doThrow(new IOException("boom")).when(mockWriter).write(any(char[].class), eq(0), eq(5));
        FastWriter writer = new FastWriter(mockWriter, 5);
        // Write 10 chars; exception expected on second direct write
        assertThrows(IOException.class, () -> writer.write("ABCDEFGHIJ", 0, 10));
        // The first 5 chars should have been flushed already; the remaining 5 are still in the buffer
        // Verify by flushing (which should attempt to write the remaining fragment and re‑throw)
        IOException flushEx = assertThrows(IOException.class, writer::flush);
        assertTrue(flushEx.getMessage().contains("boom"));
    }
}
