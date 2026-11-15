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
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toByteBuffer_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    private static Stream<Arguments> provideByteArraysForWrapping() {
        return Stream.of(Arguments.of(new byte[] { 1, 2, 3 }), // empty array edge case
        Arguments.of(new byte[] {}));
    }

    @Test
    void toByteBuffer_givenByteArray_returnsWrappedByteBuffer() {
        // GIVEN
        byte[] source = new byte[] { 10, 20, 30, 40 };
        // WHEN
        ByteBuffer result = ByteArrayConversions.toByteBuffer(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer should not be null");
        assertEquals(source.length, result.remaining(), "ByteBuffer should contain all bytes from the source array");
        // Verify content
        for (byte b : source) {
            assertEquals(b, result.get(), "ByteBuffer content should match source array");
        }
    }

    @Test
    void toByteBuffer_givenNull_returnsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toByteBuffer(source, mockConverter), "Passing null should result in NullPointerException because ByteBuffer.wrap(null) is illegal");
    }

    @Test
    void toByteBuffer_givenNonByteArray_throwsClassCastException() {
        // GIVEN
        // not a byte[]
        Object source = new char[] { 'a', 'b', 'c' };
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ByteArrayConversions.toByteBuffer(source, mockConverter), "Passing a non‑byte[] should cause a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] source={0}")
    @MethodSource("provideByteArraysForWrapping")
    void toByteBuffer_givenByteArray_wrapsWithoutCopy(byte[] source) {
        // WHEN
        ByteBuffer result = ByteArrayConversions.toByteBuffer(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer should not be null");
        assertEquals(source.length, result.remaining(), "ByteBuffer should contain exactly the bytes from the source array");
        // Verify each byte matches the source
        for (byte expected : source) {
            assertEquals(expected, result.get(), "ByteBuffer content should match source array");
        }
    }

    @Test
    void toByteBuffer_givenNull_throwsNullPointerException() {
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toByteBuffer(null, mockConverter), "Passing null should result in NullPointerException because ByteBuffer.wrap(null) is illegal");
    }

    @Test
    void toByteBuffer_givenIntArray_throwsClassCastException() {
        // GIVEN
        Object source = new int[] { 1, 2, 3 };
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ByteArrayConversions.toByteBuffer(source, mockConverter), "An int[] is not a byte[] and should cause a ClassCastException");
    }

    @Test
    void toByteBuffer_givenString_throwsClassCastException() {
        // GIVEN
        Object source = "not a byte array";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ByteArrayConversions.toByteBuffer(source, mockConverter), "A String is not a byte[] and should cause a ClassCastException");
    }

    @Test
    void toByteBuffer_givenNonNullConverter_ignoresConverter() {
        // GIVEN
        byte[] source = new byte[] { 10, 20 };
        Converter dummyConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                // deliberately return something else to prove it's not used
                return "ignored";
            }
        };
        // WHEN
        ByteBuffer result = ByteArrayConversions.toByteBuffer(source, dummyConverter);
        // THEN
        assertNotNull(result);
        assertEquals(2, result.remaining());
        assertEquals(10, result.get(0));
        assertEquals(20, result.get(1));
    }

    @Test
    void toByteBuffer_givenLargeArray_wrapsCorrectly() {
        // GIVEN: 10 000 bytes containing values 0‑255 repeated
        final int size = 10_000;
        byte[] source = new byte[size];
        IntStream.range(0, size).forEach(i -> source[i] = (byte) (i % 256));
        // WHEN
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, mockConverter);
        // THEN
        assertEquals(size, buffer.capacity(), "Buffer capacity should match the source array length");
        assertEquals(size, buffer.limit(), "Buffer limit should match the source array length");
        assertEquals(0, buffer.position(), "Buffer position should start at zero");
        // Spot‑check a few positions to ensure data integrity
        assertEquals((byte) 0, buffer.get(0));
        assertEquals((byte) 255, buffer.get(255));
        assertEquals((byte) (size - 1) % 256, buffer.get(size - 1));
    }

    @Test
    void toByteBuffer_sharedBackingArray_modifyingSourceReflectsInBuffer() {
        // GIVEN
        byte[] source = new byte[] { 5, 6, 7 };
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, mockConverter);
        // WHEN: mutate the original array
        source[0] = 9;
        // THEN: buffer sees the change
        assertEquals(9, buffer.get(0), "Changes to the original array must be visible through the ByteBuffer");
    }

    @Test
    void toByteBuffer_sharedBackingArray_modifyingBufferReflectsInSource() {
        // GIVEN
        byte[] source = new byte[] { 8, 9, 10 };
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, mockConverter);
        // WHEN: write via the buffer
        buffer.put(0, (byte) 12);
        // THEN: source array is updated
        assertEquals(12, source[0], "Writes to the ByteBuffer must affect the original backing array");
    }

    @Test
    void toByteBuffer_returnedBuffer_isMutable() {
        // GIVEN
        byte[] source = new byte[] { 1 };
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, mockConverter);
        // THEN
        assertFalse(buffer.isReadOnly(), "ByteBuffer.wrap(byte[]) returns a mutable buffer");
    }
}
