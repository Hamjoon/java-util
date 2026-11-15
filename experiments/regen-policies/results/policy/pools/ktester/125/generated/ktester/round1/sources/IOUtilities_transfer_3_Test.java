package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_3_Test {

    /**
     * Supplies test data for {@link #transfer_basicScenarios(String, byte[])}.
     */
    private static Stream<Arguments> basicTransferProvider() {
        // empty payload
        byte[] empty = new byte[0];
        // small payload – fits into the internal 32 KB buffer
        byte[] small = "Hello, world!".getBytes(StandardCharsets.UTF_8);
        // large payload – 200 KB deterministic pattern, forces several buffer cycles
        byte[] large = new byte[200_000];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) (i % 256);
        }
        return Stream.of(Arguments.of("Empty input stream", empty), Arguments.of("Small input (< buffer size)", small), Arguments.of("Large input (> buffer size, multiple reads)", large));
    }

    /**
     * -----------------------------------------------------------------
     *  Test that a typical non‑empty stream is copied verbatim.
     *  -----------------------------------------------------------------
     */
    @Test
    void transfer_copiesAllBytes_correctly() throws IOException {
        // given
        byte[] source = "The quick brown fox jumps over the lazy dog".getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // when
        IOUtilities.transfer(in, out);
        // then
        assertArrayEquals(source, out.toByteArray(), "Output stream should contain exactly the bytes read from the input stream");
    }

    /**
     * -----------------------------------------------------------------
     *  Test that an empty input produces an empty output and does not
     *  throw any exception.
     *  -----------------------------------------------------------------
     */
    @Test
    void transfer_emptyInput_producesEmptyOutput() throws IOException {
        // given
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // when
        IOUtilities.transfer(in, out);
        // then
        assertEquals(0, out.size(), "Output stream should be empty when input stream is empty");
    }

    /**
     * -----------------------------------------------------------------
     *  Test that data larger than the internal buffer (32 KB) is copied
     *  correctly. This forces the loop to iterate more than once.
     *  -----------------------------------------------------------------
     */
    @Test
    void transfer_largeData_multipleBuffers_copiesAll() throws IOException {
        // given – 100 KB of deterministic data
        byte[] source = new byte[100 * 1024];
        for (int i = 0; i < source.length; i++) {
            source[i] = (byte) (i % 256);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // when
        IOUtilities.transfer(in, out);
        // then
        assertArrayEquals(source, out.toByteArray(), "All bytes, including those requiring multiple buffer reads, must be transferred");
    }

    /**
     * -----------------------------------------------------------------
     *  Verify that the method propagates an IOException thrown by the
     *  OutputStream. The InputStream should remain open after the call.
     *  -----------------------------------------------------------------
     */
    @Test
    void transfer_outputThrowsIOException_propagatesException() {
        // given
        byte[] data = "sample".getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        OutputStream faultyOut = mock(OutputStream.class);
        try {
            doThrow(new IOException("simulated write failure")).when(faultyOut).write(any(byte[].class), anyInt(), anyInt());
        } catch (IOException e) {
            // Mockito's doThrow does not actually throw here; this block will not be executed.
        }
        // when / then
        IOException thrown = assertThrows(IOException.class, () -> IOUtilities.transfer(in, faultyOut), "IOUtilities.transfer should re‑throw IOException from the output stream");
        assertEquals("simulated write failure", thrown.getMessage());
        // The input stream must still be readable (i.e., not closed by the method)
        assertDoesNotThrow(() -> {
            int remaining = in.read();
            // The stream may have read some bytes before the failure; we only assert that calling read does not throw.
        });
    }

    /**
     * -----------------------------------------------------------------
     *  Ensure that the method does not close either the input or the
     *  output stream – callers are responsible for resource management.
     *  -----------------------------------------------------------------
     */
    @Test
    void transfer_doesNotCloseStreams() throws IOException {
        // given
        byte[] data = "stay open".getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // when
        IOUtilities.transfer(in, out);
        // then – both streams should still be usable
        assertEquals(-1, in.read(), "InputStream should be at EOF but not closed");
        // should not throw IllegalStateException
        out.write(0);
    }

    /* -----------------------------------------------------------------
     *  Parameterized test covering the three basic functional cases:
     *   1. Empty input
     *   2. Small input (< 32 KB buffer)
     *   3. Large input (> 32 KB buffer, forces multiple reads)
     *  ----------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicTransferProvider")
    void transfer_basicScenarios(String description, byte[] source) throws IOException {
        // given
        InputStream in = new ByteArrayInputStream(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // when
        IOUtilities.transfer(in, out);
        // then
        assertArrayEquals(source, out.toByteArray(), "The output must contain exactly the bytes read from the input");
    }

    /* -----------------------------------------------------------------
     *  New test: InputStream throws IOException during read.
     *  The method must propagate the exception unchanged.
     *  ----------------------------------------------------------------- */
    @Test
    void transfer_inputThrowsIOException_propagatesException() {
        // given – custom InputStream that fails on the first read call
        InputStream badIn = new InputStream() {

            @Override
            public int read(byte[] b) throws IOException {
                throw new IOException("forced read error");
            }

            @Override
            public int read() throws IOException {
                throw new IOException("forced read error");
            }
        };
        OutputStream out = new ByteArrayOutputStream();
        // when / then
        IOException thrown = assertThrows(IOException.class, () -> IOUtilities.transfer(badIn, out), "IOUtilities.transfer should propagate IOException from the input stream");
        assertEquals("forced read error", thrown.getMessage());
    }
}
