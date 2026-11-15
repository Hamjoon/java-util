package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StreamGobbler_getResult_Test {

    private static final String SAMPLE_TEXT = "Line1\nLine2\r\nLine3";

    private InputStream inputStream;

    private StreamGobbler gobbler;

    @BeforeEach
    void setupBeforeEach() {
        // By default each test starts with a fresh gobbler that reads SAMPLE_TEXT.
        inputStream = new ByteArrayInputStream(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        gobbler = new StreamGobbler(inputStream);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the stream if it hasn't been closed by the gobbler.
        if (inputStream != null) {
            inputStream.close();
        }
    }

    /**
     * Supplies a variety of InputStreams together with the expected result
     * after {@link StreamGobbler#run()}.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> streamProvider() {
        return Stream.of(// Empty stream → empty string
        org.junit.jupiter.params.provider.Arguments.of(new ByteArrayInputStream(new byte[0]), ""), // Single‑line without line‑separator
        org.junit.jupiter.params.provider.Arguments.of(new ByteArrayInputStream("Hello World".getBytes(StandardCharsets.UTF_8)), "Hello World"), // Multi‑line preserving both LF and CRLF
        org.junit.jupiter.params.provider.Arguments.of(new ByteArrayInputStream("line1\nline2\r\nline3".getBytes(StandardCharsets.UTF_8)), "line1\nline2\r\nline3"), // Large stream (10 000 'a' characters)
        org.junit.jupiter.params.provider.Arguments.of(new ByteArrayInputStream(generateLargeAText(10_000).getBytes(StandardCharsets.UTF_8)), generateLargeAText(10_000)), // Unicode characters
        org.junit.jupiter.params.provider.Arguments.of(new ByteArrayInputStream("✓ ☃ 漢字".getBytes(StandardCharsets.UTF_8)), "✓ ☃ 漢字"));
    }

    private static String generateLargeAText(int length) {
        char[] chars = new char[length];
        java.util.Arrays.fill(chars, 'a');
        return new String(chars);
    }

    @Test
    void getResult_beforeRun_returnsNull() {
        // The gobbler has not processed the stream yet, so the result should be null.
        assertNull(gobbler.getResult(), "Expected getResult() to return null before run() is invoked.");
    }

    @Test
    void getResult_afterRun_returnsStreamContent() {
        // Execute the gobbler's run method directly (no need for a separate thread).
        gobbler.run();
        // The result should exactly match the original content of the InputStream.
        assertEquals(SAMPLE_TEXT, gobbler.getResult(), "getResult() should return the full text read from the InputStream.");
    }

    @ParameterizedTest(name = "[{index}] run() on stream should produce expected result")
    @MethodSource("streamProvider")
    void getResult_afterRun_variousInputs_returnsExpected(String description, InputStream stream, String expected) {
        // The description argument is not used directly; it allows the test name to be expressive.
        StreamGobbler localGobbler = new StreamGobbler(stream);
        localGobbler.run();
        assertEquals(expected, localGobbler.getResult(), () -> "Result mismatch for input: " + description);
    }

    @Test
    void getResult_afterMultipleRuns_overwritesPreviousResult() throws Exception {
        // First run with "first"
        InputStream first = new ByteArrayInputStream("first".getBytes(StandardCharsets.UTF_8));
        StreamGobbler sameInstance = new StreamGobbler(first);
        sameInstance.run();
        assertEquals("first", sameInstance.getResult(), "Result after first run should be 'first'.");
        // Replace the internal InputStream via reflection with a new one containing "second"
        InputStream second = new ByteArrayInputStream("second".getBytes(StandardCharsets.UTF_8));
        Field inputField = StreamGobbler.class.getDeclaredField("_inputStream");
        inputField.setAccessible(true);
        inputField.set(sameInstance, second);
        // Second run – should overwrite the previous result
        sameInstance.run();
        assertEquals("second", sameInstance.getResult(), "Result after second run should be 'second', overwriting the first.");
    }

    @Test
    void getResult_whenIOExceptionOccurs_resultRemainsUnchanged() {
        // InputStream that throws IOException on any read()
        InputStream faulty = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("forced");
            }
        };
        StreamGobbler errorGobbler = new StreamGobbler(faulty);
        // Before run, result is null
        assertNull(errorGobbler.getResult(), "Pre‑run result should be null.");
        // Run should swallow the exception and leave result untouched
        errorGobbler.run();
        // After the failed run, result must still be null
        assertNull(errorGobbler.getResult(), "Result should remain null when an IOException occurs during run().");
    }
}
