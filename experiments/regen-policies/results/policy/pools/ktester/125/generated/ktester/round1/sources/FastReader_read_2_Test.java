package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class FastReader_read_2_Test {

    private Reader underlying;

    private FastReader fastReader;

    @BeforeEach
    void setupBeforeEach() {
        // Underlying source contains a known sequence of characters
        underlying = new StringReader("abcdefghij");
        // Small buffer sizes force the FastReader to invoke fill() multiple times
        fastReader = new FastReader(underlying, 4, 2);
        // Default fixture: a simple underlying source and a modest buffer size.
        underlying = new StringReader("abcdefghij");
        // buf size = 4, push‑back = 2
        fastReader = new FastReader(underlying, 4, 2);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the FastReader (and its underlying reader) are closed after each test
        fastReader.close();
        fastReader.close();
    }

    /**
     * Sets a private field on the given object, making it accessible.
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
     * Returns the value of a private field on the given object.
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

    /**
     * Group: End‑of‑stream after fill() returns -1
     */
    static Stream<Arguments> eofScenarios() {
        return Stream.of(// empty underlying source -> immediate -1
        Arguments.of("", -1, new char[3]), // source "OK" -> read 2 then -1
        Arguments.of("OK", 2, new char[3]));
    }

    @Test
    void testReadSimple() throws IOException {
        char[] buf = new char[5];
        int read = fastReader.read(buf, 0, 5);
        assertEquals(5, read, "Should read exactly 5 characters");
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, buf, "Buffer should contain the first five characters from the source");
    }

    @Test
    void testReadSpanningMultipleFills() throws IOException {
        // Request more characters than the internal buffer size (4) to trigger multiple fills
        char[] buf = new char[9];
        int read = fastReader.read(buf, 0, 9);
        assertEquals(9, read, "Should read 9 characters");
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i' }, buf, "Buffer should contain the first nine characters from the source");
    }

    @Test
    void testReadWithPushback() throws IOException {
        // Push back a character; it should be returned before any characters from the underlying source
        fastReader.pushback('z');
        char[] buf = new char[3];
        int read = fastReader.read(buf, 0, 3);
        assertEquals(3, read, "Should read three characters (pushback + two from source)");
        assertArrayEquals(new char[] { 'z', 'a', 'b' }, buf, "First character should be the pushed‑back one, followed by source characters");
    }

    @Test
    void testReadAfterCloseThrows() throws IOException {
        fastReader.close();
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(new char[1], 0, 1), "Reading after close should throw IOException");
        assertTrue(ex.getMessage().contains("FastReader stream is closed"), "Exception message should indicate that the stream is closed");
    }

    /**
     * Group: Null underlying stream
     */
    @Test
    void testReadWhenUnderlyingIsNull() throws Exception {
        // Simulate a closed FastReader by nulling the internal 'in' field.
        setPrivateField(fastReader, "in", null);
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(new char[10], 0, 5));
        assertTrue(ex.getMessage().contains("FastReader stream is closed"));
    }

    /**
     * Group: Read only from push‑back buffer
     */
    @Test
    void testReadOnlyFromPushbackBuffer() throws IOException {
        // Build a FastReader with a push‑back buffer of size 5.
        fastReader = new FastReader(new StringReader("unused"), 4, 5);
        // Fill push‑back buffer with "abcde"
        for (char ch : "abcde".toCharArray()) {
            fastReader.pushback(ch);
        }
        // 1) read first 3 chars from push‑back
        char[] buf1 = new char[4];
        int read1 = fastReader.read(buf1, 0, 3);
        assertEquals(3, read1);
        assertArrayEquals(new char[] { 'a', 'b', 'c', '\0' }, buf1);
        // 2) read 4 chars with offset 1 (remaining push‑back chars are "de")
        char[] buf2 = new char[5];
        int read2 = fastReader.read(buf2, 1, 4);
        assertEquals(4, read2);
        assertArrayEquals(new char[] { '\0', 'd', 'e', '\0', '\0' }, buf2);
    }

    /**
     * Group: Read from underlying after push‑back buffer is empty
     */
    @Test
    void testReadFromUnderlyingWhenPushbackEmpty() throws IOException {
        // pushback buffer size = 10, but we set position to make it appear empty
        fastReader = new FastReader(new StringReader("hello!world"), 10, 10);
        // Simulate empty push‑back by moving position to the end
        // pushbackBufferSize
        setPrivateField(fastReader, "pushbackPosition", 10);
        char[] buf = new char[6];
        int read = fastReader.read(buf, 0, 6);
        assertEquals(6, read);
        assertArrayEquals(new char[] { 'h', 'e', 'l', 'l', 'o', '!' }, buf);
    }

    @ParameterizedTest
    @MethodSource("eofScenarios")
    void testEofHandling(String source, int expectedResult, char[] dest) throws IOException {
        fastReader = new FastReader(new StringReader(source), 4, 2);
        int read = fastReader.read(dest, 0, 3);
        assertEquals(expectedResult, read);
        if (expectedResult > 0) {
            // verify that the characters read are exactly the source characters
            for (int i = 0; i < expectedResult; i++) {
                assertEquals(source.charAt(i), dest[i]);
            }
        }
    }

    /**
     * Group: Loop across push‑back and underlying buffers
     */
    @Test
    void testLoopAcrossPushbackAndUnderlying() throws IOException {
        fastReader = new FastReader(new StringReader("12345"), 5, 3);
        // pushback "xyz"
        for (char ch : "xyz".toCharArray()) {
            fastReader.pushback(ch);
        }
        char[] buf = new char[8];
        int read = fastReader.read(buf, 0, 8);
        assertEquals(8, read);
        assertArrayEquals(new char[] { 'x', 'y', 'z', '1', '2', '3', '4', '5' }, buf);
    }

    /**
     * Group: Invalid arguments - null buffer
     */
    @Test
    void testReadNullBuffer() {
        assertThrows(NullPointerException.class, () -> fastReader.read(null, 0, 5));
    }

    /**
     * Group: Invalid arguments - negative offset
     */
    @Test
    void testReadNegativeOffset() {
        char[] buf = new char[5];
        assertThrows(IndexOutOfBoundsException.class, () -> fastReader.read(buf, -1, 3));
    }

    /**
     * Group: Invalid arguments - negative length
     */
    @Test
    void testReadNegativeLength() {
        char[] buf = new char[5];
        assertThrows(IndexOutOfBoundsException.class, () -> fastReader.read(buf, 0, -2));
    }

    /**
     * Group: Invalid arguments - offset+len exceeds buffer
     */
    @Test
    void testReadOffsetLengthOverflow() {
        char[] buf = new char[5];
        assertThrows(IndexOutOfBoundsException.class, () -> fastReader.read(buf, 3, 3));
    }

    /**
     * Group: Zero‑length read
     */
    @Test
    void testZeroLengthReadLeavesBufferIntact() throws IOException {
        char[] buf = new char[] { 'x', 'x', 'x', 'x', 'x' };
        int read = fastReader.read(buf, 2, 0);
        assertEquals(0, read);
        assertArrayEquals(new char[] { 'x', 'x', 'x', 'x', 'x' }, buf);
    }

    /**
     * Group: Partial push‑back consumption
     */
    @Test
    void testPartialPushbackConsumption() throws IOException {
        fastReader = new FastReader(new StringReader("unused"), 4, 5);
        // fill push‑back with "abcde"
        for (char ch : "abcde".toCharArray()) {
            fastReader.pushback(ch);
        }
        // Simulate that first two chars have already been consumed
        setPrivateField(fastReader, "pushbackPosition", 2);
        char[] buf = new char[4];
        int read = fastReader.read(buf, 0, 2);
        assertEquals(2, read);
        assertArrayEquals(new char[] { 'c', 'd', '\0', '\0' }, buf);
    }

    /**
     * Group: Read across partially consumed push‑back then underlying
     */
    @Test
    void testReadAcrossPartialPushbackThenUnderlying() throws IOException {
        fastReader = new FastReader(new StringReader("12"), 4, 3);
        // pushback "xyz"
        for (char ch : "xyz".toCharArray()) {
            fastReader.pushback(ch);
        }
        // make only "yz" available
        setPrivateField(fastReader, "pushbackPosition", 1);
        char[] buf = new char[4];
        int read = fastReader.read(buf, 0, 4);
        assertEquals(4, read);
        assertArrayEquals(new char[] { 'y', 'z', '1', '2' }, buf);
    }

    /**
     * Group: Read when underlying provides fewer chars than requested and then EOF
     */
    @Test
    void testReadFewerThanRequestedThenEof() throws IOException {
        fastReader = new FastReader(new StringReader("OK"), 4, 0);
        char[] buf = new char[5];
        int read = fastReader.read(buf, 0, 5);
        assertEquals(2, read);
        assertArrayEquals(new char[] { 'O', 'K', '\0', '\0', '\0' }, buf);
    }

    /**
     * Group: Read after close (duplicate of earlier test, kept for completeness)
     */
    @Test
    void testReadAfterCloseAgain() throws IOException {
        fastReader.close();
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(new char[3], 0, 3));
        assertTrue(ex.getMessage().contains("FastReader stream is closed"));
    }

    /**
     * Group: Read with offset at array end
     */
    @Test
    void testReadWithOffsetAtEnd() throws IOException {
        fastReader = new FastReader(new StringReader("A"), 4, 0);
        char[] buf = new char[5];
        int read = fastReader.read(buf, 4, 1);
        assertEquals(1, read);
        assertArrayEquals(new char[] { '\0', '\0', '\0', '\0', 'A' }, buf);
    }

    /**
     * Group: Zero‑size push‑back buffer
     */
    @Test
    void testZeroSizePushbackBuffer() throws IOException {
        fastReader = new FastReader(new StringReader("abc"), 4, 0);
        char[] buf = new char[3];
        int read = fastReader.read(buf, 0, 3);
        assertEquals(3, read);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, buf);
    }

    /**
     * Group: Underlying reader throws IOException
     */
    @Test
    void testUnderlyingReaderThrowsIOException() throws Exception {
        // Mock a Reader that throws on the first read()
        Reader mockReader = mock(Reader.class);
        when(mockReader.read(any(char[].class), anyInt(), anyInt())).thenThrow(new IOException("simulated I/O error"));
        // Construct a FastReader with a normal buffer size; then replace its 'in' field.
        fastReader = new FastReader(new StringReader("unused"), 4, 2);
        setPrivateField(fastReader, "in", mockReader);
        // Attempt to read – the IOException should propagate.
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(new char[10], 0, 5));
        assertEquals("simulated I/O error", ex.getMessage());
        // Verify that the mock was indeed invoked.
        verify(mockReader, times(1)).read(any(char[].class), anyInt(), anyInt());
    }
}
