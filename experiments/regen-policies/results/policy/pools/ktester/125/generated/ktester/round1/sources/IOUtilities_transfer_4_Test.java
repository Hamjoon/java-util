package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_4_Test {

    /**
     * Sample data used by several tests
     */
    private static final byte[] SAMPLE_DATA = "The quick brown fox jumps over the lazy dog".getBytes();

    /**
     * Provides test data for {@link #transfer_ExactSize_Succeeds(byte[], int, byte[])}.
     */
    private static Stream<Arguments> exactSizeProvider() {
        return Stream.of(// case from JSON: new byte[]{10,20,30,40} into new byte[4]
        Arguments.of(new byte[] { 10, 20, 30, 40 }, 4, new byte[] { 10, 20, 30, 40 }), // additional confidence case
        Arguments.of(new byte[] { 5, 6, 7, 8, 9 }, 5, new byte[] { 5, 6, 7, 8, 9 }));
    }

    /**
     * Provides test data for {@link #transfer_InsufficientBytes_ThrowsIOException(byte[], int)}.
     */
    private static Stream<Arguments> insufficientBytesProvider() {
        return Stream.of(// case from JSON: new byte[]{1,2} into new byte[5]
        Arguments.of(new byte[] { 1, 2 }, 5), // another variation
        Arguments.of(new byte[] { 9, 8, 7 }, 4));
    }

    /**
     * Verify that {@code IOUtilities.transfer(InputStream, byte[])} correctly fills the supplied
     * byte array when the stream contains at least as many bytes as the array length.
     */
    @Test
    void transfer_FullRead_Succeeds() throws IOException {
        // Arrange: an InputStream that contains more bytes than the target array
        InputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        // request only the first 10 bytes
        byte[] target = new byte[10];
        // Act
        IOUtilities.transfer(in, target);
        // Assert: the target array contains the first 10 bytes of SAMPLE_DATA
        byte[] expected = new byte[10];
        System.arraycopy(SAMPLE_DATA, 0, expected, 0, 10);
        assertArrayEquals(expected, target);
    }

    /**
     * Verify that an {@link IOException} is thrown when the stream does not contain enough
     * bytes to fill the supplied array.
     */
    @Test
    void transfer_PartialRead_ThrowsIOException() {
        // Arrange: an InputStream that is shorter than the requested array length
        InputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        // need 5 bytes but only 3 are available
        byte[] target = new byte[5];
        // Act & Assert
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(in, target));
        assertTrue(ex.getMessage().contains("Retry"));
    }

    /**
     * Edge case: transferring into a zero‑length array should succeed without reading
     * from the stream and without throwing an exception.
     */
    @Test
    void transfer_ZeroLengthArray_Succeeds() throws IOException {
        InputStream in = new ByteArrayInputStream(new byte[] { 9, 8, 7 });
        byte[] empty = new byte[0];
        // Should complete instantly – nothing to read, nothing to write
        IOUtilities.transfer(in, empty);
        // No exception means the test passes; also verify that the stream is left untouched
        assertEquals(3, in.available());
    }

    /**
     * Successful transfer when the stream supplies exactly the number of bytes requested.
     *
     * @param inBytes   the bytes that the {@link InputStream} will provide
     * @param size      size of the destination array (must match {@code inBytes.length})
     * @param expected  the expected content of the destination array after the call
     */
    @ParameterizedTest(name = "[{index}] exact size transfer – {0}")
    @MethodSource("exactSizeProvider")
    void transfer_ExactSize_Succeeds(byte[] inBytes, int size, byte[] expected) throws IOException {
        InputStream in = new ByteArrayInputStream(inBytes);
        byte[] target = new byte[size];
        IOUtilities.transfer(in, target);
        assertArrayEquals(expected, target);
    }

    /**
     * Verify that an {@link IOException} is thrown when the stream ends before the
     * destination array is completely filled.
     *
     * @param inBytes   bytes supplied by the {@link InputStream}
     * @param size      length of the destination array (greater than {@code inBytes.length})
     */
    @ParameterizedTest(name = "[{index}] insufficient bytes – expected IOException")
    @MethodSource("insufficientBytesProvider")
    void transfer_InsufficientBytes_ThrowsIOException(byte[] inBytes, int size) {
        InputStream in = new ByteArrayInputStream(inBytes);
        byte[] target = new byte[size];
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(in, target));
        assertTrue(ex.getMessage().contains("Retry"), "Exception message should contain the word 'Retry'");
    }
}
