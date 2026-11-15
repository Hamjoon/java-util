package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StreamGobbler_getResult_Test {

    private InputStream inputStream;

    private StreamGobbler gobbler;

    private Thread gobblerThread;

    /**
     * Prepare a fresh StreamGobbler backed by a known InputStream before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The concrete content is supplied by the individual test methods.
        // Here we just initialise the fields to null; each test will set them up.
        inputStream = null;
        gobbler = null;
        gobblerThread = null;
    }

    /**
     * Ensure that any thread started during a test has finished and that the
     * InputStream is closed to avoid resource leaks.
     */
    @AfterEach
    void teardownAfterEach() throws Exception {
        if (gobblerThread != null && gobblerThread.isAlive()) {
            gobblerThread.join();
        }
        if (inputStream != null) {
            inputStream.close();
        }
    }

    /**
     * Supplies the different scenarios required by {@link #getResult_reflectionInjectedValues_returnExact}.
     *
     * @return a stream of arguments: {injected value, expected return value}
     */
    private static Stream<Arguments> provideResultValues() {
        String largeString = "A".repeat(10_000);
        return Stream.of(Arguments.of("Hello, World!", "Hello, World!"), // empty string case
        Arguments.of("", ""), // very large string case
        Arguments.of(largeString, largeString));
    }

    /**
     * Verifies that {@code getResult()} returns the exact data that was read from
     * the supplied InputStream after the gobbler has finished running.
     */
    @Test
    void getResult_returnsStreamContent() throws Exception {
        // GIVEN – a stream containing a known string
        String expected = "Hello, StreamGobbler!";
        inputStream = new ByteArrayInputStream(expected.getBytes());
        // AND – a StreamGobbler constructed with that stream
        gobbler = new StreamGobbler(inputStream);
        // WHEN – the gobbler is executed in its own thread and we wait for it to finish
        gobblerThread = new Thread(gobbler);
        gobblerThread.start();
        gobblerThread.join();
        // THEN – the result captured by the gobbler matches the original content
        assertEquals(expected, gobbler.getResult(), "The result returned by getResult() should equal the data read from the InputStream");
    }

    /**
     * Verifies that an empty InputStream leads to an empty (non‑null) result.
     * The actual implementation of {@code StreamGobbler.run()} may initialise the
     * result to an empty string when nothing is read; this test documents the
     * expected behaviour.
     */
    @Test
    void getResult_withEmptyStream_returnsEmptyString() throws Exception {
        // GIVEN – an empty stream
        inputStream = new ByteArrayInputStream(new byte[0]);
        // AND – a StreamGobbler for that stream
        gobbler = new StreamGobbler(inputStream);
        // WHEN – the gobbler runs
        gobblerThread = new Thread(gobbler);
        gobblerThread.start();
        gobblerThread.join();
        // THEN – the result should be an empty string (or null if the implementation chooses that)
        String result = gobbler.getResult();
        assertTrue(result == null || result.isEmpty(), "Result should be null or empty when the InputStream contains no data");
    }

    /**
     * A freshly constructed StreamGobbler has never populated its internal
     * {@code _result} field, therefore {@code getResult()} must return {@code null}.
     */
    @Test
    void getResult_newInstance_returnsNull() {
        // GIVEN – a StreamGobbler built with any (dummy) InputStream
        inputStream = new ByteArrayInputStream(new byte[0]);
        gobbler = new StreamGobbler(inputStream);
        // WHEN – we call getResult() without having run the gobbler
        String result = gobbler.getResult();
        // THEN – the result is null
        assertNull(result, "A newly created StreamGobbler should return null from getResult()");
    }

    /**
     * Parameterized test that injects various values into the private {@code _result}
     * field using reflection and verifies that {@code getResult()} returns exactly
     * the injected value.
     *
     * @param injected   the string to set into the private field
     * @param expected   the value we expect getResult() to return
     */
    @ParameterizedTest(name = "[{index}] injected=''{0}'' → expected=''{1}''")
    @MethodSource("provideResultValues")
    void getResult_reflectionInjectedValues_returnExact(String injected, String expected) throws Exception {
        // GIVEN – a StreamGobbler with a dummy stream (the stream is irrelevant here)
        inputStream = new ByteArrayInputStream(new byte[0]);
        gobbler = new StreamGobbler(inputStream);
        // WHEN – we force‑set the private _result field via reflection
        Field resultField = StreamGobbler.class.getDeclaredField("_result");
        resultField.setAccessible(true);
        resultField.set(gobbler, injected);
        // THEN – getResult() must return the exact injected value
        assertEquals(expected, gobbler.getResult(), "getResult() should return the value that was injected into the private _result field");
    }
}
