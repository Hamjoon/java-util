package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Executor_getOut_Test {

    private Executor executor;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test to avoid state leakage
        executor = new Executor();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference (helps GC in long test suites)
        executor = null;
    }

    /**
     * Provides a stream of test data for the "default and manipulated states" group.
     * Each element is an array: {valueToSet, expectedResult, description}
     */
    private static Stream<Object[]> defaultAndManipulatedProvider() {
        return Stream.of(new Object[] { null, null, "When a newly instantiated Executor has never executed anything, the private field `_out` is still null." }, new Object[] { "", "", "After the private `_out` field is explicitly set to an empty string, getOut() must return the empty string." }, new Object[] { "Hello, World!", "Hello, World!", "After the private `_out` field is set to a non‑empty value, getOut() should return exactly that value." });
    }

    /**
     * Provides a stream of test data for the "edge string values" group.
     * Each element is an array: {valueToSet, expectedResult, description}
     */
    private static Stream<Object[]> edgeStringValuesProvider() {
        return Stream.of(new Object[] { "   \t  \n  ", "   \t  \n  ", "Whitespace‑only string should be returned unchanged." }, new Object[] { "😀🚀✨漢字テスト", "😀🚀✨漢字テスト", "Unicode characters (emojis, non‑Latin scripts) must be preserved." }, new Object[] { "Line1\nLine2\r\nLine3", "Line1\nLine2\r\nLine3", "Mixed line‑break representations should be kept verbatim." }, new Object[] { "null\u0000character", "null\u0000character", "Embedded null character must be present in the returned string." }, new Object[] { "a", "a", "Single‑character string should be returned as‑is." }, // Very long string (truncated for readability)
        new Object[] { generateLongString(), generateLongString(), "Very long string should be returned unchanged." });
    }

    /**
     * Uses reflection to set the private `_out` field of the current {@code executor}
     * instance. Accepts {@code null} as a legitimate value.
     */
    private void setPrivateOutField(Object value) throws Exception {
        Field outField = Executor.class.getDeclaredField("_out");
        outField.setAccessible(true);
        outField.set(executor, value);
    }

    /**
     * Generates a long string (~10 KB) for the “very long string” test case.
     * Keeping the generation in a method avoids cluttering the provider array.
     */
    private static String generateLongString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10_000; i++) {
            sb.append('x');
        }
        return sb.toString();
    }

    @Test
    @DisplayName("getOut() should return null before any command is executed")
    void testGetOutInitiallyNull() {
        // GIVEN – a newly created Executor (handled in @BeforeEach)
        // WHEN
        String out = executor.getOut();
        // THEN
        assertNull(out, "Expected getOut() to return null before any exec() call");
    }

    @Test
    @DisplayName("getOut() should contain command output after exec(String) succeeds")
    void testGetOutAfterExec() {
        // GIVEN – a simple command that is guaranteed to exist on most platforms
        // Using 'echo' which prints its arguments followed by a newline.
        String command = "echo hello";
        // WHEN
        int exitCode = executor.exec(command);
        String out = executor.getOut();
        // THEN
        assertEquals(0, exitCode, "Expected exec() to return 0 for successful execution");
        assertNotNull(out, "Expected getOut() to return a non‑null value after exec()");
        // The exact output may contain a trailing newline depending on the platform.
        assertTrue(out.trim().equals("hello"), "Expected getOut() to contain the echoed text; actual output: [" + out + "]");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("defaultAndManipulatedProvider")
    @DisplayName("Executor.getOut() - default and manipulated states")
    void testGetOutWithDirectFieldManipulation(Object valueToSet, String expected, String description) throws Exception {
        // GIVEN – an Executor instance; we set its private `_out` field via reflection
        setPrivateOutField(valueToSet);
        // WHEN
        String actual = executor.getOut();
        // THEN
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("edgeStringValuesProvider")
    @DisplayName("Executor.getOut() - edge string values")
    void testGetOutWithEdgeValues(Object valueToSet, String expected, String description) throws Exception {
        // GIVEN – set the private `_out` field to the edge case value
        setPrivateOutField(valueToSet);
        // WHEN
        String actual = executor.getOut();
        // THEN
        assertEquals(expected, actual, description);
    }
}
