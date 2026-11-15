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

    private static class ReflectionCase {

        final int value;

        final String description;

        ReflectionCase(int value, String description) {
            this.value = value;
            this.description = description;
        }

        @Override
        public String toString() {
            return description + " (expected=" + value + ")";
        }
    }

    /**
     * Create a FastReader instance that reads from a simple StringReader.
     * The buffer sizes are chosen arbitrarily but must be positive.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The underlying source contains three characters and a newline.
        StringReader source = new StringReader("abc\n");
        // bufferSize > 0, pushbackBufferSize >= 0
        fastReader = new FastReader(source, 8, 2);
    }

    /**
     * Close the FastReader after each test to release the underlying Reader.
     */
    @AfterEach
    void teardownAfterEach() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
    }

    private static Stream<ReflectionCase> reflectionCases() {
        return Stream.of(new ReflectionCase(5, "Set col to positive value 5"), new ReflectionCase(-3, "Set col to negative value -3"), new ReflectionCase(10, "Simulate ten reads by setting col to 10"), new ReflectionCase(0, "Reset col to 0 (e.g., after a line‑break)"));
    }

    private void setPrivateCol(FastReader fr, int value) throws Exception {
        Field colField = FastReader.class.getDeclaredField("col");
        colField.setAccessible(true);
        colField.setInt(fr, value);
    }

    @Test
    void getCol_initialValue_isZero() {
        // The column counter should start at 0 before any reading occurs.
        assertEquals(0, fastReader.getCol(), "Initial column should be 0");
    }

    @Test
    void getCol_afterSingleRead_isOne() throws IOException {
        // Read a single character; column should advance to 1.
        // reads 'a'
        int ch = fastReader.read();
        assertNotEquals(-1, ch, "Reader should not be at EOF");
        assertEquals(1, fastReader.getCol(), "Column should be 1 after reading one character");
    }

    @Test
    void getCol_afterMultipleReads_isCorrect() throws IOException {
        // Read three characters sequentially.
        // 'a'
        fastReader.read();
        // 'b'
        fastReader.read();
        // 'c'
        fastReader.read();
        assertEquals(3, fastReader.getCol(), "Column should be 3 after reading three characters");
    }

    @Test
    void getCol_resetsAfterNewline() throws IOException {
        // Read up to and including the newline character.
        // 'a' -> col 1
        fastReader.read();
        // 'b' -> col 2
        fastReader.read();
        // 'c' -> col 3
        fastReader.read();
        // '\n' -> should reset column to 0
        fastReader.read();
        assertEquals(0, fastReader.getCol(), "Column should reset to 0 after reading a newline");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("reflectionCases")
    void getCol_afterManualStateManipulation(ReflectionCase rc) throws Exception {
        setPrivateCol(fastReader, rc.value);
        assertEquals(rc.value, fastReader.getCol(), "getCol() should return the manually set value: " + rc.description);
    }

    @Test
    void getCol_afterSequentialReads_threeChars() throws IOException {
        FastReader fr = new FastReader(new StringReader("abc"), 8, 2);
        // a
        fr.read();
        // b
        fr.read();
        // c
        fr.read();
        assertEquals(3, fr.getCol(), "Column should be 3 after reading three characters");
        fr.close();
    }

    @Test
    void getCol_resetAfterNewline_thenOneMoreChar() throws IOException {
        FastReader fr = new FastReader(new StringReader("ab\nc"), 8, 2);
        // a -> col 1
        fr.read();
        // b -> col 2
        fr.read();
        // \n -> col 0
        fr.read();
        assertEquals(0, fr.getCol(), "Column should be 0 after newline");
        // c -> col 1
        fr.read();
        assertEquals(1, fr.getCol(), "Column should be 1 after reading character following newline");
        fr.close();
    }

    @Test
    void getCol_afterPushback_decrements() throws IOException {
        FastReader fr = new FastReader(new StringReader("x"), 8, 2);
        // col becomes 1
        fr.read();
        assertEquals(1, fr.getCol(), "Column should be 1 after reading 'x'");
        // should decrement column
        fr.pushback('x');
        assertEquals(0, fr.getCol(), "Column should return to 0 after pushback");
        fr.close();
    }

    @Test
    void getCol_afterConsecutivePushbacks() throws IOException {
        FastReader fr = new FastReader(new StringReader("xyz"), 8, 5);
        // x -> col 1
        fr.read();
        // y -> col 2
        fr.read();
        // z -> col 3
        fr.read();
        assertEquals(3, fr.getCol(), "Column should be 3 after three reads");
        // push back 'z' then 'y'
        fr.pushback('z');
        assertEquals(2, fr.getCol(), "Column should be 2 after first pushback");
        fr.pushback('y');
        assertEquals(1, fr.getCol(), "Column should be 1 after second pushback");
        // finally push back 'x'
        fr.pushback('x');
        assertEquals(0, fr.getCol(), "Column should be 0 after third pushback");
        fr.close();
    }

    @Test
    void getCol_afterBulkRead_advancesCorrectly() throws IOException {
        FastReader fr = new FastReader(new StringReader("hello world"), 16, 2);
        char[] buffer = new char[5];
        int read = fr.read(buffer, 0, 5);
        assertEquals(5, read, "Bulk read should return 5 characters");
        assertEquals(5, fr.getCol(), "Column should be 5 after bulk read of 5 chars");
        fr.close();
    }

    @Test
    void getCol_afterEOF_staysAtLastPosition() throws IOException {
        FastReader fr = new FastReader(new StringReader("test"), 8, 2);
        for (int i = 0; i < 4; i++) {
            assertNotEquals(-1, fr.read(), "Should still have characters");
        }
        assertEquals(4, fr.getCol(), "Column should be 4 after reading all characters");
        // One more read returns -1 (EOF)
        assertEquals(-1, fr.read(), "EOF should be indicated by -1");
        assertEquals(4, fr.getCol(), "Column should remain 4 after EOF");
        fr.close();
    }

    @Test
    void getCol_stableAfterClose() throws IOException {
        FastReader fr = new FastReader(new StringReader("ab"), 8, 2);
        // a -> col 1
        fr.read();
        // b -> col 2
        fr.read();
        assertEquals(2, fr.getCol(), "Column should be 2 before close");
        fr.close();
        // Even after close, getCol should still return the last known column
        assertEquals(2, fr.getCol(), "Column should remain 2 after close");
    }

    @Test
    void getCol_mixedLineAndCharOperations() throws IOException {
        FastReader fr = new FastReader(new StringReader("a\nb"), 8, 2);
        // 'a' -> col 1
        fr.read();
        assertEquals(1, fr.getCol(), "Column after 'a'");
        // '\n' -> col reset to 0
        fr.read();
        assertEquals(0, fr.getCol(), "Column after newline");
        // 'b' -> col 1
        fr.read();
        assertEquals(1, fr.getCol(), "Column after 'b'");
        fr.close();
    }

    @Test
    void getCol_afterCarriageReturn_resetsColumn() throws IOException {
        FastReader fr = new FastReader(new StringReader("ab\rcd"), 8, 2);
        // 'a' -> col 1
        fr.read();
        // 'b' -> col 2
        fr.read();
        assertEquals(2, fr.getCol(), "Column before '\\r'");
        // '\r' -> should reset column
        fr.read();
        assertEquals(0, fr.getCol(), "Column should be 0 after carriage return");
        fr.close();
    }
}
