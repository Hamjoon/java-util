package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FastReader_read_Test {

    // size of the main buffer
    private static final int BUFFER_SIZE = 8;

    // size of the push‑back buffer
    private static final int PUSHBACK_SIZE = 4;

    private FastReader fastReader;

    // the underlying reader used in most tests
    private StringReader source;

    private static class PushbackScenario {

        final char[] buffer;

        final int initialPosition;

        final int declaredSize;

        final char expectedChar;

        final int expectedNewPos;

        PushbackScenario(char[] buffer, int initialPosition, int declaredSize, char expectedChar, int expectedNewPos) {
            this.buffer = buffer;
            this.initialPosition = initialPosition;
            this.declaredSize = declaredSize;
            this.expectedChar = expectedChar;
            this.expectedNewPos = expectedNewPos;
        }
    }

    /**
     * Reader that limits the number of characters returned per read() call.
     */
    private static class LimitedReader extends Reader {

        private final char[] data;

        private int pos = 0;

        private final int maxPerRead;

        LimitedReader(String content, int maxPerRead) {
            this.data = content.toCharArray();
            this.maxPerRead = maxPerRead;
        }

        @Override
        public int read(char[] cbuf, int off, int len) {
            if (pos >= data.length) {
                return -1;
            }
            int toCopy = Math.min(Math.min(len, maxPerRead), data.length - pos);
            System.arraycopy(data, pos, cbuf, off, toCopy);
            pos += toCopy;
            return toCopy;
        }

        @Override
        public void close() {
            /* no‑op */
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // the object exists for tests that replace the source later.
        source = new StringReader("");
        fastReader = new FastReader(source, BUFFER_SIZE, PUSHBACK_SIZE);
        // The concrete source will be created in each test that needs it.
        // Here we just initialise the FastReader with a dummy (empty) source so that
        // Initialise with a dummy source – individual tests will replace it when needed.
        source = new StringReader("");
        fastReader = new FastReader(source, BUFFER_SIZE, PUSHBACK_SIZE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the FastReader (and its underlying source) are closed after each test.
        fastReader.close();
        fastReader.close();
    }

    /**
     * Sets a private field via reflection.
     */
    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the value of a private int field via reflection.
     */
    private static int getIntField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Stream<PushbackScenario> pushbackScenarios() {
        return Stream.of(new PushbackScenario(// buffer content
        new char[] { 'A', 'B', 'C', 'D', 'E' }, // initial position
        0, // declared size
        5, // expected char
        'A', // expected new position
        1), new PushbackScenario(new char[] { 'X', 'Y', 'Z', 'W', 'V' }, 2, 5, 'Z', 3));
    }

    @Test
    void read_throwsIOExceptionWhenClosed() throws IOException {
        // GIVEN: a FastReader that has been closed
        fastReader.close();
        // WHEN / THEN: reading after close must throw IOException
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(), "Expected read() to throw IOException after the stream is closed");
        assertTrue(ex.getMessage().contains("FastReader stream is closed"));
    }

    @Test
    void read_returnsPushedBackCharacter() throws IOException {
        // GIVEN: a FastReader with a push‑back character 'X'
        fastReader.pushback('X');
        // WHEN: we read from the FastReader
        int result = fastReader.read();
        // THEN: the returned character is the one we pushed back
        assertEquals('X', result, "read() should return the character that was pushed back");
    }

    @Test
    void read_returnsCharactersFromUnderlyingReaderWhenNoPushback() throws IOException {
        // GIVEN: an underlying source containing "AB"
        source = new StringReader("AB");
        fastReader = new FastReader(source, BUFFER_SIZE, PUSHBACK_SIZE);
        // WHEN: we read two characters sequentially
        int first = fastReader.read();
        int second = fastReader.read();
        // THEN: the characters match the source content
        assertEquals('A', first, "First character read should be 'A'");
        assertEquals('B', second, "Second character read should be 'B'");
    }

    @Test
    void read_returnsMinusOneAtEof() throws IOException {
        // GIVEN: an empty underlying source and a push‑back buffer of size zero
        source = new StringReader("");
        fastReader = new FastReader(source, BUFFER_SIZE, 0);
        // WHEN: we attempt to read
        int result = fastReader.read();
        // THEN: EOF is indicated by -1
        assertEquals(-1, result, "read() should return -1 when the underlying stream is at EOF");
    }

    @ParameterizedTest
    @MethodSource("pushbackScenarios")
    void read_returnsCharacterFromPushbackBuffer(PushbackScenario s) throws IOException {
        // Initialise a FastReader with a dummy source (won't be used)
        fastReader = new FastReader(new StringReader(""), BUFFER_SIZE, s.declaredSize);
        // Directly inject the push‑back buffer state
        setField(fastReader, "pushbackBuffer", s.buffer);
        setField(fastReader, "pushbackPosition", s.initialPosition);
        setField(fastReader, "pushbackBufferSize", s.declaredSize);
        int result = fastReader.read();
        assertEquals(s.expectedChar, result, "read() should return the character at the current pushback position");
        assertEquals(s.expectedNewPos, getIntField(fastReader, "pushbackPosition"), "pushbackPosition should be incremented after reading");
    }

    @Test
    void read_returnsMinusOneWhenFillDetectsEof() throws Exception {
        // FastReader with empty underlying source; push‑back buffer exhausted
        fastReader = new FastReader(new StringReader(""), BUFFER_SIZE, PUSHBACK_SIZE);
        // Simulate that fill() will set limit = -1 (EOF)
        // We achieve this by using a Reader that returns -1 immediately.
        // exhaust pushback
        setField(fastReader, "pushbackPosition", PUSHBACK_SIZE);
        // invoke read() – it will call fill() which reads -1 from the empty source
        int result = fastReader.read();
        assertEquals(-1, result, "read() must return -1 when underlying stream is at EOF");
    }

    @Test
    void read_returnsFirstTwoCharactersFromUnderlyingReader() throws IOException {
        source = new StringReader("hello");
        fastReader = new FastReader(source, BUFFER_SIZE, PUSHBACK_SIZE);
        int first = fastReader.read();
        int second = fastReader.read();
        assertEquals('h', first);
        assertEquals('e', second);
        // Ensure internal position advanced correctly (position should now be 2)
        assertEquals(2, getIntField(fastReader, "position"));
    }

    @Test
    void read_loadsFromUnderlyingWhenPushbackExhausted_case1() throws IOException {
        // pushback buffer size 3, already exhausted
        fastReader = new FastReader(new StringReader("XYZ"), BUFFER_SIZE, 3);
        // exhausted
        setField(fastReader, "pushbackPosition", 3);
        setField(fastReader, "pushbackBufferSize", 3);
        // No need to set pushbackBuffer (won't be accessed)
        int result = fastReader.read();
        assertEquals('X', result);
    }

    @Test
    void read_loadsFromUnderlyingWhenPushbackExhausted_case2() throws IOException {
        fastReader = new FastReader(new StringReader("AB"), BUFFER_SIZE, 2);
        // exhausted
        setField(fastReader, "pushbackPosition", 2);
        setField(fastReader, "pushbackBufferSize", 2);
        int result = fastReader.read();
        assertEquals('A', result);
    }

    @Test
    void read_ignoresPushbackWhenSizeZero() throws IOException {
        fastReader = new FastReader(new StringReader("hello"), BUFFER_SIZE, 0);
        // pushbackPosition defaults to 0, pushbackBufferSize = 0 → condition false
        int result = fastReader.read();
        assertEquals('h', result);
    }

    @Test
    void read_multipleRefillsPartialConsumption() throws IOException {
        // Custom reader that returns at most 2 characters per read call
        Reader limitedReader = new LimitedReader("1234", 2);
        fastReader = new FastReader(limitedReader, 8, PUSHBACK_SIZE);
        // 1st read → fills "12", returns '1'
        assertEquals('1', fastReader.read());
        // 2nd read → returns remaining '2' without refill
        assertEquals('2', fastReader.read());
        // 3rd read → buffer exhausted, triggers refill → reads "34", returns '3'
        assertEquals('3', fastReader.read());
        // 4th read → returns '4'
        assertEquals('4', fastReader.read());
        // 5th read → EOF
        assertEquals(-1, fastReader.read());
    }

    @Test
    void read_propagatesIOExceptionFromUnderlyingReader(@Mock Reader mockReader) throws Exception {
        // Configure mock to throw on any read operation
        when(mockReader.read(any(char[].class), anyInt(), anyInt())).thenThrow(new IOException("boom"));
        fastReader = new FastReader(mockReader, BUFFER_SIZE, PUSHBACK_SIZE);
        // Ensure pushback is exhausted so fill() is invoked
        setField(fastReader, "pushbackPosition", PUSHBACK_SIZE);
        setField(fastReader, "pushbackBufferSize", PUSHBACK_SIZE);
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(), "IOException from underlying reader must be propagated");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void read_updatesLineAndColumnOnNewline() throws IOException {
        // Underlying source contains "a\n b"
        source = new StringReader("a\n b");
        fastReader = new FastReader(source, BUFFER_SIZE, PUSHBACK_SIZE);
        // 1st char 'a'
        assertEquals('a', fastReader.read());
        assertEquals(1, fastReader.getLine(), "Line should be 1 after first char");
        assertEquals(2, fastReader.getCol(), "Column should be 2 after first char");
        // 2nd char '\n'
        assertEquals('\n', fastReader.read());
        assertEquals(2, fastReader.getLine(), "Line should increment after newline");
        assertEquals(1, fastReader.getCol(), "Column should reset to 1 after newline");
        // 3rd char ' '
        assertEquals(' ', fastReader.read());
        assertEquals(2, fastReader.getLine(), "Line unchanged after space");
        assertEquals(2, fastReader.getCol(), "Column should be 2 after space");
    }

    @Test
    void read_throwsNullPointerExceptionWhenPushbackBufferIsNull() throws Exception {
        fastReader = new FastReader(new StringReader("abc"), BUFFER_SIZE, 3);
        // Force internal pushbackBuffer to null while size > 0
        setField(fastReader, "pushbackBuffer", null);
        setField(fastReader, "pushbackPosition", 0);
        setField(fastReader, "pushbackBufferSize", 3);
        assertThrows(NullPointerException.class, () -> fastReader.read(), "Accessing a null pushbackBuffer should throw NPE");
    }

    @Test
    void read_throwsArrayIndexOutOfBoundsWhenPushbackPositionNegative() throws Exception {
        fastReader = new FastReader(new StringReader("abc"), BUFFER_SIZE, 5);
        setField(fastReader, "pushbackBuffer", new char[] { 'a', 'b', 'c', 'd', 'e' });
        setField(fastReader, "pushbackPosition", -1);
        setField(fastReader, "pushbackBufferSize", 5);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> fastReader.read(), "Negative pushbackPosition should cause AIOOBE");
    }

    @Test
    void read_throwsArrayIndexOutOfBoundsWhenBufferTooSmall() throws Exception {
        fastReader = new FastReader(new StringReader("abc"), BUFFER_SIZE, 5);
        // Actual array length is 3 but declared size is 5
        setField(fastReader, "pushbackBuffer", new char[] { 'x', 'y', 'z' });
        setField(fastReader, "pushbackPosition", 3);
        setField(fastReader, "pushbackBufferSize", 5);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> fastReader.read(), "Access beyond actual array length should throw AIOOBE");
    }

    @Test
    void read_throwsNullPointerExceptionWhenBufIsNullAfterFill() throws Exception {
        // Subclass that overrides fill()
        FastReader brokenReader = new FastReader(new StringReader("x"), BUFFER_SIZE, PUSHBACK_SIZE) {

            @Override
            protected void fill() throws IOException {
                // Simulate a successful fill that sets limit but leaves buf null
                setField(this, "limit", 1);
                setField(this, "buf", null);
            }
        };
        // Exhaust pushback so that read() will call our overridden fill()
        setField(brokenReader, "pushbackPosition", PUSHBACK_SIZE);
        setField(brokenReader, "pushbackBufferSize", PUSHBACK_SIZE);
        assertThrows(NullPointerException.class, brokenReader::read, "Null buf after fill should cause NPE when accessed");
    }
}
