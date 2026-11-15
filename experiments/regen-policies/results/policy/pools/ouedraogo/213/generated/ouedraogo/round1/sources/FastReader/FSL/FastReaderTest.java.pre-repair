package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;

/**
 * Comprehensive JUnit‑4 test suite for {@link FastReader}.
 * The tests cover:
 * <ul>
 *   <li>Constructor validation (default & custom buffer sizes, illegal arguments)</li>
 *   <li>Single‑character {@code read()}</li>
 *   <li>Bulk {@code read(char[],int,int)} including buffer boundaries and push‑back handling</li>
 *   <li>Push‑back functionality, overflow detection and line/column tracking</li>
 *   <li>Line/column counters after normal characters and newline characters</li>
 *   <li>{@code getLastSnippet()} behavior before and after an internal buffer refill</li>
 *   <li>Proper exception handling after {@code close()}</li>
 * </ul>
 */
public class FastReaderTest {

    /** Helper to create a FastReader from a string using default buffer sizes. */
    private FastReader newReader(String data) {
        return new FastReader(new StringReader(data));
    }

    /** Helper to create a FastReader with explicit small buffers to force multiple fills. */
    private FastReader newReader(String data, int bufferSize, int pushbackSize) {
        return new FastReader(new StringReader(data), bufferSize, pushbackSize);
    }

    // -------------------------------------------------------------------------
    // Constructor validation
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNonPositiveBufferSize() {
        new FastReader(new StringReader("test"), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNegativePushbackSize() {
        new FastReader(new StringReader("test"), 10, -1);
    }

    @Test
    public void defaultConstructorInitialisesFields() throws IOException {
        FastReader fr = newReader("abc");
        assertEquals(1, fr.getLine());   // line starts at 1
        assertEquals(0, fr.getCol());    // column starts at 0
        assertEquals(-1, fr.read()); // reading before any fill should return first char
        fr.close();
    }

    // -------------------------------------------------------------------------
    // Single‑character read()
    // -------------------------------------------------------------------------

    @Test
    public void readSingleCharactersUntilEof() throws IOException {
        FastReader fr = newReader("xyz");
        assertEquals('x', fr.read());
        assertEquals('y', fr.read());
        assertEquals('z', fr.read());
        assertEquals(-1, fr.read()); // EOF
        fr.close();
    }

    @Test
    public void readThrowsWhenClosed() throws IOException {
        FastReader fr = newReader("data");
        fr.close();
        try {
            fr.read();
            fail("Expected IOException after close()");
        } catch (IOException e) {
            // expected
        }
    }

    // -------------------------------------------------------------------------
    // Bulk read(char[],int,int)
    // -------------------------------------------------------------------------

    @Test
    public void bulkReadCopiesExactRequestedLength() throws IOException {
        String src = "0123456789";
        FastReader fr = newReader(src);
        char[] buf = new char[20];
        int n = fr.read(buf, 5, 8); // read 8 chars into buf[5]..buf[12]
        assertEquals(8, n);
        assertArrayEquals(new char[]{'\0','\0','\0','\0','\0','0','1','2','3','4','5','6','7','8','9','\0','\0','\0','\0','\0'}, buf);
        fr.close();
    }

    @Test
    public void bulkReadSpansMultipleInternalBuffers() throws IOException {
        // Use a tiny internal buffer (4) to force at least two fills.
        String src = "abcdefghij"; // 10 chars
        FastReader fr = newReader(src, 4, 2);
        char[] out = new char[10];
        int n = fr.read(out, 0, out.length);
        assertEquals(10, n);
        assertEquals(src, new String(out));
        fr.close();
    }

    @Test
    public void bulkReadReturnsMinusOneWhenEofBeforeAnyRead() throws IOException {
        FastReader fr = newReader("");
        char[] buf = new char[5];
        int n = fr.read(buf, 0, 5);
        assertEquals(-1, n);
        fr.close();
    }

    // -------------------------------------------------------------------------
    // Push‑back handling
    // -------------------------------------------------------------------------

    @Test
    public void pushbackRestoresPreviouslyReadCharacter() throws IOException {
        FastReader fr = newReader("AB");
        assertEquals('A', fr.read());
        fr.pushback('A');               // push back the same char
        assertEquals('A', fr.read());   // should read it again
        assertEquals('B', fr.read());   // then continue
        fr.close();
    }

    @Test
    public void pushbackUpdatesLineAndColumnCorrectlyForNewline() throws IOException {
        FastReader fr = newReader("x\ny");
        // read 'x'
        assertEquals('x', fr.read());
        assertEquals(1, fr.getLine());
        assertEquals(1, fr.getCol()); // col incremented after 'x'

        // read newline
        assertEquals('\n', fr.read());
        assertEquals(2, fr.getLine()); // line incremented
        assertEquals(0, fr.getCol());  // column reset

        // push back newline
        fr.pushback('\n');
        assertEquals(1, fr.getLine()); // line decremented
        assertEquals(-1, fr.getCol()); // col decremented from 0

        // read newline again
        assertEquals('\n', fr.read());
        assertEquals(2, fr.getLine());
        assertEquals(0, fr.getCol());

        fr.close();
    }

    @Test
    public void pushbackOverflowThrowsIOException() throws IOException {
        FastReader fr = newReader("data", 8, 3); // small pushback buffer (size 3)
        // Fill the pushback buffer completely
        fr.pushback('a');
        fr.pushback('b');
        fr.pushback('c');
        // Next pushback must overflow
        try {
            fr.pushback('d');
            fail("Expected IOException due to pushback buffer overflow");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Pushback buffer overflow"));
        }
        fr.close();
    }

    // -------------------------------------------------------------------------
    // Line / column tracking for normal characters
    // -------------------------------------------------------------------------

    @Test
    public void lineAndColumnAdvanceOnNormalCharacters() throws IOException {
        FastReader fr = newReader("abc");
        assertEquals(1, fr.getLine());
        assertEquals(0, fr.getCol());

        assertEquals('a', fr.read());
        assertEquals(1, fr.getLine());
        assertEquals(1, fr.getCol());

        assertEquals('b', fr.read());
        assertEquals(1, fr.getLine());
        assertEquals(2, fr.getCol());

        assertEquals('c', fr.read());
        assertEquals(1, fr.getLine());
        assertEquals(3, fr.getCol());

        fr.close();
    }

    // -------------------------------------------------------------------------
    // getLastSnippet()
    // -------------------------------------------------------------------------

    @Test
    public void getLastSnippetBeforeBufferRefill() throws IOException {
        String src = "hello";
        FastReader fr = newReader(src, 10, 2); // buffer larger than src
        // read first three characters
        assertEquals('h', fr.read());
        assertEquals('e', fr.read());
        assertEquals('l', fr.read());

        // position should now be 3, snippet should be "hel"
        assertEquals("hel", fr.getLastSnippet());
        fr.close();
    }

    @Test
    public void getLastSnippetAfterInternalRefill() throws IOException {
        // Buffer size 4 forces a refill after reading 4 chars.
        String src = "abcdefgh";
        FastReader fr = newReader(src, 4, 2);
        // read all 8 characters
        for (int i = 0; i < 8; i++) {
            fr.read();
        }
        // After the second refill, position will be 4 (last 4 chars read from buffer)
        // The internal buffer now holds the last 4 characters "efgh".
        assertEquals("efgh", fr.getLastSnippet());
        fr.close();
    }

    // -------------------------------------------------------------------------
    // close() behaviour
    // -------------------------------------------------------------------------

    @Test
    public void closeMakesSubsequentReadsFail() throws IOException {
        FastReader fr = newReader("xyz");
        assertEquals('x', fr.read());
        fr.close();

        try {
            fr.read();
            fail("Expected IOException after close()");
        } catch (IOException e) {
            // expected
        }

        char[] buf = new char[2];
        try {
            fr.read(buf, 0, 2);
            fail("Expected IOException after close() for bulk read");
        } catch (IOException e) {
            // expected
        }
    }

    // -------------------------------------------------------------------------
    // Additional edge cases for bulk read with push‑back data present
    // -------------------------------------------------------------------------

    @Test
    public void bulkReadUsesPendingPushbackBeforeReadingUnderlyingStream() throws IOException {
        FastReader fr = newReader("12345", 4, 4);
        // Read two chars normally
        assertEquals('1', fr.read());
        assertEquals('2', fr.read());

        // Push back three chars (including a newline to test line handling)
        fr.pushback('a');
        fr.pushback('\n');
        fr.pushback('b');

        // Now perform a bulk read of 5 chars
        char[] out = new char[5];
        int n = fr.read(out, 0, 5);
        assertEquals(5, n);
        // Expected order: b, newline, a, '3', '4'
        assertArrayEquals(new char[]{'b', '\n', 'a', '3', '4'}, out);

        // Verify line/col after reading the newline (line should have advanced once)
        assertEquals(2, fr.getLine()); // started at 1, newline moved to line 2
        assertEquals(1, fr.getCol());  // after newline, col reset then 'a' increments to 1

        fr.close();
    }
}
