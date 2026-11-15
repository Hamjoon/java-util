package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toByteBuffer_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us via the @Mock annotation.
        // Ensure the mock is clean before each test.
        reset(mockConverter);
    }

    /**
     * Provides a range of valid {@code byte[]} inputs together with a description.
     * The {@code Converter} argument is either {@code null} or a mock – the method
     * must ignore it.
     */
    private static Stream<Arguments> provideValidByteArrays() {
        return Stream.of(// Standard non‑empty array, null converter
        Arguments.of(new byte[] { 1, 2, 3 }, null, "Standard case – non‑empty array, null converter"), // Empty array, null converter
        Arguments.of(new byte[] {}, null, "Edge case – empty array, null converter"), // Large array (1024 zeros), null converter
        Arguments.of(new byte[1024], null, "Large array – 1024 zero‑filled bytes, null converter"), // Boundary values, null converter
        Arguments.of(new byte[] { (byte) -128, 0, 127 }, null, "Boundary values – min, zero, max signed byte, null converter"), // Single‑element array, non‑null converter (mock)
        Arguments.of(new byte[] { 42 }, mock(Converter.class), "Single element array with non‑null Converter (ignored)"));
    }

    @Test
    void toByteBuffer_wrapsByteArrayCorrectly() {
        // GIVEN
        byte[] source = new byte[] { 1, 2, 3, 4, 5 };
        // WHEN
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, mockConverter);
        // THEN
        assertNotNull(buffer, "Returned ByteBuffer should not be null");
        assertEquals(source.length, buffer.remaining(), "Buffer remaining should equal array length");
        // Verify content
        for (byte expected : source) {
            assertEquals(expected, buffer.get(), "ByteBuffer should contain the same bytes in order");
        }
        // Buffer position should now be at the limit after reading all bytes
        assertEquals(buffer.limit(), buffer.position(), "Position should be at limit after full read");
    }

    @Test
    void toByteBuffer_reflectsChangesInOriginalArray() {
        // GIVEN
        byte[] source = new byte[] { 10, 20, 30 };
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, mockConverter);
        // WHEN
        // mutate the original array
        source[1] = 99;
        // THEN
        // The buffer should see the updated value because wrap uses the original array.
        assertEquals(10, buffer.get(0));
        assertEquals(99, buffer.get(1));
        assertEquals(30, buffer.get(2));
    }

    @Test
    void toByteBuffer_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toByteBuffer(nullInput, mockConverter), "Passing null as the source should throw NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideValidByteArrays")
    void toByteBuffer_wrapsValidByteArray(byte[] source, Converter converter, String description) {
        // WHEN
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, converter);
        // THEN – basic sanity checks
        assertNotNull(buffer, "Returned ByteBuffer must not be null");
        assertEquals(source.length, buffer.remaining(), "Remaining bytes must equal the length of the source array");
        // Verify the buffer contains the same bytes in the same order.
        for (int i = 0; i < source.length; i++) {
            assertEquals(source[i], buffer.get(i), "Byte at position " + i + " should match the source array");
        }
        // The method must not interact with the Converter argument.
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }

    @Test
    void toByteBuffer_invalidType_throwsClassCastException() {
        // GIVEN
        Object notByteArray = "not a byte array";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ByteArrayConversions.toByteBuffer(notByteArray, null), "Passing a non‑byte[] object must result in ClassCastException");
    }

    @Test
    void toByteBuffer_sharedBackingArray_mutationsReflectedInSource() {
        // GIVEN
        byte[] source = new byte[] { 5, 5, 5 };
        // WHEN – obtain a buffer that wraps the same array
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, null);
        // mutate via the buffer
        buffer.put(0, (byte) 9);
        // THEN – the original array must reflect the mutation
        assertEquals(9, source[0], "Changes made through the ByteBuffer must be visible in the original array");
    }

    @Test
    void toByteBuffer_sharedBackingArray_sourceMutationReflectedInBuffer() {
        // GIVEN
        byte[] source = new byte[] { 10, 20, 30 };
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, null);
        // WHEN – mutate the source array after the buffer has been created
        source[1] = 99;
        // THEN – the buffer must see the updated value (wrap uses the original array)
        assertEquals(10, buffer.get(0));
        assertEquals(99, buffer.get(1));
        assertEquals(30, buffer.get(2));
    }

    @Test
    void toByteBuffer_positionAfterReadingAllBytes() {
        // GIVEN
        byte[] source = new byte[] { 1, 2, 3, 4, 5 };
        // WHEN
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, mockConverter);
        while (buffer.hasRemaining()) {
            // consume all bytes
            buffer.get();
        }
        // THEN
        assertEquals(buffer.limit(), buffer.position(), "After reading all bytes the buffer's position should equal its limit");
    }
}
