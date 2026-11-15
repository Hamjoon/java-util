package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastReader_movePosition_Test {

    private TestFastReader fastReader;

    private static final int BUFFER_SIZE = 16;

    private static final int PUSHBACK_SIZE = 4;

    /**
     * Simple concrete subclass exposing the protected {@code movePosition} method.
     */
    private static class TestFastReader extends FastReader {

        TestFastReader(Reader in, int bufferSize, int pushbackBufferSize) {
            super(in, bufferSize, pushbackBufferSize);
        }

        /**
         * Public wrapper used by the test cases.
         */
        void invokeMovePosition(char ch) {
            movePosition(ch);
        }
    }

    /* --------------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final char[] sequence;

        final int expectedLine;

        final int expectedCol;

        final String description;

        ArgumentsHolder(char[] sequence, int expectedLine, int expectedCol, String description) {
            this.sequence = sequence;
            this.expectedLine = expectedLine;
            this.expectedCol = expectedCol;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Use a simple StringReader as the underlying source – its content is irrelevant for movePosition.
        Reader source = new StringReader("dummy");
        fastReader = new TestFastReader(source, BUFFER_SIZE, PUSHBACK_SIZE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
    }

    /**
     * Provides arguments for the branch‑coverage group.
     * Each element is an array: {char[] sequence, expectedLine, expectedCol, description}
     */
    private static Stream<ArgumentsHolder> branchCoverageProvider() {
        return Stream.of(new ArgumentsHolder(new char[] { '\n' }, 2, 0, "When the character is a line‑feed (0x0a), the line counter should be incremented and the column reset to zero."), new ArgumentsHolder(new char[] { 'a' }, 1, 1, "A regular non‑newline character should leave the line unchanged and increment the column from its initial value (0) to 1."), new ArgumentsHolder(new char[] { 'b', 'c' }, 1, 2, "Calling movePosition twice with non‑newline characters should increase the column consecutively while keeping the line at 1."), new ArgumentsHolder(new char[] { 'x', '\n' }, 2, 0, "After advancing the column with a normal character, a subsequent newline should reset the column to zero and increment the line."), new ArgumentsHolder(new char[] { '\0' }, 1, 1, "The null character (0x00) is not a line‑feed; it should be treated like any other non‑newline and only increment the column."), new ArgumentsHolder(new char[] { '\n', '\n' }, 3, 0, "Multiple consecutive newline characters should keep resetting the column to zero while continuously incrementing the line counter."));
    }

    /**
     * Provides arguments for the additional‑cases group.
     */
    private static Stream<ArgumentsHolder> additionalCasesProvider() {
        return Stream.of(new ArgumentsHolder(new char[] { '\r' }, 1, 1, "Carriage‑return (0x0d) is not a line‑feed; it should be treated like any other non‑newline character, incrementing the column while leaving the line unchanged."), new ArgumentsHolder(new char[] { '\t' }, 1, 1, "Tab character (0x09) is also a non‑newline; the column should increase by one and the line should stay at 1."), new ArgumentsHolder(new char[] { 'a', 'b', 'c', '\n', 'd', 'e' }, 2, 2, "Three normal characters advance column to 3, a newline resets column to 0 and increments line to 2, then two more characters bring column to 2 on line 2."), new ArgumentsHolder(new char[] { 'x', '\n', '\n', 'y' }, 3, 1, "A normal character, two consecutive newlines, then another normal character: column resets after each newline, line ends at 3, final column is 1."), new ArgumentsHolder(new char[] { '\u03A9' }, 1, 1, "A Unicode character (Greek capital omega) is not a line‑feed; it should increment the column exactly like any other non‑newline character."));
    }

    @Test
    void movePosition_newlineIncrementsLineAndResetsColumn() {
        // Given: initial line = 1, column = 0 (as per FastReader defaults)
        assertEquals(1, fastReader.getLine(), "Initial line should be 1");
        assertEquals(0, fastReader.getCol(), "Initial column should be 0");
        // When: a newline character (0x0A) is processed
        fastReader.invokeMovePosition('\n');
        // Then: line is incremented and column reset to 0
        assertEquals(2, fastReader.getLine(), "Line should be incremented after newline");
        assertEquals(0, fastReader.getCol(), "Column should be reset to 0 after newline");
    }

    @Test
    void movePosition_regularCharacterIncrementsColumnOnly() {
        // Given: start at line 1, column 0
        assertEquals(1, fastReader.getLine());
        assertEquals(0, fastReader.getCol());
        // When: a non‑newline character is processed
        fastReader.invokeMovePosition('a');
        // Then: line stays the same, column increments by 1
        assertEquals(1, fastReader.getLine(), "Line should remain unchanged for non‑newline");
        assertEquals(1, fastReader.getCol(), "Column should increment by 1 for non‑newline");
    }

    @Test
    void movePosition_multipleCallsMaintainStateCorrectly() {
        // Sequence: 'x', '\n', 'y', 'z'
        // col = 1
        fastReader.invokeMovePosition('x');
        // line = 2, col = 0
        fastReader.invokeMovePosition('\n');
        // col = 1
        fastReader.invokeMovePosition('y');
        // col = 2
        fastReader.invokeMovePosition('z');
        assertEquals(2, fastReader.getLine(), "Line should be 2 after one newline");
        assertEquals(2, fastReader.getCol(), "Column should be 2 after processing y and z");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("branchCoverageProvider")
    void movePosition_branchCoverage(ArgumentsHolder args) {
        // Given: initial state (line=1, col=0) verified by FastReader defaults
        assertEquals(1, fastReader.getLine(), "Pre‑condition: line must start at 1");
        assertEquals(0, fastReader.getCol(), "Pre‑condition: column must start at 0");
        // When: invoke movePosition for each character in the sequence
        for (char ch : args.sequence) {
            fastReader.invokeMovePosition(ch);
        }
        // Then: verify final line/column match expected values
        assertEquals(args.expectedLine, fastReader.getLine(), "Line after processing sequence " + java.util.Arrays.toString(args.sequence));
        assertEquals(args.expectedCol, fastReader.getCol(), "Column after processing sequence " + java.util.Arrays.toString(args.sequence));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("additionalCasesProvider")
    void movePosition_additionalCases(ArgumentsHolder args) {
        // Same structure as the branch‑coverage test
        assertEquals(1, fastReader.getLine(), "Pre‑condition: line must start at 1");
        assertEquals(0, fastReader.getCol(), "Pre‑condition: column must start at 0");
        for (char ch : args.sequence) {
            fastReader.invokeMovePosition(ch);
        }
        assertEquals(args.expectedLine, fastReader.getLine(), "Line after processing sequence " + java.util.Arrays.toString(args.sequence));
        assertEquals(args.expectedCol, fastReader.getCol(), "Column after processing sequence " + java.util.Arrays.toString(args.sequence));
    }
}
