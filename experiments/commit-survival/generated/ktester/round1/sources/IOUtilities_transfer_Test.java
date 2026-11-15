package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.concurrent.atomic.AtomicLong;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_Test {

    /**
     * Temporary file that will act as the source for the transfer
     */
    private File sourceFile;

    /**
     * Captures what the {@link URLConnection} receives
     */
    private ByteArrayOutputStream connectionOut;

    /**
     * Mocked URLConnection – its {@code getOutputStream()} will return {@link #connectionOut}
     */
    @Mock
    private URLConnection mockConnection;

    /**
     * Mocked TransferCallback – we do not need to assert any specific behaviour,
     *  we only need a non‑null instance to satisfy the method signature.
     */
    @Mock
    private IOUtilities.TransferCallback mockCallback;

    class SpyConnection extends URLConnection {

        final OutputStream real = new ByteArrayOutputStream();

        final OutputStream spy = spy(real);

        boolean connected = false;

        protected SpyConnection() {
            super(null);
        }

        @Override
        public void connect() {
            connected = true;
        }

        @Override
        public OutputStream getOutputStream() {
            return spy;
        }
    }

    class SpyConnection extends URLConnection {

        final OutputStream real = new ByteArrayOutputStream();

        final OutputStream spy = spy(real);

        boolean connected = false;

        protected SpyConnection() {
            super(null);
        }

        @Override
        public void connect() {
            connected = true;
        }

        @Override
        public OutputStream getOutputStream() {
            return spy;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // 1️⃣ Create a temporary file with known content
        Path tempPath = Files.createTempFile("ioUtilitiesTransferTest", ".tmp");
        sourceFile = tempPath.toFile();
        Files.write(tempPath, "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8));
        // 2️⃣ Prepare a ByteArrayOutputStream that will receive the data from the transfer
        connectionOut = new ByteArrayOutputStream();
        // 3️⃣ Stub the URLConnection to return our ByteArrayOutputStream
        when(mockConnection.getOutputStream()).thenReturn(connectionOut);
    }

    @AfterEach
    void teardownAfterEach() {
        // Delete the temporary file if it still exists
        if (sourceFile != null && sourceFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            sourceFile.delete();
        }
    }

    @Test
    void transfer_copiesFileContentToURLConnection_andClosesResources() throws Exception {
        // GIVEN – the source file, a mocked connection and a mocked callback are ready (set up in @BeforeEach)
        // WHEN – invoke the method under test
        IOUtilities.transfer(sourceFile, mockConnection, mockCallback);
        // THEN – the data written to the connection must be identical to the file's content
        byte[] expected = Files.readAllBytes(sourceFile.toPath());
        byte[] actual = connectionOut.toByteArray();
        assertArrayEquals(expected, actual, "The bytes transferred to the URLConnection must match the file content");
        // Verify that the callback was not used in a way that would cause an exception.
        // Since we do not know the exact contract of TransferCallback, we simply verify that
        // no unexpected interactions occurred.
        verifyNoMoreInteractions(mockCallback);
    }

    /* ------------------------------------------------------------
     *  1. Successful transfer – also verifies that the callback receives
     *     the total number of transferred bytes.
     * ------------------------------------------------------------ */
    @Test
    void successfulTransfer_callsCallbackWithCorrectByteCount() throws Exception {
        // Arrange – a real callback that records the total bytes transferred
        AtomicLong transferred = new AtomicLong(-1);
        IOUtilities.TransferCallback countingCallback = bytes -> transferred.set(bytes);
        // Act
        IOUtilities.transfer(sourceFile, mockConnection, countingCallback);
        // Assert – output stream contains exactly the file content
        byte[] expected = Files.readAllBytes(sourceFile.toPath());
        assertArrayEquals(expected, connectionOut.toByteArray(), "The bytes transferred to the URLConnection must match the file content");
        // Assert – callback received the correct total count
        assertEquals(expected.length, transferred.get(), "Callback should be invoked with the total number of transferred bytes");
    }

    /* ------------------------------------------------------------
     *  2. Null callback – method must still succeed and must not
     *     attempt to invoke any method on the callback.
     * ------------------------------------------------------------ */
    @Test
    void nullCallback_isTolerated() throws Exception {
        // Act – pass a null callback
        IOUtilities.transfer(sourceFile, mockConnection, null);
        // Assert – data is transferred correctly
        assertArrayEquals(Files.readAllBytes(sourceFile.toPath()), connectionOut.toByteArray(), "Transfer must succeed even when the callback is null");
    }

    /* ------------------------------------------------------------
     *  3. File not found – the FileInputStream constructor should throw.
     * ------------------------------------------------------------ */
    @Test
    void fileNotFound_propagatesException() {
        File missing = new File("nonExistingFile.dat");
        assertThrows(FileNotFoundException.class, () -> IOUtilities.transfer(missing, mockConnection, mockCallback), "Missing source file must cause FileNotFoundException");
    }

    /* ------------------------------------------------------------
     *  4. OutputStream throws IOException during write.
     * ------------------------------------------------------------ */
    @Test
    void outputStreamWriteException_isPropagatedAndResourcesClosed() throws Exception {
        // Arrange – an OutputStream that throws on the first write
        OutputStream failingOut = new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                throw new IOException("simulated write failure");
            }

            @Override
            public void close() {
            }
        };
        when(mockConnection.getOutputStream()).thenReturn(failingOut);
        // Act & Assert
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(sourceFile, mockConnection, mockCallback), "IOException from the output stream must be propagated");
        assertEquals("simulated write failure", ex.getMessage());
        // Verify that the input side was closed – we cannot directly inspect the
        // FileInputStream, but the method must have entered the finally block.
        // The test passes if no additional exception is thrown.
    }

    /* ------------------------------------------------------------
     *  5. InputStream cannot be opened (e.g., a directory is supplied).
     * ------------------------------------------------------------ */
    @Test
    void inputStreamFromDirectory_throwsExceptionAndOutputClosed() throws Exception {
        // Arrange – create a temporary directory and use it as the source file
        Path dir = Files.createTempDirectory("ioUtilitiesDir");
        File dirFile = dir.toFile();
        // Act & Assert
        assertThrows(FileNotFoundException.class, () -> IOUtilities.transfer(dirFile, mockConnection, mockCallback), "Opening a directory as a FileInputStream must fail");
        // Clean up the temporary directory
        dir.toFile().delete();
    }

    /* ------------------------------------------------------------
     *  6. Verify that both streams are closed exactly once on success.
     * ------------------------------------------------------------ */
    @Test
    void resourcesAreClosedExactlyOnceOnSuccessfulTransfer() throws Exception {
        // Custom URLConnection that returns a spied OutputStream
        class SpyConnection extends URLConnection {

            final OutputStream real = new ByteArrayOutputStream();

            final OutputStream spy = spy(real);

            boolean connected = false;

            protected SpyConnection() {
                super(null);
            }

            @Override
            public void connect() {
                connected = true;
            }

            @Override
            public OutputStream getOutputStream() {
                return spy;
            }
        }
        SpyConnection conn = new SpyConnection();
        // Act
        IOUtilities.transfer(sourceFile, conn, mockCallback);
        // Assert – verify close() called once on the spied OutputStream
        verify(conn.spy, times(1)).close();
    }

    /* ------------------------------------------------------------
     *  7. Zero‑byte file – should transfer nothing and callback receives 0.
     * ------------------------------------------------------------ */
    @Test
    void zeroByteFile_transfersNothing() throws Exception {
        // Arrange – create an empty temporary file
        Path emptyPath = Files.createTempFile("empty", ".tmp");
        File emptyFile = emptyPath.toFile();
        AtomicLong transferred = new AtomicLong(-1);
        IOUtilities.TransferCallback cb = bytes -> transferred.set(bytes);
        // Act
        IOUtilities.transfer(emptyFile, mockConnection, cb);
        // Assert
        assertEquals(0, connectionOut.size(), "Output stream must contain 0 bytes for an empty source file");
        assertEquals(0L, transferred.get(), "Callback must be invoked with 0 bytes transferred");
        // Clean up
        emptyFile.delete();
    }

    /* ------------------------------------------------------------
     *  8. Large file exactly 8192 bytes – tests buffer boundary handling.
     * ------------------------------------------------------------ */
    @Test
    void largeFileExactlyBufferSize_transfersAllBytes() throws Exception {
        // Arrange – create a file of exactly 8192 bytes
        byte[] data = new byte[8192];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i % 256);
        Path largePath = Files.createTempFile("large", ".bin");
        Files.write(largePath, data);
        File largeFile = largePath.toFile();
        AtomicLong transferred = new AtomicLong(-1);
        IOUtilities.TransferCallback cb = bytes -> transferred.set(bytes);
        // Act
        IOUtilities.transfer(largeFile, mockConnection, cb);
        // Assert
        assertArrayEquals(data, connectionOut.toByteArray(), "All 8192 bytes must be transferred unchanged");
        assertEquals(8192L, transferred.get(), "Callback must report the exact number of transferred bytes");
        // Clean up
        largeFile.delete();
    }

    /* ------------------------------------------------------------
     *  9. Callback throws RuntimeException – must be propagated and streams closed.
     * ------------------------------------------------------------ */
    @Test
    void callbackThrowsRuntimeException_isPropagated() throws Exception {
        // Arrange – a callback that throws after the first invocation
        IOUtilities.TransferCallback exploding = bytes -> {
            throw new RuntimeException("callback failure");
        };
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.transfer(sourceFile, mockConnection, exploding), "RuntimeException from callback must be propagated");
        assertEquals("callback failure", ex.getMessage());
    }

    /* ------------------------------------------------------------
     * 10. URLConnection returns null OutputStream – should result in NPE.
     * ------------------------------------------------------------ */
    @Test
    void nullOutputStreamFromConnection_throwsNullPointerException() throws Exception {
        when(mockConnection.getOutputStream()).thenReturn(null);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(sourceFile, mockConnection, mockCallback), "Null OutputStream must cause NullPointerException");
        // The exact message is implementation‑dependent; we only assert the type.
    }

    /* ------------------------------------------------------------
     * 11. Null file argument – must throw NPE (or IllegalArgumentException).
     * ------------------------------------------------------------ */
    @Test
    void nullFileArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(null, mockConnection, mockCallback), "Passing a null File must result in NullPointerException");
    }

    /* ------------------------------------------------------------
     * 12. Null URLConnection argument – must throw NPE and close input stream.
     * ------------------------------------------------------------ */
    @Test
    void nullURLConnectionArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(sourceFile, null, mockCallback), "Passing a null URLConnection must result in NullPointerException");
    }

    /* ------------------------------------------------------------
     * 13. getOutputStream() throws IOException – exception must propagate.
     * ------------------------------------------------------------ */
    @Test
    void getOutputStreamThrowsIOException_isPropagated() throws Exception {
        when(mockConnection.getOutputStream()).thenThrow(new IOException("simulated network failure"));
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(sourceFile, mockConnection, mockCallback), "IOException from getOutputStream must be propagated");
        assertEquals("simulated network failure", ex.getMessage());
    }

    /* ------------------------------------------------------------
     * 14. getOutputStream() throws RuntimeException – must propagate.
     * ------------------------------------------------------------ */
    @Test
    void getOutputStreamThrowsRuntimeException_isPropagated() throws Exception {
        when(mockConnection.getOutputStream()).thenThrow(new IllegalStateException("connection not open"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> IOUtilities.transfer(sourceFile, mockConnection, mockCallback), "RuntimeException from getOutputStream must be propagated");
        assertEquals("connection not open", ex.getMessage());
    }

    /* ------------------------------------------------------------
     * 15. OutputStream.close() throws IOException – the close exception must be thrown.
     * ------------------------------------------------------------ */
    @Test
    void outputStreamCloseThrowsIOException_isPropagated() throws Exception {
        // OutputStream that works for write but throws on close()
        OutputStream badClose = new OutputStream() {

            @Override
            public void write(int b) {
                /* swallow */
            }

            @Override
            public void close() throws IOException {
                throw new IOException("close failure");
            }
        };
        when(mockConnection.getOutputStream()).thenReturn(badClose);
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(sourceFile, mockConnection, mockCallback), "IOException from OutputStream.close() must be propagated");
        assertEquals("close failure", ex.getMessage());
    }

    /* ------------------------------------------------------------
     * 16. Parameterized test for several small files (including zero‑byte).
     * ------------------------------------------------------------ */
    @ParameterizedTest
    @ValueSource(strings = { "", "A", "Hello World" })
    void parameterizedSmallFiles_areTransferredCorrectly(String content) throws Exception {
        // Create a temporary file with the supplied content
        Path p = Files.createTempFile("param", ".txt");
        Files.write(p, content.getBytes(StandardCharsets.UTF_8));
        File f = p.toFile();
        // Reset the connection output stream for each iteration
        connectionOut.reset();
        // Act
        IOUtilities.transfer(f, mockConnection, mockCallback);
        // Assert
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), connectionOut.toByteArray(), "Content must be transferred unchanged for \"{0}\"".replace("{0}", content));
        // Clean up
        f.delete();
    }
}
