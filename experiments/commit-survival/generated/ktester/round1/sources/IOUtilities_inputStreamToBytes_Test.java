package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_inputStreamToBytes_Test {

    /**
     * Sample data used by several tests.
     */
    private static final byte[] SAMPLE_DATA = "The quick brown fox jumps over the lazy dog".getBytes();

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (needed for @Mock fields if added later).
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Parameter source for normal‑operation scenarios.
     * Each element is an {@code Object[]} where:
     *   - index 0: the InputStream to pass,
     *   - index 1: the expected byte[] result (may be empty but never null).
     */
    private static Stream<Object[]> normalOperationProvider() {
        return Stream.of(// Regular non‑empty stream
        new Object[] { new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8)), "hello world".getBytes(StandardCharsets.UTF_8) }, // Empty stream – should yield an empty array, not null
        new Object[] { new ByteArrayInputStream(new byte[0]), new byte[0] });
    }

    /**
     * Verify that a normal {@link InputStream} is fully read and the exact bytes are returned.
     */
    @Test
    void inputStreamToBytes_returnsAllBytes() {
        // GIVEN a ByteArrayInputStream containing known data
        InputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        // WHEN the utility method is invoked
        byte[] result = IOUtilities.inputStreamToBytes(in);
        // THEN the returned array matches the original content
        assertNotNull(result, "Result should not be null for a successful read");
        assertArrayEquals(SAMPLE_DATA, result, "Returned bytes must be identical to the source");
    }

    /**
     * Verify that the method returns {@code null} when the supplied stream throws an exception.
     */
    @Test
    void inputStreamToBytes_returnsNullOnException() throws IOException {
        // GIVEN a mock InputStream that throws IOException on the first read
        InputStream faulty = mock(InputStream.class);
        when(faulty.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Simulated read failure"));
        // Also make read() (single‑byte) throw to cover all read paths
        when(faulty.read()).thenThrow(new IOException("Simulated read failure"));
        // WHEN the utility method is invoked
        byte[] result = IOUtilities.inputStreamToBytes(faulty);
        // THEN the method swallows the exception and returns null
        assertNull(result, "Result should be null when an exception occurs while reading");
        // Verify that read was indeed attempted
        verify(faulty, atLeastOnce()).read(any(byte[].class), anyInt(), anyInt());
    }

    /**
     * Verify that an empty stream yields an empty byte array (not {@code null}).
     */
    @Test
    void inputStreamToBytes_emptyStreamReturnsEmptyArray() {
        // GIVEN an empty ByteArrayInputStream
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        // WHEN the utility method is invoked
        byte[] result = IOUtilities.inputStreamToBytes(empty);
        // THEN the result is a zero‑length array
        assertNotNull(result, "Result should not be null for an empty stream");
        assertEquals(0, result.length, "Resulting array should be empty");
    }

    @ParameterizedTest(name = "[{index}] Normal operation with stream size {1}.length")
    @MethodSource("normalOperationProvider")
    void inputStreamToBytes_normalOperation(InputStream in, byte[] expected) {
        // WHEN the utility method is invoked
        byte[] result = IOUtilities.inputStreamToBytes(in);
        // THEN the result matches expectations
        assertNotNull(result, "Result should never be null for a successful read");
        assertArrayEquals(expected, result, "Returned bytes must be identical to the source");
    }

    /**
     * Passing {@code null} as the InputStream triggers a NullPointerException
     * inside the utility method, which is caught and results in {@code null}.
     */
    @Test
    void inputStreamToBytes_nullInputReturnsNull() {
        // GIVEN a null InputStream
        InputStream in = null;
        // WHEN the utility method is invoked
        byte[] result = IOUtilities.inputStreamToBytes(in);
        // THEN null is returned
        assertNull(result, "Result should be null when a null InputStream is supplied");
    }

    /**
     * An InputStream that throws a checked IOException during read should cause
     * the method to return {@code null}.
     */
    @Test
    void inputStreamToBytes_ioExceptionDuringReadReturnsNull() {
        // GIVEN an InputStream that always throws IOException
        InputStream faulty = mock(InputStream.class);
        try {
            when(faulty.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Simulated read failure"));
            when(faulty.read()).thenThrow(new IOException("Simulated single‑byte read failure"));
        } catch (IOException ignored) {
            // Mockito's stubbing does not actually throw here
        }
        // WHEN the utility method is invoked
        byte[] result = IOUtilities.inputStreamToBytes(faulty);
        // THEN null is returned and the read attempt was made
        assertNull(result, "Result should be null when an IOException occurs while reading");
        verify(faulty, atLeastOnce()).read(any(byte[].class), anyInt(), anyInt());
    }

    /**
     * An InputStream that throws an unchecked RuntimeException during read should also
     * be caught by the generic catch block, resulting in {@code null}.
     */
    @Test
    void inputStreamToBytes_runtimeExceptionDuringReadReturnsNull() {
        // GIVEN an InputStream that throws RuntimeException on read()
        InputStream bad = new InputStream() {

            @Override
            public int read() {
                throw new RuntimeException("forced");
            }
        };
        // WHEN the utility method is invoked
        byte[] result = IOUtilities.inputStreamToBytes(bad);
        // THEN null is returned
        assertNull(result, "Result should be null when a RuntimeException occurs while reading");
    }

    /**
     * Verify that the method can handle a moderately sized stream (16 KB buffer size
     * used internally) without truncation.
     */
    @Test
    void inputStreamToBytes_handlesBufferBoundary() {
        // GIVEN a stream whose size exceeds the internal initial buffer (16 KB)
        byte[] largeData = new byte[20_000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        InputStream in = new ByteArrayInputStream(largeData);
        // WHEN the utility method is invoked
        byte[] result = IOUtilities.inputStreamToBytes(in);
        // THEN the entire content is returned unchanged
        assertNotNull(result, "Result should not be null for a successful read");
        assertArrayEquals(largeData, result, "Returned bytes must match the original large payload");
    }
}
