package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Reader;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class FastReader_pushback_Test {

    // Mocked underlying Reader that FastReader wraps
    @Mock
    private Reader mockIn;

    // Instance under test – created fresh for each test method
    private FastReader fastReader;

    /**
     * For every test we create a new {@link FastReader} with a small
     * buffer and a push‑back buffer that is large enough for the
     * scenarios we want to verify.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The concrete buffer sizes are not important for the logic
        // we are testing; they just need to be >0.
        fastReader = new FastReader(mockIn, 8, 2);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock between tests to avoid cross‑test interference.
        reset(mockIn);
    }

    /**
     * Sets a private field on the given target object.
     */
    private static void setPrivateField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Retrieves a private field value from the given target object.
     */
    private static Object getPrivateField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Convenience wrapper to get the pushback buffer as a char array.
     */
    private static char[] getPushbackBuffer(FastReader fr) throws Exception {
        return (char[]) getPrivateField(fr, "pushbackBuffer");
    }

    /**
     * Convenience wrapper to get the current pushback position.
     */
    private static int getPushbackPosition(FastReader fr) throws Exception {
        return (int) getPrivateField(fr, "pushbackPosition");
    }

    /**
     * Verifies that calling {@code pushback(char)} with a non‑newline
     * character decrements the column counter.
     */
    @Test
    void pushbackDecrementsColumnForNonNewline() throws IOException {
        // Arrange – the underlying reader will deliver a single character 'a'
        when(mockIn.read()).thenReturn('a').thenReturn(-1);
        // Act – read one character so that the column becomes 1
        int read = fastReader.read();
        assertEquals('a', read);
        assertEquals(1, fastReader.getCol(), "Column should be 1 after reading one char");
        // Now push the same character back
        fastReader.pushback((char) read);
        // Assert – column must have been decremented back to 0
        assertEquals(0, fastReader.getCol(), "Column should be decremented after pushback");
    }

    /**
     * Verifies that calling {@code pushback(char)} with a line‑feed
     * character decrements the line counter.
     */
    @Test
    void pushbackDecrementsLineForNewline() throws IOException {
        // Arrange – the underlying reader will deliver a newline (0x0a)
        when(mockIn.read()).thenReturn('\n').thenReturn(-1);
        // Act – read the newline; FastReader should have advanced the line number
        int read = fastReader.read();
        assertEquals('\n', read);
        assertEquals(2, fastReader.getLine(), "Line should be incremented after reading a newline");
        // Push the newline back
        fastReader.pushback('\n');
        // Assert – line number must be back to the original value (1)
        assertEquals(1, fastReader.getLine(), "Line should be decremented after pushback of newline");
    }

    /**
     * Verifies that an {@link IOException} is thrown when the push‑back
     * buffer is exhausted.
     */
    @Test
    void pushbackBufferOverflowThrowsIOException() throws IOException {
        // Arrange – create a FastReader with a push‑back buffer of size 1
        FastReader smallPushbackReader = new FastReader(mockIn, 8, 1);
        // First pushback succeeds (buffer has one free slot)
        smallPushbackReader.pushback('x');
        // Second pushback must fail because the buffer is now full
        IOException ex = assertThrows(IOException.class, () -> smallPushbackReader.pushback('y'), "Expected IOException when pushback buffer overflows");
        assertEquals("Pushback buffer overflow", ex.getMessage());
    }

    /**
     * Pushback normal – newline character (0x0A).
     * With a non‑empty buffer (pushbackPosition = 1) and line = 5, col = 10,
     * pushing back a newline should store the char, decrement the line
     * counter and leave the column counter unchanged.
     */
    @Test
    void pushbackNewlineNormal() throws Exception {
        // arrange internal state
        setPrivateField(fastReader, "pushbackPosition", 1);
        setPrivateField(fastReader, "line", 5);
        setPrivateField(fastReader, "col", 10);
        // act
        fastReader.pushback('\n');
        // assert
        assertEquals(0, getPushbackPosition(fastReader), "pushbackPosition should be decremented to 0");
        assertEquals('\n', getPushbackBuffer(fastReader)[0], "buffer[0] should contain the newline");
        assertEquals(4, fastReader.getLine(), "Line should be decremented by 1");
        assertEquals(10, fastReader.getCol(), "Column should remain unchanged");
    }

    /**
     * Pushback normal – non‑newline character.
     * With pushbackPosition = 2, line = 3, col = 7, pushing back a regular
     * character should store the char, decrement the column counter and leave
     * the line counter unchanged.
     */
    @Test
    void pushbackNonNewlineNormal() throws Exception {
        setPrivateField(fastReader, "pushbackPosition", 2);
        setPrivateField(fastReader, "line", 3);
        setPrivateField(fastReader, "col", 7);
        fastReader.pushback('x');
        assertEquals(1, getPushbackPosition(fastReader), "pushbackPosition should become 1");
        assertEquals('x', getPushbackBuffer(fastReader)[1], "buffer[1] should contain 'x'");
        assertEquals(3, fastReader.getLine(), "Line should stay unchanged");
        assertEquals(6, fastReader.getCol(), "Column should be decremented by 1");
    }

    /**
     * Multiple sequential pushbacks.
     * First pushback('b') then pushback('\\n') on a full buffer.
     */
    @Test
    void multipleSequentialPushbacks() throws Exception {
        // start with a full buffer (size 2) – pushbackPosition = 2
        setPrivateField(fastReader, "pushbackPosition", 2);
        setPrivateField(fastReader, "line", 5);
        setPrivateField(fastReader, "col", 8);
        // first pushback('b')
        fastReader.pushback('b');
        assertEquals(1, getPushbackPosition(fastReader));
        assertEquals('b', getPushbackBuffer(fastReader)[1]);
        assertEquals(5, fastReader.getLine());
        assertEquals(7, fastReader.getCol());
        // second pushback('\n')
        fastReader.pushback('\n');
        assertEquals(0, getPushbackPosition(fastReader));
        assertEquals('\n', getPushbackBuffer(fastReader)[0]);
        assertEquals(4, fastReader.getLine(), "Line should be decremented after newline pushback");
        assertEquals(7, fastReader.getCol(), "Column should stay unchanged after newline pushback");
    }

    /**
     * Pushback at buffer limit (non‑newline).
     * Buffer size = 5, pushbackPosition starts at 5 (full).
     */
    @Test
    void pushbackAtBufferLimitNonNewline() throws Exception {
        FastReader reader = new FastReader(mockIn, 8, 5);
        setPrivateField(reader, "pushbackPosition", 5);
        setPrivateField(reader, "line", 10);
        setPrivateField(reader, "col", 8);
        reader.pushback('z');
        assertEquals(4, getPushbackPosition(reader));
        assertEquals('z', getPushbackBuffer(reader)[4]);
        assertEquals(10, reader.getLine());
        assertEquals(7, reader.getCol());
    }

    /**
     * Pushback with line counter at 1 and newline character.
     */
    @Test
    void pushbackLineCounterAtOneWithNewline() throws Exception {
        setPrivateField(fastReader, "pushbackPosition", 2);
        setPrivateField(fastReader, "line", 1);
        setPrivateField(fastReader, "col", 5);
        fastReader.pushback('\n');
        assertEquals(1, getPushbackPosition(fastReader));
        assertEquals('\n', getPushbackBuffer(fastReader)[1]);
        assertEquals(0, fastReader.getLine(), "Line should become 0");
        assertEquals(5, fastReader.getCol(), "Column should stay unchanged");
    }

    /**
     * Pushback with column counter at 0 and non‑newline character.
     */
    @Test
    void pushbackColumnAtZeroNonNewline() throws Exception {
        setPrivateField(fastReader, "pushbackPosition", 3);
        setPrivateField(fastReader, "line", 4);
        setPrivateField(fastReader, "col", 0);
        fastReader.pushback('c');
        assertEquals(2, getPushbackPosition(fastReader));
        assertEquals('c', getPushbackBuffer(fastReader)[2]);
        assertEquals(4, fastReader.getLine());
        assertEquals(-1, fastReader.getCol(), "Column may become negative");
    }

    /**
     * Pushback with null character (\\0). Treated like any other non‑newline.
     */
    @Test
    void pushbackNullCharacter() throws Exception {
        setPrivateField(fastReader, "pushbackPosition", 2);
        setPrivateField(fastReader, "line", 7);
        setPrivateField(fastReader, "col", 3);
        fastReader.pushback('\0');
        assertEquals(1, getPushbackPosition(fastReader));
        assertEquals('\0', getPushbackBuffer(fastReader)[1]);
        assertEquals(7, fastReader.getLine());
        assertEquals(2, fastReader.getCol());
    }

    /**
     * Pushback buffer overflow after multiple pushes.
     * Start with two free slots, push two characters successfully,
     * third push must throw IOException.
     */
    @Test
    void pushbackOverflowAfterMultiplePushes() throws IOException {
        FastReader reader = new FastReader(mockIn, 8, 2);
        // initial pushbackPosition = 2 (buffer empty)
        // first push
        assertDoesNotThrow(() -> reader.pushback('x'));
        // second push
        assertDoesNotThrow(() -> reader.pushback('y'));
        // third push should overflow
        IOException ex = assertThrows(IOException.class, () -> reader.pushback('z'), "Expected overflow after three pushes into a size‑2 buffer");
        assertEquals("Pushback buffer overflow", ex.getMessage());
    }

    /**
     * Multiple sequential pushbacks mixing newline and non‑newline at buffer edge.
     */
    @Test
    void mixedSequentialPushbacksAtEdge() throws Exception {
        // Buffer size 3, start full (position = 3)
        FastReader reader = new FastReader(mockIn, 8, 3);
        setPrivateField(reader, "pushbackPosition", 3);
        setPrivateField(reader, "line", 6);
        setPrivateField(reader, "col", 4);
        // 1) push newline
        reader.pushback('\n');
        assertEquals(2, getPushbackPosition(reader));
        assertEquals('\n', getPushbackBuffer(reader)[2]);
        assertEquals(5, reader.getLine());
        assertEquals(4, reader.getCol());
        // 2) push 'a'
        reader.pushback('a');
        assertEquals(1, getPushbackPosition(reader));
        assertEquals('a', getPushbackBuffer(reader)[1]);
        assertEquals(5, reader.getLine());
        assertEquals(3, reader.getCol());
        // 3) push another newline
        reader.pushback('\n');
        assertEquals(0, getPushbackPosition(reader));
        assertEquals('\n', getPushbackBuffer(reader)[0]);
        assertEquals(4, reader.getLine());
        assertEquals(3, reader.getCol());
    }

    /**
     * Pushback with negative pushbackPosition (simulated internal corruption).
     * Expect an IndexOutOfBoundsException (or ArrayIndexOutOfBoundsException)
     * because the method will try to write to a negative array index.
     */
    @Test
    void pushbackWithCorruptedNegativePosition() throws Exception {
        FastReader reader = new FastReader(mockIn, 8, 5);
        // corrupt state
        setPrivateField(reader, "pushbackPosition", -1);
        // The exact exception type depends on JVM, so we accept either.
        assertThrows(RuntimeException.class, () -> {
            try {
                reader.pushback('a');
            } catch (Exception e) {
                // rethrow to satisfy assertThrows
                throw e;
            }
        }, "Expected an index‑out‑of‑bounds exception due to negative pushbackPosition");
    }
}
