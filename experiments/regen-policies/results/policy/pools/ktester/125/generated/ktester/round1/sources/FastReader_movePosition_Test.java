package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class FastReader_movePosition_Test {

    private TestableFastReader reader;

    /**
     * Simple subclass that makes {@code movePosition} public for test access.
     */
    private static class TestableFastReader extends FastReader {

        TestableFastReader(Reader in, int bufferSize, int pushbackBufferSize) {
            super(in, bufferSize, pushbackBufferSize);
        }

        /**
         * Expose the protected method so the test can call it directly.
         */
        @Override
        public void movePosition(char ch) {
            super.movePosition(ch);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a minimal, non‑null Reader; the actual content is irrelevant for movePosition.
        Reader dummy = new StringReader("");
        // Reasonable buffer sizes – they are not used by movePosition.
        reader = new TestableFastReader(dummy, 16, 4);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the reader is closed after each test to avoid resource leaks.
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ignored) {
            // Ignored – cleanup only.
        }
        reader = null;
    }

    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(Arguments.of('\n', "line incremented by 1, col reset to 0"), Arguments.of('a', "col incremented by 1, line unchanged"), Arguments.of('\r', "col incremented by 1, line unchanged"), Arguments.of('\0', "col incremented by 1, line unchanged"));
    }

    private static Stream<Arguments> unicodeCharProvider() {
        return Stream.of(Arguments.of('\u20AC'), Arguments.of('\uFFFF'), Arguments.of('\u000B'));
    }

    private static Stream<Arguments> sequenceProvider() {
        return Stream.of(Arguments.of(new String[] { "a", "b", "c", "\n", "d", "e" }, 2, 2), Arguments.of(new String[] { "\n", "\n", "\n" }, 4, 0), Arguments.of(new String[] { "a", "b", "c", "d", "e", "\n", "f", "g", "\n", "\n" }, 4, 0));
    }

    private static char unescape(String token) {
        return switch(token) {
            case "\\n" ->
                '\n';
            case "\\r" ->
                '\r';
            case "\\t" ->
                '\t';
            case "\\b" ->
                '\b';
            case "\\f" ->
                '\f';
            case "\\0" ->
                '\0';
            default ->
                {
                    if (token.startsWith("\\u") && token.length() == 6) {
                        int code = Integer.parseInt(token.substring(2), 16);
                        ???;
                    }
                    ???;
                }
        };
    }

    @Test
    void movePosition_incrementsColumnForRegularChar() {
        // Initial state: line = 1, col = 0
        assertEquals(1, reader.getLine(), "Initial line should be 1");
        assertEquals(0, reader.getCol(), "Initial column should be 0");
        // Act: move position with a regular character (e.g., 'a')
        reader.movePosition('a');
        // Verify: line unchanged, column incremented by 1
        assertEquals(1, reader.getLine(), "Line should remain unchanged for non‑newline");
        assertEquals(1, reader.getCol(), "Column should increment for regular character");
    }

    @Test
    void movePosition_resetsColumnAndIncrementsLineOnNewline() {
        // Simulate having read some characters first
        // col = 1
        reader.movePosition('x');
        // col = 2
        reader.movePosition('y');
        assertEquals(1, reader.getLine(), "Line should still be 1 before newline");
        assertEquals(2, reader.getCol(), "Column should be 2 before newline");
        // Act: move position with a newline character (0x0a)
        reader.movePosition('\n');
        // Verify: line incremented, column reset to 0
        assertEquals(2, reader.getLine(), "Line should increment after newline");
        assertEquals(0, reader.getCol(), "Column should reset to 0 after newline");
    }

    @Test
    void movePosition_multipleNewlinesAccumulateCorrectly() {
        // First newline
        reader.movePosition('\n');
        assertEquals(2, reader.getLine(), "Line after first newline");
        assertEquals(0, reader.getCol(), "Column after first newline");
        // Second newline
        reader.movePosition('\n');
        assertEquals(3, reader.getLine(), "Line after second newline");
        assertEquals(0, reader.getCol(), "Column after second newline");
    }

    @Test
    void movePosition_mixedSequenceProducesExpectedLineAndColumn() {
        // Sequence: a, b, newline, c, newline, d
        // col=1
        reader.movePosition('a');
        // col=2
        reader.movePosition('b');
        // line=2, col=0
        reader.movePosition('\n');
        // col=1
        reader.movePosition('c');
        // line=3, col=0
        reader.movePosition('\n');
        // col=1
        reader.movePosition('d');
        assertEquals(3, reader.getLine(), "Final line after mixed sequence");
        assertEquals(1, reader.getCol(), "Final column after mixed sequence");
    }

    @ParameterizedTest(name = "[{index}] ch={0} → {1}")
    @MethodSource("branchCoverageProvider")
    void movePosition_branchCoverage(char ch, String expectation) {
        assertEquals(1, reader.getLine(), "Line should start at 1");
        assertEquals(0, reader.getCol(), "Column should start at 0");
        reader.movePosition(ch);
        switch(expectation) {
            case "line incremented by 1, col reset to 0" ->
                {
                    assertEquals(2, reader.getLine(), "Line should have been incremented");
                    assertEquals(0, reader.getCol(), "Column should have been reset");
                }
            case "col incremented by 1, line unchanged" ->
                {
                    assertEquals(1, reader.getLine(), "Line should remain unchanged");
                    assertEquals(1, reader.getCol(), "Column should have been incremented");
                }
            default ->
                fail("Unexpected expectation: " + expectation);
        }
    }

    @ParameterizedTest(name = "[{index}] Unicode char {0} → column+1")
    @MethodSource("unicodeCharProvider")
    void movePosition_unicodeCharacters(char ch) {
        int startLine = reader.getLine();
        int startCol = reader.getCol();
        reader.movePosition(ch);
        assertEquals(startLine, reader.getLine(), "Unicode character should not affect line number");
        assertEquals(startCol + 1, reader.getCol(), "Unicode character should increment column");
    }

    @ParameterizedTest(name = "[{index}] Sequence {0} → line={1}, col={2}")
    @MethodSource("sequenceProvider")
    void movePosition_sequenceHandling(String[] chars, int expectedLine, int expectedCol) {
        for (String s : chars) {
            char ch = unescape(s);
            reader.movePosition(ch);
        }
        assertEquals(expectedLine, reader.getLine(), "Final line number does not match expected");
        assertEquals(expectedCol, reader.getCol(), "Final column number does not match expected");
    }
}
