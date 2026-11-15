package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastReader_pushback_Test {

    private static final int BUFFER_SIZE = 16;

    // enough for two pushbacks
    private static final int PUSHBACK_BUFFER_SIZE = 2;

    private Reader underlyingReader;

    private FastReader fastReader;

    // enough for two pushbacks
    private static final int DEFAULT_PUSHBACK_BUFFER_SIZE = 2;

    @BeforeEach
    void setupBeforeEach() {
        // a simple empty reader – the pushback logic does not depend on the underlying data
        underlyingReader = new StringReader("");
        fastReader = new FastReader(underlyingReader, BUFFER_SIZE, DEFAULT_PUSHBACK_BUFFER_SIZE);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the FastReader (and its underlying reader) are closed after each test
        fastReader.close();
    }

    private static Object getField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> normalPushbackProvider() {
        return Stream.of(// newline character – line should decrement, column unchanged
        Arguments.of('\n', 1, 0, -1, 0, "newline"), // regular character – column should decrement, line unchanged
        Arguments.of('a', 1, 0, -1, -1, "non‑newline"));
    }

    @Test
    void pushbackNonNewlineDecrementsColumn() throws IOException {
        // initial state
        assertEquals(1, fastReader.getLine(), "Initial line should be 1");
        assertEquals(0, fastReader.getCol(), "Initial column should be 0");
        // push back a regular character (e.g., 'a')
        fastReader.pushback('a');
        // after pushback, column should be decremented, line unchanged
        assertEquals(1, fastReader.getLine(), "Line should remain unchanged after non‑newline pushback");
        assertEquals(-1, fastReader.getCol(), "Column should be decremented after non‑newline pushback");
    }

    @Test
    void pushbackNewlineDecrementsLine() throws IOException {
        // initial state
        assertEquals(1, fastReader.getLine(), "Initial line should be 1");
        assertEquals(0, fastReader.getCol(), "Initial column should be 0");
        // push back a newline character (0x0a)
        fastReader.pushback('\n');
        // after pushback, line should be decremented, column unchanged
        assertEquals(0, fastReader.getLine(), "Line should be decremented after newline pushback");
        assertEquals(0, fastReader.getCol(), "Column should remain unchanged after newline pushback");
    }

    @Test
    void pushbackWhenBufferEmptyThrowsIOException() throws IOException {
        // Fill the buffer completely (pushbackPosition == 0)
        // pos: 2 -> 1
        fastReader.pushback('x');
        // pos: 1 -> 0
        fastReader.pushback('y');
        int beforePos = (int) getField(fastReader, "pushbackPosition");
        int beforeLine = fastReader.getLine();
        int beforeCol = fastReader.getCol();
        IOException ex = assertThrows(IOException.class, () -> fastReader.pushback('z'), "Expected overflow to raise IOException");
        assertEquals("Pushback buffer overflow", ex.getMessage(), "Exception message should indicate buffer overflow");
        // Verify that internal state has NOT changed
        int afterPos = (int) getField(fastReader, "pushbackPosition");
        assertEquals(beforePos, afterPos, "pushbackPosition must remain unchanged after overflow");
        assertEquals(beforeLine, fastReader.getLine(), "Line counter must remain unchanged after overflow");
        assertEquals(beforeCol, fastReader.getCol(), "Column counter must remain unchanged after overflow");
    }

    @ParameterizedTest(name = "pushback ''{5}'' (char={0})")
    @MethodSource("normalPushbackProvider")
    void pushbackNormalOperation(char ch, int initialLine, int initialCol, int expectedLineDelta, int expectedColDelta, String description) throws IOException {
        // Arrange – set line/col to known values (the default is line=1, col=0)
        setField(fastReader, "line", initialLine);
        setField(fastReader, "col", initialCol);
        int startPos = (int) getField(fastReader, "pushbackPosition");
        // Act
        fastReader.pushback(ch);
        // Assert – internal pushbackPosition decreased by one
        int afterPos = (int) getField(fastReader, "pushbackPosition");
        assertEquals(startPos - 1, afterPos, "pushbackPosition should be decremented by 1");
        // Assert – character stored at the new position
        char[] buffer = (char[]) getField(fastReader, "pushbackBuffer");
        assertEquals(ch, buffer[afterPos], "pushbackBuffer should contain the pushed‑back character at the new index");
        // Assert – line/col counters updated correctly
        assertEquals(initialLine + expectedLineDelta, fastReader.getLine(), "Line counter after pushback (" + description + ")");
        assertEquals(initialCol + expectedColDelta, fastReader.getCol(), "Column counter after pushback (" + description + ")");
    }

    @Test
    void multipleSequentialPushbacks() throws IOException {
        // Set custom line/col values
        setField(fastReader, "line", 5);
        setField(fastReader, "col", 3);
        // should be 2
        int startPos = (int) getField(fastReader, "pushbackPosition");
        // First pushback – non‑newline
        fastReader.pushback('x');
        int posAfterFirst = (int) getField(fastReader, "pushbackPosition");
        assertEquals(startPos - 1, posAfterFirst, "After first pushback position should decrement by 1");
        char[] buffer = (char[]) getField(fastReader, "pushbackBuffer");
        assertEquals('x', buffer[posAfterFirst], "Buffer should contain 'x' at the new index");
        assertEquals(5, fastReader.getLine(), "Line should stay unchanged after non‑newline pushback");
        assertEquals(2, fastReader.getCol(), "Column should decrement from 3 to 2 after non‑newline pushback");
        // Second pushback – newline
        fastReader.pushback('\n');
        int posAfterSecond = (int) getField(fastReader, "pushbackPosition");
        assertEquals(posAfterFirst - 1, posAfterSecond, "After second pushback position should decrement again");
        assertEquals('\n', buffer[posAfterSecond], "Buffer should contain newline at the new index");
        assertEquals(4, fastReader.getLine(), "Line should decrement from 5 to 4 after newline pushback");
        assertEquals(2, fastReader.getCol(), "Column should remain unchanged after newline pushback");
    }

    @Test
    void bufferIndexVerification() throws IOException {
        // Use a larger pushback buffer so we can start at position 3
        // close the default instance created in @BeforeEach
        fastReader.close();
        fastReader = new FastReader(underlyingReader, BUFFER_SIZE, 4);
        // Manually set the internal position to 3
        setField(fastReader, "pushbackPosition", 3);
        setField(fastReader, "line", 10);
        setField(fastReader, "col", 7);
        // First pushback – non‑newline character 'A'
        fastReader.pushback('A');
        int posAfterA = (int) getField(fastReader, "pushbackPosition");
        assertEquals(2, posAfterA, "pushbackPosition should be 2 after pushing back 'A'");
        char[] buffer = (char[]) getField(fastReader, "pushbackBuffer");
        assertEquals('A', buffer[2], "Character 'A' should be stored at index 2");
        assertEquals(10, fastReader.getLine(), "Line unchanged after non‑newline");
        assertEquals(6, fastReader.getCol(), "Column decremented from 7 to 6");
        // Second pushback – newline
        fastReader.pushback('\n');
        int posAfterNl = (int) getField(fastReader, "pushbackPosition");
        assertEquals(1, posAfterNl, "pushbackPosition should be 1 after pushing back newline");
        assertEquals('\n', buffer[1], "Newline should be stored at index 1");
        assertEquals(9, fastReader.getLine(), "Line decremented from 10 to 9");
        assertEquals(6, fastReader.getCol(), "Column unchanged after newline");
    }

    @Test
    void lineCounterLowerBound() throws IOException {
        // line starts at 1 (default). Push back a newline.
        fastReader.pushback('\n');
        assertEquals(0, fastReader.getLine(), "Line should be decremented from 1 to 0 (no lower‑bound guard)");
        assertEquals(0, fastReader.getCol(), "Column should stay unchanged when pushing back a newline");
    }

    @Test
    void columnCounterLowerBound() throws IOException {
        // col starts at 0 (default). Push back a regular character.
        fastReader.pushback('z');
        assertEquals(-1, fastReader.getCol(), "Column should be decremented from 0 to -1 (no lower‑bound guard)");
        assertEquals(1, fastReader.getLine(), "Line should stay unchanged when pushing back a non‑newline");
    }
}
