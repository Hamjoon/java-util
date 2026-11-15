package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.io.IOException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_markSupported_Test {

    private static final byte[] SAMPLE_DATA = { 1, 2, 3, 4, 5 };

    private FastByteArrayInputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh stream before each test to guarantee isolation.
        stream = new FastByteArrayInputStream(SAMPLE_DATA.clone());
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference; not strictly necessary but keeps the pattern clear.
        stream = null;
    }

    private static Stream<Arguments> provideBasicMarkSupportedCases() {
        return Stream.of(// Case 1: newly created empty stream
        Arguments.of(new byte[] {}, false), // Case 2: stream closed before checking
        Arguments.of(new byte[] { 0x01, 0x02, 0x03 }, true));
    }

    private static Stream<Arguments> provideAdditionalMarkSupportedCases() {
        return Stream.of(// 1. read() on a single‑byte stream
        Arguments.of(new byte[] { 0x0A }, "read()"), // 2. mark(10); reset()
        Arguments.of(new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 }, "mark(10); reset()"), // 3. skip(5) – skips past EOF
        Arguments.of(new byte[] { 0x11, 0x22, 0x33 }, "skip(5)"), // 4. available()
        Arguments.of(new byte[] { 0x55, 0x66, 0x77, 0x88 }, "available()"), // 5. bulk read of first two bytes
        Arguments.of(new byte[] { 0x09, 0x0A, 0x0B, 0x0C }, "read(byteArray,0,2)"));
    }

    /**
     * Helper that interprets the textual {@code preAction} values from the JSON
     * and invokes the corresponding {@link FastByteArrayInputStream} methods.
     *
     * @param stream   the stream under test
     * @param action   the action description (e.g., "read()", "mark(10); reset()")
     * @param originalData the original byte array (needed for bulk read)
     */
    private void executePreAction(FastByteArrayInputStream stream, String action, byte[] originalData) throws IOException {
        // Split on semicolons to allow multiple statements.
        String[] parts = action.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty())
                continue;
            switch(trimmed) {
                case "read()":
                    // Consume a single byte; ignore the returned value.
                    stream.read();
                    break;
                case "mark(10); reset()":
                case "mark(10)":
                    // Mark with a read limit of 10.
                    stream.mark(10);
                    // If the full string includes reset, perform it.
                    if (trimmed.contains("reset()")) {
                        stream.reset();
                    }
                    break;
                case "reset()":
                    stream.reset();
                    break;
                case "skip(5)":
                    stream.skip(5);
                    break;
                case "available()":
                    // Call available() and discard the result.
                    stream.available();
                    break;
                case "read(byteArray,0,2)":
                    // Perform a bulk read of two bytes into a temporary buffer.
                    byte[] buffer = new byte[2];
                    stream.read(buffer, 0, 2);
                    break;
                default:
                    // If an unknown action appears, fail fast to highlight the missing implementation.
                    fail("Unsupported preAction encountered: '" + trimmed + "'");
            }
        }
    }

    @Test
    void testMarkSupportedOnNewStream() {
        assertTrue(stream.markSupported(), "markSupported() should return true on a newly created stream");
    }

    @Test
    void testMarkSupportedAfterReadingSomeBytes() {
        // Consume a couple of bytes.
        assertEquals(1, stream.read());
        assertEquals(2, stream.read());
        assertTrue(stream.markSupported(), "markSupported() should still return true after reading bytes");
    }

    @Test
    void testMarkSupportedAfterMarkAndReset() {
        // Read a byte, mark, read more, then reset.
        assertEquals(1, stream.read());
        stream.mark(0);
        assertEquals(2, stream.read());
        assertEquals(3, stream.read());
        stream.reset();
        assertTrue(stream.markSupported(), "markSupported() should return true after a mark/reset cycle");
    }

    @Test
    void testMarkSupportedAfterClose() throws java.io.IOException {
        stream.close();
        // According to the existing test suite, the stream remains usable after close,
        // so markSupported() should still be true.
        assertTrue(stream.markSupported(), "markSupported() should return true after the stream has been closed");
    }

    /**
     * Parameterized test for the first JSON group:
     * "FastByteArrayInputStream.markSupported() Tests".
     *
     * @param data            the byte array used to construct the stream
     * @param closeBeforeCheck whether the stream should be closed before invoking
     *                         {@code markSupported()}
     */
    @ParameterizedTest(name = "[{index}] empty={0.length == 0}, closeBeforeCheck={1}")
    @MethodSource("provideBasicMarkSupportedCases")
    void testMarkSupported_BasicCases(byte[] data, boolean closeBeforeCheck) throws IOException {
        FastByteArrayInputStream localStream = new FastByteArrayInputStream(data.clone());
        if (closeBeforeCheck) {
            localStream.close();
        }
        assertTrue(localStream.markSupported(), "markSupported() must always return true regardless of stream state");
    }

    /**
     * Parameterized test for the second JSON group:
     * "FastByteArrayInputStream.markSupported() Additional Tests".
     *
     * @param data       the byte array used to construct the stream
     * @param preAction  a textual description of actions to perform before the assertion
     */
    @ParameterizedTest(name = "[{index}] preAction=''{1}''")
    @MethodSource("provideAdditionalMarkSupportedCases")
    void testMarkSupported_AdditionalCases(byte[] data, String preAction) throws IOException {
        FastByteArrayInputStream localStream = new FastByteArrayInputStream(data.clone());
        // Execute the pre‑action described in the JSON test case.
        executePreAction(localStream, preAction, data);
        // The method under test – it should still return true.
        assertTrue(localStream.markSupported(), () -> "markSupported() must return true after preAction: " + preAction);
    }
}
