package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toByteBuffer_Test {

    @Mock
    private Converter // dependency – not used by the method but required by signature
    mockConverter;

    // the buffer we will pass to the method
    private ByteBuffer original;

    @BeforeEach
    void setupBeforeEach() {
        // Create a writable ByteBuffer with known contents – used by a few tests
        original = ByteBuffer.allocate(8);
        original.put(new byte[] { 10, 20, 30, 40, 50, 60, 70, 80 });
        // prepare for reading
        original.flip();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        original = null;
    }

    // Example of how a parameterized test could be added later.
    static Stream<org.junit.jupiter.params.provider.Arguments> dummyProvider() {
        return Stream.of(arguments(ByteBuffer.wrap(new byte[] { 0, 1, 2, 3 }), null));
    }

    @Test
    void toByteBuffer_returnsReadOnlyBufferWithSameContent() {
        // GIVEN: a writable ByteBuffer (set up in @BeforeEach) and a mock Converter
        // WHEN
        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, mockConverter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer should not be null");
        assertNotSame(original, result, "Result should be a different instance (read‑only view)");
        assertTrue(result.isReadOnly(), "Resulting buffer must be read‑only");
        assertEquals(original.capacity(), result.capacity(), "Capacity should be unchanged");
        assertEquals(original.limit(), result.limit(), "Limit should be unchanged");
        assertEquals(original.position(), result.position(), "Position should be unchanged");
        // Verify that the underlying bytes are identical
        byte[] expected = new byte[original.remaining()];
        original.duplicate().get(expected);
        byte[] actual = new byte[result.remaining()];
        result.duplicate().get(actual);
        assertArrayEquals(expected, actual, "Byte content must be identical");
    }

    @Test
    void toByteBuffer_throwsClassCastExceptionWhenSourceIsNotByteBuffer() {
        // GIVEN
        Object notABuffer = "I am not a ByteBuffer";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ByteBufferConversions.toByteBuffer(notABuffer, mockConverter), "A non‑ByteBuffer source should cause a ClassCastException");
    }

    @Test
    void toByteBuffer_writableBufferReturnedReadOnlyAndUnchanged() {
        // GIVEN
        ByteBuffer src = ByteBuffer.wrap(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 });
        // WHEN
        ByteBuffer result = ByteBufferConversions.toByteBuffer(src, null);
        // THEN
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        assertEquals(src.capacity(), result.capacity());
        assertEquals(src.limit(), result.limit());
        assertEquals(src.position(), result.position());
        // Content check
        byte[] expected = new byte[src.remaining()];
        src.duplicate().get(expected);
        byte[] actual = new byte[result.remaining()];
        result.duplicate().get(actual);
        assertArrayEquals(expected, actual, "Content must match source");
        // Modifying the view must not affect the original
        result.position(result.position() + 2);
        result.limit(result.limit() - 2);
        assertEquals(0, src.position(), "Original position must stay unchanged");
        assertEquals(src.capacity(), src.limit(), "Original limit must stay unchanged");
    }

    @Test
    void toByteBuffer_alreadyReadOnlyBufferReturnsAnotherReadOnlyView() {
        // GIVEN
        ByteBuffer src = ByteBuffer.wrap(new byte[] { 10, 20, 30 }).asReadOnlyBuffer();
        // WHEN
        ByteBuffer result = ByteBufferConversions.toByteBuffer(src, null);
        // THEN
        assertTrue(result.isReadOnly(), "Returned buffer must be read‑only");
        assertEquals(src.capacity(), result.capacity());
        assertEquals(src.limit(), result.limit());
        assertEquals(src.position(), result.position());
        // Content check
        byte[] expected = new byte[src.remaining()];
        src.duplicate().get(expected);
        byte[] actual = new byte[result.remaining()];
        result.duplicate().get(actual);
        assertArrayEquals(expected, actual, "Content must be identical");
    }

    @Test
    void toByteBuffer_writableBufferWithNonNullConverter() {
        // GIVEN
        ByteBuffer src = ByteBuffer.allocate(4);
        src.put(new byte[] { 5, 6, 7, 8 });
        src.flip();
        Converter dummy = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                // Not used – just a placeholder
                return null;
            }
        };
        // WHEN
        ByteBuffer result = ByteBufferConversions.toByteBuffer(src, dummy);
        // THEN
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        assertEquals(src.capacity(), result.capacity());
        assertEquals(src.limit(), result.limit());
        assertEquals(src.position(), result.position());
        byte[] expected = new byte[src.remaining()];
        src.duplicate().get(expected);
        byte[] actual = new byte[result.remaining()];
        result.duplicate().get(actual);
        assertArrayEquals(expected, actual, "Content must match source");
    }

    @Test
    void toByteBuffer_invalidIntegerInputThrowsClassCastException() {
        // GIVEN
        Object notABuffer = Integer.valueOf(12345);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ByteBufferConversions.toByteBuffer(notABuffer, mockConverter));
    }

    @Test
    void toByteBuffer_nullSourceThrowsNullPointerException() {
        // GIVEN
        Object src = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toByteBuffer(src, null));
    }

    @Test
    void toByteBuffer_nullSourceWithNonNullConverterThrowsNullPointerException() {
        // GIVEN
        Object src = null;
        Converter dummy = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                return null;
            }
        };
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toByteBuffer(src, dummy));
    }

    @Test
    void toByteBuffer_nonNullBufferNullConverterSucceeds() {
        // GIVEN
        ByteBuffer src = ByteBuffer.allocate(2);
        // WHEN
        ByteBuffer result = ByteBufferConversions.toByteBuffer(src, null);
        // THEN
        assertNotNull(result);
        assertTrue(result.isReadOnly(), "Result must be read‑only even when converter is null");
    }

    @Test
    void toByteBuffer_partialViewPreservesPositionAndLimit() {
        // GIVEN
        byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        ByteBuffer src = ByteBuffer.wrap(data);
        src.position(2);
        // view over bytes 2,3,4,5
        src.limit(6);
        // WHEN
        ByteBuffer result = ByteBufferConversions.toByteBuffer(src, null);
        // THEN
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        assertEquals(2, result.position(), "Position must be preserved");
        assertEquals(6, result.limit(), "Limit must be preserved");
        // Content check – should contain 2,3,4,5
        byte[] expected = new byte[] { 2, 3, 4, 5 };
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(expected, actual, "Slice content must match expected view");
        // Original buffer must stay unchanged
        assertEquals(2, src.position(), "Original position must stay unchanged");
        assertEquals(6, src.limit(), "Original limit must stay unchanged");
    }

    @Test
    void toByteBuffer_directBufferReturnedReadOnlyDirect() {
        // GIVEN
        ByteBuffer src = ByteBuffer.allocateDirect(4);
        src.put(new byte[] { 9, 8, 7, 6 });
        src.flip();
        // WHEN
        ByteBuffer result = ByteBufferConversions.toByteBuffer(src, null);
        // THEN
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        assertTrue(result.isDirect(), "Result must be a direct buffer");
        assertEquals(src.capacity(), result.capacity());
        byte[] expected = new byte[src.remaining()];
        src.duplicate().get(expected);
        byte[] actual = new byte[result.remaining()];
        result.duplicate().get(actual);
        assertArrayEquals(expected, actual, "Content must be identical");
    }

    @Test
    void toByteBuffer_emptyBufferReturnsEmptyReadOnly() {
        // GIVEN
        ByteBuffer src = ByteBuffer.allocate(0);
        // WHEN
        ByteBuffer result = ByteBufferConversions.toByteBuffer(src, null);
        // THEN
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        assertEquals(0, result.capacity(), "Capacity should be zero");
        assertEquals(0, result.remaining(), "No bytes should be readable");
    }

    @Test
    void toByteBuffer_converterIsNeverUsed() {
        // GIVEN
        ByteBuffer src = ByteBuffer.wrap(new byte[] { 1, 2, 3 });
        Converter hostile = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new AssertionError("Converter should not be invoked");
            }
        };
        // WHEN / THEN – no AssertionError should be thrown
        ByteBuffer result = ByteBufferConversions.toByteBuffer(src, hostile);
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        byte[] expected = new byte[] { 1, 2, 3 };
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(expected, actual, "Content must be correct");
    }

    @Test
    void toByteBuffer_returnedBufferIsTrulyReadOnly() {
        // GIVEN
        ByteBuffer src = ByteBuffer.wrap(new byte[] { 5, 6, 7 });
        // WHEN
        ByteBuffer result = ByteBufferConversions.toByteBuffer(src, null);
        // THEN
        assertThrows(ReadOnlyBufferException.class, () -> result.put((byte) 0), "Attempting to put into a read‑only buffer must throw ReadOnlyBufferException");
    }
}
