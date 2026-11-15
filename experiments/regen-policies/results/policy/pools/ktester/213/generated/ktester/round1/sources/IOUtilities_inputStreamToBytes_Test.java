package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_inputStreamToBytes_Test {

    /**
     * Sample data used for the successful conversion test.
     */
    private static final byte[] SAMPLE_DATA = "Hello, IOUtilities!".getBytes();

    class CloseTrackingInputStream extends InputStream {

        private final byte[] data = new byte[] { 9, 8, 7 };

        private int pos = 0;

        private boolean closed = false;

        @Override
        public int read() {
            return (pos < data.length) ? (data[pos++] & 0xFF) : -1;
        }

        @Override
        public void close() {
            closed = true;
        }

        boolean isClosed() {
            return closed;
        }
    }

    class CloseTrackingInputStream extends InputStream {

        private final byte[] data = new byte[] { 9, 8, 7 };

        private int pos = 0;

        private boolean closed = false;

        @Override
        public int read() {
            return (pos < data.length) ? (data[pos++] & 0xFF) : -1;
        }

        @Override
        public void close() {
            closed = true;
        }

        boolean isClosed() {
            return closed;
        }
    }

    /**
     * Helper that creates a deterministic byte array of the requested size.
     */
    private static byte[] createLargeByteArray(int size) {
        byte[] arr = new byte[size];
        for (int i = 0; i < size; i++) {
            arr[i] = (byte) (i % 256);
        }
        return arr;
    }

    /**
     * Verify that a normal {@link InputStream} is fully read and the resulting byte array
     * matches the original content.
     */
    @Test
    void testInputStreamToBytesNormal() {
        InputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        byte[] result = IOUtilities.inputStreamToBytes(in);
        assertNotNull(result, "Result should not be null for a valid stream");
        assertArrayEquals(SAMPLE_DATA, result, "The bytes read from the stream must match the original data");
    }

    /**
     * Verify that passing {@code null} as the input stream triggers the validation
     * performed by {@code Convention.throwIfNull}. The exact exception type is not
     * part of the public contract, but it must be a {@link RuntimeException}.
     */
    @Test
    void testInputStreamToBytesNull() {
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> IOUtilities.inputStreamToBytes(null));
        assertTrue(thrown.getMessage().contains("Inputstream cannot be null"), "Exception message should indicate a null input stream");
    }

    /**
     * Simulate an {@link InputStream} that throws an {@link IOException} during read.
     * According to the implementation, any exception results in a {@code null} return value.
     */
    @Test
    void testInputStreamToBytesWhenExceptionOccursReturnsNull(@Mock InputStream faultyStream) throws IOException {
        // Configure the mock to throw an IOException on the first read operation.
        when(faultyStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Simulated read failure"));
        // The method should swallow the exception and return null.
        byte[] result = IOUtilities.inputStreamToBytes(faultyStream);
        assertNull(result, "When an exception occurs during transfer, the method should return null");
    }

    /**
     * Simulate an {@link InputStream} that throws an {@link IOException} during read.
     * The method should swallow the exception and return {@code null}.
     */
    @Test
    void testInputStreamToBytesWhenIOExceptionOccursReturnsNull(@Mock InputStream faultyStream) throws IOException {
        when(faultyStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Simulated read failure"));
        byte[] result = IOUtilities.inputStreamToBytes(faultyStream);
        assertNull(result, "When an exception occurs during transfer, the method should return null");
    }

    /**
     * Verify that an empty stream results in an empty byte array (length zero).
     */
    @Test
    void testInputStreamToBytesEmptyStream() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        byte[] result = IOUtilities.inputStreamToBytes(empty);
        assertNotNull(result, "Result should not be null for an empty stream");
        assertEquals(0, result.length, "Resulting array should be empty");
    }

    /**
     * Verify that a large stream (20 000 bytes) is correctly read even though it exceeds
     * the initial buffer size of {@code FastByteArrayOutputStream} (16 384).
     */
    @Test
    void testInputStreamToBytesLargeStream() {
        byte[] large = createLargeByteArray(20_000);
        InputStream in = new ByteArrayInputStream(large);
        byte[] result = IOUtilities.inputStreamToBytes(in);
        assertNotNull(result);
        assertArrayEquals(large, result, "All bytes must be read correctly for a large stream");
    }

    /**
     * Stream that returns a few bytes and then throws an {@link IOException}.
     * The method should return {@code null} because the exception is caught.
     */
    @Test
    void testInputStreamToBytesPartialReadThenIOException() {
        InputStream partial = new InputStream() {

            private final byte[] data = new byte[] { 1, 2, 3, 4, 5 };

            private int pos = 0;

            @Override
            public int read() throws IOException {
                if (pos < data.length) {
                    return data[pos++] & 0xFF;
                }
                throw new IOException("forced error after some bytes");
            }
        };
        byte[] result = IOUtilities.inputStreamToBytes(partial);
        assertNull(result, "Method should return null when an IOException occurs after partial read");
    }

    /**
     * Stream that throws a {@link RuntimeException} on read.
     * The generic catch block should also handle this and return {@code null}.
     */
    @Test
    void testInputStreamToBytesRuntimeExceptionDuringRead() {
        InputStream runtimeFail = new InputStream() {

            @Override
            public int read() {
                throw new IllegalStateException("runtime failure");
            }
        };
        byte[] result = IOUtilities.inputStreamToBytes(runtimeFail);
        assertNull(result, "Method should return null when a RuntimeException occurs during read");
    }

    /**
     * Custom InputStream that records whether {@code close()} was called.
     * After invoking {@code inputStreamToBytes}, we assert both the returned data
     * and that the stream was closed.
     */
    @Test
    void testInputStreamToBytesClosesStream() throws Exception {
        // Define a stream that can report its closed state.
        class CloseTrackingInputStream extends InputStream {

            private final byte[] data = new byte[] { 9, 8, 7 };

            private int pos = 0;

            private boolean closed = false;

            @Override
            public int read() {
                return (pos < data.length) ? (data[pos++] & 0xFF) : -1;
            }

            @Override
            public void close() {
                closed = true;
            }

            boolean isClosed() {
                return closed;
            }
        }
        CloseTrackingInputStream tracking = new CloseTrackingInputStream();
        byte[] result = IOUtilities.inputStreamToBytes(tracking);
        assertArrayEquals(new byte[] { 9, 8, 7 }, result, "Returned bytes must match the stream content");
        assertTrue(tracking.isClosed(), "The InputStream should be closed by the try‑with‑resources block");
    }
}
