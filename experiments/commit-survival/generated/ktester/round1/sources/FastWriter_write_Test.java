package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.io.Writer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class FastWriter_write_Test {

    /**
     * Mocked underlying writer used by {@link FastWriter}.
     */
    @Mock
    private Writer mockOut;

    /**
     * Instance under test.
     */
    private FastWriter fastWriter;

    /**
     * Small buffer size to make overflow testing easy.
     */
    private static final int SMALL_BUFFER = 2;

    /**
     * Larger buffer used for some tests (size 5).
     */
    private static final int MEDIUM_BUFFER = 5;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock automatically because of the @Mock annotation.
        // Initialise the FastWriter with the mocked Writer and a tiny buffer.
        fastWriter = new FastWriter(mockOut, SMALL_BUFFER);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the FastWriter is closed after each test to avoid side‑effects.
        if (fastWriter != null) {
            fastWriter.close();
        }
    }

    /**
     * Retrieves the private {@code cb} field.
     */
    private char[] getBuffer(FastWriter writer) throws Exception {
        Field f = FastWriter.class.getDeclaredField("cb");
        f.setAccessible(true);
        return (char[]) f.get(writer);
    }

    /**
     * Retrieves the private {@code nextChar} field.
     */
    private int getNextChar(FastWriter writer) throws Exception {
        Field f = FastWriter.class.getDeclaredField("nextChar");
        f.setAccessible(true);
        return f.getInt(writer);
    }

    /**
     * Sets the private {@code nextChar} field.
     */
    private void setNextChar(FastWriter writer, int value) throws Exception {
        Field f = FastWriter.class.getDeclaredField("nextChar");
        f.setAccessible(true);
        f.setInt(writer, value);
    }

    /**
     * Sets the private {@code cb} field (used for the NPE test).
     */
    private void setBuffer(FastWriter writer, char[] buffer) throws Exception {
        Field f = FastWriter.class.getDeclaredField("cb");
        f.setAccessible(true);
        f.set(writer, buffer);
    }

    /**
     * Verify that a single character write stores the character in the internal
     * buffer and does not immediately invoke any write operation on the underlying
     * {@link Writer}.
     */
    @Test
    void writeSingleChar_DoesNotFlushImmediately() throws IOException {
        fastWriter.write('A');
        // The underlying writer should not have been called yet because the buffer
        // has not reached its capacity.
        verify(mockOut, never()).write(any(char[].class), anyInt(), anyInt());
        verify(mockOut, never()).flush();
    }

    /**
     * When the FastWriter is constructed with a {@code null} underlying writer,
     * any attempt to write should result in an {@link IOException}.
     */
    @Test
    void writeWhenUnderlyingWriterIsNull_ThrowsIOException() {
        FastWriter writerWithNull = new FastWriter(null, SMALL_BUFFER);
        IOException ex = assertThrows(IOException.class, () -> writerWithNull.write('B'));
        assertEquals("FastWriter stream is closed.", ex.getMessage());
    }

    /**
     * Fill the buffer to its limit and then write one more character. This should
     * cause the internal buffer to be flushed, which results in a call to the
     * underlying writer's {@code write(char[],int,int)} method.
     */
    @Test
    void writeBeyondBufferCapacity_TriggersFlush() throws IOException {
        // Fill the buffer (size = 2)
        fastWriter.write('X');
        fastWriter.write('Y');
        // At this point the buffer is full but not yet flushed.
        verify(mockOut, never()).write(any(char[].class), anyInt(), anyInt());
        // Writing the third character should cause a flush.
        fastWriter.write('Z');
        // Verify that the underlying writer received a write call at least once.
        verify(mockOut, atLeastOnce()).write(any(char[].class), eq(0), anyInt());
    }

    /**
     * Group: write_whenStreamClosed
     *
     * Parameterised version of the "stream closed" test – verifies that any
     * integer value results in the same IOException when the underlying writer
     * is {@code null}.
     */
    @ParameterizedTest
    @ValueSource(ints = { 65, 0, 128512 })
    void write_whenStreamClosed_throwsIOException(int c) {
        FastWriter writerWithNull = new FastWriter(null, SMALL_BUFFER);
        IOException ex = assertThrows(IOException.class, () -> writerWithNull.write(c));
        assertEquals("FastWriter stream is closed.", ex.getMessage());
    }

    /**
     * Group: write_whenBufferHasSpace
     *
     * Verify that a character is stored correctly when the buffer still has
     * capacity and that no flush occurs.
     */
    @Test
    void write_whenBufferHasSpace_storesCharWithoutFlush() throws Exception {
        // buffer size = 2, nextChar becomes 1
        fastWriter.write('Z');
        // Verify internal state via reflection
        char[] buffer = getBuffer(fastWriter);
        int next = getNextChar(fastWriter);
        assertEquals('Z', buffer[0], "Character should be stored at index 0");
        assertEquals(1, next, "nextChar should be incremented to 1");
        // No interaction with the underlying writer yet
        verify(mockOut, never()).write(any(char[].class), anyInt(), anyInt());
    }

    /**
     * Group: write_whenBufferFullFlushes
     *
     * When the buffer is full, {@code write(int)} must first flush the buffer,
     * then store the new character at position 0 and set {@code nextChar} to 1.
     */
    @Test
    void write_whenBufferFullFlushes_flushesAndStoresChar() throws Exception {
        // Fill the buffer completely (size = 2)
        fastWriter.write('a');
        fastWriter.write('b');
        // Configure the mock to capture the data written during flush
        ArgumentCaptor<char[]> captor = ArgumentCaptor.forClass(char[].class);
        doNothing().when(mockOut).write(captor.capture(), eq(0), anyInt());
        // Write a third character – this should trigger flushBuffer()
        fastWriter.write('c');
        // Verify that flushBuffer caused a write to the underlying writer
        verify(mockOut, times(1)).write(any(char[].class), eq(0), anyInt());
        // The flushed data should contain the first two characters ('a','b')
        char[] flushed = captor.getValue();
        assertEquals('a', flushed[0]);
        assertEquals('b', flushed[1]);
        // After the flush, the new character should be at position 0
        char[] buffer = getBuffer(fastWriter);
        int next = getNextChar(fastWriter);
        assertEquals('c', buffer[0], "New character should be stored at index 0 after flush");
        assertEquals(1, next, "nextChar should be reset to 1 after flush");
    }

    /**
     * Group: write_characterCasting
     *
     * Verify that a code point larger than the BMP is cast to {@code char}
     * correctly (the high‑surrogate part is stored).
     */
    @Test
    void write_characterCasting_storesHighSurrogate() throws Exception {
        // 😀 (U+1F600) -> high surrogate 0xD83D
        int codePoint = 128512;
        fastWriter.write(codePoint);
        char[] buffer = getBuffer(fastWriter);
        int next = getNextChar(fastWriter);
        assertEquals((char) codePoint, buffer[0], "Stored char should be the high surrogate part of the code point");
        assertEquals(1, next, "nextChar should be 1 after writing one character");
        verify(mockOut, never()).write(any(char[].class), anyInt(), anyInt());
    }

    /**
     * Group: write_nullCharacter
     *
     * The NUL character should be treated like any other character.
     */
    @Test
    void write_nullCharacter_storedCorrectly() throws Exception {
        // NUL
        fastWriter.write(0);
        char[] buffer = getBuffer(fastWriter);
        int next = getNextChar(fastWriter);
        assertEquals('\0', buffer[0], "NUL character should be stored at index 0");
        assertEquals(1, next, "nextChar should be incremented to 1");
        verify(mockOut, never()).write(any(char[].class), anyInt(), anyInt());
    }

    /**
     * Group: write_multipleWrites_noFlush
     *
     * Pre‑condition: buffer length = 5, nextChar = 2 with existing characters.
     * Verify that writing another character stores it at index 2 without flushing.
     */
    @Test
    void write_multipleWrites_noFlush_respectsExistingState() throws Exception {
        // Re‑create writer with a larger buffer
        fastWriter = new FastWriter(mockOut, MEDIUM_BUFFER);
        // Manually set up existing state: 'A' at 0, 'C' at 1, nextChar = 2
        char[] buffer = getBuffer(fastWriter);
        buffer[0] = 'A';
        buffer[1] = 'C';
        setNextChar(fastWriter, 2);
        // Write 'B' (66)
        fastWriter.write('B');
        // Verify new state
        assertEquals('B', buffer[2], "Character 'B' should be stored at index 2");
        assertEquals(3, getNextChar(fastWriter), "nextChar should become 3");
        // No flush should have happened
        verify(mockOut, never()).write(any(char[].class), anyInt(), anyInt());
    }

    /**
     * Group: write_flushBufferIOException
     *
     * Simulate an IOException thrown from the underlying writer during the flush
     * operation and verify that the exception propagates out of {@code write(int)}.
     */
    @Test
    void write_flushBufferIOException_propagatesException() throws Exception {
        // Fill the buffer to force a flush on the next write
        fastWriter.write('x');
        fastWriter.write('y');
        // Configure the mock to throw when the writer is asked to write the buffer
        doThrow(new IOException("forced error")).when(mockOut).write(any(char[].class), eq(0), anyInt());
        // The third write should attempt to flush and thus throw
        IOException ex = assertThrows(IOException.class, () -> fastWriter.write('z'));
        assertEquals("forced error", ex.getMessage());
    }

    /**
     * Group: write_whenBufferNull
     *
     * If the internal character buffer {@code cb} is {@code null}, a
     * {@link NullPointerException} should be thrown before any other logic.
     */
    @Test
    void write_whenBufferNull_throwsNullPointerException() throws Exception {
        // Set the internal buffer to null via reflection
        setBuffer(fastWriter, null);
        // Attempt to write – NPE is expected
        assertThrows(NullPointerException.class, () -> fastWriter.write('A'));
    }
}
