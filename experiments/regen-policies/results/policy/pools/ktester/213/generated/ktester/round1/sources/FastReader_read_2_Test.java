package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class FastReader_read_2_Test {

    private FastReader fastReader;

    @BeforeEach
    void setupBeforeEach() {
        // Each test creates its own FastReader; the field is cleared here for safety
        fastReader = null;
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
    }

    /**
     * Sets a private field on the given instance via reflection.
     *
     * @param target    the object whose field should be modified
     * @param fieldName the name of the private field
     * @param value     the new value to assign
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Simple read of the whole content into a correctly‑sized buffer.
     */
    @Test
    void testReadWholeContentIntoExactBuffer() throws IOException {
        fastReader = new FastReader(new StringReader("hello"));
        char[] buf = new char[5];
        int read = fastReader.read(buf, 0, 5);
        assertEquals(5, read, "Should read all 5 characters");
        assertArrayEquals(new char[] { 'h', 'e', 'l', 'l', 'o' }, buf);
    }

    /**
     * Read with an offset – the method must leave the preceding part of the buffer untouched.
     */
    @Test
    void testReadWithOffset() throws IOException {
        fastReader = new FastReader(new StringReader("world"));
        char[] buf = new char[10];
        // pre‑fill buffer with sentinel values
        for (int i = 0; i < buf.length; i++) {
            buf[i] = '-';
        }
        int read = fastReader.read(buf, 2, 5);
        assertEquals(5, read);
        // positions 0‑1 should stay unchanged
        assertEquals('-', buf[0]);
        assertEquals('-', buf[1]);
        // positions 2‑6 contain the data
        assertArrayEquals(new char[] { 'w', 'o', 'r', 'l', 'd' }, new char[] { buf[2], buf[3], buf[4], buf[5], buf[6] });
        // the rest stays unchanged
        assertEquals('-', buf[7]);
        assertEquals('-', buf[8]);
        assertEquals('-', buf[9]);
    }

    /**
     * Request more characters than are available – the method should return the amount actually read.
     */
    @Test
    void testReadPartialWhenRequestedMoreThanAvailable() throws IOException {
        fastReader = new FastReader(new StringReader("abc"));
        char[] buf = new char[10];
        int read = fastReader.read(buf, 0, 10);
        assertEquals(3, read, "Only three characters are present");
        assertArrayEquals(new char[] { 'a', 'b', 'c', '\0', '\0', '\0', '\0', '\0', '\0', '\0' }, buf);
    }

    /**
     * Verify that reading after the stream has been closed throws an IOException.
     */
    @Test
    void testReadAfterCloseThrows() throws IOException {
        fastReader = new FastReader(new StringReader("data"));
        fastReader.close();
        char[] buf = new char[4];
        assertThrows(IOException.class, () -> fastReader.read(buf, 0, 4));
    }

    /**
     * When the underlying stream is empty, read should return -1.
     */
    @Test
    void testReadFromEmptyStreamReturnsMinusOne() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        char[] buf = new char[4];
        int read = fastReader.read(buf, 0, 4);
        assertEquals(-1, read, "Empty stream must return -1");
    }

    /**
     * Push back a character and then read it via the bulk read method.
     */
    @Test
    void testReadAfterPushback() throws IOException {
        fastReader = new FastReader(new StringReader("xyz"));
        // read first two characters normally
        assertEquals('x', fastReader.read());
        assertEquals('y', fastReader.read());
        // push back 'y' and then read into an array
        fastReader.pushback('y');
        char[] buf = new char[2];
        int read = fastReader.read(buf, 0, 2);
        assertEquals(2, read);
        assertArrayEquals(new char[] { 'y', 'z' }, buf);
    }

    /**
     * Verify that the method correctly handles a zero‑length request – it should return 0
     * without modifying the buffer.
     */
    @Test
    void testReadZeroLength() throws IOException {
        fastReader = new FastReader(new StringReader("ignore"));
        char[] buf = new char[5];
        // Fill buffer with known data
        for (int i = 0; i < buf.length; i++) {
            buf[i] = '*';
        }
        int read = fastReader.read(buf, 1, 0);
        assertEquals(0, read);
        // Buffer must stay unchanged
        for (char c : buf) {
            assertEquals('*', c);
        }
    }

    /**
     * ClosedStream – the internal {@code in} field is null, so any read must throw IOException.
     */
    @Test
    void testReadWhenUnderlyingStreamIsClosed() throws Exception {
        fastReader = new FastReader(new StringReader("unused"));
        // Force the private field 'in' to null via reflection
        setPrivateField(fastReader, "in", null);
        char[] cbuf = new char[10];
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(cbuf, 0, 5));
        assertEquals("FastReader stream is closed.", ex.getMessage());
    }

    /**
     * PushbackOnly – only data in the push‑back buffer should be returned.
     */
    @Test
    void testReadOnlyFromPushbackBuffer_SmallerThanAvailable() throws IOException {
        fastReader = new FastReader(new StringReader("ignored"));
        // push back five characters
        for (char ch : new char[] { 'a', 'b', 'c', 'd', 'e' }) {
            fastReader.pushback(ch);
        }
        char[] cbuf = new char[4];
        // request less than available (5)
        int read = fastReader.read(cbuf, 0, 3);
        assertEquals(3, read);
        assertArrayEquals(new char[] { 'a', 'b', 'c', '\0' }, cbuf);
    }

    @Test
    void testReadOnlyFromPushbackBuffer_ExactAvailable() throws IOException {
        fastReader = new FastReader(new StringReader("ignored"));
        // push back five characters
        for (char ch : new char[] { 'p', 'q', 'r', 's', 't' }) {
            fastReader.pushback(ch);
        }
        char[] cbuf = new char[6];
        // len equals available
        int read = fastReader.read(cbuf, 1, 5);
        assertEquals(5, read);
        assertArrayEquals(new char[] { '\0', 'p', 'q', 'r', 's', 't' }, cbuf);
    }

    /**
     * PushbackThenBuffer – data from both push‑back and underlying buffers.
     */
    @Test
    void testReadFromPushbackThenUnderlying() throws IOException {
        fastReader = new FastReader(new StringReader("ABCDE"));
        // push back three characters
        for (char ch : new char[] { 'x', 'y', 'z' }) {
            fastReader.pushback(ch);
        }
        char[] cbuf = new char[8];
        int read = fastReader.read(cbuf, 0, 8);
        assertEquals(8, read);
        assertArrayEquals(new char[] { 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E' }, cbuf);
    }

    /**
     * EOF_NoData – empty stream with no push‑back data must return -1.
     */
    @Test
    void testReadEofWhenNoDataAvailable() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        char[] cbuf = new char[5];
        int read = fastReader.read(cbuf, 0, 5);
        assertEquals(-1, read);
    }

    /**
     * EOF_WithData – some data is read before EOF; the method should return the amount read,
     * not -1.
     */
    @Test
    void testReadEofAfterPartialData() throws IOException {
        fastReader = new FastReader(new StringReader("ab"));
        char[] cbuf = new char[4];
        int read = fastReader.read(cbuf, 0, 4);
        assertEquals(2, read);
        assertArrayEquals(new char[] { 'a', 'b', '\0', '\0' }, cbuf);
    }

    /**
     * NormalRead – simple read without any push‑back involvement.
     */
    @Test
    void testNormalReadWithOffsetAndLength() throws IOException {
        fastReader = new FastReader(new StringReader("0123456789"));
        char[] cbuf = new char[10];
        int read = fastReader.read(cbuf, 2, 4);
        assertEquals(4, read);
        assertArrayEquals(new char[] { '\0', '\0', '0', '1', '2', '3', '\0', '\0', '\0', '\0' }, cbuf);
    }

    /**
     * NormalRead – request more characters than fit in a single internal buffer.
     * We shrink the internal buffer via reflection to force multiple fills.
     */
    @Test
    void testNormalReadMultipleFills() throws Exception {
        fastReader = new FastReader(new StringReader("abcdefghijklmnop"));
        // force a tiny internal buffer (size 6) to guarantee multiple fills
        setPrivateField(fastReader, "buf", new char[6]);
        // also reset position/limit so fill will be called
        setPrivateField(fastReader, "position", 0);
        setPrivateField(fastReader, "limit", 0);
        char[] cbuf = new char[12];
        int read = fastReader.read(cbuf, 0, 10);
        assertEquals(10, read);
        // first 10 characters of the source string
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', '\0', '\0' }, cbuf);
    }

    /**
     * InvalidParameters – null destination buffer.
     */
    @Test
    void testReadNullBufferThrowsNPE() throws IOException {
        fastReader = new FastReader(new StringReader("data"));
        assertThrows(NullPointerException.class, () -> fastReader.read(null, 0, 5));
    }

    /**
     * InvalidParameters – negative offset.
     */
    @Test
    void testReadNegativeOffsetThrowsIOOBE() throws IOException {
        fastReader = new FastReader(new StringReader("data"));
        char[] buf = new char[5];
        assertThrows(IndexOutOfBoundsException.class, () -> fastReader.read(buf, -1, 3));
    }

    /**
     * InvalidParameters – offset + length exceeds buffer size.
     */
    @Test
    void testReadOffsetPlusLengthExceedsBufferThrowsIOOBE() throws IOException {
        fastReader = new FastReader(new StringReader("data"));
        char[] buf = new char[5];
        assertThrows(IndexOutOfBoundsException.class, () -> fastReader.read(buf, 4, 2));
    }

    /**
     * InvalidParameters – negative length.
     */
    @Test
    void testReadNegativeLengthThrowsIOOBE() throws IOException {
        fastReader = new FastReader(new StringReader("data"));
        char[] buf = new char[5];
        assertThrows(IndexOutOfBoundsException.class, () -> fastReader.read(buf, 0, -3));
    }

    /**
     * UnderlyingIOException – the wrapped Reader throws IOException during fill().
     */
    @Test
    void testReadPropagatesIOExceptionFromUnderlyingReader() throws IOException {
        // Mock a Reader that throws IOException on any read()
        Reader mockReader = mock(Reader.class);
        when(mockReader.read(any(char[].class), anyInt(), anyInt())).thenThrow(new IOException("underlying failure"));
        fastReader = new FastReader(mockReader);
        char[] cbuf = new char[5];
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(cbuf, 0, 5));
        assertEquals("underlying failure", ex.getMessage());
        verify(mockReader, atLeastOnce()).read(any(char[].class), anyInt(), anyInt());
    }
}
