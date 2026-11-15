package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class FastReader_getLastSnippet_Test {

    private FastReader fastReader;

    private static final int CUSTOM_BUFFER_SIZE = 16;

    private static final int CUSTOM_PUSHBACK_SIZE = 4;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test
        fastReader = null;
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
    }

    /* -----------------------------------------------------------
     *  Helper for reflection access
     * ----------------------------------------------------------- */
    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetLastSnippetEmptyReader() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        // No read operation – buffer stays empty
        assertEquals("", fastReader.getLastSnippet(), "Snippet of an empty reader should be an empty string");
    }

    @Test
    void testGetLastSnippetSingleCharacter() throws IOException {
        fastReader = new FastReader(new StringReader("x"));
        assertEquals('x', fastReader.read(), "First character should be 'x'");
        assertEquals("x", fastReader.getLastSnippet(), "Snippet after reading a single character should contain that character");
    }

    @Test
    void testGetLastSnippetMultipleCharactersWithinDefaultBuffer() throws IOException {
        String content = "HelloWorld";
        fastReader = new FastReader(new StringReader(content));
        for (int i = 0; i < content.length(); i++) {
            assertEquals(content.charAt(i), fastReader.read(), "Read character at position " + i + " should match");
        }
        assertEquals(content, fastReader.getLastSnippet(), "Snippet after reading whole content should equal the content");
    }

    @Test
    void testGetLastSnippetWithNewlines() throws IOException {
        String content = "ab\ncd\nef";
        fastReader = new FastReader(new StringReader(content));
        // Exhaust the reader
        while (fastReader.read() != -1) {
            // intentionally empty
        }
        assertEquals(content, fastReader.getLastSnippet(), "Snippet should preserve newline characters");
    }

    @Test
    void testGetLastSnippetAfterBufferRefillWithCustomSizes() throws IOException {
        // Build a string longer than the custom buffer to force a refill
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CUSTOM_BUFFER_SIZE * 2; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        String largeContent = sb.toString();
        fastReader = new FastReader(new StringReader(largeContent), CUSTOM_BUFFER_SIZE, CUSTOM_PUSHBACK_SIZE);
        // Read exactly half of the content – this will cause at least one buffer refill
        int half = largeContent.length() / 2;
        for (int i = 0; i < half; i++) {
            fastReader.read();
        }
        String snippet = fastReader.getLastSnippet();
        // The snippet length must be equal to the buffer size because position is reset after a refill
        assertEquals(CUSTOM_BUFFER_SIZE, snippet.length(), "Snippet length after a buffer refill should equal the configured buffer size");
        // Compute the expected substring: the last CUSTOM_BUFFER_SIZE characters that were read
        int startPos = half - CUSTOM_BUFFER_SIZE;
        if (startPos < 0)
            startPos = 0;
        String expected = largeContent.substring(startPos, half);
        assertEquals(expected, snippet, "Snippet should contain the most recent characters read from the stream");
    }

    @Test
    void testGetLastSnippetAfterReaderClosed() throws IOException {
        fastReader = new FastReader(new StringReader("data"));
        // Read a few characters
        fastReader.read();
        fastReader.read();
        fastReader.close();
        // Even after closing, getLastSnippet should still return the data that was read before close
        assertEquals("da", fastReader.getLastSnippet(), "Snippet after closing should reflect characters read before the close");
    }

    // ---------- Basic functionality ----------
    @Test
    void testGetLastSnippetWhenNoCharactersRead() throws IOException {
        fastReader = new FastReader(new StringReader("any"));
        // No reads – position stays 0
        assertEquals("", fastReader.getLastSnippet(), "When position == 0, getLastSnippet should return an empty string");
    }

    @Test
    void testGetLastSnippetPartialBuffer() throws IOException {
        fastReader = new FastReader(new StringReader("HelloWorld"));
        // Force buffer to contain the full string without reading (using reflection)
        setField(fastReader, "buf", "HelloWorld".toCharArray());
        setField(fastReader, "position", 5);
        assertEquals("Hello", fastReader.getLastSnippet(), "When position is 5, snippet should be the first 5 characters");
    }

    @Test
    void testGetLastSnippetFullBuffer() throws IOException {
        fastReader = new FastReader(new StringReader("HelloWorld"));
        setField(fastReader, "buf", "HelloWorld".toCharArray());
        setField(fastReader, "position", 10);
        assertEquals("HelloWorld", fastReader.getLastSnippet(), "When position equals buffer length, snippet should be the whole content");
    }

    // ---------- Edge cases ----------
    @Test
    void testGetLastSnippetEmptyInternalBuffer() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        setField(fastReader, "buf", new char[0]);
        setField(fastReader, "position", 0);
        assertEquals("", fastReader.getLastSnippet(), "Empty internal buffer with position 0 should yield empty string");
    }

    @Test
    void testGetLastSnippetNegativePosition() throws IOException {
        fastReader = new FastReader(new StringReader("abc"));
        setField(fastReader, "buf", "abc".toCharArray());
        setField(fastReader, "position", -3);
        // The for‑loop condition i < position is false from the start → empty string
        assertEquals("", fastReader.getLastSnippet(), "Negative position should safely return empty string");
    }

    @Test
    void testGetLastSnippetPositionBeyondBuffer() {
        fastReader = new FastReader(new StringReader("0123456789"));
        // length 10
        setField(fastReader, "buf", "0123456789".toCharArray());
        setField(fastReader, "position", 15);
        // Expect ArrayIndexOutOfBoundsException because the loop will try to access buf[10] …
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> fastReader.getLastSnippet(), "Position beyond buffer length should cause ArrayIndexOutOfBoundsException");
    }

    // ---------- Unicode and special characters ----------
    @Test
    void testGetLastSnippetUnicodeSurrogatePairs() throws IOException {
        // each emoji is a surrogate pair (2 chars)
        String emoji = "😀🚀";
        fastReader = new FastReader(new StringReader(emoji));
        setField(fastReader, "buf", emoji.toCharArray());
        // length = 4
        setField(fastReader, "position", emoji.length());
        assertEquals(emoji, fastReader.getLastSnippet(), "Unicode surrogate pairs must be returned correctly");
    }

    @Test
    void testGetLastSnippetWithNullCharacter() throws IOException {
        String withNull = "ab\u0000cd";
        fastReader = new FastReader(new StringReader(withNull));
        setField(fastReader, "buf", withNull.toCharArray());
        setField(fastReader, "position", 5);
        assertEquals(withNull, fastReader.getLastSnippet(), "Snippet should include embedded null character");
    }

    // ---------- Immutability and repeatability ----------
    @Test
    void testGetLastSnippetImmutabilityAfterBufferMutation() throws IOException {
        fastReader = new FastReader(new StringReader("Hello"));
        setField(fastReader, "buf", "Hello".toCharArray());
        setField(fastReader, "position", 5);
        String first = fastReader.getLastSnippet();
        // Mutate the internal buffer via reflection
        char[] mutated = "Jello".toCharArray();
        setField(fastReader, "buf", mutated);
        // The previously returned string must stay unchanged
        assertEquals("Hello", first, "Returned string must be immutable even after internal buffer changes");
    }

    @Test
    void testGetLastSnippetDistinctStringInstances() throws IOException {
        fastReader = new FastReader(new StringReader("Hello"));
        setField(fastReader, "buf", "Hello".toCharArray());
        setField(fastReader, "position", 5);
        String first = fastReader.getLastSnippet();
        String second = fastReader.getLastSnippet();
        assertEquals(first, second, "Both calls should return equal content");
        assertNotSame(first, second, "Each call should produce a distinct String instance");
    }

    // ---------- Null buffer handling ----------
    @Test
    void testGetLastSnippetNullBufferThrowsNPE() throws IOException {
        fastReader = new FastReader(new StringReader("abc"));
        setField(fastReader, "buf", null);
        setField(fastReader, "position", 3);
        assertThrows(NullPointerException.class, () -> fastReader.getLastSnippet(), "Calling getLastSnippet with a null internal buffer should throw NPE");
    }
}
