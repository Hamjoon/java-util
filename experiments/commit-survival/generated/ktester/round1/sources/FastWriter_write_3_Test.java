package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Writer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastWriter_write_3_Test {

    // A small buffer size makes it easy to trigger the different branches in write(...)
    private static final int BUFFER_SIZE = 4;

    @Mock
    private Writer mockUnderlyingWriter;

    private FastWriter fastWriter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a FastWriter that delegates to the mocked Writer.
        fastWriter = new FastWriter(mockUnderlyingWriter, BUFFER_SIZE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the writer is closed after each test to avoid side‑effects.
        fastWriter.close();
    }

    private static Stream<Arguments> indexValidationProvider() {
        return Stream.of(// Negative offset
        Arguments.of("Negative offset", new char[] { 'x', 'y', 'z' }, -1, 2, IndexOutOfBoundsException.class), // Offset greater than array length
        Arguments.of("Offset greater than array length", new char[] { 'x', 'y', 'z' }, 4, 0, IndexOutOfBoundsException.class), // Negative length
        Arguments.of("Negative length", new char[] { 'x', 'y', 'z' }, 1, -1, IndexOutOfBoundsException.class), // off+len exceeds array length
        Arguments.of("off+len exceeds array length", new char[] { 'x', 'y', 'z' }, 2, 2, IndexOutOfBoundsException.class), // Zero length (should NOT throw)
        Arguments.of("Zero length – normal return", new char[] { 'a', 'b', 'c', 'd' }, 0, 0, null));
    }

    @Test
    void write_WithZeroLength_DoesNotDelegateToUnderlyingWriter() throws IOException {
        char[] data = "abcd".toCharArray();
        fastWriter.write(data, 0, 0);
        // No interaction with the underlying writer should have happened.
        verifyNoInteractions(mockUnderlyingWriter);
    }

    @Test
    void write_WithLengthLessThanBuffer_FillsInternalBufferAndFlushesOnClose() throws IOException {
        // length 2 < BUFFER_SIZE
        char[] data = "ab".toCharArray();
        fastWriter.write(data, 0, data.length);
        // Still nothing should have been written to the underlying writer yet.
        verifyNoInteractions(mockUnderlyingWriter);
        // Flushing (or closing) should cause the buffered data to be written.
        fastWriter.flush();
        // The underlying writer should receive exactly the characters we wrote.
        verify(mockUnderlyingWriter).write(eq(data), eq(0), eq(data.length));
        verifyNoMoreInteractions(mockUnderlyingWriter);
    }

    @Test
    void write_WhenLengthExceedsBuffer_WritesDirectlyToUnderlyingWriter() throws IOException {
        // Length 5 > BUFFER_SIZE (4) forces the direct‑write path.
        char[] data = "abcde".toCharArray();
        fastWriter.write(data, 0, data.length);
        // The FastWriter should have delegated the write directly.
        verify(mockUnderlyingWriter).write(eq(data), eq(0), eq(data.length));
        verifyNoMoreInteractions(mockUnderlyingWriter);
    }

    @Test
    void write_WhenRemainingSpaceInsufficient_FlushesBufferThenCopies() throws IOException {
        // First fill the buffer partially.
        // 2 chars, buffer size 4 -> 2 slots left
        char[] first = "ab".toCharArray();
        fastWriter.write(first, 0, first.length);
        verifyNoInteractions(mockUnderlyingWriter);
        // Now write more than the remaining space (3 chars). This should trigger a flush
        // of the existing buffer and then copy the new chars into the (now empty) buffer.
        // 3 chars > remaining 2
        char[] second = "cde".toCharArray();
        fastWriter.write(second, 0, second.length);
        // The flush should have caused the first two chars to be written.
        verify(mockUnderlyingWriter).write(eq(first), eq(0), eq(first.length));
        // The second write does not exceed the buffer size, so it should not be written yet.
        verifyNoMoreInteractions(mockUnderlyingWriter);
        // Finally, flush the writer to emit the remaining buffered chars.
        fastWriter.flush();
        // The remaining chars should now be written.
        verify(mockUnderlyingWriter).write(eq(second), eq(0), eq(second.length));
        verifyNoMoreInteractions(mockUnderlyingWriter);
    }

    @Test
    void write_WithInvalidParameters_ThrowsIndexOutOfBoundsException() {
        char[] data = "abc".toCharArray();
        // off < 0
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriter.write(data, -1, 1));
        // len < 0
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriter.write(data, 0, -1));
        // off + len > array length
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriter.write(data, 2, 2));
        // off > array length
        assertThrows(IndexOutOfBoundsException.class, () -> fastWriter.write(data, 4, 0));
    }

    /**
     * When the internal {@code out} writer is {@code null} the method must
     * throw an {@link IOException} with the exact message defined in the
     * implementation.
     */
    @Test
    void write_WithNullUnderlyingWriter_ThrowsIOException() throws Exception {
        // Use reflection to set the private field 'out' to null after construction.
        Field outField = FastWriter.class.getDeclaredField("out");
        outField.setAccessible(true);
        outField.set(fastWriter, null);
        char[] data = new char[] { 'a', 'b', 'c' };
        IOException ex = assertThrows(IOException.class, () -> fastWriter.write(data, 0, 3));
        assertEquals("FastWriter stream is closed.", ex.getMessage());
    }

    /**
     * Passing a {@code null} character array must result in a
     * {@link NullPointerException}.  This occurs before any index checks.
     */
    @Test
    void write_WithNullCharArray_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> fastWriter.write(null, 0, 1));
    }

    /**
     * Parameterised test that covers all index‑validation scenarios from the
     * JSON description.  Each case supplies {@code cbuf}, {@code off},
     * {@code len} and the expected exception type.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("indexValidationProvider")
    void write_IndexValidation(String description, char[] cbuf, int off, int len, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> fastWriter.write(cbuf, off, len));
    }

    /**
     * Verify that a zero‑length write returns normally without interacting
     * with the underlying writer.  This case is also exercised in the original
     * test suite but is kept here for completeness with the JSON spec.
     */
    @Test
    void write_ZeroLength_NoInteraction() throws IOException {
        char[] data = "abcd".toCharArray();
        fastWriter.write(data, 0, 0);
        verifyNoInteractions(mockUnderlyingWriter);
    }

    /**
     * When the requested length is greater than or equal to the internal
     * buffer size, the buffer must be flushed first and the data written
     * directly to the underlying writer.
     */
    @Test
    void write_LengthEqualsBuffer_FlushesAndWritesDirectly() throws IOException {
        // Buffer size is 4, so length 4 triggers the direct‑write path.
        char[] data = "1234".toCharArray();
        fastWriter.write(data, 0, data.length);
        // The direct write should have been performed exactly once.
        verify(mockUnderlyingWriter).write(eq(data), eq(0), eq(data.length));
        verifyNoMoreInteractions(mockUnderlyingWriter);
    }

    /**
     * When the length fits into the remaining buffer space, the characters
     * are copied into the internal buffer and no flush occurs until the
     * buffer becomes full or {@link FastWriter#flush()} / {@link FastWriter#close()}
     * is called.
     */
    @Test
    void write_FitsInRemainingSpace_NoFlushUntilLater() throws IOException {
        // First fill half the buffer.
        fastWriter.write("ab".toCharArray(), 0, 2);
        verifyNoInteractions(mockUnderlyingWriter);
        // Now write 2 more characters – they fit exactly into the remaining space.
        fastWriter.write("cd".toCharArray(), 0, 2);
        // Still no interaction because the buffer is only flushed on explicit flush/close.
        verifyNoInteractions(mockUnderlyingWriter);
        // Flush should now emit the combined 4 characters.
        fastWriter.flush();
        verify(mockUnderlyingWriter).write(eq("abcd".toCharArray()), eq(0), eq(4));
        verifyNoMoreInteractions(mockUnderlyingWriter);
    }

    /**
     * When the length is greater than the remaining space but still smaller
     * than the total buffer capacity, the writer must flush the existing
     * contents first, then copy the new characters into the now‑empty buffer.
     */
    @Test
    void write_InsufficientRemainingSpace_FlushesThenBuffers() throws IOException {
        // Fill buffer partially (3 chars, leaving 1 slot).
        fastWriter.write("abc".toCharArray(), 0, 3);
        verifyNoInteractions(mockUnderlyingWriter);
        // Write 2 chars – this exceeds the remaining 1 slot, causing a flush.
        fastWriter.write("de".toCharArray(), 0, 2);
        // The first three chars should have been flushed.
        verify(mockUnderlyingWriter).write(eq("abc".toCharArray()), eq(0), eq(3));
        verifyNoMoreInteractions(mockUnderlyingWriter);
        // The second write should now sit in the buffer; flush to emit them.
        fastWriter.flush();
        verify(mockUnderlyingWriter).write(eq("de".toCharArray()), eq(0), eq(2));
        verifyNoMoreInteractions(mockUnderlyingWriter);
    }
}
