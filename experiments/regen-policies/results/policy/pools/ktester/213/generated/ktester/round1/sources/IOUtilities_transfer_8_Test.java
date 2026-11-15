package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_8_Test {

    /**
     * Temporary folder used for all tests.
     */
    private static Path tempDir;

    /**
     * A mock {@link URLConnection} that will be configured per‑test.
     */
    @Mock
    private URLConnection mockConnection;

    /**
     * Simple callback implementation that records the total number of bytes transferred.
     */
    private static class RecordingCallback implements IOUtilities.TransferCallback {

        long transferred = 0L;

        @Override
        public void transferred(long bytes) {
            transferred += bytes;
        }
    }

    /**
     * Dummy callback used only to satisfy the method signature when we do not care about the callback.
     */
    private static class DummyCallback implements IOUtilities.TransferCallback {

        @Override
        public void transferred(long bytes) {
            /* no‑op */
        }
    }

    /**
     * InputStream that always throws an {@link IOException} on any read operation.
     */
    private static class FailingInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException("forced read failure");
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            throw new IOException("forced read failure");
        }
    }

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Create a temporary directory that lives for the whole test run.
        tempDir = Files.createTempDirectory("IOUtilities_transfer_test");
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock before each test so previous stubbings do not leak.
        reset(mockConnection);
    }

    @AfterAll
    static void teardownAfterAll() throws IOException {
        // Recursively delete the temporary directory.
        Files.walk(tempDir).sorted(// delete children before parent
        (a, b) -> b.compareTo(a)).forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
            }
        });
    }

    private static Stream<Arguments> nullArgumentProvider() {
        return Stream.of(Arguments.of(null, new File("/tmp/out.dat"), new DummyCallback(), "URLConnection cannot be null"), Arguments.of(mock(URLConnection.class), null, new DummyCallback(), "File cannot be null"));
    }

    /**
     * Verify that a normal (uncompressed) stream is copied correctly to the target file.
     */
    @Test
    void testTransferSimpleStream() throws Exception {
        // GIVEN
        byte[] data = "Hello IOUtilities!".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        when(mockConnection.getInputStream()).thenReturn(in);
        // no compression
        when(mockConnection.getContentEncoding()).thenReturn(null);
        File target = Files.createTempFile(tempDir, "simple", ".txt").toFile();
        RecordingCallback callback = new RecordingCallback();
        // WHEN
        IOUtilities.transfer(mockConnection, target, callback);
        // THEN
        byte[] result = Files.readAllBytes(target.toPath());
        assertArrayEquals(data, result, "File content should match the source stream");
        assertEquals(data.length, callback.transferred, "Callback must report total bytes transferred");
    }

    /**
     * Verify that a gzip‑encoded stream is automatically decompressed.
     */
    @Test
    void testTransferGzipEncodedStream() throws Exception {
        // GIVEN
        byte[] original = "Compressed content for testing".getBytes("UTF-8");
        // gzip the data
        ByteArrayOutputStream gzOut = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(gzOut)) {
            gzip.write(original);
        }
        byte[] gzipped = gzOut.toByteArray();
        InputStream gzippedIn = new ByteArrayInputStream(gzipped);
        when(mockConnection.getInputStream()).thenReturn(gzippedIn);
        when(mockConnection.getContentEncoding()).thenReturn("gzip");
        File target = Files.createTempFile(tempDir, "gzip", ".txt").toFile();
        // WHEN
        IOUtilities.transfer(mockConnection, target, null);
        // THEN
        byte[] result = Files.readAllBytes(target.toPath());
        assertArrayEquals(original, result, "GZIP‑encoded stream should be decompressed into the file");
    }

    /**
     * Verify that a {@code null} {@link URLConnection} results in an exception.
     */
    @Test
    void testTransferNullConnectionThrows() throws Exception {
        File dummy = Files.createTempFile(tempDir, "dummy", ".txt").toFile();
        Exception ex = assertThrows(Exception.class, () -> IOUtilities.transfer(null, dummy, null));
        assertNotNull(ex.getMessage());
    }

    /**
     * Verify that a {@code null} destination {@link File} results in an exception.
     */
    @Test
    void testTransferNullFileThrows() throws Exception {
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Exception ex = assertThrows(Exception.class, () -> IOUtilities.transfer(mockConnection, null, null));
        assertNotNull(ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] c={0}, f={1}")
    @MethodSource("nullArgumentProvider")
    void testTransferNullArgumentsThrows(URLConnection c, File f, IOUtilities.TransferCallback cb, String expectedMessage) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> IOUtilities.transfer(c, f, cb));
        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void testTransferSimpleStreamWithCallback() throws Exception {
        // GIVEN
        byte[] data = "Hello IOUtilities!".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        when(mockConnection.getInputStream()).thenReturn(in);
        // no compression
        when(mockConnection.getContentEncoding()).thenReturn(null);
        File target = Files.createTempFile(tempDir, "normal", ".dat").toFile();
        RecordingCallback callback = new RecordingCallback();
        // WHEN
        IOUtilities.transfer(mockConnection, target, callback);
        // THEN
        byte[] result = Files.readAllBytes(target.toPath());
        assertArrayEquals(data, result, "File content should match the source stream");
        assertEquals(data.length, callback.transferred, "Callback must report total bytes transferred");
    }

    @Test
    void testTransferSimpleStreamWithNullCallback() throws Exception {
        // GIVEN
        byte[] data = { 9, 8, 7 };
        InputStream in = new ByteArrayInputStream(data);
        when(mockConnection.getInputStream()).thenReturn(in);
        when(mockConnection.getContentEncoding()).thenReturn(null);
        File target = Files.createTempFile(tempDir, "nullCallback", ".dat").toFile();
        // WHEN – callback is null, should not throw
        IOUtilities.transfer(mockConnection, target, null);
        // THEN
        byte[] result = Files.readAllBytes(target.toPath());
        assertArrayEquals(data, result, "File should contain the bytes even when callback is null");
    }

    @Test
    void testTransferInputStreamRetrievalFailure() throws Exception {
        // GIVEN
        when(mockConnection.getInputStream()).thenThrow(new IOException("forced getInputStream failure"));
        File target = Files.createTempFile(tempDir, "failedGetStream", ".dat").toFile();
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, target, new DummyCallback()));
        assertEquals("forced getInputStream failure", ex.getMessage());
        // Verify that the target file was not created or is empty
        assertTrue(Files.size(target.toPath()) == 0, "Target file should be empty when getInputStream fails");
    }

    @Test
    void testTransferInnerFailurePropagation() throws Exception {
        // GIVEN
        InputStream failingIn = new FailingInputStream();
        when(mockConnection.getInputStream()).thenReturn(failingIn);
        when(mockConnection.getContentEncoding()).thenReturn(null);
        File target = Files.createTempFile(tempDir, "transferFail", ".dat").toFile();
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, target, new DummyCallback()));
        assertEquals("forced read failure", ex.getMessage());
        // The file may be created but should be empty because the read never succeeded.
        assertEquals(0L, Files.size(target.toPath()), "File should be empty after a read failure");
    }
}
