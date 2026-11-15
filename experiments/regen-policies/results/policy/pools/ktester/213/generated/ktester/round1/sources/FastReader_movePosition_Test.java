package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FastReader_movePosition_Test {

    private TestFastReader fastReader;

    /**
     * Simple concrete subclass that makes {@code movePosition} public for the test.
     */
    private static final class TestFastReader extends FastReader {

        TestFastReader() {
            super(new StringReader(""));
        }

        /**
         * Expose the protected method so the test can invoke it directly.
         */
        void exposeMovePosition(char ch) {
            super.movePosition(ch);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        fastReader = new TestFastReader();
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
    }

    /**
     * Arguments for newline‑handling group.
     */
    private static Stream<Arguments> newlineHandlingProvider() {
        return Stream.of(// first newline
        Arguments.of(new char[] { '\n' }, 2, 0, "First newline should increment line to 2 and reset column."), // two consecutive newlines
        Arguments.of(new char[] { '\n', '\n' }, 3, 0, "Two newlines should increment line twice, column stays 0."));
    }

    /**
     * Arguments for regular‑character handling group.
     */
    private static Stream<Arguments> regularCharHandlingProvider() {
        return Stream.of(// single regular char
        Arguments.of(new char[] { 'a' }, 1, 1, "One regular char increments column to 1, line unchanged."), // two regular chars sequentially
        Arguments.of(new char[] { 'a', 'b' }, 1, 2, "Two regular chars should result in column 2, line unchanged."));
    }

    /**
     * Arguments for mixed newline/character sequences.
     */
    private static Stream<Arguments> mixedSequenceProvider() {
        return Stream.of(Arguments.of(new char[] { 'a', '\n', 'c' }, 2, 1, "a → newline → c results in line 2, column 1."));
    }

    /**
     * Arguments for carriage‑return handling (treated as a normal char).
     */
    private static Stream<Arguments> carriageReturnProvider() {
        return Stream.of(Arguments.of(new char[] { '\r' }, 1, 1, "Carriage return should behave like a normal character."));
    }

    /**
     * Arguments for null‑character handling (treated as a normal char).
     */
    private static Stream<Arguments> nullCharProvider() {
        return Stream.of(Arguments.of(new char[] { '\0' }, 1, 1, "Null character should increment column, line unchanged."));
    }

    /**
     * Arguments for the more elaborate multiple‑newline scenario.
     */
    private static Stream<Arguments> multipleNewlinesProvider() {
        return Stream.of(Arguments.of(new char[] { 'x', '\n', '\n', 'y' }, 3, 1, "x → newline → newline → y should end at line 3, column 1."));
    }

    @Test
    void movePosition_withNewlineIncrementsLineAndResetsColumn() {
        // initial state: line = 1, col = 0
        assertEquals(1, fastReader.getLine(), "Initial line should be 1");
        assertEquals(0, fastReader.getCol(), "Initial column should be 0");
        // Simulate reading a normal character first
        fastReader.exposeMovePosition('a');
        assertEquals(1, fastReader.getLine(), "Line should stay 1 after a normal char");
        assertEquals(1, fastReader.getCol(), "Column should increment to 1 after a normal char");
        // Now feed a newline character (0x0a)
        fastReader.exposeMovePosition('\n');
        assertEquals(2, fastReader.getLine(), "Line should increment after newline");
        assertEquals(0, fastReader.getCol(), "Column should reset to 0 after newline");
    }

    @Test
    void movePosition_withRegularCharIncrementsColumnOnly() {
        // Starting state
        assertEquals(1, fastReader.getLine());
        assertEquals(0, fastReader.getCol());
        // Feed several regular characters
        fastReader.exposeMovePosition('x');
        fastReader.exposeMovePosition('y');
        fastReader.exposeMovePosition('z');
        assertEquals(1, fastReader.getLine(), "Line should remain unchanged for regular chars");
        assertEquals(3, fastReader.getCol(), "Column should reflect number of regular chars read");
    }

    @Test
    void movePosition_multipleNewlinesTrackLineAndColumnCorrectly() {
        // Feed pattern: char, newline, char, newline, newline
        // col=1
        fastReader.exposeMovePosition('a');
        // line=2, col=0
        fastReader.exposeMovePosition('\n');
        // col=1
        fastReader.exposeMovePosition('b');
        // line=3, col=0
        fastReader.exposeMovePosition('\n');
        // line=4, col=0
        fastReader.exposeMovePosition('\n');
        assertEquals(4, fastReader.getLine(), "Line count should be 4 after three newlines");
        assertEquals(0, fastReader.getCol(), "Column should be reset to 0 after the last newline");
    }

    @Test
    void movePosition_withoutAnyCharactersLeavesCountersUnchanged() {
        // No calls to exposeMovePosition – counters should stay at their defaults.
        assertEquals(1, fastReader.getLine(), "Default line should be 1");
        assertEquals(0, fastReader.getCol(), "Default column should be 0");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("newlineHandlingProvider")
    void movePosition_newlineHandling(char[] sequence, int expectedLine, int expectedCol, String description) {
        for (char ch : sequence) {
            fastReader.exposeMovePosition(ch);
        }
        assertEquals(expectedLine, fastReader.getLine(), "Line counter mismatch");
        assertEquals(expectedCol, fastReader.getCol(), "Column counter mismatch");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("regularCharHandlingProvider")
    void movePosition_regularCharHandling(char[] sequence, int expectedLine, int expectedCol, String description) {
        for (char ch : sequence) {
            fastReader.exposeMovePosition(ch);
        }
        assertEquals(expectedLine, fastReader.getLine(), "Line counter mismatch");
        assertEquals(expectedCol, fastReader.getCol(), "Column counter mismatch");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("mixedSequenceProvider")
    void movePosition_mixedSequence(char[] sequence, int expectedLine, int expectedCol, String description) {
        for (char ch : sequence) {
            fastReader.exposeMovePosition(ch);
        }
        assertEquals(expectedLine, fastReader.getLine(), "Line counter mismatch");
        assertEquals(expectedCol, fastReader.getCol(), "Column counter mismatch");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("carriageReturnProvider")
    void movePosition_carriageReturn(char[] sequence, int expectedLine, int expectedCol, String description) {
        for (char ch : sequence) {
            fastReader.exposeMovePosition(ch);
        }
        assertEquals(expectedLine, fastReader.getLine(), "Line counter mismatch");
        assertEquals(expectedCol, fastReader.getCol(), "Column counter mismatch");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullCharProvider")
    void movePosition_nullChar(char[] sequence, int expectedLine, int expectedCol, String description) {
        for (char ch : sequence) {
            fastReader.exposeMovePosition(ch);
        }
        assertEquals(expectedLine, fastReader.getLine(), "Line counter mismatch");
        assertEquals(expectedCol, fastReader.getCol(), "Column counter mismatch");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("multipleNewlinesProvider")
    void movePosition_multipleNewlinesWithPrecedingColumns(char[] sequence, int expectedLine, int expectedCol, String description) {
        for (char ch : sequence) {
            fastReader.exposeMovePosition(ch);
        }
        assertEquals(expectedLine, fastReader.getLine(), "Line counter mismatch");
        assertEquals(expectedCol, fastReader.getCol(), "Column counter mismatch");
    }
}
