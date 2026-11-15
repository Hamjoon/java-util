package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;

@ExtendWith(MockitoExtension.class)
class IOUtilities_uncompressBytes_Test {

    /**
     * Sample data used by several tests.
     */
    private static byte[] sampleData;

    /**
     * Random generator for creating unpredictable byte patterns.
     */
    private static final Random RANDOM = new Random();

    @BeforeAll
    static void setupBeforeAll() {
        // Create a reproducible but non‑trivial byte array (size > 0, contains all possible values)
        sampleData = new byte[1024];
        RANDOM.nextBytes(sampleData);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources (not strictly necessary here).
        sampleData = null;
    }

    /**
     * Compresses the supplied UTF‑8 string using {@link Deflater}.
     *
     * @param text the text to compress
     * @return a byte array containing the compressed representation
     */
    private static byte[] compressString(String text) {
        byte[] input = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Deflater deflater = new Deflater();
        deflater.setInput(input);
        deflater.finish();
        byte[] buffer = new byte[1024];
        int compressedDataLength = deflater.deflate(buffer);
        deflater.end();
        byte[] result = new byte[compressedDataLength];
        System.arraycopy(buffer, 0, result, 0, compressedDataLength);
        return result;
    }

    /**
     * Data provider for the “basic delegation” group.
     */
    private static Stream<Arguments> basicDelegationProvider() {
        return Stream.of(// Empty input – should return empty array
        Arguments.of(new byte[0], new byte[0], "Empty input array should return an empty array without error."), // Valid compressed data for the string "test"
        Arguments.of(compressString("test"), "test".getBytes(java.nio.charset.StandardCharsets.UTF_8), "Valid compressed data should be correctly decompressed to original bytes."), // Null input – expected exception class
        Arguments.of(null, NullPointerException.class, "Passing null should cause a NullPointerException as the method dereferences the array."));
    }

    /**
     * Data provider for the “offset/length handling” group.
     */
    private static Stream<Arguments> offsetLengthProvider() {
        // Helper compressed payloads
        byte[] helloWorld = compressString("hello world");
        byte[] partial = compressString("partial");
        byte[] edge = compressString("edge");
        // zero‑length payload
        byte[] zero = new byte[0];
        return Stream.of(// Full array decompresses correctly (offset=0, len=bytes.length)
        Arguments.of(helloWorld, 0, helloWorld.length, "hello world".getBytes(java.nio.charset.StandardCharsets.UTF_8), "Full array decompresses correctly when offset=0 and len=bytes.length (delegated method)."), // Insufficient length – expect DataFormatException wrapped in RuntimeException
        Arguments.of(new byte[] { 0, 0, 0, 0 }, 0, 4, DataFormatException.class, "Array of insufficient length for a valid compressed stream should cause a DataFormatException from Inflater."), // Non‑zero offset but length covering only the compressed payload
        Arguments.of(// prepend 5 dummy bytes before the actual payload
        prependBytes(new byte[] { 1, 2, 3, 4, 5 }, partial), 5, partial.length, "partial".getBytes(java.nio.charset.StandardCharsets.UTF_8), "When offset is non‑zero and length covers only the compressed payload, method should correctly decompress."), // Offset 0 but length equals exact compressed length (no extra bytes)
        Arguments.of(edge, 0, edge.length, "edge".getBytes(java.nio.charset.StandardCharsets.UTF_8), "Offset set to 0 but length less than full array (exact compressed length) should decompress correctly."), // Length shorter than actual compressed data – expect DataFormatException
        Arguments.of(edge, 0, edge.length - 2, DataFormatException.class, "Length shorter than actual compressed data should trigger a DataFormatException."), // Negative offset – expect IllegalArgumentException
        Arguments.of(partial, -1, partial.length, IllegalArgumentException.class, "Negative offset or length should cause IllegalArgumentException before attempting decompression."), // Negative length – expect IllegalArgumentException
        Arguments.of(partial, 0, -5, IllegalArgumentException.class, "Negative offset or length should cause IllegalArgumentException before attempting decompression."), // Zero length – should return empty array without invoking Inflater
        Arguments.of(zero, 0, 0, new byte[0], "Length zero should return an empty array without invoking Inflater."));
    }

    /**
     * Helper that creates a new array consisting of {@code prefix} followed by {@code payload}.
     */
    private static byte[] prependBytes(byte[] prefix, byte[] payload) {
        byte[] result = new byte[prefix.length + payload.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(payload, 0, result, prefix.length, payload.length);
        return result;
    }

    /**
     * Verify that a byte array that has been compressed with {@link IOUtilities#compressBytes(byte[])}
     * can be restored to its original form using {@link IOUtilities#uncompressBytes(byte[])}.
     */
    @Test
    void uncompressBytes_returnsOriginalData_whenCompressed() {
        // Arrange – compress the sample data
        byte[] compressed = IOUtilities.compressBytes(sampleData);
        // Act – uncompress the data
        byte[] result = IOUtilities.uncompressBytes(compressed);
        // Assert – the round‑trip must yield the original bytes
        assertArrayEquals(sampleData, result, "The uncompressed bytes should be identical to the original input.");
    }

    /**
     * An empty (zero‑length) array should round‑trip without error and remain empty.
     */
    @Test
    void uncompressBytes_returnsEmptyArray_whenInputIsEmpty() {
        // Arrange
        byte[] empty = new byte[0];
        byte[] compressed = IOUtilities.compressBytes(empty);
        // Act
        byte[] result = IOUtilities.uncompressBytes(compressed);
        // Assert
        assertNotNull(result, "Result must not be null for an empty input.");
        assertEquals(0, result.length, "Result length should be zero for an empty input.");
    }

    /**
     * Passing {@code null} to the method should throw a {@link NullPointerException}.
     * This behaviour is consistent with the implementation that dereferences the argument.
     */
    @Test
    void uncompressBytes_throwsNullPointerException_whenInputIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> IOUtilities.uncompressBytes(null), "Calling uncompressBytes(null) should throw NullPointerException.");
    }

    /**
     * Corrupted/completely random data that is not a valid compressed stream should cause an
     * {@link IllegalArgumentException} (or a subclass) to be thrown. The exact exception type
     * depends on the internal implementation, but we assert that *some* runtime exception is raised.
     */
    @Test
    void uncompressBytes_throwsException_whenDataIsNotCompressed() {
        // Arrange – generate random bytes that are unlikely to be a valid compressed payload
        byte[] random = new byte[256];
        RANDOM.nextBytes(random);
        // Act & Assert
        assertThrows(RuntimeException.class, () -> IOUtilities.uncompressBytes(random), "Uncompressing non‑compressed data should result in a runtime exception.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicDelegationProvider")
    void uncompressBytes_basicDelegation(Object inputObj, Object expectedObj, String description) {
        // The method under test is the one‑argument overload.
        if (inputObj == null) {
            // Expect an exception
            assertThrows((Class<? extends Throwable>) expectedObj, () -> IOUtilities.uncompressBytes(null), description);
            return;
        }
        byte[] input = (byte[]) inputObj;
        byte[] expected = (byte[]) expectedObj;
        byte[] actual = IOUtilities.uncompressBytes(input);
        assertArrayEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("offsetLengthProvider")
    void uncompressBytes_offsetLengthHandling(byte[] input, int offset, int len, Object expectedOrException, String description) {
        if (expectedOrException instanceof Class) {
            // Expected an exception type
            Class<? extends Throwable> expectedException = (Class<? extends Throwable>) expectedOrException;
            assertThrows(expectedException, () -> IOUtilities.uncompressBytes(input, offset, len), description);
        } else {
            // Expected a successful decompression
            byte[] expected = (byte[]) expectedOrException;
            byte[] actual = IOUtilities.uncompressBytes(input, offset, len);
            assertArrayEquals(expected, actual, description);
        }
    }
}
