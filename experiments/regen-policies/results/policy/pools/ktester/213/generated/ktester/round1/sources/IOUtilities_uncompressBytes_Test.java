package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

class IOUtilities_uncompressBytes_Test {

    private static final byte[] SAMPLE_DATA = "The quick brown fox jumps over the lazy dog".getBytes();

    private static Stream<Arguments> corruptDataProvider() {
        // case 1: completely random bytes
        byte[] random = new byte[] { 1, 2, 3, 4, 5 };
        // case 2: slice of a valid gzip stream that cuts it in half (offset 2, len 2)
        byte[] valid = IOUtilities.compressBytes("Data".getBytes(StandardCharsets.UTF_8));
        byte[] slice = new byte[2];
        System.arraycopy(valid, 2, slice, 0, 2);
        return Stream.of(Arguments.of(random, null, null), // the slice is taken from the original array, not the slice itself
        Arguments.of(valid, 2, 2));
    }

    private static Stream<Arguments> validCompressionProvider() {
        String hello = "Hello World";
        String empty = "";
        String large = "a".repeat(10_000);
        return Stream.of(Arguments.of(hello, IOUtilities.compressBytes(hello.getBytes(StandardCharsets.UTF_8))), Arguments.of(empty, IOUtilities.compressBytes(empty.getBytes(StandardCharsets.UTF_8))), Arguments.of(large, IOUtilities.compressBytes(large.getBytes(StandardCharsets.UTF_8))));
    }

    private static Stream<Arguments> boundaryConditionProvider() {
        byte[] empty = new byte[0];
        byte[] compressedBoundary = IOUtilities.compressBytes("boundary".getBytes(StandardCharsets.UTF_8));
        byte[] compressedSingle = IOUtilities.compressBytes("single".getBytes(StandardCharsets.UTF_8));
        return Stream.of(// empty input array -> empty output
        Arguments.of(empty, null, null, empty), // length 0 slice -> empty output
        Arguments.of(compressedBoundary, 0, 0, empty), // length 1 slice -> illegal argument
        Arguments.of(compressedSingle, 0, 1, IllegalArgumentException.class));
    }

    private static Stream<Arguments> invalidParametersProvider() {
        byte[] compressedTest = IOUtilities.compressBytes("test".getBytes(StandardCharsets.UTF_8));
        return Stream.of(// null array
        Arguments.of(null, null, null, NullPointerException.class), // negative offset
        Arguments.of(compressedTest, -1, 5, IllegalArgumentException.class), // offset+len exceeds array length
        Arguments.of(compressedTest, 2, 100, IllegalArgumentException.class), // negative length
        Arguments.of(compressedTest, 5, -3, IllegalArgumentException.class));
    }

    @Test
    void uncompressBytes_returnsOriginalData() {
        // given: a compressed representation of SAMPLE_DATA
        byte[] compressed = IOUtilities.compressBytes(SAMPLE_DATA);
        // when: the bytes are uncompressed
        byte[] result = IOUtilities.uncompressBytes(compressed);
        // then: the result matches the original data
        assertArrayEquals(SAMPLE_DATA, result);
    }

    @Test
    void uncompressBytes_withOffsetAndLength_returnsOriginalData() {
        // given: a compressed payload
        byte[] compressed = IOUtilities.compressBytes(SAMPLE_DATA);
        // embed the compressed payload inside a larger array
        byte[] wrapper = new byte[compressed.length + 10];
        // start of the real payload inside wrapper
        int offset = 5;
        System.arraycopy(compressed, 0, wrapper, offset, compressed.length);
        // when: uncompressBytes is called with the wrapper, offset and length of the payload
        byte[] result = IOUtilities.uncompressBytes(wrapper, offset, compressed.length);
        // then: the uncompressed data equals the original sample
        assertArrayEquals(SAMPLE_DATA, result);
    }

    @Test
    void uncompressBytes_emptyArray_returnsEmptyArray() {
        // given: an empty byte array
        byte[] empty = new byte[0];
        // when: uncompressBytes is invoked
        byte[] result = IOUtilities.uncompressBytes(empty);
        // then: the result is also an empty array
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void uncompressBytes_invalidData_throwsException() {
        // given: a byte array that is not a valid compressed stream
        byte[] bogus = "not a compressed payload".getBytes(StandardCharsets.UTF_8);
        // when / then: the method should throw a runtime exception (inflater throws DataFormatException)
        assertThrows(RuntimeException.class, () -> IOUtilities.uncompressBytes(bogus));
    }

    @ParameterizedTest(name = "[{index}] Corrupt data throws IllegalArgumentException")
    @MethodSource("corruptDataProvider")
    void uncompressBytes_corruptData_throwsIllegalArgumentException(byte[] input, Integer offset, Integer len) {
        Executable call;
        if (offset == null) {
            call = () -> IOUtilities.uncompressBytes(input);
        } else {
            call = () -> IOUtilities.uncompressBytes(input, offset, len);
        }
        assertThrows(IllegalArgumentException.class, call);
    }

    @ParameterizedTest(name = "[{index}] Valid compressed payload decompresses correctly")
    @MethodSource("validCompressionProvider")
    void uncompressBytes_validCompression_returnsOriginal(String originalString, byte[] compressed) {
        byte[] result = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(originalString.getBytes(StandardCharsets.UTF_8), result);
    }

    @ParameterizedTest(name = "[{index}] Boundary condition")
    @MethodSource("boundaryConditionProvider")
    void uncompressBytes_boundaryConditions(byte[] input, Integer offset, Integer len, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // expected exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            Executable call = () -> {
                if (offset == null) {
                    IOUtilities.uncompressBytes(input);
                } else {
                    IOUtilities.uncompressBytes(input, offset, len);
                }
            };
            assertThrows(exClass, call);
        } else {
            // expected byte[]
            byte[] result;
            if (offset == null) {
                result = IOUtilities.uncompressBytes(input);
            } else {
                result = IOUtilities.uncompressBytes(input, offset, len);
            }
            assertArrayEquals((byte[]) expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] Invalid parameters should throw")
    @MethodSource("invalidParametersProvider")
    void uncompressBytes_invalidParameters(byte[] input, Integer offset, Integer len, Class<? extends Throwable> expectedException) {
        Executable call = () -> {
            if (offset == null) {
                IOUtilities.uncompressBytes(input);
            } else {
                IOUtilities.uncompressBytes(input, offset, len);
            }
        };
        assertThrows(expectedException, call);
    }

    @Test
    void uncompressBytes_doesNotModifyInputArray_andReturnsNewInstance() {
        // given: a compressed representation of a known string
        byte[] original = IOUtilities.compressBytes("immutable".getBytes(StandardCharsets.UTF_8));
        // keep a copy to verify immutability
        byte[] originalClone = original.clone();
        // when: we decompress using the overload (any overload works – using the simple one)
        byte[] result = IOUtilities.uncompressBytes(original.clone());
        // then: the returned array is a new instance containing the original data
        assertNotSame(original, result, "Returned array should be a new instance");
        assertArrayEquals("immutable".getBytes(StandardCharsets.UTF_8), result);
        // and: the input array has not been altered by the method
        assertArrayEquals(originalClone, original, "Input array must remain unchanged");
    }

    @Test
    void uncompressBytes_threeArg_overload_nullArray_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> IOUtilities.uncompressBytes(null, 0, 0));
    }
}
