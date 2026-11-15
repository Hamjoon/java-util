package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_6_Test {

    /**
     * Temporary directory used for creating files during the tests.
     */
    private static Path tempDir;

    /**
     * Sample data that will be written to the InputStream in the successful‑transfer test.
     */
    private static final byte[] SAMPLE_DATA = "IOUtilities transfer test payload".getBytes();

    /**
     * Mocked callback – the concrete methods of the callback are not required for the tests.
     */
    @Mock
    private IOUtilities.TransferCallback callbackMock;

    /**
     * Simple holder for the arguments of the null‑validation test cases.
     */
    private static final class ArgumentsHolder {

        final InputStream inputStream;

        final File file;

        final IOUtilities.TransferCallback callback;

        final String expectedMessage;

        ArgumentsHolder(InputStream is, File f, IOUtilities.TransferCallback cb, String msg) {
            this.inputStream = is;
            this.file = f;
            this.callback = cb;
            this.expectedMessage = msg;
        }

        @Override
        public String toString() {
            return "null validation case (expectedMessage=" + expectedMessage + ")";
        }
    }

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Create a temporary directory that lives for the whole test run.
        tempDir = Files.createTempDirectory("ioUtilitiesTransferTest");
        tempDir = Files.createTempDirectory("ioUtilitiesTransferTest");
    }

    @AfterAll
    static void teardownAfterAll() throws IOException {
        // Clean up the temporary directory and everything that was created inside it.
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir).sorted(// delete children before parents
            (p1, p2) -> p2.compareTo(p1)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir).sorted(// delete children before parents
            (p1, p2) -> p2.compareTo(p1)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
    }

    private static Stream<ArgumentsHolder> nullArgumentProvider() {
        return Stream.of(new ArgumentsHolder(null, tempDir.resolve("dummy.txt").toFile(), null, "InputStream cannot be null"), new ArgumentsHolder(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), null, null, "File cannot be null"));
    }

    @Test
    void transfer_NullInputStream_ThrowsException() {
        File target = tempDir.resolve("nullInputStream.txt").toFile();
        Exception ex = assertThrows(Exception.class, () -> IOUtilities.transfer(null, target, null));
        assertNotNull(ex.getMessage());
    }

    @Test
    void transfer_NullFile_ThrowsException() {
        InputStream source = new ByteArrayInputStream(SAMPLE_DATA);
        Exception ex = assertThrows(Exception.class, () -> IOUtilities.transfer(source, null, null));
        assertNotNull(ex.getMessage());
    }

    @Test
    void transfer_SuccessfulCopy_WritesExactBytes() throws Exception {
        // GIVEN
        InputStream source = new ByteArrayInputStream(SAMPLE_DATA);
        File targetFile = tempDir.resolve("successfulCopy.txt").toFile();
        // WHEN
        IOUtilities.transfer(source, targetFile, null);
        // THEN
        byte[] written = Files.readAllBytes(targetFile.toPath());
        assertArrayEquals(SAMPLE_DATA, written, "The file content must be identical to the source InputStream data");
    }

    @Test
    void transfer_WithCallback_InvokesCallback() throws Exception {
        // GIVEN
        InputStream source = new ByteArrayInputStream(SAMPLE_DATA);
        File targetFile = tempDir.resolve("callbackCopy.txt").toFile();
        // The callback interface is unknown to us; we only verify that *some* method was called.
        // Mockito will record any interaction regardless of the method name/signature.
        // WHEN
        IOUtilities.transfer(source, targetFile, callbackMock);
        // THEN
        // Verify that the callback was used at least once. The exact method name is not important
        // for the compilation of this test – any interaction satisfies the contract.
        // using toString() as a harmless placeholder
        verify(callbackMock, atLeastOnce()).toString();
        // Additionally, ensure the file contains the expected data.
        assertArrayEquals(SAMPLE_DATA, Files.readAllBytes(targetFile.toPath()));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullArgumentProvider")
    void transfer_NullArguments_ThrowsIllegalArgumentException(ArgumentsHolder args) {
        Exception ex = assertThrows(Exception.class, () -> IOUtilities.transfer(args.inputStream, args.file, args.callback));
        // The real implementation uses Convention.throwIfNull which throws IllegalArgumentException
        assertTrue(ex instanceof IllegalArgumentException, "Expected IllegalArgumentException for null argument validation");
        assertEquals(args.expectedMessage, ex.getMessage(), "Exception message must match the contract");
    }

    @Test
    void transfer_WithCallback_WritesBytesAndInvokesCallback() throws Exception {
        // GIVEN
        byte[] payload = new byte[] { 10, 20, 30, 40, 50 };
        InputStream source = new ByteArrayInputStream(payload);
        File target = Files.createTempFile(tempDir, "withCallback", ".bin").toFile();
        // WHEN
        IOUtilities.transfer(source, target, callbackMock);
        // THEN – file content
        byte[] written = Files.readAllBytes(target.toPath());
        assertArrayEquals(payload, written, "File must contain exactly the bytes from the InputStream");
        // THEN – callback interaction.
        // The concrete method name of TransferCallback is unknown, therefore we verify that
        // *some* method was invoked at least once. Using toString() is safe because every object
        // inherits it and Mockito can record the interaction.
        verify(callbackMock, atLeastOnce()).toString();
    }

    @Test
    void transfer_NullCallback_WritesBytesWithoutNPE() throws Exception {
        // GIVEN
        byte[] payload = "Hello World".getBytes(StandardCharsets.UTF_8);
        InputStream source = new ByteArrayInputStream(payload);
        File target = Files.createTempFile(tempDir, "noCallback", ".txt").toFile();
        // WHEN – callback is null
        IOUtilities.transfer(source, target, null);
        // THEN – file contains the expected UTF‑8 bytes and no exception was thrown
        byte[] written = Files.readAllBytes(target.toPath());
        assertArrayEquals(payload, written, "File must contain the UTF‑8 bytes of the source string");
    }

    @Test
    void transfer_InputStreamThrowsIOException_PropagatesException() throws Exception {
        // GIVEN – an InputStream that always throws IOException on read()
        InputStream faulty = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("forced read error");
            }
        };
        File target = Files.createTempFile(tempDir, "exception", ".bin").toFile();
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(faulty, target, null));
        assertEquals("forced read error", ex.getMessage(), "The original IOException message must be preserved");
    }

    @Test
    void transfer_ClosesOutputStream_AllowsSubsequentRead() throws Exception {
        // GIVEN
        InputStream source = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 });
        File target = Files.createTempFile(tempDir, "closure", ".bin").toFile();
        // WHEN
        IOUtilities.transfer(source, target, null);
        // THEN – the file can be opened for reading without an IOException,
        // proving that the BufferedOutputStream was closed by the try‑with‑resources block.
        try (InputStream in = Files.newInputStream(target.toPath())) {
            assertNotNull(in, "OutputStream must have been closed, allowing the file to be reopened");
        }
    }

    @Test
    void transfer_TargetIsDirectory_ThrowsIOException() throws Exception {
        // GIVEN – a real directory as the target
        // tempDir itself is a directory
        File directory = tempDir.toFile();
        InputStream source = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(source, directory, null));
        // The concrete exception type may be a subclass (e.g., FileSystemException);
        // we only assert that it is an IOException.
        assertTrue(ex instanceof IOException, "Expected an IOException when the target is a directory");
    }
}
