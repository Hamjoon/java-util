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

    private static final int BUFFER_SIZE = 16;

    private static final int PUSHBACK_BUFFER_SIZE = 8;

    private StringReader stringReader;

    private FastReader fastReader;

    /**
     * Helper subclass that makes the protected {@code movePosition} method publicly accessible for testing.
     */
    private static class TestableFastReader extends FastReader {

        TestableFastReader(StringReader r, int bufferSize, int pushbackSize) {
            super(r, bufferSize, pushbackSize);
        }

        /**
         * Expose protected method
         */
        @Override
        public void movePosition(char ch) {
            super.movePosition(ch);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The underlying reader supplies a known string so we can predict reads
        stringReader = new StringReader("abcdef");
        fastReader = new FastReader(stringReader, BUFFER_SIZE, PUSHBACK_BUFFER_SIZE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Close the FastReader (which also closes the underlying StringReader)
        fastReader.close();
    }

    /* --------------------------------------------------------------------- *
     * Utility method for reflection manipulation of the private 'col' field *
     * --------------------------------------------------------------------- */
    private void setPrivateCol(int value) throws Exception {
        Field colField = FastReader.class.getDeclaredField("col");
        colField.setAccessible(true);
        colField.setInt(fastReader, value);
    }

    @Test
    void getCol_initiallyZero() {
        assertEquals(0, fastReader.getCol(), "Column count should be 0 before any read operation");
    }

    @Test
    void getCol_afterSingleCharRead() throws IOException {
        // reads 'a'
        int ch = fastReader.read();
        assertNotEquals(-1, ch, "Read should return a character");
        assertEquals(1, fastReader.getCol(), "Column count should be 1 after reading a single character");
    }

    @Test
    void getCol_afterMultipleCharRead() throws IOException {
        char[] buffer = new char[3];
        // reads "abc"
        int read = fastReader.read(buffer, 0, buffer.length);
        assertEquals(3, read, "Should have read three characters");
        assertEquals(3, fastReader.getCol(), "Column count should reflect the number of characters read");
    }

    @Test
    void getCol_afterReadThenPushback() throws IOException {
        // Read two characters: "ab"
        char[] buf = new char[2];
        int read = fastReader.read(buf, 0, 2);
        assertEquals(2, read);
        assertEquals(2, fastReader.getCol());
        // Push back the last character ('b')
        fastReader.pushback('b');
        // After pushback, the column should be decremented by one
        // (the implementation of pushback ultimately calls movePosition with the char,
        // which updates the column accordingly)
        assertEquals(1, fastReader.getCol(), "Column count should decrement after a pushback operation");
    }

    /**
     * Group: Column after moving position with a regular character
     */
    @Test
    void getCol_afterMovePositionRegularChar() {
        ((TestableFastReader) fastReader).movePosition('a');
        assertEquals(1, fastReader.getCol(), "movePosition('a') should increment column to 1");
    }

    /**
     * Group: Column after moving position with a newline character
     */
    @Test
    void getCol_afterMovePositionNewline() {
        ((TestableFastReader) fastReader).movePosition('\n');
        assertEquals(0, fastReader.getCol(), "movePosition('\\n') should reset column to 0");
    }

    /**
     * Group: Column after moving position with carriage return
     */
    @Test
    void getCol_afterMovePositionCarriageReturn() {
        ((TestableFastReader) fastReader).movePosition('\r');
        assertEquals(0, fastReader.getCol(), "movePosition('\\r') should reset column to 0");
    }

    /**
     * Group: Column after multiple moves
     */
    @Test
    void getCol_afterMultipleMovePositionCalls() {
        ((TestableFastReader) fastReader).movePosition('a');
        ((TestableFastReader) fastReader).movePosition('b');
        assertEquals(2, fastReader.getCol(), "Two regular movePosition calls should result in column 2");
    }

    /**
     * Group: Column after read and pushback (single char)
     */
    @Test
    void getCol_afterReadAndPushbackSingleChar() throws IOException {
        // reads 'a' -> col = 1
        fastReader.read();
        assertEquals(1, fastReader.getCol());
        // should decrement back to 0
        fastReader.pushback('a');
        assertEquals(0, fastReader.getCol(), "Read then pushback same char should bring column back to 0");
    }

    /**
     * Group: Column value set via reflection
     */
    @Test
    void getCol_reflectionSetValue() throws Exception {
        setPrivateCol(5);
        assertEquals(5, fastReader.getCol(), "Reflection should allow setting col to 5");
    }

    /**
     * Group: Column after reading multiple characters (read() three times)
     */
    @Test
    void getCol_afterThreeSingleCharReads() throws IOException {
        // a
        fastReader.read();
        // b
        fastReader.read();
        // c
        fastReader.read();
        assertEquals(3, fastReader.getCol(), "Three successive read() calls should increment column to 3");
    }

    /**
     * Group: Column after reading with buffer and offset
     */
    @Test
    void getCol_afterReadWithOffsetAndLen() throws IOException {
        char[] buf = new char[5];
        // offset = 1, len = 2 -> should read two chars and increase column by 2
        int read = fastReader.read(buf, 1, 2);
        assertEquals(2, read);
        assertEquals(2, fastReader.getCol(), "read(buf, off, len) should increase column by len irrespective of offset");
    }

    /**
     * Group: Column unchanged after EOF read
     */
    @Test
    void getCol_unchangedAfterEof() throws IOException {
        // Exhaust the underlying source
        while (fastReader.read() != -1) {
            /* consume */
        }
        int colBefore = fastReader.getCol();
        // should return -1
        int eof = fastReader.read();
        assertEquals(-1, eof, "EOF should be indicated by -1");
        assertEquals(colBefore, fastReader.getCol(), "Column must remain unchanged after EOF read");
    }

    /**
     * Group: Column after multiple pushbacks
     */
    @Test
    void getCol_afterMultiplePushbacks() throws IOException {
        // a -> col 1
        fastReader.read();
        // b -> col 2
        fastReader.read();
        assertEquals(2, fastReader.getCol());
        fastReader.pushback('b');
        fastReader.pushback('a');
        assertEquals(0, fastReader.getCol(), "Two pushbacks should bring column back to 0");
    }

    /**
     * Group: Column after close() call
     */
    @Test
    void getCol_afterCloseDoesNotChange() throws IOException {
        fastReader.close();
        assertEquals(0, fastReader.getCol(), "Close should not modify column; newly constructed reader stays at 0");
    }

    /**
     * Group: Column after setting negative value via reflection
     */
    @Test
    void getCol_reflectionSetNegativeValue() throws Exception {
        setPrivateCol(-3);
        assertEquals(-3, fastReader.getCol(), "Reflection should allow negative column values to be returned unchanged");
    }
}
