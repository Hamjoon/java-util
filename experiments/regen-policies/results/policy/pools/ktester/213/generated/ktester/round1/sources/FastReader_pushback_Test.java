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
class FastReader_pushback_Test {

    private FastReader fastReader;

    @BeforeEach
    void setupBeforeEach() {
        // Use a modest buffer size – the values are not important for the
        // pushback behaviour we are testing, but they keep the tests fast.
        fastReader = new FastReader(new StringReader("placeholder"), // buffer size
        16, // push‑back buffer size
        4);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
    }

    /* -----------------------------------------------------------------
     *  Helper methods for reflective field access
     *  ----------------------------------------------------------------- */
    private static void setPrivateField(Object target, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field f = FastReader.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getPrivateField(Object target, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field f = FastReader.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * -----------------------------------------------------------------
     *  1️⃣  Basic push‑back – the character that was read can be put back
     *      and read again.
     *  -----------------------------------------------------------------
     */
    @Test
    void testPushbackReturnsPushedChar() throws IOException {
        // Arrange – a simple source string
        fastReader = new FastReader(new StringReader("xyz"), 16, 4);
        // Act – read the first character and push it back
        // should be 'x'
        int first = fastReader.read();
        assertEquals('x', first);
        // push back 'x'
        fastReader.pushback((char) first);
        // Assert – the next read must return the same character again
        int afterPushback = fastReader.read();
        assertEquals('x', afterPushback);
        // The following read should now give the original second character
        assertEquals('y', fastReader.read());
    }

    /**
     * -----------------------------------------------------------------
     *  2️⃣  Column tracking – after a normal character push‑back the column
     *      counter must be decreased by one.
     *  -----------------------------------------------------------------
     */
    @Test
    void testPushbackDecrementsColumn() throws IOException {
        fastReader = new FastReader(new StringReader("ab"), 16, 4);
        // Read a character so that the column counter moves forward
        // reads 'a'
        fastReader.read();
        // column after reading 'a'
        int colBefore = fastReader.getCol();
        // Push the same character back
        fastReader.pushback('a');
        // Column should be one less than before the pushback
        assertEquals(colBefore - 1, fastReader.getCol(), "Column should be decremented after pushback of a non‑newline character");
    }

    /**
     * -----------------------------------------------------------------
     *  3️⃣  Line tracking – a newline character affects the line counter.
     *      Pushing back a newline must decrease the line number.
     *  -----------------------------------------------------------------
     */
    @Test
    void testPushbackNewlineUpdatesLine() throws IOException {
        fastReader = new FastReader(new StringReader("a\nb"), 16, 4);
        // Read 'a' and then the newline
        // 'a'
        fastReader.read();
        // '\n' – line becomes 2
        fastReader.read();
        assertEquals(2, fastReader.getLine(), "Line number should be 2 after reading a newline");
        // Push the newline back
        fastReader.pushback('\n');
        // Line number must go back to 1
        assertEquals(1, fastReader.getLine(), "Line number should be decremented after pushback of a newline");
    }

    /**
     * -----------------------------------------------------------------
     *  4️⃣  Buffer overflow – when the push‑back buffer is exhausted an
     *      IOException must be thrown.
     *  -----------------------------------------------------------------
     */
    @Test
    void testPushbackBufferOverflow() throws IOException {
        // Create a reader with a *single* slot in the push‑back buffer
        fastReader = new FastReader(new StringReader("xy"), 16, 1);
        // First pushback works (buffer goes from position 1 → 0)
        // consume 'x'
        fastReader.read();
        // ok
        fastReader.pushback('x');
        // Second pushback should overflow
        IOException ex = assertThrows(IOException.class, () -> fastReader.pushback('y'), "Expected IOException when pushback buffer overflows");
        assertTrue(ex.getMessage().contains("Pushback buffer overflow"));
    }

    /* -----------------------------------------------------------------
     *  5️⃣  Pushback buffer overflow when pushbackPosition == 0 (explicit case)
     *  ----------------------------------------------------------------- */
    @Test
    void testPushbackWhenBufferAlreadyEmpty() throws IOException {
        // Build a reader with a buffer size of 2 and manually set position to 0
        fastReader = new FastReader(new StringReader("ab"), 16, 2);
        setPrivateField(fastReader, "pushbackPosition", 0);
        IOException ex = assertThrows(IOException.class, () -> fastReader.pushback('a'));
        assertTrue(ex.getMessage().contains("Pushback buffer overflow"));
    }

    /* -----------------------------------------------------------------
     *  6️⃣  Pushback of regular character – verify internal state changes
     *  ----------------------------------------------------------------- */
    @Test
    void testPushbackRegularCharStateChanges() throws IOException, NoSuchFieldException, IllegalAccessException {
        fastReader = new FastReader(new StringReader(""), 16, 2);
        // initialise pushbackPosition to buffer length (full capacity)
        setPrivateField(fastReader, "pushbackPosition", 2);
        int lineBefore = fastReader.getLine();
        int colBefore = fastReader.getCol();
        fastReader.pushback('x');
        // pushbackPosition should have decremented
        int pos = (int) getPrivateField(fastReader, "pushbackPosition");
        assertEquals(1, pos);
        // buffer should contain the pushed char at the new position
        char[] buf = (char[]) getPrivateField(fastReader, "pushbackBuffer");
        assertEquals('x', buf[pos]);
        // line unchanged, col decremented
        assertEquals(lineBefore, fastReader.getLine());
        assertEquals(colBefore - 1, fastReader.getCol());
    }

    /* -----------------------------------------------------------------
     *  7️⃣  Pushback of newline character – line decrement, col unchanged
     *  ----------------------------------------------------------------- */
    @Test
    void testPushbackNewlineStateChanges() throws IOException, NoSuchFieldException, IllegalAccessException {
        fastReader = new FastReader(new StringReader(""), 16, 2);
        setPrivateField(fastReader, "pushbackPosition", 2);
        // simulate that we are on line 3, column 5
        setPrivateField(fastReader, "line", 3);
        setPrivateField(fastReader, "col", 5);
        fastReader.pushback('\n');
        int pos = (int) getPrivateField(fastReader, "pushbackPosition");
        assertEquals(1, pos);
        char[] buf = (char[]) getPrivateField(fastReader, "pushbackBuffer");
        assertEquals('\n', buf[pos]);
        // line decremented
        assertEquals(2, fastReader.getLine());
        // column unchanged
        assertEquals(5, fastReader.getCol());
    }

    /* -----------------------------------------------------------------
     *  8️⃣  Multiple pushbacks until buffer empty and then overflow
     *  ----------------------------------------------------------------- */
    @Test
    void testMultiplePushbacksAndOverflow() throws IOException, NoSuchFieldException, IllegalAccessException {
        fastReader = new FastReader(new StringReader(""), 16, 2);
        setPrivateField(fastReader, "pushbackPosition", 2);
        int initialLine = fastReader.getLine();
        int initialCol = fastReader.getCol();
        // first pushback
        fastReader.pushback('a');
        // second pushback
        fastReader.pushback('b');
        // after two pushes buffer should be full (position == 0)
        int pos = (int) getPrivateField(fastReader, "pushbackPosition");
        assertEquals(0, pos);
        // line/col should have been decremented twice (non‑newline chars)
        assertEquals(initialLine, fastReader.getLine());
        assertEquals(initialCol - 2, fastReader.getCol());
        // third pushback must overflow
        IOException ex = assertThrows(IOException.class, () -> fastReader.pushback('c'));
        assertTrue(ex.getMessage().contains("Pushback buffer overflow"));
    }

    /* -----------------------------------------------------------------
     *  9️⃣  Pushback does not affect unrelated state (reader & snippet)
     *  ----------------------------------------------------------------- */
    @Test
    void testPushbackDoesNotAffectUnrelatedState() throws IOException, NoSuchFieldException, IllegalAccessException {
        String source = "hello";
        fastReader = new FastReader(new StringReader(source), 16, 4);
        // capture state before pushback
        int lineBefore = fastReader.getLine();
        int colBefore = fastReader.getCol();
        // underlying Reader field (named 'in' in java.io.Reader) should stay the same
        Field inField = Reader.class.getDeclaredField("in");
        inField.setAccessible(true);
        Object underlyingReaderBefore = inField.get(fastReader);
        fastReader.pushback('z');
        // verify line/col updated as per rules
        assertEquals(lineBefore, fastReader.getLine());
        assertEquals(colBefore - 1, fastReader.getCol());
        // verify underlying reader unchanged
        Object underlyingReaderAfter = inField.get(fastReader);
        assertSame(underlyingReaderBefore, underlyingReaderAfter);
    }

    /* -----------------------------------------------------------------
     *  🔟  Null pushback buffer – expect NullPointerException
     *  ----------------------------------------------------------------- */
    @Test
    void testPushbackWithNullBuffer() throws NoSuchFieldException, IllegalAccessException {
        fastReader = new FastReader(new StringReader(""), 16, 2);
        // force pushbackBuffer to null via reflection
        setPrivateField(fastReader, "pushbackBuffer", null);
        // also set a valid position to avoid the overflow check first
        setPrivateField(fastReader, "pushbackPosition", 1);
        assertThrows(NullPointerException.class, () -> fastReader.pushback('a'));
    }

    /* -----------------------------------------------------------------
     *  1️⃣1️⃣  Pushback position out of upper bound – ArrayIndexOutOfBoundsException
     *  ----------------------------------------------------------------- */
    @Test
    void testPushbackPositionAboveUpperBound() throws NoSuchFieldException, IllegalAccessException {
        fastReader = new FastReader(new StringReader(""), 16, 2);
        // set position beyond buffer length
        // buffer length is 2
        setPrivateField(fastReader, "pushbackPosition", 5);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> fastReader.pushback('b'));
    }

    /* -----------------------------------------------------------------
     *  1️⃣2️⃣  Pushback position negative – ArrayIndexOutOfBoundsException
     *  ----------------------------------------------------------------- */
    @Test
    void testPushbackPositionNegative() throws NoSuchFieldException, IllegalAccessException {
        fastReader = new FastReader(new StringReader(""), 16, 2);
        setPrivateField(fastReader, "pushbackPosition", -1);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> fastReader.pushback('c'));
    }
}
