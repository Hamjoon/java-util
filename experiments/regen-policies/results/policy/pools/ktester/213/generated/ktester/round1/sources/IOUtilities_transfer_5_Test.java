package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URLConnection;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_5_Test {

    /**
     * Sample data used for the successful transfer test.
     */
    private static final byte[] SAMPLE_BYTES = "Sample payload for IOUtilities.transfer".getBytes();

    /**
     * Mocked URLConnection that will provide a controllable OutputStream.
     */
    @Mock
    private URLConnection mockConnection;

    /**
     * The OutputStream returned by the mocked connection.
     */
    private ByteArrayOutputStream connectionOut;

    /**
     * An OutputStream that throws an IOException on any write operation.
     */
    private static class FaultyOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            throw new IOException("write exception");
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            throw new IOException("write exception");
        }
    }

    /**
     * Tracks whether {@link #close()} has been called and optionally throws an IOException on close.
     */
    private static class CloseTrackingOutputStream extends OutputStream {

        private final boolean throwOnClose;

        private boolean closed = false;

        CloseTrackingOutputStream(boolean throwOnClose) {
            this.throwOnClose = throwOnClose;
        }

        @Override
        public void write(int b) {
            // no‑op – we only care about close behaviour for these tests
        }

        @Override
        public void close() throws IOException {
            closed = true;
            if (throwOnClose) {
                throw new IOException("close exception");
            }
        }

        boolean isClosed() {
            return closed;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Each test gets a fresh ByteArrayOutputStream so that assertions are isolated.
        connectionOut = new ByteArrayOutputStream();
        when(mockConnection.getOutputStream()).thenReturn(connectionOut);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Clean up the stream – ByteArrayOutputStream's close is a no‑op but we call it for completeness.
        connectionOut.close();
    }

    /* --------------------------------------------------------------
     *  Null argument validation (two cases)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> nullArgumentProvider() {
        return Stream.of(Arguments.of(null, SAMPLE_BYTES, "URLConnection cannot be null"), Arguments.of(mock(URLConnection.class), null, "byte array cannot be null"));
    }

    /* --------------------------------------------------------------
     *  OutputStream exception propagation
     * -------------------------------------------------------------- */
    private static Stream<Arguments> exceptionPropagationProvider() {
        return Stream.of(// write() throws IOException
        Arguments.of(new FaultyOutputStream(), "write", "write exception"), // getOutputStream() throws IOException
        Arguments.of(null, "getOutputStream", "getOutputStream exception"));
    }

    /* --------------------------------------------------------------
     *  OutputStream close verification
     * -------------------------------------------------------------- */
    private static Stream<Arguments> closeVerificationProvider() {
        return Stream.of(// normal close tracking
        Arguments.of(new CloseTrackingOutputStream(false), false, "stream should be closed"), // close throws IOException
        Arguments.of(new CloseTrackingOutputStream(true), true, "close exception"));
    }

    /**
     * Verify that the method writes the supplied bytes to the connection's output stream.
     */
    @Test
    void testTransferWritesBytesToConnection() throws IOException {
        // When
        IOUtilities.transfer(mockConnection, SAMPLE_BYTES);
        // Then
        assertArrayEquals(SAMPLE_BYTES, connectionOut.toByteArray(), "The bytes written to the connection's OutputStream should match the input array.");
    }

    /**
     * Verify that a {@code null} URLConnection causes an {@link IllegalArgumentException}.
     */
    @Test
    void testTransferNullConnectionThrows() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> IOUtilities.transfer(null, SAMPLE_BYTES));
        assertTrue(thrown.getMessage().contains("URLConnection cannot be null"));
    }

    /**
     * Verify that a {@code null} byte array causes an {@link IllegalArgumentException}.
     */
    @Test
    void testTransferNullByteArrayThrows() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> IOUtilities.transfer(mockConnection, null));
        assertTrue(thrown.getMessage().contains("byte array cannot be null"));
    }

    @ParameterizedTest(name = "[{index}] null argument test")
    @MethodSource("nullArgumentProvider")
    void testNullArgumentValidation(URLConnection conn, byte[] bytes, String expectedMessage) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> IOUtilities.transfer(conn, bytes));
        assertTrue(ex.getMessage().contains(expectedMessage));
    }

    /* --------------------------------------------------------------
     *  Empty byte array
     * -------------------------------------------------------------- */
    @Test
    void testTransferEmptyByteArray() throws IOException {
        byte[] empty = new byte[0];
        IOUtilities.transfer(mockConnection, empty);
        assertEquals(0, connectionOut.size(), "When an empty array is transferred the underlying stream should remain empty.");
    }

    /* --------------------------------------------------------------
     *  Large byte array (10 000 bytes)
     * -------------------------------------------------------------- */
    @Test
    void testTransferLargeByteArray() throws IOException {
        byte[] large = new byte[10_000];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) (i % 256);
        }
        IOUtilities.transfer(mockConnection, large);
        assertArrayEquals(large, connectionOut.toByteArray(), "All 10 000 bytes must be transferred unchanged.");
    }

    @ParameterizedTest(name = "[{index}] exception propagation: {1}")
    @MethodSource("exceptionPropagationProvider")
    void testOutputStreamExceptionPropagation(OutputStream customOut, String scenario, String message) throws IOException {
        if ("getOutputStream".equals(scenario)) {
            when(mockConnection.getOutputStream()).thenThrow(new IOException(message));
        } else {
            // use the custom faulty stream
            when(mockConnection.getOutputStream()).thenReturn(customOut);
        }
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, new byte[] { 1, 2, 3 }));
        assertTrue(ex.getMessage().contains(message));
    }

    @ParameterizedTest(name = "[{index}] close verification (throwsOnClose={1})")
    @MethodSource("closeVerificationProvider")
    void testCloseVerification(CloseTrackingOutputStream out, boolean throwsOnClose, String expectedMessage) {
        when(mockConnection.getOutputStream()).thenReturn(out);
        if (throwsOnClose) {
            IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, new byte[] { 9, 9, 9 }));
            assertTrue(ex.getMessage().contains(expectedMessage));
        } else {
            assertDoesNotThrow(() -> IOUtilities.transfer(mockConnection, new byte[] { 5, 6, 7 }));
            assertTrue(out.isClosed(), "The underlying OutputStream must be closed by the try‑with‑resources block.");
        }
    }

    /* --------------------------------------------------------------
     *  Null OutputStream from URLConnection
     * -------------------------------------------------------------- */
    @Test
    void testNullOutputStreamFromConnection() throws IOException {
        when(mockConnection.getOutputStream()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(mockConnection, new byte[] { 1, 2, 3 }));
    }

    /**
     * Utility method to create a mock without generic warnings.
     */
    @SuppressWarnings("unchecked")
    private static <T> T mock(Class<T> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }
}
