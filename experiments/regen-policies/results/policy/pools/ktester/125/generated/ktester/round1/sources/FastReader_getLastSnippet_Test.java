package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class FastReader_getLastSnippet_Test {

    /**
     * The FastReader instance under test.
     */
    private FastReader fastReader;

    /**
     * Simple source data used for the tests.
     */
    private static final String SOURCE = "Hello, FastReader!";

    /**
     * Buffer size for the FastReader – must be at least as long as the test string.
     */
    private static final int BUFFER_SIZE = 32;

    /**
     * Push‑back buffer size – not used directly in these tests.
     */
    private static final int PUSHBACK_SIZE = 8;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a new FastReader for each test to guarantee isolation.
        Reader underlying = new StringReader(SOURCE);
        fastReader = new FastReader(underlying, BUFFER_SIZE, PUSHBACK_SIZE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Close the FastReader (and consequently the underlying StringReader).
        fastReader.close();
    }

    /* ----------------------------------------------------------------------
     * Helper to inject private fields via reflection.
     * ---------------------------------------------------------------------- */
    private void setInternalState(FastReader fr, char[] buf, int position) throws Exception {
        Field bufField = FastReader.class.getDeclaredField("buf");
        bufField.setAccessible(true);
        bufField.set(fr, buf);
        Field posField = FastReader.class.getDeclaredField("position");
        posField.setAccessible(true);
        posField.setInt(fr, position);
    }

    @Test
    void getLastSnippet_ShouldReturnEmptyString_WhenNoCharactersRead() {
        // GIVEN – a freshly created FastReader (setup in @BeforeEach)
        // WHEN
        String snippet = fastReader.getLastSnippet();
        // THEN
        assertEquals("", snippet, "Snippet should be empty before any reads");
    }

    @Test
    void getLastSnippet_ShouldReturnCharactersReadSoFar() throws IOException {
        // GIVEN – a FastReader positioned at the start of the source string
        // Read the first 5 characters: "Hello"
        for (int i = 0; i < 5; i++) {
            int ch = fastReader.read();
            assertTrue(ch != -1, "Unexpected end of stream while reading test data");
        }
        // WHEN
        String snippetAfterFive = fastReader.getLastSnippet();
        // THEN
        assertEquals("Hello", snippetAfterFive, "Snippet should contain exactly the characters that have been read");
        // GIVEN – read the next 7 characters: ", FastR"
        for (int i = 0; i < 7; i++) {
            int ch = fastReader.read();
            assertTrue(ch != -1, "Unexpected end of stream while reading additional data");
        }
        // WHEN
        String snippetAfterTwelve = fastReader.getLastSnippet();
        // THEN
        assertEquals("Hello, FastR", snippetAfterTwelve, "Snippet should reflect the cumulative characters read so far");
    }

    @Test
    void getLastSnippet_ShouldReturnFullContent_WhenAllCharactersRead() throws IOException {
        // GIVEN – read the entire source string
        int ch;
        StringBuilder expected = new StringBuilder();
        while ((ch = fastReader.read()) != -1) {
            expected.append((char) ch);
        }
        // WHEN
        String snippet = fastReader.getLastSnippet();
        // THEN
        assertEquals(SOURCE, snippet, "Snippet should equal the full source after the entire stream is consumed");
        assertEquals(expected.toString(), snippet, "Snippet should match the characters accumulated during the read loop");
    }

    @Test
    void getLastSnippet_EmptySnippet_WhenPositionZero() throws Exception {
        // buf content is irrelevant when position == 0
        char[] buf = { 'x', 'y', 'z' };
        setInternalState(fastReader, buf, 0);
        String snippet = fastReader.getLastSnippet();
        assertEquals("", snippet, "Snippet should be empty when position is 0");
    }

    @Test
    void getLastSnippet_PartialSnippet() throws Exception {
        char[] buf = { 'a', 'b', 'c', 'd', 'e' };
        // expect "abc"
        setInternalState(fastReader, buf, 3);
        String snippet = fastReader.getLastSnippet();
        assertEquals("abc", snippet, "Snippet should contain characters up to position-1");
    }

    @Test
    void getLastSnippet_FullBufferSnippet() throws Exception {
        char[] buf = { 'a', 'b', 'c', 'd', 'e' };
        // expect whole buffer
        setInternalState(fastReader, buf, 5);
        String snippet = fastReader.getLastSnippet();
        assertEquals("abcde", snippet, "Snippet should return the whole buffer when position == length");
    }

    @Test
    void getLastSnippet_WhitespaceAndControlCharacters() throws Exception {
        char[] buf = { ' ', '\t', '\n', '\r' };
        setInternalState(fastReader, buf, 4);
        String snippet = fastReader.getLastSnippet();
        assertEquals(" \t\n\r", snippet, "Snippet must preserve whitespace and control characters");
    }

    @Test
    void getLastSnippet_UnicodeCharacters() throws Exception {
        // snowman, white smiley, extra char
        char[] buf = { '\u2603', '\u263A', 'x' };
        setInternalState(fastReader, buf, 2);
        String snippet = fastReader.getLastSnippet();
        assertEquals("\u2603\u263A", snippet, "Snippet should correctly handle Unicode characters");
    }

    @Test
    void getLastSnippet_SurrogatePairSnippet() throws Exception {
        // 😀 emoji is represented by the surrogate pair \uD83D\uDE00
        char[] buf = { '\uD83D', '\uDE00', 'a' };
        setInternalState(fastReader, buf, 2);
        String snippet = fastReader.getLastSnippet();
        assertEquals("\uD83D\uDE00", snippet, "Snippet should return the surrogate pair as a single character");
    }

    @Test
    void getLastSnippet_WithNullCharacter() throws Exception {
        char[] buf = { 'a', '\0', 'b', 'c' };
        setInternalState(fastReader, buf, 4);
        String snippet = fastReader.getLastSnippet();
        assertEquals("a\u0000bc", snippet, "Snippet must include embedded null character unchanged");
    }

    @Test
    void getLastSnippet_WithBackspaceCharacter() throws Exception {
        char[] buf = { 'a', '\b', 'c' };
        setInternalState(fastReader, buf, 3);
        String snippet = fastReader.getLastSnippet();
        assertEquals("a\bc", snippet, "Snippet should preserve the backspace control character");
    }

    @Test
    void getLastSnippet_PositionExceedsBufferLength_ShouldThrowArrayIndexOutOfBoundsException() throws Exception {
        char[] buf = { 'a', 'b' };
        // position > buf.length
        setInternalState(fastReader, buf, 5);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> fastReader.getLastSnippet(), "Accessing beyond the buffer should throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void getLastSnippet_EmptyBufferWithNonZeroPosition_ShouldThrowArrayIndexOutOfBoundsException() throws Exception {
        char[] buf = new char[0];
        setInternalState(fastReader, buf, 1);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> fastReader.getLastSnippet(), "Accessing buf[0] when buffer length is 0 should throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void getLastSnippet_LargeBufferPartialSnippet() throws Exception {
        // 1000 'a' characters
        char[] buf = new char[1000];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = 'a';
        }
        int position = 500;
        setInternalState(fastReader, buf, position);
        String snippet = fastReader.getLastSnippet();
        assertEquals("a".repeat(position), snippet, "Snippet should contain exactly 'position' number of 'a' characters");
    }

    @Test
    void getLastSnippet_NullBufferWithPositivePosition_ShouldThrowNullPointerException() throws Exception {
        // buf is null, position > 0
        setInternalState(fastReader, null, 3);
        assertThrows(NullPointerException.class, () -> fastReader.getLastSnippet(), "Calling getLastSnippet with a null internal buffer should throw NullPointerException");
    }
}
