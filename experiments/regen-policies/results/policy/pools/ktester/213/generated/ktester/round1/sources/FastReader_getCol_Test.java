package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastReader_getCol_Test {

    private FastReader fastReader;

    /**
     * Creates a fresh {@link FastReader} instance before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Use a simple StringReader – the content can be overridden in individual tests.
        fastReader = new FastReader(new StringReader(""));
    }

    /**
     * Closes the {@link FastReader} after each test to release the underlying
     * {@link java.io.Reader}.
     */
    @AfterEach
    void teardownAfterEach() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideSourceAndExpectedCol() {
        return Stream.of(// source, expected column after full consumption
        org.junit.jupiter.params.provider.Arguments.of("abc", 3), org.junit.jupiter.params.provider.Arguments.of("a\nb", 1), org.junit.jupiter.params.provider.Arguments.of("line1\nline2\nline3", 5));
    }

    /**
     * Verify that a newly created FastReader reports column 0.
     */
    @Test
    void testInitialColumnIsZero() {
        assertEquals(0, fastReader.getCol(), "Column should be 0 immediately after construction");
    }

    /**
     * After reading characters, the column count should increase accordingly.
     */
    @Test
    void testColumnIncrementsWithCharacters() throws IOException {
        fastReader = new FastReader(new StringReader("abc"));
        assertEquals(0, fastReader.getCol());
        // read 'a'
        assertEquals('a', fastReader.read());
        assertEquals(1, fastReader.getCol());
        // read 'b'
        assertEquals('b', fastReader.read());
        assertEquals(2, fastReader.getCol());
        // read 'c'
        assertEquals('c', fastReader.read());
        assertEquals(3, fastReader.getCol());
    }

    /**
     * When a newline character is encountered, the column should reset to 0.
     */
    @Test
    void testColumnResetsAfterNewline() throws IOException {
        fastReader = new FastReader(new StringReader("ab\nc"));
        // read 'a' and 'b'
        // 'a'
        fastReader.read();
        // 'b'
        fastReader.read();
        assertEquals(2, fastReader.getCol(), "Column should be 2 after two characters");
        // read newline – column should reset
        // '\n'
        fastReader.read();
        assertEquals(0, fastReader.getCol(), "Column should reset to 0 after newline");
        // read 'c' – column should be 1 again
        // 'c'
        fastReader.read();
        assertEquals(1, fastReader.getCol(), "Column should be 1 after reading a character post‑newline");
    }

    /**
     * Verify that pushback correctly adjusts the column count.
     * After pushing back a character, the column should decrement.
     */
    @Test
    void testColumnAfterPushback() throws IOException {
        fastReader = new FastReader(new StringReader("xy"));
        // 'x' -> col = 1
        fastReader.read();
        // 'y' -> col = 2
        fastReader.read();
        assertEquals(2, fastReader.getCol());
        // push back 'y'
        fastReader.pushback('y');
        // column should go back to 1 (the position before the pushed‑back char)
        assertEquals(1, fastReader.getCol(), "Column should decrement after a pushback of a non‑newline character");
    }

    /**
     * Ensure that the column handling works with custom buffer sizes.
     */
    @Test
    void testColumnWithCustomBufferSizes() throws IOException {
        FastReader customReader = new FastReader(new StringReader("12345"), 8, 2);
        // read three characters
        // '1'
        customReader.read();
        // '2'
        customReader.read();
        // '3'
        customReader.read();
        assertEquals(3, customReader.getCol());
        // clean up
        customReader.close();
    }

    /**
     * Basic behavior – using reflection to set the private {@code col} field.
     */
    @Test
    void testGetColReflectivelySetValue() throws Exception {
        // set private field 'col' to 7 via reflection
        Field colField = FastReader.class.getDeclaredField("col");
        colField.setAccessible(true);
        colField.setInt(fastReader, 7);
        // getCol should return the value we just set
        assertEquals(7, fastReader.getCol(), "getCol() must return the value set reflectively on the private field");
    }

    /**
     * After reading three characters from a simple source, column should be 3.
     */
    @Test
    void testGetColAfterReadingThreeChars() throws IOException {
        fastReader = new FastReader(new StringReader("abc"));
        // a
        fastReader.read();
        // b
        fastReader.read();
        // c
        fastReader.read();
        assertEquals(3, fastReader.getCol(), "After three reads the column counter must be 3");
    }

    /**
     * Newline handling – read 'a', newline, then 'b'. Expected column = 1.
     */
    @Test
    void testGetColAfterNewlineSequence() throws IOException {
        fastReader = new FastReader(new StringReader("a\nb"));
        // 'a' -> col 1
        fastReader.read();
        // '\n' -> col reset to 0
        fastReader.read();
        // 'b' -> col 1
        fastReader.read();
        assertEquals(1, fastReader.getCol(), "Column should be 1 after reading a newline‑reset sequence");
    }

    /**
     * Pushback after reading one character should bring column back to 0.
     */
    @Test
    void testGetColAfterPushbackToZero() throws IOException {
        fastReader = new FastReader(new StringReader("ab"));
        // 'a' -> col 1
        fastReader.read();
        // push back same char
        fastReader.pushback('a');
        assertEquals(0, fastReader.getCol(), "Column should be 0 after pushing back the only character read");
    }

    /**
     * Bulk read using {@code read(char[],int,int)} should increase column by the
     * number of characters actually read.
     */
    @Test
    void testGetColAfterBulkRead() throws IOException {
        fastReader = new FastReader(new StringReader("hello"));
        char[] buffer = new char[5];
        int read = fastReader.read(buffer, 0, buffer.length);
        assertEquals(5, read, "Should read 5 characters");
        assertEquals(5, fastReader.getCol(), "Column must equal the number of characters read in bulk");
    }

    /**
     * EOF on an empty source – column stays at its initial value 0.
     */
    @Test
    void testGetColAfterEofOnEmptySource() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        // should be -1
        int result = fastReader.read();
        assertEquals(-1, result, "Reading from empty source must return -1 (EOF)");
        assertEquals(0, fastReader.getCol(), "Column should remain 0 after EOF on an empty source");
    }

    /**
     * Multiple line reads – after processing three lines, column equals length of
     * the last line (5).
     */
    @Test
    void testGetColAfterMultipleLineReads() throws IOException {
        String source = "line1\nline2\nline3";
        fastReader = new FastReader(new StringReader(source));
        // read everything character‑by‑character
        int ch;
        while ((ch = fastReader.read()) != -1) {
            // loop just to exhaust the reader
        }
        // after the final newline, column should be length of "line3" = 5
        assertEquals(5, fastReader.getCol(), "After reading multiple lines, column must equal length of the final line");
    }

    /**
     * Calling {@code getCol()} on a {@code null} reference must throw
     * {@link NullPointerException}.
     */
    @Test
    void testGetColOnNullReferenceThrowsNPE() {
        FastReader nullReader = null;
        assertThrows(NullPointerException.class, nullReader::getCol, "Invoking getCol() on a null FastReader should throw NullPointerException");
    }

    /* ----------------------------------------------------------------------
     * Parameterized test example – demonstrates how multiple similar cases
     * could be expressed with a single method. Not required by the JSON but
     * kept for illustration and future extension.
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] source=''{0}'' expectedCol={1}")
    @MethodSource("provideSourceAndExpectedCol")
    void parameterizedColumnTests(String source, int expectedCol) throws IOException {
        fastReader = new FastReader(new StringReader(source));
        // read everything
        while (fastReader.read() != -1) {
            /* consume */
        }
        assertEquals(expectedCol, fastReader.getCol(), "Column after reading entire source must match expected value");
    }
}
