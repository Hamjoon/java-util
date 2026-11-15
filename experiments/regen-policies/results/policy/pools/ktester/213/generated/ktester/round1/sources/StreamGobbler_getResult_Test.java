package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StreamGobbler_getResult_Test {

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideRunScenarios() {
        return Stream.of(// short ASCII string
        org.junit.jupiter.params.provider.Arguments.of("Hello World", "Hello World"), // empty stream
        org.junit.jupiter.params.provider.Arguments.of("", ""), // multiline content (preserve line‑breaks)
        org.junit.jupiter.params.provider.Arguments.of("line1\nline2\r\nline3", "line1\nline2\r\nline3"), // Unicode characters (Japanese + emoji)
        org.junit.jupiter.params.provider.Arguments.of("こんにちは世界 🌍", "こんにちは世界 🌍"), // Embedded null byte – Java strings can contain '\0'
        org.junit.jupiter.params.provider.Arguments.of("abc\u0000def", "abc\u0000def"));
    }

    /* --------------------------------------------------------------
     *  Helper to set the private _result field via reflection.
     * -------------------------------------------------------------- */
    private static void setPrivateResult(StreamGobbler gobbler, String value) throws Exception {
        Field field = StreamGobbler.class.getDeclaredField("_result");
        field.setAccessible(true);
        field.set(gobbler, value);
    }

    /**
     * Verifies that {@code getResult()} returns the exact text that was present
     * in the supplied {@link InputStream}.
     */
    @Test
    void testGetResultReturnsContentWhenStreamHasData() {
        // GIVEN
        String expected = "Hello, StreamGobbler!\nSecond line.";
        InputStream source = new ByteArrayInputStream(expected.getBytes());
        // WHEN
        StreamGobbler gobbler = new StreamGobbler(source);
        // directly invoke the runnable logic
        gobbler.run();
        String actual = gobbler.getResult();
        // THEN
        assertEquals(expected, actual, "The result returned by getResult() should match the stream content.");
    }

    /**
     * Verifies that an empty input stream yields an empty (non‑null) result.
     */
    @Test
    void testGetResultReturnsEmptyStringWhenStreamIsEmpty() {
        // GIVEN
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        // WHEN
        StreamGobbler gobbler = new StreamGobbler(empty);
        gobbler.run();
        String actual = gobbler.getResult();
        // THEN
        assertNotNull(actual, "Result should never be null even for an empty stream.");
        assertEquals("", actual, "Result should be an empty string for an empty stream.");
    }

    /**
     * Verifies that the gobbler correctly handles a stream that contains only line
     * separators (i.e., blank lines). The expected behaviour is that the resulting
     * string contains those line separators.
     */
    @Test
    void testGetResultPreservesLineSeparators() {
        // GIVEN
        // two blank lines
        String expected = "\n\n";
        InputStream source = new ByteArrayInputStream(expected.getBytes());
        // WHEN
        StreamGobbler gobbler = new StreamGobbler(source);
        gobbler.run();
        String actual = gobbler.getResult();
        // THEN
        assertEquals(expected, actual, "Line separators should be preserved exactly as they appear in the stream.");
    }

    @Test
    @DisplayName("When a newly created StreamGobbler has never been run, getResult() returns null")
    void testGetResultBeforeRunReturnsNull() {
        // GIVEN a gobbler that has not been run
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(empty);
        // WHEN we query the result
        String actual = gobbler.getResult();
        // THEN the result should be null
        assertNull(actual, "Result should be null before run() is invoked.");
    }

    @Test
    @DisplayName("After manually setting the private _result field, getResult() returns that value")
    void testGetResultAfterReflectionSetNonEmpty() throws Exception {
        // GIVEN a gobbler with its private field set via reflection
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(empty);
        setPrivateResult(gobbler, "simple result");
        // WHEN we query the result
        String actual = gobbler.getResult();
        // THEN it should match the value we injected
        assertEquals("simple result", actual);
    }

    @Test
    @DisplayName("When the private _result field is set to an empty string, getResult() returns empty string")
    void testGetResultAfterReflectionSetEmpty() throws Exception {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(empty);
        setPrivateResult(gobbler, "");
        String actual = gobbler.getResult();
        assertNotNull(actual, "Result should never be null once the field is assigned.");
        assertEquals("", actual);
    }

    @Test
    @DisplayName("The method correctly returns a very long string stored in _result")
    void testGetResultAfterReflectionSetVeryLongString() throws Exception {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(empty);
        // generate a long string (100 KB) – any size that could expose truncation issues
        String longString = "x".repeat(100_000);
        setPrivateResult(gobbler, longString);
        String actual = gobbler.getResult();
        assertEquals(longString, actual, "The long string must be returned unchanged.");
    }

    @ParameterizedTest(name = "run() on stream \"{0}\" should yield \"{1}\"")
    @MethodSource("provideRunScenarios")
    @DisplayName("After run(), getResult() returns the exact stream content")
    void testGetResultAfterRunVariousInputs(String streamContent, String expected) {
        InputStream source = new ByteArrayInputStream(streamContent.getBytes(StandardCharsets.UTF_8));
        StreamGobbler gobbler = new StreamGobbler(source);
        // invoke the gobbler logic directly
        gobbler.run();
        String actual = gobbler.getResult();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("After run() on a 1 MiB payload, getResult() returns the full data")
    void testGetResultAfterRunLargePayload() {
        // 1 MiB of 'a'
        String payload = "a".repeat(1024 * 1024);
        InputStream source = new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));
        StreamGobbler gobbler = new StreamGobbler(source);
        gobbler.run();
        String actual = gobbler.getResult();
        assertEquals(payload, actual, "The 1 MiB payload must be returned without truncation.");
    }
}
