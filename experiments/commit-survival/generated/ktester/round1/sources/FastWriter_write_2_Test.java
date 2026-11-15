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
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastWriter_write_2_Test {

    /**
     * The FastWriter instance under test.
     */
    private FastWriter fastWriter;

    /**
     * Mocked underlying Writer that FastWriter delegates to.
     */
    @Mock
    private Writer mockOut;

    /**
     * Use a very small buffer so that we can easily trigger a flush
     * when the amount of data written exceeds the buffer size.
     */
    private static final int SMALL_BUFFER = 4;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us (annotated with @Mock).
        // Initialise FastWriter with the mock and a tiny buffer.
        fastWriter = new FastWriter(mockOut, SMALL_BUFFER);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that no unexpected interactions have occurred.
        // Individual tests verify the interactions they care about.
        verifyNoMoreInteractions(mockOut);
    }

    /**
     * Helper to set a private field via reflection.
     */
    private static void setPrivateField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper to get a private field via reflection.
     */
    private static Object getPrivateField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> invalidArgumentProvider() {
        return Stream.of(Arguments.of("abc", -1, 2, StringIndexOutOfBoundsException.class), Arguments.of("abc", 1, -1, StringIndexOutOfBoundsException.class), Arguments.of("short", 3, 5, StringIndexOutOfBoundsException.class));
    }

    /**
     * Verify that writing a string whose length fits entirely inside the
     * internal buffer does **not** cause the underlying Writer to be invoked.
     */
    @Test
    void writeStringFitsInBuffer_NoFlushOccurs() throws IOException {
        // "abc" is 3 characters, smaller than the 4‑character buffer.
        fastWriter.write("abc", 0, 3);
        // The underlying writer should not have been called yet.
        verify(mockOut, never()).write(any(char[].class), anyInt(), anyInt());
    }

    /**
     * Verify that when the amount of data exceeds the buffer size,
     * FastWriter flushes the full buffer to the underlying Writer.
     * The remaining characters stay in the buffer for a later flush.
     */
    @Test
    void writeStringExceedsBuffer_TriggersFlush() throws IOException {
        // "abcdef" is 6 characters; with a buffer of 4 we expect one flush.
        fastWriter.write("abcdef", 0, 6);
        // Capture the arguments passed to the underlying writer.
        ArgumentCaptor<char[]> captor = ArgumentCaptor.forClass(char[].class);
        verify(mockOut, times(1)).write(captor.capture(), eq(0), eq(4));
        // The flushed characters must be the first four of the original string.
        char[] flushed = captor.getValue();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, flushed, "FastWriter should flush the first bufferSize characters");
        // The remaining two characters ('e','f') stay in the internal buffer,
        // therefore no additional write calls should have been made.
        verify(mockOut, never()).write(any(char[].class), eq(0), eq(2));
    }

    /**
     * After the FastWriter is closed, any subsequent write operation must
     * throw an {@link IOException}.
     */
    @Test
    void writeAfterClose_ThrowsIOException() throws IOException {
        fastWriter.close();
        IOException ex = assertThrows(IOException.class, () -> fastWriter.write("x", 0, 1), "Writing after close should throw IOException");
        assertEquals("FastWriter stream is closed.", ex.getMessage());
    }

    @Test
    void writeWhenOutIsNull_ThrowsIOException() throws Exception {
        // Simulate a FastWriter whose underlying writer has been set to null.
        setPrivateField(fastWriter, "out", null);
        IOException ex = assertThrows(IOException.class, () -> fastWriter.write("any", 0, 1));
        assertEquals("FastWriter stream is closed.", ex.getMessage());
    }

    @Test
    void writeWholeString_FitsInBuffer_FlushWritesAll() throws IOException {
        // Buffer is large enough (SMALL_BUFFER = 4) to hold the whole string.
        // Use a larger buffer via reflection to avoid intermediate flushes.
        setPrivateField(fastWriter, "cb", new char[20]);
        fastWriter.write("Hello World", 0, 11);
        // explicit flush to push remaining chars
        fastWriter.flush();
        // Verify that the underlying writer receives the exact characters.
        verify(mockOut).write(argThat(arr -> {
            String s = new String(arr, 0, 11);
            return "Hello World".equals(s);
        }), eq(0), eq(11));
    }

    @Test
    void writeSubstring_OffsetAndLength_FlushWritesOnlySubstring() throws IOException {
        // Ensure buffer is big enough.
        setPrivateField(fastWriter, "cb", new char[20]);
        // "Writer"
        fastWriter.write("FastWriterTest", 4, 6);
        fastWriter.flush();
        verify(mockOut).write(argThat(arr -> {
            String s = new String(arr, 0, 6);
            return "Writer".equals(s);
        }), eq(0), eq(6));
    }

    @Test
    void writeZeroLength_NoInteraction() throws IOException {
        fastWriter.write("NoOp", 0, 0);
        fastWriter.flush();
        // No write should have been delegated because nothing was buffered.
        verify(mockOut, never()).write(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void writeExceedsSmallBuffer_TwoAutomaticFlushes() throws Exception {
        // Force internal buffer size to 5 via reflection.
        setPrivateField(fastWriter, "cb", new char[5]);
        // 10 chars, buffer 5 -> 2 flushes
        fastWriter.write("ABCDEFGHIJ", 0, 10);
        // flush any remainder (should be none)
        fastWriter.flush();
        // Verify two writes of 5 chars each.
        InOrder inOrder = inOrder(mockOut);
        inOrder.verify(mockOut).write(argThat(arr -> new String(arr, 0, 5).equals("ABCDE")), eq(0), eq(5));
        inOrder.verify(mockOut).write(argThat(arr -> new String(arr, 0, 5).equals("FGHIJ")), eq(0), eq(5));
    }

    @Test
    void writeExactlyBufferSize_TriggersSingleFlush() throws Exception {
        // Buffer size exactly 5.
        setPrivateField(fastWriter, "cb", new char[5]);
        // fills buffer, should flush at end of loop
        fastWriter.write("12345", 0, 5);
        // flush any remainder (none expected)
        fastWriter.flush();
        verify(mockOut).write(argThat(arr -> new String(arr, 0, 5).equals("12345")), eq(0), eq(5));
    }

    @ParameterizedTest(name = "{index}: str=\"{0}\", off={1}, len={2}")
    @MethodSource("invalidArgumentProvider")
    void writeWithInvalidArguments_ThrowsException(String str, int off, int len, Class<? extends Exception> expected) {
        Exception ex = assertThrows(expected, () -> fastWriter.write(str, off, len));
        // No further assertions needed; the type of exception is enough.
    }

    @Test
    void writeAfterExplicitClose_ThrowsIOException() throws IOException {
        // sets out to null internally
        fastWriter.close();
        IOException ex = assertThrows(IOException.class, () -> fastWriter.write("afterClose", 0, 10));
        assertEquals("FastWriter stream is closed.", ex.getMessage());
    }

    @Test
    void writeNullString_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> fastWriter.write(null, 0, 1));
    }
}
