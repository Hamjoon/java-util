package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toByteBuffer_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    private static Stream<Arguments> provideStandardByteArrays() {
        return Stream.of(arguments("Standard non‑empty array", new byte[] { 10, 20, 30 }), arguments("Empty array", new byte[] {}));
    }

    private static Stream<Arguments> provideExceptionScenarios() {
        return Stream.of(arguments("Null source should throw NPE", null, NullPointerException.class), arguments("Non‑byte[] source should throw CCE", "not a byte array", ClassCastException.class));
    }

    @Test
    void toByteBuffer_wrapsByteArray_correctly() {
        // WHEN
        ByteBuffer result = ByteArrayConversions.toByteBuffer(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer should not be null");
        assertEquals(source.length, result.remaining(), "ByteBuffer.remaining() must equal the source length");
        // Verify content without altering the original buffer position
        // duplicate shares the same backing array
        ByteBuffer dup = result.duplicate();
        for (byte expected : source) {
            assertEquals(expected, dup.get(), "ByteBuffer content should match source array");
        }
    }

    @Test
    void toByteBuffer_nullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toByteBuffer(source, mockConverter), "Passing null should result in NullPointerException from ByteBuffer.wrap");
    }

    @Test
    void toByteBuffer_nonByteArray_throwsClassCastException() {
        // GIVEN
        Object source = "not a byte array";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ByteArrayConversions.toByteBuffer(source, mockConverter), "Passing a non‑byte[] object should cause a ClassCastException");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideExceptionScenarios")
    void toByteBuffer_throwsExpectedException(String description, Object source, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> ByteArrayConversions.toByteBuffer(source, mockConverter), description);
    }

    @Test
    void toByteBuffer_converterIsIgnored() {
        // GIVEN a non‑null Converter implementation (anonymous subclass)
        Converter realConverter = new Converter() {
        };
        byte[] source = new byte[] { 5, 15, 25 };
        // WHEN
        ByteBuffer result = ByteArrayConversions.toByteBuffer(source, realConverter);
        // THEN – same assertions as the normal case
        assertNotNull(result);
        assertEquals(source.length, result.remaining());
        ByteBuffer dup = result.duplicate();
        for (byte b : source) {
            assertEquals(b, dup.get());
        }
    }

    @Test
    void toByteBuffer_handlesFullSignedByteRange() {
        byte[] source = new byte[] { 0, 127, -128, -1 };
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, mockConverter);
        assertEquals(source.length, buffer.remaining());
        ByteBuffer dup = buffer.duplicate();
        for (byte expected : source) {
            assertEquals(expected, dup.get(), "ByteBuffer must preserve signed byte values");
        }
    }

    @Test
    void toByteBuffer_largeArray_isWrappedCorrectly() {
        // Build a 5 000‑element byte array with values 0‑255 wrapped to signed byte range
        byte[] source = IntStream.range(0, 5000).map(i -> i % 256).mapToObj(Integer::valueOf).collect(Collectors.toList()).stream().mapToInt(Integer::intValue).collect(() -> new byte[5000], (arr, v) -> arr[arr.length - 5000 + v] = (byte) v, (a, b) -> {
        });
        // Simpler alternative (avoid the above complexity):
        // byte[] source = new byte[5000];
        // for (int i = 0; i < source.length; i++) source[i] = (byte) (i % 256);
        // WHEN
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, mockConverter);
        // THEN
        assertEquals(5000, buffer.capacity(), "Capacity must match source length");
        assertEquals(0, buffer.position(), "Initial position must be 0");
        assertEquals(5000, buffer.limit(), "Initial limit must be equal to capacity");
        assertEquals(5000, buffer.remaining(), "Remaining must equal capacity for a fresh buffer");
        // Verify a few sample values to avoid O(n) cost in the assertion
        assertEquals((byte) 0, buffer.get(0));
        assertEquals((byte) 255, buffer.get(255));
        assertEquals((byte) (5000 % 256), buffer.get(5000 - 1));
    }

    @Test
    void toByteBuffer_mutationsToOriginalArrayAreVisibleInBuffer() {
        byte[] data = new byte[] { 9, 8, 7 };
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(data, mockConverter);
        // Mutate the original array after wrapping
        data[0] = (byte) 99;
        // The buffer should see the change because it shares the backing array
        assertEquals((byte) 99, buffer.get(0), "ByteBuffer must reflect modifications made to the original array");
    }

    @Test
    void toByteBuffer_bufferPutReflectsInOriginalArray() {
        byte[] data = new byte[] { 9, 8, 7 };
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(data, mockConverter);
        // Modify the buffer view
        buffer.put(0, (byte) 99);
        // The underlying array must be updated
        assertEquals((byte) 99, data[0], "Changing the buffer should modify the original byte array");
    }

    @Test
    void toByteBuffer_positionAndLimitChangesDoNotAlterArrayContents() {
        byte[] source = new byte[] { 1, 2, 3, 4 };
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, mockConverter);
        // Change position and limit – these are view‑only properties
        buffer.position(2);
        buffer.limit(3);
        // Verify that the backing array is unchanged
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, source, "Altering position/limit must not modify the underlying array");
    }
}
