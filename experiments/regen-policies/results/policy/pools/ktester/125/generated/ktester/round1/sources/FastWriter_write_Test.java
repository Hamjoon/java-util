package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Writer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class FastWriter_write_Test {

    @Mock
    Writer // mock underlying writer used by FastWriter
    mockWriter;

    @AfterEach
    void teardownAfterEach() {
        // Reset mock after each test to avoid cross‑test interference
        reset(mockWriter);
    }

    /**
     * Verify that {@code write(int)} throws an {@link IOException}
     * when the FastWriter has been closed (its internal stream is null).
     */
    @Test
    void writeThrowsWhenClosed() throws IOException {
        // arrange – create a FastWriter and close it
        FastWriter fastWriter = new FastWriter(mockWriter);
        // close() sets the internal 'out' to null
        fastWriter.close();
        // act & assert
        assertThrows(IOException.class, () -> fastWriter.write('A'));
    }

    /**
     * Verify that a single character written is stored in the internal buffer
     * and later flushed to the underlying writer when {@code flush()} is called.
     */
    @Test
    void writeAddsCharToBufferAndFlushesOnFlush() throws IOException {
        // arrange – use the default buffer size (large enough for a single char)
        FastWriter fastWriter = new FastWriter(mockWriter);
        // act – write a character and then flush
        fastWriter.write('Z');
        fastWriter.flush();
        // assert – the underlying writer should receive exactly one character 'Z'
        ArgumentCaptor<char[]> captor = ArgumentCaptor.forClass(char[].class);
        verify(mockWriter, times(1)).write(captor.capture(), eq(0), eq(1));
        char[] written = captor.getValue();
        assertEquals('Z', written[0]);
    }

    /**
     * Verify that when the internal buffer becomes full, {@code write(int)} triggers
     * a flush of the current buffer before storing the new character.
     *
     * The test uses a small buffer size (2) to force the overflow condition.
     */
    @Test
    void writeTriggersFlushWhenBufferIsFull() throws IOException {
        // arrange – buffer size of 2 characters
        FastWriter fastWriter = new FastWriter(mockWriter, 2);
        // act – write three characters; the third write should cause a flush of the first two
        // buffer: [A]
        fastWriter.write('A');
        // buffer: [A, B] – now full
        fastWriter.write('B');
        // should flush [A, B] then store 'C' in buffer
        fastWriter.write('C');
        // flush remaining data (the 'C')
        fastWriter.flush();
        // assert – verify order of writes to the underlying writer
        InOrder inOrder = inOrder(mockWriter);
        // first flush: A and B
        ArgumentCaptor<char[]> firstCaptor = ArgumentCaptor.forClass(char[].class);
        inOrder.verify(mockWriter).write(firstCaptor.capture(), eq(0), eq(2));
        char[] firstWrite = firstCaptor.getValue();
        assertEquals('A', firstWrite[0]);
        assertEquals('B', firstWrite[1]);
        // second flush: C
        ArgumentCaptor<char[]> secondCaptor = ArgumentCaptor.forClass(char[].class);
        inOrder.verify(mockWriter).write(secondCaptor.capture(), eq(0), eq(1));
        char[] secondWrite = secondCaptor.getValue();
        assertEquals('C', secondWrite[0]);
        // no more interactions
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Normal write when there is still space in the buffer.
     * The character should be stored at {@code cb[nextChar]} and {@code nextChar}
     * must be incremented. No interaction with the underlying writer should happen.
     */
    @Test
    void writeWithinBufferStoresCharAndDoesNotFlush() throws Exception {
        // ample space
        FastWriter fastWriter = new FastWriter(mockWriter, 10);
        // write character 'A' (code 65)
        fastWriter.write(65);
        // Use reflection to inspect private fields
        Field cbField = FastWriter.class.getDeclaredField("cb");
        cbField.setAccessible(true);
        char[] buffer = (char[]) cbField.get(fastWriter);
        Field nextCharField = FastWriter.class.getDeclaredField("nextChar");
        nextCharField.setAccessible(true);
        int nextChar = (int) nextCharField.get(fastWriter);
        assertEquals('A', buffer[0], "First buffer slot should contain 'A'");
        assertEquals(1, nextChar, "nextChar should be incremented to 1");
        // verify that no write was delegated to the mock yet
        verifyNoInteractions(mockWriter);
    }

    /**
     * Write a code point that does not fit into a {@code char}.
     * The method casts the int to {@code char}, discarding high bits.
     */
    @Test
    void writeTruncatesHighBitsWhenCodePointBeyondBMP() throws Exception {
        FastWriter fastWriter = new FastWriter(mockWriter, 5);
        // U+1F600 (😀) – outside BMP
        int codePoint = 128512;
        fastWriter.write(codePoint);
        Field cbField = FastWriter.class.getDeclaredField("cb");
        cbField.setAccessible(true);
        char[] buffer = (char[]) cbField.get(fastWriter);
        assertEquals((char) codePoint, buffer[0], "Stored character must be the low 16 bits of the supplied int");
    }

    /**
     * Buffer size of zero forces an {@link ArrayIndexOutOfBoundsException}
     * when the method attempts to write into {@code cb[0]}.
     */
    @Test
    void writeWithZeroLengthBufferThrowsArrayIndexOutOfBoundsException() {
        FastWriter fastWriter = new FastWriter(mockWriter, 0);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> fastWriter.write('C'));
    }

    /**
     * When the internal character buffer {@code cb} is {@code null},
     * any attempt to write must result in a {@link NullPointerException}.
     */
    @Test
    void writeWithNullInternalBufferThrowsNullPointerException() throws Exception {
        FastWriter fastWriter = new FastWriter(mockWriter, 5);
        // Force cb to null via reflection
        Field cbField = FastWriter.class.getDeclaredField("cb");
        cbField.setAccessible(true);
        cbField.set(fastWriter, null);
        assertThrows(NullPointerException.class, () -> fastWriter.write('A'));
    }

    /**
     * If {@code flushBuffer()} throws an {@link IOException} (propagated from the
     * underlying writer), {@code write(int)} must propagate the same exception.
     */
    @Test
    void writeWhenFlushBufferThrowsIOExceptionPropagatesIt() throws IOException {
        // Mock writer that throws IOException on any write call
        doThrow(new IOException("underlying write failed")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());
        // Buffer size 1 – first write fills it, second write triggers flushBuffer
        FastWriter fastWriter = new FastWriter(mockWriter, 1);
        // First character fills the buffer (no flush yet)
        fastWriter.write('X');
        // Second character should cause flush of the first char and then throw
        IOException ex = assertThrows(IOException.class, () -> fastWriter.write('Y'));
        assertEquals("underlying write failed", ex.getMessage());
    }
}
