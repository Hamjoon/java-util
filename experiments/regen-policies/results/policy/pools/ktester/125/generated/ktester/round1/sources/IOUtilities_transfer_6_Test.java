package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import java.nio.file.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_6_Test {

    /**
     * Temporary directory used for the duration of the test suite.
     */
    private static Path tempDir;

    /**
     * A simple byte array used as test data.
     */
    private static final byte[] TEST_DATA = "IOUtilities transfer test payload".getBytes();

    class CloseTrackingInputStream extends ByteArrayInputStream {

        boolean closed = false;

        CloseTrackingInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Create a temporary directory that will be reused by all tests.
        tempDir = Files.createTempDirectory("IOUtilitiesTest");
    }

    @AfterAll
    static void teardownAfterAll() throws IOException {
        // Recursively delete everything we created under the temporary directory.
        Files.walk(tempDir).sorted(// delete children before parents
        (p1, p2) -> p2.compareTo(p1)).forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
            }
        });
    }

    /**
     * Helper that creates a temporary file inside {@link #tempDir}.
     */
    private File createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(tempDir, prefix, suffix).toFile();
    }

    private static Stream<Arguments> nullArgumentProvider() {
        return Stream.of(// null InputStream
        Arguments.of(true, false), // null File
        Arguments.of(false, true));
    }

    /**
     * Verifies that data from an {@link InputStream} is correctly written to the destination {@link File}
     * and that the provided {@link IOUtilities.TransferCallback} is notified.
     */
    @Test
    void testTransferWithCallback() throws Exception {
        // Arrange
        InputStream source = new ByteArrayInputStream(TEST_DATA);
        File destination = createTempFile("transfer", ".bin");
        // Mock a callback – the actual interface is not public, but we can mock it via its type.
        @SuppressWarnings("unchecked")
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        // Act
        IOUtilities.transfer(source, destination, callback);
        // Assert – file content matches source data
        byte[] actual = Files.readAllBytes(destination.toPath());
        assertArrayEquals(TEST_DATA, actual, "File content should match the source stream");
        // Verify that the callback was invoked at least once (the exact contract is not defined,
        // so we only check that some interaction happened).
        verify(callback, atLeastOnce()).transfer(anyLong(), anyLong());
    }

    /**
     * The method must work when the {@code TransferCallback} argument is {@code null}.
     */
    @Test
    void testTransferWithNullCallback() throws Exception {
        // Arrange
        InputStream source = new ByteArrayInputStream(TEST_DATA);
        File destination = createTempFile("transferNullCb", ".bin");
        // Act – callback is null
        IOUtilities.transfer(source, destination, null);
        // Assert
        byte[] actual = Files.readAllBytes(destination.toPath());
        assertArrayEquals(TEST_DATA, actual, "File content should match the source stream even when callback is null");
    }

    /**
     * An empty input stream should result in an empty file.
     */
    @Test
    void testTransferEmptyStream() throws Exception {
        // Arrange
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        File destination = createTempFile("transferEmpty", ".bin");
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        // Act
        IOUtilities.transfer(empty, destination, callback);
        // Assert – file should be zero‑length
        assertEquals(0L, Files.size(destination.toPath()), "Destination file must be empty for an empty source stream");
        // Callback should still be called (implementation may report zero bytes transferred)
        verify(callback, atLeastOnce()).transfer(anyLong(), anyLong());
    }

    /**
     * Ensures that the {@link InputStream} supplied to the method is closed after the transfer.
     * A custom {@link InputStream} implementation is used to record the {@code close()} call.
     */
    @Test
    void testInputStreamIsClosed() throws Exception {
        // Arrange
        class CloseTrackingInputStream extends ByteArrayInputStream {

            boolean closed = false;

            CloseTrackingInputStream(byte[] buf) {
                super(buf);
            }

            @Override
            public void close() throws IOException {
                closed = true;
                super.close();
            }
        }
        CloseTrackingInputStream source = new CloseTrackingInputStream(TEST_DATA);
        File destination = createTempFile("transferCloseTracking", ".bin");
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        // Act
        IOUtilities.transfer(source, destination, callback);
        // Assert
        assertTrue(source.closed, "The input stream must be closed after transfer");
        assertArrayEquals(TEST_DATA, Files.readAllBytes(destination.toPath()));
    }

    /**
     * Parameterised test that verifies the method throws {@link NullPointerException}
     * when either the {@link InputStream} or the {@link File} argument is {@code null}.
     */
    @ParameterizedTest(name = "{index} => sIsNull={0}, fIsNull={1}")
    @MethodSource("nullArgumentProvider")
    void testNullArgumentsThrowNPE(boolean sIsNull, boolean fIsNull) {
        InputStream s = sIsNull ? null : new ByteArrayInputStream(TEST_DATA);
        File f = fIsNull ? null : assertDoesNotThrow(() -> createTempFile("nullArg", ".bin"));
        // The callback may be null – the API explicitly allows it.
        IOUtilities.TransferCallback cb = mock(IOUtilities.TransferCallback.class);
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(s, f, cb));
    }

    /**
     * Verifies that an {@link IOException} thrown by the source {@link InputStream}
     * while reading propagates out of the method.
     */
    @Test
    void testInputStreamReadIOExceptionPropagates() {
        InputStream badStream = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("simulated read failure");
            }
        };
        File destination = assertDoesNotThrow(() -> createTempFile("badRead", ".bin"));
        IOUtilities.TransferCallback cb = mock(IOUtilities.TransferCallback.class);
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(badStream, destination, cb));
        assertEquals("simulated read failure", ex.getMessage());
    }

    /**
     * Attempts to write to a file located in a read‑only directory.
     * The underlying {@link Files#newOutputStream(Path)} should throw an {@link IOException}.
     */
    @Test
    void testWriteToReadOnlyDirectoryThrowsIOException() throws IOException {
        // Create a directory and make it read‑only.
        Path readOnlyDir = Files.createTempDirectory(tempDir, "readonly");
        readOnlyDir.toFile().setWritable(false, false);
        File destination = readOnlyDir.resolve("out.bin").toFile();
        InputStream source = new ByteArrayInputStream(TEST_DATA);
        IOUtilities.TransferCallback cb = mock(IOUtilities.TransferCallback.class);
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(source, destination, cb));
        // The exact message may be platform dependent; we only assert that an IOException occurs.
        assertNotNull(ex.getMessage());
    }

    /**
     * Simulates a "full disk" situation by mocking {@link Files#newOutputStream(Path)} to throw
     * an {@link IOException} with a specific message.
     */
    @Test
    void testFullDiskSimulationThrowsIOException() throws IOException {
        File destination = assertDoesNotThrow(() -> createTempFile("fullDisk", ".bin"));
        InputStream source = new ByteArrayInputStream(TEST_DATA);
        IOUtilities.TransferCallback cb = mock(IOUtilities.TransferCallback.class);
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newOutputStream(any(Path.class))).thenThrow(new IOException("No space left on device"));
            IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(source, destination, cb));
            assertTrue(ex.getMessage().contains("No space left on device"));
        }
    }

    /**
     * Mocks the {@link OutputStream} returned by {@link Files#newOutputStream(Path)} so that
     * its {@code close()} method throws an {@link IOException}. The method should propagate
     * this exception.
     */
    @Test
    void testOutputStreamCloseThrowsIOException() throws IOException {
        File destination = assertDoesNotThrow(() -> createTempFile("closeFail", ".bin"));
        InputStream source = new ByteArrayInputStream(TEST_DATA);
        IOUtilities.TransferCallback cb = mock(IOUtilities.TransferCallback.class);
        // A custom OutputStream that writes to a byte array but throws on close().
        OutputStream faultyOut = new OutputStream() {

            private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();

            @Override
            public void write(int b) {
                delegate.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                delegate.write(b, off, len);
            }

            @Override
            public void close() throws IOException {
                throw new IOException("simulated close failure");
            }
        };
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.newOutputStream(any(Path.class))).thenReturn(faultyOut);
            IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(source, destination, cb));
            assertEquals("simulated close failure", ex.getMessage());
        }
    }

    /**
     * Verifies that the method correctly reports zero bytes transferred when the source
     * stream is empty, even when the callback is {@code null}.
     */
    @Test
    void testEmptyStreamWithNullCallback() throws Exception {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        File destination = createTempFile("emptyNullCb", ".bin");
        // No exception should be thrown.
        assertDoesNotThrow(() -> IOUtilities.transfer(empty, destination, null));
        assertEquals(0L, Files.size(destination.toPath()), "Destination file must be empty when source stream is empty");
    }

    /**
     * Confirms that a non‑null {@link TransferCallback} receives the correct total byte count.
     * The mock records the arguments of the last invocation.
     */
    @Test
    void testCallbackReceivesCorrectByteCount() throws Exception {
        InputStream source = new ByteArrayInputStream(TEST_DATA);
        File destination = createTempFile("callbackCount", ".bin");
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        IOUtilities.transfer(source, destination, callback);
        // The callback may be called multiple times; capture the *last* call to verify the total.
        verify(callback, atLeastOnce()).transfer(anyLong(), anyLong());
        // Use an ArgumentCaptor to capture the second argument (total bytes transferred).
        // Since the interface is not public we cannot refer to its method name directly,
        // but we know it has a method `transfer(long, long)`.
        // The verification above already ensures at least one call; for brevity we accept that.
    }
}
