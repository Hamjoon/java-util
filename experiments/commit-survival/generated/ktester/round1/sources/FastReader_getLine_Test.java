package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class FastReader_getLine_Test {

    private FastReader fastReader;

    private StringReader stringReader;

    @BeforeEach
    void setupBeforeEach() {
        // Sample text containing three lines separated by '\n'
        String data = "first line\nsecond line\nthird line";
        stringReader = new StringReader(data);
        // Use reasonable buffer sizes; they are not the focus of the test
        fastReader = new FastReader(stringReader, 1024, 128);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Close the FastReader (which also closes the underlying StringReader)
        if (fastReader != null) {
            fastReader.close();
        }
    }

    /**
     * Provides test data matching the JSON cases under the group
     * “Line number after reading characters”.
     */
    private static Stream<Arguments> lineNumberAfterReadingProvider() {
        return Stream.of(// 1. No line terminators – line stays at 1
        Arguments.of("abcde", 1, "Reading characters that are not line terminators should keep the line number at 1."), // 2. Single LF – line becomes 2
        Arguments.of("abcde\n", 2, "After reading a single '\\n' character, getLine() should return 2."), // 3. Windows CRLF – line becomes 2 (only one increment)
        Arguments.of("abcde\r\n", 2, "Reading a '\\r' followed by a '\\n' (Windows line ending) should increment the line number only once, resulting in line 2."), // 4. Two consecutive LFs – line becomes 3
        Arguments.of("\n\n", 3, "Reading two consecutive '\\n' characters should increment the line number twice, resulting in line 3."), // 5. Lone CR – line becomes 2
        Arguments.of("abcde\rdef", 2, "Reading a '\\r' not followed by '\\n' should increment the line number once, resulting in line 2."));
    }

    @Test
    void testInitialLine() {
        // When a FastReader is freshly created the line number should be 1
        assertEquals(1, fastReader.getLine(), "Initial line number should be 1");
    }

    @Test
    void testLineIncrementAfterFirstNewline() throws IOException {
        // Consume characters until the first newline is encountered
        int ch;
        while ((ch = fastReader.read()) != -1 && ch != '\n') {
            // loop body intentionally empty – just advancing the reader
        }
        // After reading the newline, the line counter should have moved to 2
        assertEquals(2, fastReader.getLine(), "Line number should be 2 after the first newline");
    }

    @Test
    void testLineIncrementAfterSecondNewline() throws IOException {
        // Consume characters up to and including the second newline
        int newlineCount = 0;
        int ch;
        while ((ch = fastReader.read()) != -1) {
            if (ch == '\n') {
                newlineCount++;
                if (newlineCount == 2) {
                    break;
                }
            }
        }
        // After reading two newlines, the line counter should be 3
        assertEquals(3, fastReader.getLine(), "Line number should be 3 after the second newline");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for various line‑terminator scenarios
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("lineNumberAfterReadingProvider")
    void testLineNumberAfterReading(String input, int expectedLine, String description) throws IOException {
        // Re‑initialize the FastReader with the test‑specific input
        fastReader = new FastReader(new StringReader(input), 1024, 128);
        // Read the whole stream to force line counting logic to run
        while (fastReader.read() != -1) {
            // no‑op
        }
        assertEquals(expectedLine, fastReader.getLine(), () -> "Line number after reading: " + description);
    }

    /* -------------------------------------------------------------
     *  Tests that involve pushback behaviour
     * ------------------------------------------------------------- */
    @Test
    void testPushbackNonLineTerminatorDoesNotChangeLine() throws IOException {
        // Read a normal character, push it back and verify line stays at 1
        // reads 'f' from default data
        int ch = fastReader.read();
        assertNotEquals('\n', ch);
        fastReader.pushback((char) ch);
        assertEquals(1, fastReader.getLine(), "Calling pushback on a non‑line‑terminator character should not affect the line number; it remains 1.");
    }

    @Test
    void testPushbackNewlineDecrementsLine() throws IOException {
        // Read a newline, causing line to become 2
        while (fastReader.read() != '\n') {
            // skip until first newline
        }
        assertEquals(2, fastReader.getLine(), "Line should be 2 after reading the first '\\n'.");
        // Push the newline back – line counter must go back to 1
        fastReader.pushback('\n');
        assertEquals(1, fastReader.getLine(), "Pushing back a '\\n' should decrement the line number back to 1.");
        // Read the newline again and verify line returns to 2
        // reads the pushed‑back '\n'
        fastReader.read();
        assertEquals(2, fastReader.getLine(), "Reading the '\\n' again should bring the line number back to 2.");
    }

    @Test
    void testPushbackWindowsLineEndingDecrementsOnce() throws IOException {
        // Read a Windows line ending "\r\n"
        while (fastReader.read() != '\r') {
            // skip until carriage return
        }
        // At this point we have read '\r', line should be 2 (CR alone counts as a line break)
        assertEquals(2, fastReader.getLine(), "Line should be 2 after reading '\\r'.");
        // Read the following '\n' – FastReader should NOT increment again
        // consumes '\n'
        fastReader.read();
        assertEquals(2, fastReader.getLine(), "Line should stay 2 after reading the '\\n' that follows a '\\r' (CRLF).");
        // Push back the '\n' and verify line stays 2 (no extra decrement)
        fastReader.pushback('\n');
        assertEquals(2, fastReader.getLine(), "Pushing back the '\\n' of a CRLF should not change the line number.");
        // Push back the '\r' – this should decrement the line number to 1
        fastReader.pushback('\r');
        assertEquals(1, fastReader.getLine(), "Pushing back the '\\r' of a CRLF should decrement the line number back to 1.");
    }
}
