package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Writer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastWriter_write_2_Test {

    /**
     * Mocked underlying writer that FastWriter will delegate to.
     */
    @Mock
    private Writer mockWriter;

    /**
     * Instance under test – created with a very small buffer so we can trigger internal flushes.
     */
    private FastWriter fastWriter;

    /**
     * Size of the internal buffer used by FastWriter for the tests.
     */
    private static final int BUFFER_SIZE = 4;

    @BeforeEach
    void setupBeforeEach() {
        // Create a new FastWriter for each test so state does not leak between tests.
        fastWriter = new FastWriter(mockWriter, BUFFER_SIZE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the writer is closed after each test to avoid side‑effects.
        fastWriter.close();
    }

    private static Stream<Arguments> provideNormalAndBoundaryCases() {
        return Stream.of(// Write the entire string with off=0 and len equal to string length.
        Arguments.of("Write whole string fitting buffer", "HelloWorld", 0, 10, 10, false), // Write a substring starting at a non‑zero offset.
        Arguments.of("Write substring with non‑zero offset", "HelloWorld", 5, 5, 5, false), // Zero length write – buffer unchanged.
        Arguments.of("Zero length write (off=0)", "ABCDEFGHIJ", 0, 0, 0, false), // Zero length write with off equal to string length.
        Arguments.of("Zero length write (off=string length)", "ABCDEFGHIJ", 10, 0, 0, false));
    }

    /**
     * Verify that writing a string that fits entirely within the buffer does not cause an
     * immediate write to the underlying Writer, but a flush writes the buffered characters.
     */
    @Test
    void write_StringWithinBuffer_FlushesOnFlush() throws IOException {
        // GIVEN
        // length (3) < BUFFER_SIZE (4)
        String data = "abc";
        // WHEN
        fastWriter.write(data, 0, data.length());
        // No interaction should have happened yet because the buffer is not full.
        verifyNoInteractions(mockWriter);
        // Trigger the actual write to the underlying writer.
        fastWriter.flush();
        // THEN
        ArgumentCaptor<char[]> captor = ArgumentCaptor.forClass(char[].class);
        verify(mockWriter).write(captor.capture(), eq(0), eq(data.length()));
        assertArrayEquals(data.toCharArray(), captor.getValue(), "The characters written to the underlying writer must match the input string");
    }

    /**
     * Verify that when the input string exceeds the internal buffer size, FastWriter flushes
     * the buffer automatically during the write operation and then flushes any remaining
     * characters when {@code flush()} is called.
     */
    @Test
    void write_StringExceedsBuffer_FlushesDuringWriteAndOnFlush() throws IOException {
        // GIVEN
        // length (8) > BUFFER_SIZE (4)
        String data = "abcdefgh";
        InOrder inOrder = inOrder(mockWriter);
        // WHEN
        fastWriter.write(data, 0, data.length());
        // At this point the internal buffer should have flushed once (first 4 chars).
        // The second half remains in the buffer until an explicit flush().
        // Flush remaining characters.
        fastWriter.flush();
        // THEN
        // Capture the two write calls.
        ArgumentCaptor<char[]> captor = ArgumentCaptor.forClass(char[].class);
        inOrder.verify(mockWriter).write(captor.capture(), eq(0), eq(BUFFER_SIZE));
        inOrder.verify(mockWriter).write(captor.capture(), eq(0), eq(data.length() - BUFFER_SIZE));
        // Verify the first call contained the first 4 characters.
        char[] firstCall = captor.getAllValues().get(0);
        assertArrayEquals("abcd".toCharArray(), firstCall, "First flush should contain the first BUFFER_SIZE characters");
        // Verify the second call contained the remaining characters.
        char[] secondCall = captor.getAllValues().get(1);
        assertArrayEquals("efgh".toCharArray(), secondCall, "Second flush should contain the remaining characters");
    }

    /**
     * Verify that invoking {@code write} after the FastWriter has been closed results in an
     * {@link IOException}. The {@code close()} method of FastWriter is expected to null‑out the
     * underlying writer, causing subsequent writes to fail.
     */
    @Test
    void write_AfterClose_ThrowsIOException() throws IOException {
        // GIVEN
        // Close the writer – this should render the internal 'out' null.
        fastWriter.close();
        // WHEN / THEN
        assertThrows(IOException.class, () -> fastWriter.write("x", 0, 1), "Writing after the FastWriter is closed must throw IOException");
    }

    /**
     * Simulate a FastWriter whose internal {@code out} field is {@code null}
     * (as if the writer was constructed without a backing Writer). Any call to
     * {@code write(String,off,len)} must throw an {@link IOException} with the
     * expected message.
     */
    @Test
    void write_ClosedStream_ThrowsIOExceptionWithMessage() throws Exception {
        // GIVEN: create a FastWriter with a valid writer, then null out via reflection
        FastWriter writer = new FastWriter(mockWriter, BUFFER_SIZE);
        Field outField = FastWriter.class.getDeclaredField("out");
        outField.setAccessible(true);
        // simulate closed internal stream
        outField.set(writer, null);
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> writer.write("any", 0, 3), "Expected IOException when internal writer is null");
        assertEquals("FastWriter stream is closed.", ex.getMessage(), "Exception message must match contract");
    }

    /**
     * Parameterized normal/boundary cases for the {@code write(String,off,len)} method.
     * The test verifies that the internal buffer state (nextChar) matches the expected
     * value after the write operation and that no unexpected interactions with the
     * underlying writer occur unless a flush is required.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideNormalAndBoundaryCases")
    void write_NormalAndBoundaryCases(String description, String str, int off, int len, int expectedNextChar, boolean expectFlushDuringWrite) throws Exception {
        // GIVEN
        // Use a fresh FastWriter with a buffer size large enough to avoid automatic flushes
        // unless the test explicitly expects them.
        FastWriter writer = new FastWriter(mockWriter, BUFFER_SIZE);
        // WHEN
        writer.write(str, off, len);
        // THEN
        // Verify internal buffer position
        Field nextCharField = FastWriter.class.getDeclaredField("nextChar");
        nextCharField.setAccessible(true);
        int actualNextChar = (int) nextCharField.get(writer);
        assertEquals(expectedNextChar, actualNextChar, "nextChar should reflect number of buffered characters");
        // If a flush is expected during write (i.e., buffer overflow), verify that the mock
        // writer received at least one write call.
        if (expectFlushDuringWrite) {
            verify(mockWriter, atLeastOnce()).write(any(char[].class), eq(0), anyInt());
        } else {
            // No automatic flush should have occurred.
            verifyNoInteractions(mockWriter);
        }
    }

    /**
     * Verify that a write operation whose length exceeds the internal buffer size
     * triggers multiple internal flushes and eventually writes all characters to the
     * underlying writer.
     */
    @Test
    void write_MultipleFlushes_DuringLongWrite() throws Exception {
        // GIVEN
        // 30 chars
        String data = "ABCDEFGHIJABCDEFGHIJABCDEFGHIJ";
        // small buffer to force many flushes
        FastWriter writer = new FastWriter(mockWriter, 8);
        InOrder inOrder = inOrder(mockWriter);
        // WHEN
        writer.write(data, 0, data.length());
        // THEN
        // The writer should have flushed 3 times (8+8+8) and left 6 chars in buffer.
        // Finally, flush to write remaining chars.
        writer.flush();
        // Capture calls
        ArgumentCaptor<char[]> captor = ArgumentCaptor.forClass(char[].class);
        inOrder.verify(mockWriter, times(4)).write(captor.capture(), eq(0), anyInt());
        // Validate each captured chunk size
        assertEquals(8, captor.getAllValues().get(0).length);
        assertEquals(8, captor.getAllValues().get(1).length);
        assertEquals(8, captor.getAllValues().get(2).length);
        assertEquals(6, captor.getAllValues().get(3).length);
        // Validate content concatenated equals original data
        StringBuilder combined = new StringBuilder();
        for (char[] chunk : captor.getAllValues()) {
            combined.append(new String(chunk));
        }
        assertEquals(data, combined.toString(), "All characters must be written in order after flushes");
    }

    /**
     * Verify that a write spanning across a buffer boundary with a non‑zero offset
     * writes the correct slice of characters.
     */
    @Test
    void write_SpanningBoundary_WithOffset() throws Exception {
        // GIVEN
        // 20 chars
        String source = "ABCDEFGHIJABCDEFGHIJ";
        // small buffer
        FastWriter writer = new FastWriter(mockWriter, 6);
        // Write 15 chars starting at offset 5 -> characters 5..19 (15 chars)
        writer.write(source, 5, 15);
        writer.flush();
        // THEN
        ArgumentCaptor<char[]> captor = ArgumentCaptor.forClass(char[].class);
        verify(mockWriter, atLeastOnce()).write(captor.capture(), eq(0), anyInt());
        // Reconstruct what was written
        StringBuilder written = new StringBuilder();
        for (char[] chunk : captor.getAllValues()) {
            written.append(new String(chunk));
        }
        assertEquals(source.substring(5, 20), written.toString(), "The characters written must match the expected slice");
    }

    /**
     * Verify that a negative offset results in a {@link StringIndexOutOfBoundsException}
     * (thrown by {@code String#getChars}).
     */
    @Test
    void write_NegativeOffset_ThrowsStringIndexOutOfBoundsException() {
        // GIVEN
        String str = "ABC";
        // WHEN / THEN
        assertThrows(StringIndexOutOfBoundsException.class, () -> fastWriter.write(str, -1, 2), "Negative offset should cause StringIndexOutOfBoundsException");
    }

    /**
     * Verify that an offset+len that exceeds the source string length results in a
     * {@link StringIndexOutOfBoundsException}.
     */
    @Test
    void write_OffsetPlusLenBeyondString_ThrowsStringIndexOutOfBoundsException() {
        // GIVEN
        String str = "ABC";
        // WHEN / THEN
        assertThrows(StringIndexOutOfBoundsException.class, () -> fastWriter.write(str, 1, 5), "off+len beyond string length should cause StringIndexOutOfBoundsException");
    }

    /**
     * Verify that passing a {@code null} string argument results in a {@link NullPointerException}.
     */
    @Test
    void write_NullString_ThrowsNullPointerException() {
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> fastWriter.write(null, 0, 0), "Calling write with a null String must throw NullPointerException");
    }

    /**
     * Verify that an offset greater than the string length with a positive length
     * causes a {@link StringIndexOutOfBoundsException}.
     */
    @Test
    void write_OffsetBeyondStringLength_WithPositiveLen_ThrowsStringIndexOutOfBoundsException() {
        // GIVEN
        String str = "ABC";
        // WHEN / THEN
        assertThrows(StringIndexOutOfBoundsException.class, () -> fastWriter.write(str, 5, 1), "Offset beyond string length with positive len should throw StringIndexOutOfBoundsException");
    }

    /**
     * Simulate an underlying writer that throws an {@link IOException} during a flush.
     * The FastWriter should propagate this exception.
     */
    @Test
    void write_UnderlyingWriterThrowsIOException_DuringFlush() throws Exception {
        // GIVEN
        // Configure mock to throw on any write call
        doThrow(new IOException("mock writer failure")).when(mockWriter).write(any(char[].class), eq(0), anyInt());
        // Use a tiny buffer to force a flush during the write of 5 characters
        FastWriter writer = new FastWriter(mockWriter, 2);
        String data = "ABCDE";
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> writer.write(data, 0, data.length()), "IOException from underlying writer must be propagated");
        assertEquals("mock writer failure", ex.getMessage(), "Exception message should match the mock's message");
    }
}
