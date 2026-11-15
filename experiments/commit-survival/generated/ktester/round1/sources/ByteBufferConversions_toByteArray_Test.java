package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.nio.BufferUnderflowException;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toByteArray_Test {

    /**
     * -----------------------------------------------------------------
     *  Per‑test initialisation – we create a dummy Converter instance.
     *  The concrete behaviour of Converter is irrelevant because the
     *  static method `toByteBuffer` is mocked.
     *  ----------------------------------------------------------------- **
     */
    private Converter dummyConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance; we can safely pass a
        // minimal (empty) options object because the mock will never invoke it.
        dummyConverter = new Converter(new ConverterOptions());
    }

    /**
     * -----------------------------------------------------------------
     *  Test 1 – when the delegated `toByteBuffer` returns null the method
     *  must return the constant EMPTY_BYTE_ARRAY.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void toByteArray_returnsEmptyArrayWhenBufferIsNull() {
        try (MockedStatic<ByteBufferConversions> mocked = mockStatic(ByteBufferConversions.class, CALLS_REAL_METHODS)) {
            // Stub only the helper method; let the real `toByteArray` execute.
            mocked.when(() -> ByteBufferConversions.toByteBuffer(any(), any())).thenReturn(null);
            byte[] result = ByteBufferConversions.toByteArray("any input", dummyConverter);
            assertArrayEquals(EMPTY_BYTE_ARRAY, result, "Expected EMPTY_BYTE_ARRAY when buffer is null");
        }
    }

    /**
     * -----------------------------------------------------------------
     *  Test 2 – when the buffer exists but has no remaining bytes the
     *  method must also return EMPTY_BYTE_ARRAY.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void toByteArray_returnsEmptyArrayWhenBufferHasNoRemaining() {
        // hasRemaining() == false
        ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
        try (MockedStatic<ByteBufferConversions> mocked = mockStatic(ByteBufferConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> ByteBufferConversions.toByteBuffer(any(), any())).thenReturn(emptyBuffer);
            byte[] result = ByteBufferConversions.toByteArray("ignored", dummyConverter);
            assertArrayEquals(EMPTY_BYTE_ARRAY, result, "Expected EMPTY_BYTE_ARRAY when buffer has no remaining bytes");
        }
    }

    /**
     * -----------------------------------------------------------------
     *  Test 3 – a normal, non‑empty buffer should be converted to a byte
     *  array containing the same data. The returned array must be a copy,
     *  not a view onto the original buffer.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void toByteArray_returnsCopyOfBufferContents() {
        byte[] source = new byte[] { 10, 20, 30, 40 };
        // position = 0, limit = source.length
        ByteBuffer buffer = ByteBuffer.wrap(source);
        try (MockedStatic<ByteBufferConversions> mocked = mockStatic(ByteBufferConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> ByteBufferConversions.toByteBuffer(any(), any())).thenReturn(buffer);
            byte[] result = ByteBufferConversions.toByteArray("irrelevant", dummyConverter);
            assertArrayEquals(source, result, "The byte array should contain the same bytes as the buffer");
            // Mutate the original buffer after the conversion and verify the result does not change.
            buffer.put(0, (byte) 99);
            assertNotEquals(99, result[0], "Resulting array must be a copy, not affected by later buffer changes");
        }
    }

    @Test
    void toByteArray_throwsNullPointerExceptionWhenConverterIsNull() {
        // The method forwards the converter to `toByteBuffer`; a null argument should cause NPE.
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toByteArray("anyObject", null), "Passing a null Converter must result in NullPointerException");
    }

    @Test
    void toByteArray_propagatesConversionExceptionFromToByteBuffer() {
        try (MockedStatic<ByteBufferConversions> mocked = mockStatic(ByteBufferConversions.class, CALLS_REAL_METHODS)) {
            // Simulate a conversion failure by throwing the library's ConversionException.
            mocked.when(() -> ByteBufferConversions.toByteBuffer(any(), any())).thenThrow(new ConversionException("cannot convert"));
            assertThrows(ConversionException.class, () -> ByteBufferConversions.toByteArray("unsupportedType", dummyConverter), "ConversionException from toByteBuffer must be propagated");
        }
    }

    @Test
    @Disabled("Disabled by default – enabling may exhaust JVM heap. " + "Uncomment to manually verify OutOfMemoryError handling.")
    void toByteArray_throwsOutOfMemoryErrorWhenRemainingIsHuge() {
        // Mock a ByteBuffer that reports a massive remaining size.
        ByteBuffer hugeBuffer = mock(ByteBuffer.class);
        when(hugeBuffer.hasRemaining()).thenReturn(true);
        when(hugeBuffer.remaining()).thenReturn(Integer.MAX_VALUE);
        // The call to `buffer.get(byte[])` will never be reached because the
        // array allocation itself throws OOM, but we stub it to be safe.
        doThrow(new OutOfMemoryError("simulated")).when(hugeBuffer).get(any());
        try (MockedStatic<ByteBufferConversions> mocked = mockStatic(ByteBufferConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> ByteBufferConversions.toByteBuffer(any(), any())).thenReturn(hugeBuffer);
            assertThrows(OutOfMemoryError.class, () -> ByteBufferConversions.toByteArray("largeDataObject", dummyConverter), "A huge remaining size should lead to OutOfMemoryError");
        }
    }

    @Test
    void toByteArray_propagatesBufferUnderflowExceptionFromGet() {
        // Create a mock buffer that claims to have data but fails on get().
        ByteBuffer badBuffer = mock(ByteBuffer.class);
        when(badBuffer.hasRemaining()).thenReturn(true);
        when(badBuffer.remaining()).thenReturn(10);
        doThrow(new BufferUnderflowException()).when(badBuffer).get(any());
        try (MockedStatic<ByteBufferConversions> mocked = mockStatic(ByteBufferConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> ByteBufferConversions.toByteBuffer(any(), any())).thenReturn(badBuffer);
            assertThrows(BufferUnderflowException.class, () -> ByteBufferConversions.toByteArray("malformedBufferObject", dummyConverter), "BufferUnderflowException from buffer.get should be propagated");
        }
    }
}
