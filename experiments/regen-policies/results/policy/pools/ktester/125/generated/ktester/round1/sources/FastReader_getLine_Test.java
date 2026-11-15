package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class FastReader_getLine_Test {

    /**
     * The FastReader instance used in each test.
     */
    private FastReader fastReader;

    /**
     * Sample input containing two lines.
     */
    private static final String SAMPLE_TEXT = "first line\nsecond line\n";

    /**
     * Buffer size for the FastReader – large enough for the sample text.
     */
    private static final int BUFFER_SIZE = 1024;

    /**
     * Push‑back buffer size – not needed for these tests but must be non‑negative.
     */
    private static final int PUSHBACK_BUFFER_SIZE = 10;

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple StringReader as the underlying source.
        StringReader stringReader = new StringReader(SAMPLE_TEXT);
        fastReader = new FastReader(stringReader, BUFFER_SIZE, PUSHBACK_BUFFER_SIZE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the FastReader (and its underlying reader) are closed after each test.
        if (fastReader != null) {
            fastReader.close();
        }
    }

    /**
     * Supplies the test data for {@link #testGetLineVarious(String, int, String, ReadMode)}.
     *
     * The data mirrors the JSON cases:
     * <ul>
     *   <li>empty input → line 1</li>
     *   <li>\"First line\\nSecond line\" → line 2 after first newline</li>
     *   <li>\"Line1\\nLine2\\nLine3\" → line 3 after reading all</li>
     *   <li>\"No newline here\" → line 1 after reading all</li>
     *   <li>\"\\n\\n\\n\" → line 4 after reading all three newlines</li>
     * </ul>
     *
     * @return a stream of arguments for the parameterized test
     */
    private static Stream<Arguments> provideGetLineCases() {
        return Stream.of(// Case: empty content – no newline characters.
        Arguments.of("", 1, "When the reader is empty, line should remain 1", ReadMode.ALL), // Case: read up to the first newline.
        Arguments.of("First line\nSecond line", 2, "After reading up to the first newline, line should be 2", ReadMode.FIRST_NEWLINE), // Case: two newlines total → line 3.
        Arguments.of("Line1\nLine2\nLine3", 3, "Reading through two newline characters should result in line 3", ReadMode.ALL), // Case: no newline at all.
        Arguments.of("No newline here", 1, "If the input contains no newline characters, line should stay at 1", ReadMode.ALL), // Case: three consecutive newlines → line 4.
        Arguments.of("\n\n\n", 4, "Three consecutive newlines should increase line from 1 to 4", ReadMode.ALL));
    }

    /**
     * Verify that a newly created FastReader reports line number 1.
     */
    @Test
    void testInitialLineIsOne() {
        fastReader = new FastReader(new StringReader(""), BUFFER_SIZE, PUSHBACK_BUFFER_SIZE);
        assertEquals(1, fastReader.getLine(), "A freshly constructed FastReader should start on line 1");
    }

    /**
     * Verify that reading characters up to a newline increments the line counter.
     *
     * @throws IOException if an I/O error occurs while reading.
     */
    @Test
    void testLineIncrementsAfterReadingNewline() throws IOException {
        // Read characters one by one until the first newline is encountered.
        int ch;
        while ((ch = fastReader.read()) != -1) {
            if (ch == '\n') {
                break;
            }
        }
        // After the first newline, the line number should be 2.
        assertEquals(2, fastReader.getLine(), "Line number should be incremented after reading a newline character");
    }

    /**
     * Verify that the line number reflects the total number of newline characters read.
     *
     * @throws IOException if an I/O error occurs while reading.
     */
    @Test
    void testLineAfterReadingMultipleLines() throws IOException {
        // Read the entire content.
        char[] buffer = new char[64];
        while (fastReader.read(buffer) != -1) {
            // continue reading until EOF
        }
        // The sample text contains two newline characters, so line should be 3 (1 + 2).
        assertEquals(3, fastReader.getLine(), "Line number should equal 1 plus the count of newline characters read");
    }

    /**
     * Parameterized test that exercises the various scenarios described in the
     * JSON test cases.  Each case provides the input string, the expected line
     * number after reading, a human‑readable description and the reading mode.
     *
     * @param content     the text supplied to the underlying {@link StringReader}
     * @param expected    the line number that {@link FastReader#getLine()} should return
     * @param description a short description used as the assertion message
     * @param mode        how many characters to read before checking the line counter
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideGetLineCases")
    void testGetLineVarious(String content, int expected, String description, ReadMode mode) throws IOException {
        fastReader = new FastReader(new StringReader(content), BUFFER_SIZE, PUSHBACK_BUFFER_SIZE);
        switch(mode) {
            case FIRST_NEWLINE:
                // Read characters one‑by‑one until the first newline (or EOF) is encountered.
                int ch;
                while ((ch = fastReader.read()) != -1) {
                    if (ch == '\n') {
                        break;
                    }
                }
                break;
            case ALL:
                // Consume the whole stream.
                char[] buffer = new char[64];
                while (fastReader.read(buffer) != -1) {
                    // loop until EOF
                }
                break;
        }
        assertEquals(expected, fastReader.getLine(), description);
    }
}
