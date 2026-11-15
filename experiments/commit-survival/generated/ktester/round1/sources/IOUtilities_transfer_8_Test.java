package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.net.URLConnection;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_8_Test {

    @Mock
    URLConnection mockConnection;

    @Mock
    InputStream mockInputStream;

    @Mock
    IOUtilities.TransferCallback mockCallback;

    File tempFile;

    /* ------------------------------------------------------------ *
     *  InputStream throws IOException during read                  *
     * ------------------------------------------------------------ */
    private static class FaultyInputStream extends InputStream {

        private final byte[] data;

        private int pos = 0;

        // number of bytes after which IOException is thrown
        private final int failAfter;

        FaultyInputStream(byte[] data, int failAfter) {
            this.data = data;
            this.failAfter = failAfter;
        }

        @Override
        public int read() throws IOException {
            if (pos >= data.length) {
                return -1;
            }
            if (pos == failAfter) {
                throw new IOException("simulated read failure");
            }
            return data[pos++] & 0xFF;
        }
    }

    /* ------------------------------------------------------------ *
     *  InputStream close throws IOException                         *
     * ------------------------------------------------------------ */
    private static class CloseExceptionInputStream extends ByteArrayInputStream {

        CloseExceptionInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            super.close();
            throw new IOException("close failed");
        }
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a temporary file that will be passed to the method under test.
        tempFile = File.createTempFile("ioUtilitiesTransferTest", ".tmp");
        tempFile.deleteOnExit();
    }

    @AfterEach
    void teardownAfterEach() {
        // Delete the temporary file if it still exists.
        if (tempFile != null && tempFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
    }

    /**
     * Helper answer that mimics the real {@code IOUtilities.transfer(InputStream, File, TransferCallback)}.
     * It copies all bytes from the supplied InputStream to the file and, if a non‑null callback is
     * provided, invokes a method named {@code onProgress(long)} with the total number of bytes transferred.
     */
    private static Answer<Void> realTransferAnswer() {
        return invocation -> {
            InputStream in = invocation.getArgument(0);
            File f = invocation.getArgument(1);
            IOUtilities.TransferCallback cb = invocation.getArgument(2);
            long total = 0;
            try (FileOutputStream fos = new FileOutputStream(f)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) {
                    fos.write(buf, 0, read);
                    total += read;
                }
                fos.flush();
            }
            // invoke callback if it defines onProgress(long)
            if (cb != null) {
                try {
                    Method m = cb.getClass().getMethod("onProgress", long.class);
                    m.invoke(cb, total);
                } catch (NoSuchMethodException ignored) {
                    // No onProgress method – nothing to do.
                }
            }
            return null;
        };
    }

    @Test
    void testTransferInvokesGetInputStreamTransferAndClose() throws Exception {
        // Arrange: mock static methods of IOUtilities.
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            // Stub the static helper methods used by the method under test.
            utilitiesMock.when(() -> IOUtilities.getInputStream(any(URLConnection.class))).thenReturn(mockInputStream);
            utilitiesMock.when(() -> IOUtilities.transfer(any(InputStream.class), any(File.class), any(IOUtilities.TransferCallback.class))).thenAnswer(// No‑op for the delegated transfer.
            invocation -> null);
            utilitiesMock.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(// No‑op for close.
            invocation -> null);
            // Act: invoke the method under test.
            IOUtilities.transfer(mockConnection, tempFile, mockCallback);
            // Assert: verify the expected static interactions.
            utilitiesMock.verify(() -> IOUtilities.getInputStream(eq(mockConnection)), times(1));
            utilitiesMock.verify(() -> IOUtilities.transfer(eq(mockInputStream), eq(tempFile), eq(mockCallback)), times(1));
            utilitiesMock.verify(() -> IOUtilities.close(eq(mockInputStream)), times(1));
        }
    }

    /* ------------------------------------------------------------ *
     *  Normal operation                                            *
     * ------------------------------------------------------------ */
    @Test
    @DisplayName("Happy‑path: data copied, callback notified, stream closed")
    void testNormalOperation() throws Exception {
        byte[] data = "hello".getBytes();
        InputStream source = new ByteArrayInputStream(data);
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            utilitiesMock.when(() -> IOUtilities.getInputStream(any())).thenReturn(source);
            utilitiesMock.when(() -> IOUtilities.transfer(any(InputStream.class), any(File.class), any())).thenAnswer(realTransferAnswer());
            utilitiesMock.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(// swallow
            invocation -> null);
            IOUtilities.transfer(mockConnection, tempFile, mockCallback);
            // verify interactions
            utilitiesMock.verify(() -> IOUtilities.getInputStream(eq(mockConnection)), times(1));
            utilitiesMock.verify(() -> IOUtilities.transfer(eq(source), eq(tempFile), eq(mockCallback)), times(1));
            utilitiesMock.verify(() -> IOUtilities.close(eq(source)), times(1));
            // file content assertion
            assertArrayEquals(data, Files.readAllBytes(tempFile.toPath()));
            // callback progress verification (expects a single call with total bytes)
            verify(mockCallback, times(1)).onProgress(data.length);
        }
    }

    /* ------------------------------------------------------------ *
     *  getInputStream throws IOException                           *
     * ------------------------------------------------------------ */
    @Test
    @DisplayName("getInputStream throws IOException – exception propagates, no close")
    void testGetInputStreamThrows() throws Exception {
        IOException connEx = new IOException("conn error");
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            utilitiesMock.when(() -> IOUtilities.getInputStream(any())).thenThrow(connEx);
            utilitiesMock.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(invocation -> null);
            IOException thrown = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, tempFile, mockCallback));
            assertEquals("conn error", thrown.getMessage());
            utilitiesMock.verify(() -> IOUtilities.getInputStream(eq(mockConnection)), times(1));
            utilitiesMock.verify(() -> IOUtilities.transfer(any(), any(), any()), never());
            // close should be called with null (no stream created)
            utilitiesMock.verify(() -> IOUtilities.close(isNull()), times(1));
        }
    }

    /* ------------------------------------------------------------ *
     *  transfer(InputStream, File, TransferCallback) throws       *
     * ------------------------------------------------------------ */
    @Test
    @DisplayName("Delegated transfer throws – exception propagates, stream closed")
    void testDelegatedTransferThrows() throws Exception {
        byte[] data = "data".getBytes();
        InputStream source = new ByteArrayInputStream(data);
        RuntimeException inner = new RuntimeException("callback failure");
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            utilitiesMock.when(() -> IOUtilities.getInputStream(any())).thenReturn(source);
            utilitiesMock.when(() -> IOUtilities.transfer(any(InputStream.class), any(File.class), any())).thenThrow(inner);
            utilitiesMock.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(invocation -> null);
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> IOUtilities.transfer(mockConnection, tempFile, mockCallback));
            assertEquals("callback failure", thrown.getMessage());
            utilitiesMock.verify(() -> IOUtilities.getInputStream(eq(mockConnection)), times(1));
            utilitiesMock.verify(() -> IOUtilities.transfer(eq(source), eq(tempFile), eq(mockCallback)), times(1));
            utilitiesMock.verify(() -> IOUtilities.close(eq(source)), times(1));
            // file should stay empty because transfer never succeeded
            assertEquals(0, tempFile.length());
        }
    }

    /* ------------------------------------------------------------ *
     *  Null TransferCallback                                        *
     * ------------------------------------------------------------ */
    @Test
    @DisplayName("Null TransferCallback is tolerated")
    void testNullCallback() throws Exception {
        byte[] data = "abc".getBytes();
        InputStream source = new ByteArrayInputStream(data);
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            utilitiesMock.when(() -> IOUtilities.getInputStream(any())).thenReturn(source);
            utilitiesMock.when(() -> IOUtilities.transfer(any(InputStream.class), any(File.class), any())).thenAnswer(realTransferAnswer());
            utilitiesMock.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(invocation -> null);
            IOUtilities.transfer(mockConnection, tempFile, null);
            utilitiesMock.verify(() -> IOUtilities.transfer(eq(source), eq(tempFile), isNull()), times(1));
            assertArrayEquals(data, Files.readAllBytes(tempFile.toPath()));
        }
    }

    /* ------------------------------------------------------------ *
     *  Null URLConnection                                           *
     * ------------------------------------------------------------ */
    @Test
    @DisplayName("Null URLConnection causes NullPointerException")
    void testNullURLConnection() {
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            // No stubbing needed – the method will attempt getInputStream(null) and NPE.
            utilitiesMock.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(invocation -> null);
            assertThrows(NullPointerException.class, () -> IOUtilities.transfer(null, tempFile, mockCallback));
            // close should be called with null (since getInputStream never succeeded)
            utilitiesMock.verify(() -> IOUtilities.close(isNull()), times(1));
        }
    }

    /* ------------------------------------------------------------ *
     *  Null File parameter                                          *
     * ------------------------------------------------------------ */
    @Test
    @DisplayName("Null File argument results in NullPointerException, stream closed")
    void testNullFile() throws Exception {
        InputStream source = new ByteArrayInputStream("data".getBytes());
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            utilitiesMock.when(() -> IOUtilities.getInputStream(any())).thenReturn(source);
            utilitiesMock.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(invocation -> null);
            assertThrows(NullPointerException.class, () -> IOUtilities.transfer(mockConnection, null, mockCallback));
            utilitiesMock.verify(() -> IOUtilities.getInputStream(eq(mockConnection)), times(1));
            utilitiesMock.verify(() -> IOUtilities.close(eq(source)), times(1));
        }
    }

    /* ------------------------------------------------------------ *
     *  Destination is a directory                                   *
     * ------------------------------------------------------------ */
    @Test
    @DisplayName("Destination File is a directory – IOException propagated")
    void testFileIsDirectory() throws Exception {
        File dir = Files.createTempDirectory("ioDirTest").toFile();
        dir.deleteOnExit();
        InputStream source = new ByteArrayInputStream("dirtest".getBytes());
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            utilitiesMock.when(() -> IOUtilities.getInputStream(any())).thenReturn(source);
            // Let the real transfer implementation run – it will attempt to open a FileOutputStream on a directory.
            utilitiesMock.when(() -> IOUtilities.transfer(any(InputStream.class), any(File.class), any())).thenAnswer(realTransferAnswer());
            utilitiesMock.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(invocation -> null);
            IOException thrown = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, dir, mockCallback));
            assertTrue(thrown.getMessage().contains("is a directory") || thrown.getMessage().contains("Access is denied") || thrown.getMessage().contains("Is a directory"));
            utilitiesMock.verify(() -> IOUtilities.close(eq(source)), times(1));
        }
    }

    /* ------------------------------------------------------------ *
     *  Read‑only destination file                                   *
     * ------------------------------------------------------------ */
    @Test
    @DisplayName("Read‑only destination file – write fails with IOException")
    void testReadOnlyDestinationFile() throws Exception {
        // create a file and make it read‑only
        File readOnly = File.createTempFile("readonly", ".tmp");
        readOnly.setWritable(false);
        readOnly.deleteOnExit();
        InputStream source = new ByteArrayInputStream("readonly".getBytes());
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            utilitiesMock.when(() -> IOUtilities.getInputStream(any())).thenReturn(source);
            utilitiesMock.when(() -> IOUtilities.transfer(any(InputStream.class), any(File.class), any())).thenAnswer(realTransferAnswer());
            utilitiesMock.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(invocation -> null);
            IOException thrown = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, readOnly, mockCallback));
            // message may vary by platform – just ensure an IOException was thrown
            assertNotNull(thrown.getMessage());
            utilitiesMock.verify(() -> IOUtilities.close(eq(source)), times(1));
            // file should stay empty
            assertEquals(0, readOnly.length());
        } finally {
            // make writable again so the temp file can be deleted on Windows
            readOnly.setWritable(true);
        }
    }

    /* ------------------------------------------------------------ *
     *  Zero‑length InputStream                                      *
     * ------------------------------------------------------------ */
    @Test
    @DisplayName("Zero‑length InputStream – creates empty file, callback gets 0")
    void testZeroLengthInputStream() throws Exception {
        InputStream source = new ByteArrayInputStream(new byte[0]);
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            utilitiesMock.when(() -> IOUtilities.getInputStream(any())).thenReturn(source);
            utilitiesMock.when(() -> IOUtilities.transfer(any(InputStream.class), any(File.class), any())).thenAnswer(realTransferAnswer());
            utilitiesMock.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(invocation -> null);
            IOUtilities.transfer(mockConnection, tempFile, mockCallback);
            assertEquals(0, tempFile.length());
            verify(mockCallback, times(1)).onProgress(0L);
        }
    }

    @Test
    @DisplayName("InputStream throws IOException after a few bytes – partial write, exception propagates")
    void testInputStreamThrowsDuringRead() throws Exception {
        // 11 bytes
        byte[] full = "partialdata".getBytes();
        // fail after 3 bytes
        FaultyInputStream source = new FaultyInputStream(full, 3);
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            utilitiesMock.when(() -> IOUtilities.getInputStream(any())).thenReturn(source);
            utilitiesMock.when(() -> IOUtilities.transfer(any(InputStream.class), any(File.class), any())).thenAnswer(realTransferAnswer());
            utilitiesMock.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(invocation -> null);
            IOException thrown = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, tempFile, mockCallback));
            assertEquals("simulated read failure", thrown.getMessage());
            // file should contain the first 3 bytes
            byte[] expected = new byte[] { full[0], full[1], full[2] };
            assertArrayEquals(expected, Files.readAllBytes(tempFile.toPath()));
            // callback should have been invoked with 3 bytes
            verify(mockCallback, times(1)).onProgress(3L);
        }
    }

    /* ------------------------------------------------------------ *
     *  getInputStream returns null                                   *
     * ------------------------------------------------------------ */
    @Test
    @DisplayName("getInputStream returns null – NullPointerException when used")
    void testGetInputStreamReturnsNull() throws Exception {
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            utilitiesMock.when(() -> IOUtilities.getInputStream(any())).thenReturn(null);
            utilitiesMock.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(invocation -> null);
            NullPointerException thrown = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(mockConnection, tempFile, null));
            utilitiesMock.verify(() -> IOUtilities.close(isNull()), times(1));
        }
    }

    @Test
    @DisplayName("InputStream.close throws IOException – ignored by utility close")
    void testCloseThrowsIOException() throws Exception {
        byte[] data = "fulldata".getBytes();
        InputStream source = new CloseExceptionInputStream(data);
        // Let the real close method be used – it is expected to swallow the IOException.
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            utilitiesMock.when(() -> IOUtilities.getInputStream(any())).thenReturn(source);
            utilitiesMock.when(() -> IOUtilities.transfer(any(InputStream.class), any(File.class), any())).thenAnswer(realTransferAnswer());
            // No stubbing for close – we want the real implementation to run.
            IOUtilities.transfer(mockConnection, tempFile, null);
            assertArrayEquals(data, Files.readAllBytes(tempFile.toPath()));
            // verify that close was indeed invoked
            utilitiesMock.verify(() -> IOUtilities.close(eq(source)), times(1));
        }
    }

    /* ------------------------------------------------------------ *
     *  Large data transfer (5 MB)                                   *
     * ------------------------------------------------------------ */
    @Test
    @DisplayName("Large data transfer – 5 MB copied correctly, callback totals")
    void testLargeDataTransfer() throws Exception {
        // 5 MB
        int size = 5 * 1024 * 1024;
        byte[] payload = new byte[size];
        new Random().nextBytes(payload);
        InputStream source = new ByteArrayInputStream(payload);
        try (MockedStatic<IOUtilities> utilitiesMock = mockStatic(IOUtilities.class, CALLS_REAL_METHODS)) {
            utilitiesMock.when(() -> IOUtilities.getInputStream(any())).thenReturn(source);
            utilitiesMock.when(() -> IOUtilities.transfer(any(InputStream.class), any(File.class), any())).thenAnswer(realTransferAnswer());
            utilitiesMock.when(() -> IOUtilities.close(any(Closeable.class))).thenAnswer(invocation -> null);
            IOUtilities.transfer(mockConnection, tempFile, mockCallback);
            assertEquals(size, tempFile.length());
            assertArrayEquals(payload, Files.readAllBytes(tempFile.toPath()));
            verify(mockCallback, times(1)).onProgress((long) size);
        }
    }
}
