package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_3_Test {

    /* --------------------------------------------------------------------- */
    private static int getTransferBufferSize() {
        try {
            Field f = IOUtilities.class.getDeclaredField("TRANSFER_BUFFER");
            f.setAccessible(true);
            return (int) f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access TRANSFER_BUFFER", e);
        }
    }

    /**
     * Provides the normal‑scenario test data described in the JSON file.
     */
    private static Stream<Arguments> normalScenarioProvider() {
        int buffer = getTransferBufferSize();
        // 1. Empty input
        byte[] empty = new byte[0];
        InputStream emptyIn = new ByteArrayInputStream(empty);
        // 2. Small input ("Hello")
        byte[] hello = "Hello".getBytes(StandardCharsets.UTF_8);
        InputStream helloIn = new ByteArrayInputStream(hello);
        // 3. Exactly one buffer worth of zeros
        // filled with 0 by default
        byte[] exactlyOneBuffer = new byte[buffer];
        InputStream exactlyOneBufferIn = new ByteArrayInputStream(exactlyOneBuffer);
        // 4. Larger than buffer: two full buffers + 12 345 extra bytes
        byte[] large = concat(new byte[buffer], new byte[buffer], // all zeros
        new byte[12_345]);
        InputStream largeIn = new ByteArrayInputStream(large);
        // 5. Small non‑empty array 1‑10
        byte[] oneToTen = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        InputStream oneToTenIn = new ByteArrayInputStream(oneToTen);
        return Stream.of(Arguments.of("Empty input stream", emptyIn, empty), Arguments.of("Input smaller than buffer (\"Hello\")", helloIn, hello), Arguments.of("Input exactly equal to internal buffer size", exactlyOneBufferIn, exactlyOneBuffer), Arguments.of("Input larger than buffer (2×buffer + 12 345 bytes)", largeIn, large), Arguments.of("Small non‑empty stream 1‑10", oneToTenIn, oneToTen));
    }

    /**
     * Utility to concatenate several byte arrays into one.
     */
    private static byte[] concat(byte[]... arrays) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            for (byte[] a : arrays) {
                bos.write(a);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            // ByteArrayOutputStream never throws IOException
            throw new AssertionError(e);
        }
    }

    /**
     * Verify that a normal (non‑empty) stream is copied verbatim.
     */
    @Test
    void transferCopiesAllBytes() throws IOException {
        byte[] source = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        try (InputStream in = new ByteArrayInputStream(source);
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            IOUtilities.transfer(in, out);
            assertArrayEquals(source, out.toByteArray(), "Output stream should contain exactly the same bytes as the input");
        }
    }

    /**
     * Verify that an empty input stream results in an empty output stream.
     */
    @Test
    void transferHandlesEmptyStream() throws IOException {
        byte[] empty = new byte[0];
        try (InputStream in = new ByteArrayInputStream(empty);
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            IOUtilities.transfer(in, out);
            assertEquals(0, out.size(), "Output stream should be empty when input is empty");
        }
    }

    /**
     * Verify that data larger than the internal buffer (32 KB) is transferred correctly.
     */
    @Test
    void transferLargeDataBeyondBuffer() throws IOException {
        // 100 KB of data – more than three times the internal buffer size (32768)
        byte[] source = new byte[100 * 1024];
        for (int i = 0; i < source.length; i++) {
            source[i] = (byte) (i % 256);
        }
        try (InputStream in = new ByteArrayInputStream(source);
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            IOUtilities.transfer(in, out);
            assertArrayEquals(source, out.toByteArray(), "All bytes must be transferred even when the total size exceeds the buffer");
        }
    }

    /**
     * Verify that an IOException thrown by the OutputStream propagates out of the method.
     */
    @Test
    void transferPropagatesIOExceptionFromOutput(@Mock OutputStream failingOut) throws IOException {
        // Arrange: mock OutputStream that throws after a successful write of the first two bytes
        byte[] source = "abcde".getBytes(StandardCharsets.UTF_8);
        InputStream in = new ByteArrayInputStream(source);
        // First call succeeds (writes first two bytes)
        doNothing().when(failingOut).write(any(byte[].class), eq(0), eq(2));
        // Second call throws
        doThrow(new IOException("simulated write failure")).when(failingOut).write(any(byte[].class), eq(2), eq(3));
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(in, failingOut), "IOException from the OutputStream should be propagated");
        assertEquals("simulated write failure", ex.getMessage());
    }

    /**
     * Verify that an IOException thrown by the InputStream propagates out of the method.
     */
    @Test
    void transferPropagatesIOExceptionFromInput(@Mock InputStream failingIn) throws IOException {
        // Arrange: mock InputStream that throws on the first read
        when(failingIn.read(any(byte[].class))).thenThrow(new IOException("simulated read failure"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Act & Assert
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(failingIn, out), "IOException from the InputStream should be propagated");
        assertEquals("simulated read failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalScenarioProvider")
    void transferNormalScenarios(String description, InputStream in, byte[] expected) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            IOUtilities.transfer(in, out);
            assertArrayEquals(expected, out.toByteArray(), () -> "Failed scenario: " + description);
        }
    }

    @Test
    void transferPropagatesNullPointerExceptionWhenInputIsNull() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(null, out), "Passing a null InputStream should raise NullPointerException");
    }

    @Test
    void transferPropagatesNullPointerExceptionWhenOutputIsNull() {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(in, null), "Passing a null OutputStream should raise NullPointerException");
    }
}
