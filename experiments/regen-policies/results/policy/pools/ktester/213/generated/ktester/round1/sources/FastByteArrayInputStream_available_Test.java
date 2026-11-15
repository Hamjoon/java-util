package com.cedarsoftware.util;

import java.io.IOException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_available_Test {

    /**
     * Test data used for most test cases
     */
    private static final byte[] SAMPLE_DATA = { 1, 2, 3, 4, 5 };

    /**
     * Instance under test – created fresh for each test
     */
    private FastByteArrayInputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Create a new stream for every test to guarantee isolation
        stream = new FastByteArrayInputStream(SAMPLE_DATA.clone());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the stream does not hold external resources
        stream = null;
    }

    /**
     * Provides the test data defined in the JSON specification.
     *
     * <ul>
     *   <li>Initial state – a 10‑byte array, no reads, expect {@code available()==10}</li>
     *   <li>Partial read – a 5‑byte array, two reads, expect {@code available()==3}</li>
     * </ul>
     *
     * @return a stream of arguments: {description, byte[] data, int reads, int expectedAvailable}
     */
    private static Stream<Arguments> availableScenarios() {
        return Stream.of(// group: Initial state
        Arguments.of("Initial state – 10‑byte array, no reads", // array of length 10 (filled with zeros)
        new byte[10], // number of read() calls
        0, // expected available
        10), // group: Partial read
        Arguments.of("Partial read – 5‑byte array, two reads", new byte[] { 1, 2, 3, 4, 5 }, // read twice
        2, // 5 - 2 = 3
        3));
    }

    @Test
    void availableInitiallyEqualsLength() {
        assertEquals(SAMPLE_DATA.length, stream.available(), "available() should report the total number of bytes before any read operation");
    }

    @Test
    void availableDecreasesAfterSingleRead() throws IOException {
        // consume one byte
        int first = stream.read();
        assertEquals(1, first, "First byte should be 1");
        assertEquals(SAMPLE_DATA.length - 1, stream.available(), "available() must decrease by one after a single read");
    }

    @Test
    void availableDecreasesAfterMultipleReads() throws IOException {
        // read three bytes
        assertEquals(1, stream.read());
        assertEquals(2, stream.read());
        assertEquals(3, stream.read());
        assertEquals(SAMPLE_DATA.length - 3, stream.available(), "available() must reflect the remaining unread bytes");
    }

    @Test
    void availableAfterSkip() {
        long skipped = stream.skip(2);
        assertEquals(2L, skipped, "skip(2) should skip exactly two bytes");
        assertEquals(SAMPLE_DATA.length - 2, stream.available(), "available() must be reduced by the number of bytes skipped");
    }

    @Test
    void availableAtEndOfStream() throws IOException {
        // Exhaust the stream
        while (stream.read() != -1) {
            // keep reading
        }
        assertEquals(0, stream.available(), "When the stream is exhausted, available() must return 0");
        // Subsequent reads still return -1
        assertEquals(-1, stream.read());
        assertEquals(0, stream.available(), "available() should stay at 0 after the end of stream is reached");
    }

    @Test
    void availableAfterCloseRemainsUnchanged() throws IOException {
        // Read a byte to change the internal state
        stream.read();
        int beforeClose = stream.available();
        // FastByteArrayInputStream.close() is a no‑op
        stream.close();
        assertEquals(beforeClose, stream.available(), "close() must not affect the result of available()");
    }

    @Test
    void availableNeverNegativeEvenIfSkipBeyondEnd() {
        // attempt to skip far past the end
        long skipped = stream.skip(100);
        assertEquals(SAMPLE_DATA.length, skipped, "skip beyond the end should only skip up to the available bytes");
        assertEquals(0, stream.available(), "available() must never be negative after an over‑skip");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("availableScenarios")
    void availableAccordingToJsonSpecification(String description, byte[] data, int reads, int expectedAvailable) throws IOException {
        // Arrange: create a fresh stream with the supplied data
        FastByteArrayInputStream localStream = new FastByteArrayInputStream(data.clone());
        // Act: perform the requested number of reads
        for (int i = 0; i < reads; i++) {
            int value = localStream.read();
            // Guard against premature EOF in malformed test data
            assertNotEquals(-1, value, "Unexpected end‑of‑stream while performing reads for scenario: " + description);
        }
        // Assert: available() must match the expected value
        assertEquals(expectedAvailable, localStream.available(), "available() mismatch for scenario: " + description);
    }
}
