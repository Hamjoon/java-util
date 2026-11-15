package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;

class FastReader_read_Test {

    private static final int BUFFER_SIZE = 16;

    private static final int PUSHBACK_SIZE = 4;

    private FastReader fastReader;

    private StringReader backingReader;

    @BeforeEach
    void setupBeforeEach() {
        // default construction – each test can replace the reader or the FastReader instance
        backingReader = new StringReader("HelloWorld");
        fastReader = new FastReader(backingReader, BUFFER_SIZE, PUSHBACK_SIZE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the FastReader (and its underlying Reader) are closed after each test
        fastReader.close();
    }

    @Test
    void readReturnsCharactersInOrder() throws IOException {
        // read the first three characters and verify they match the source string
        assertEquals('H', fastReader.read(), "First character should be 'H'");
        assertEquals('e', fastReader.read(), "Second character should be 'e'");
        assertEquals('l', fastReader.read(), "Third character should be 'l'");
    }

    @Test
    void readReturnsMinusOneAtEndOfStream() throws IOException {
        // Exhaust the underlying source
        for (int i = 0; i < "HelloWorld".length(); i++) {
            assertNotEquals(-1, fastReader.read(), "Should not be end‑of‑stream yet");
        }
        // Next read must indicate EOF
        assertEquals(-1, fastReader.read(), "After all characters are consumed, read() must return -1");
    }

    @Test
    void readAfterPushbackReturnsPushedCharacterFirst() throws IOException {
        // Push back a character that does not appear in the source
        fastReader.pushback('X');
        // The first call to read() must return the pushed character
        assertEquals('X', fastReader.read(), "Read after pushback should return the pushed character");
        // Subsequent reads continue from the original source
        assertEquals('H', fastReader.read(), "Next character should be the first of the underlying source");
    }

    @Test
    void readAfterCloseThrowsIOException() throws IOException {
        fastReader.close();
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(), "Calling read() on a closed FastReader must throw IOException");
        assertTrue(ex.getMessage().contains("closed"), "Exception message should indicate the stream is closed");
    }

    @Test
    void readWhenUnderlyingStreamIsNullThrowsIOException() throws Exception {
        // Use reflection to set the private field `in` to null
        Field inField = FastReader.class.getDeclaredField("in");
        inField.setAccessible(true);
        inField.set(fastReader, null);
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(), "read() must throw IOException when internal Reader is null");
        assertTrue(ex.getMessage().contains("closed"), "Exception message should mention the stream is closed");
    }

    @Test
    void readReturnsCharacterFromPushbackBuffer() throws Exception {
        // construct with a pushback buffer size of 5
        fastReader = new FastReader(new StringReader("ignored"), BUFFER_SIZE, 5);
        // Fill pushback buffer manually
        Field pushbackBufferField = FastReader.class.getDeclaredField("pushbackBuffer");
        pushbackBufferField.setAccessible(true);
        char[] pushback = new char[5];
        pushback[0] = 'A';
        pushbackBufferField.set(fastReader, pushback);
        Field pushbackPosField = FastReader.class.getDeclaredField("pushbackPosition");
        pushbackPosField.setAccessible(true);
        // position points to first element
        pushbackPosField.setInt(fastReader, 0);
        Field pushbackSizeField = FastReader.class.getDeclaredField("pushbackBufferSize");
        pushbackSizeField.setAccessible(true);
        pushbackSizeField.setInt(fastReader, 5);
        assertEquals(65, fastReader.read(), "Should return the pushed‑back character 'A' (65)");
    }

    @Test
    void readReturnsSecondCharacterFromPushbackAfterFirstRead() throws Exception {
        // pushback buffer size 2, containing 'B' and 'C'
        fastReader = new FastReader(new StringReader("ignored"), BUFFER_SIZE, 2);
        Field pushbackBufferField = FastReader.class.getDeclaredField("pushbackBuffer");
        pushbackBufferField.setAccessible(true);
        char[] pushback = new char[] { 'B', 'C' };
        pushbackBufferField.set(fastReader, pushback);
        // Simulate that one character has already been read from pushback
        Field pushbackPosField = FastReader.class.getDeclaredField("pushbackPosition");
        pushbackPosField.setAccessible(true);
        // next read should return 'C'
        pushbackPosField.setInt(fastReader, 1);
        Field pushbackSizeField = FastReader.class.getDeclaredField("pushbackBufferSize");
        pushbackSizeField.setAccessible(true);
        pushbackSizeField.setInt(fastReader, 2);
        assertEquals(67, fastReader.read(), "Second pushback character 'C' should be returned (67)");
    }

    @Test
    void readWhenPushbackExhaustedFallsBackToUnderlyingReader() throws Exception {
        // pushback buffer size 3 but position already equals size (exhausted)
        fastReader = new FastReader(new StringReader("xyz"), BUFFER_SIZE, 3);
        Field pushbackPosField = FastReader.class.getDeclaredField("pushbackPosition");
        pushbackPosField.setAccessible(true);
        // exhausted
        pushbackPosField.setInt(fastReader, 3);
        // Now read – should come from underlying reader
        assertEquals(120, fastReader.read(), "First character from underlying source should be 'x' (120)");
    }

    @Test
    void readAfterPreviousReadReturnsNextCharWithoutRefill() throws Exception {
        // Use underlying "xyz"
        fastReader = new FastReader(new StringReader("xyz"), BUFFER_SIZE, 0);
        // First read consumes 'x'
        assertEquals(120, fastReader.read(), "First read should return 'x'");
        // Second read should return 'y' without another fill
        assertEquals(121, fastReader.read(), "Second read should return 'y' (121)");
    }

    @Test
    void readReturnsMinusOneWhenUnderlyingReaderIsEmpty() throws Exception {
        fastReader = new FastReader(new StringReader(""), BUFFER_SIZE, 0);
        // Force a fill – the internal fill will set limit to -1
        assertEquals(-1, fastReader.read(), "Empty underlying reader should cause read() to return -1");
    }

    @Test
    void readPropagatesIOExceptionFromUnderlyingReader() throws Exception {
        // Custom Reader that always throws IOException
        Reader throwingReader = new Reader() {

            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("underlying read error");
            }

            @Override
            public void close() throws IOException {
            }
        };
        fastReader = new FastReader(throwingReader, BUFFER_SIZE, 0);
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(), "IOException from underlying reader must propagate");
        assertEquals("underlying read error", ex.getMessage());
    }

    @Test
    void readWithZeroPushbackBufferSizeReadsDirectlyFromUnderlying() throws Exception {
        fastReader = new FastReader(new StringReader("a"), BUFFER_SIZE, 0);
        assertEquals(97, fastReader.read(), "With pushback size 0, read should return 'a' (97)");
    }

    @Test
    void readWithNullPushbackBufferThrowsNullPointerException() throws Exception {
        fastReader = new FastReader(new StringReader("ignored"), BUFFER_SIZE, 1);
        // set pushbackBuffer to null via reflection
        Field pushbackBufferField = FastReader.class.getDeclaredField("pushbackBuffer");
        pushbackBufferField.setAccessible(true);
        pushbackBufferField.set(fastReader, null);
        // Ensure pushbackPosition < pushbackBufferSize so the code enters the branch
        Field pushbackPosField = FastReader.class.getDeclaredField("pushbackPosition");
        pushbackPosField.setAccessible(true);
        pushbackPosField.setInt(fastReader, 0);
        Field pushbackSizeField = FastReader.class.getDeclaredField("pushbackBufferSize");
        pushbackSizeField.setAccessible(true);
        pushbackSizeField.setInt(fastReader, 1);
        assertThrows(NullPointerException.class, () -> fastReader.read(), "Accessing a null pushbackBuffer must throw NullPointerException");
    }

    @Test
    void readWithNegativePushbackPositionThrowsArrayIndexOutOfBoundsException() throws Exception {
        fastReader = new FastReader(new StringReader("ignored"), BUFFER_SIZE, 2);
        // set pushbackPosition to -1
        Field pushbackPosField = FastReader.class.getDeclaredField("pushbackPosition");
        pushbackPosField.setAccessible(true);
        pushbackPosField.setInt(fastReader, -1);
        // Ensure size is positive so condition passes
        Field pushbackSizeField = FastReader.class.getDeclaredField("pushbackBufferSize");
        pushbackSizeField.setAccessible(true);
        pushbackSizeField.setInt(fastReader, 2);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> fastReader.read(), "Negative pushbackPosition should cause ArrayIndexOutOfBoundsException");
    }

    @Test
    void readWhenPushbackBufferSmallerThanDeclaredSizeThrowsArrayIndexOutOfBoundsException() throws Exception {
        // Declare size 2 but provide an array of length 1
        fastReader = new FastReader(new StringReader("ignored"), BUFFER_SIZE, 2);
        Field pushbackBufferField = FastReader.class.getDeclaredField("pushbackBuffer");
        pushbackBufferField.setAccessible(true);
        // length 1
        pushbackBufferField.set(fastReader, new char[] { 'X' });
        // pushbackPosition starts at 0
        Field pushbackPosField = FastReader.class.getDeclaredField("pushbackPosition");
        pushbackPosField.setAccessible(true);
        pushbackPosField.setInt(fastReader, 0);
        // First read succeeds
        assertEquals('X', fastReader.read(), "First read should return the only element");
        // Second read should attempt index 1 -> OOB
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> fastReader.read(), "Reading beyond actual array length must throw ArrayIndexOutOfBoundsException");
    }
}
