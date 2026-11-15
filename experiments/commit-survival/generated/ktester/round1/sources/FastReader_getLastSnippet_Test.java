package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastReader_getLastSnippet_Test {

    private static final String TEST_CONTENT = "The quick brown fox jumps over the lazy dog";

    private Reader underlyingReader;

    private FastReader fastReader;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise FastReader with a small buffer; the underlying content is irrelevant for the
        // reflection‑based tests because we overwrite the internal state before invoking
        // getLastSnippet().
        underlyingReader = new StringReader(TEST_CONTENT);
        fastReader = new FastReader(underlyingReader, 32, 8);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the FastReader (and underlying StringReader) are closed after each test
        fastReader.close();
    }

    /**
     * Directly sets the private {@code buf} and {@code position} fields of a {@link FastReader}
     * instance using reflection. This method is intended for test setup only.
     */
    private static void setInternalState(FastReader fr, char[] buffer, int position) {
        try {
            Field bufField = FastReader.class.getDeclaredField("buf");
            bufField.setAccessible(true);
            bufField.set(fr, buffer);
            Field posField = FastReader.class.getDeclaredField("position");
            posField.setAccessible(true);
            posField.setInt(fr, position);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set internal state via reflection", e);
        }
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// When position is 0 and buffer is empty
        Arguments.of("Empty buffer, position 0 → empty string", new char[] {}, 0, ""), // When position is 3 and buf = {'a','b','c'}
        Arguments.of("buf = {'a','b','c'}, position = 3 → \"abc\"", new char[] { 'a', 'b', 'c' }, 3, "abc"), // When position is 5 and buf = \"hello world\"
        Arguments.of("buf = \"hello world\", position = 5 → \"hello\"", "hello world".toCharArray(), 5, "hello"), // When position equals buffer length (full buffer)
        Arguments.of("buf = \"fullbuffer\", position = length → \"fullbuffer\"", "fullbuffer".toCharArray(), "fullbuffer".length(), "fullbuffer"), // Unicode characters
        Arguments.of("buf = \"αβγδ\", position = 3 → \"αβγ\"", "αβγδ".toCharArray(), 3, "αβγ"), // Buffer containing a null character
        Arguments.of("buf = {'a','\\u0000','b','c'}, position = 4 → \"a\\u0000bc\"", new char[] { 'a', '\u0000', 'b', 'c' }, 4, "a\u0000bc"), // Buffer with leading space and punctuation
        Arguments.of("buf = \" a,b!c\", position = 5 → \" a,b!\"", " a,b!c".toCharArray(), 5, " a,b!"));
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// Negative position – loop never executes, should return empty string (no exception)
        // We handle this as a normal functional test rather than an exception test.
        // Therefore it is covered in the basic functionality provider with expected "".
        // Null buffer → NullPointerException
        Arguments.of("null buffer should cause NullPointerException", null, 0, NullPointerException.class), // Position greater than buffer length → ArrayIndexOutOfBoundsException
        Arguments.of("position > buf.length should cause ArrayIndexOutOfBoundsException", "test".toCharArray(), 6, ArrayIndexOutOfBoundsException.class));
    }

    @Test
    void getLastSnippet_returnsEmptyString_whenNoCharactersRead() {
        // Given – nothing has been read from fastReader yet (position == 0)
        // When
        String snippet = fastReader.getLastSnippet();
        // Then
        assertEquals("", snippet, "Snippet should be empty when no characters have been read");
    }

    @Test
    void getLastSnippet_returnsCorrectString_afterReadingCharacters() throws IOException {
        // Given – we will read the first N characters from the underlying source
        int charsToRead = 10;
        StringBuilder expected = new StringBuilder();
        // When – read charsToRead characters one by one using the public read() method
        for (int i = 0; i < charsToRead; i++) {
            // public API, advances internal position
            int ch = fastReader.read();
            assertTrue(ch != -1, "Unexpected end of stream while reading test data");
            expected.append((char) ch);
        }
        // Then – getLastSnippet should return exactly the characters we have read
        String snippet = fastReader.getLastSnippet();
        assertEquals(expected.toString(), snippet, "Snippet should contain exactly the characters that have been read");
    }

    @Test
    void getLastSnippet_accumulatesAcrossMultipleReadCalls() throws IOException {
        // Given – read a first chunk
        int firstChunk = 5;
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < firstChunk; i++) {
            int ch = fastReader.read();
            assertTrue(ch != -1);
            expected.append((char) ch);
        }
        // When – read a second chunk
        int secondChunk = 7;
        for (int i = 0; i < secondChunk; i++) {
            int ch = fastReader.read();
            assertTrue(ch != -1);
            expected.append((char) ch);
        }
        // Then – the snippet should reflect the total characters read so far
        String snippet = fastReader.getLastSnippet();
        assertEquals(expected.toString(), snippet, "Snippet should reflect all characters read across multiple read calls");
    }

    /* --------------------------------------------------------------
     *  Parameterized tests covering the JSON‑specified cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicFunctionalityProvider")
    void getLastSnippet_basicFunctionality(String description, char[] buf, int position, String expected) {
        // Arrange – inject the supplied buffer and position
        setInternalState(fastReader, buf, position);
        // Act
        String actual = fastReader.getLastSnippet();
        // Assert
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("edgeCaseProvider")
    void getLastSnippet_edgeCases(String description, char[] buf, int position, Class<? extends Throwable> expectedException) {
        // Arrange – set internal state (buf may be null)
        setInternalState(fastReader, buf, position);
        // Act & Assert – expecting the specified exception
        assertThrows(expectedException, fastReader::getLastSnippet, description);
    }

    /* --------------------------------------------------------------
     *  Additional explicit test for negative position (returns empty)
     * -------------------------------------------------------------- */
    @Test
    void getLastSnippet_negativePosition_returnsEmptyString() {
        // Arrange – set a non‑empty buffer but a negative position
        setInternalState(fastReader, "any".toCharArray(), -5);
        // Act
        String result = fastReader.getLastSnippet();
        // Assert
        assertEquals("", result, "Negative position should result in an empty snippet");
    }
}
