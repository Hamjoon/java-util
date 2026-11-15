package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.*;
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toByteBuffer_Test {

    @Mock
    private Converter // not used by the method, but required by signature
    converter;

    @Test
    void toByteBuffer_returnsReadOnlyBufferWithSameContentAndState() {
        // GIVEN a writable ByteBuffer with some data and a non‑default position/limit
        ByteBuffer original = ByteBuffer.allocate(10);
        original.put((byte) 1).put((byte) 2).put((byte) 3);
        // set position and limit to non‑default values
        original.position(2);
        original.limit(5);
        // WHEN the conversion method is invoked
        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);
        // THEN the returned buffer is read‑only
        assertTrue(result.isReadOnly(), "Resulting buffer should be read‑only");
        // AND it shares the same capacity and content
        assertEquals(original.capacity(), result.capacity(), "Capacity must be identical");
        // verify the bytes that are visible through the buffer are the same
        for (int i = 0; i < original.limit(); i++) {
            assertEquals(original.get(i), result.get(i), "Byte at index " + i + " should match");
        }
        // AND the original buffer's position and limit are unchanged
        assertEquals(2, original.position(), "Original position must remain unchanged");
        assertEquals(5, original.limit(), "Original limit must remain unchanged");
        // AND the read‑only buffer has the same position/limit as the original at the moment of conversion
        assertEquals(original.position(), result.position(), "Result position should match original");
        assertEquals(original.limit(), result.limit(), "Result limit should match original");
    }

    @Test
    void toByteBuffer_invalidSource_throwsClassCastException() {
        // GIVEN an object that is not a ByteBuffer
        String notABuffer = "not a buffer";
        // WHEN the method is invoked
        // THEN a ClassCastException is thrown
        assertThrows(ClassCastException.class, () -> ByteBufferConversions.toByteBuffer(notABuffer, converter), "Converting a non‑ByteBuffer should throw ClassCastException");
    }

    @Test
    void toByteBuffer_wrapHeapBuffer_returnsReadOnlyCopyAndIsIndependent() {
        // GIVEN a heap ByteBuffer wrapped around an array
        ByteBuffer original = ByteBuffer.wrap(new byte[] { 10, 20, 30 });
        // set non‑default state
        original.position(1);
        original.limit(3);
        // WHEN converting
        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);
        // THEN the result is read‑only and has the same capacity/position/limit
        assertTrue(result.isReadOnly(), "Result should be read‑only");
        assertEquals(original.capacity(), result.capacity(), "Capacity must match");
        assertEquals(original.position(), result.position(), "Position must match at conversion time");
        assertEquals(original.limit(), result.limit(), "Limit must match at conversion time");
        // AND the content is identical
        byte[] expected = new byte[] { 10, 20, 30 };
        byte[] actual = new byte[expected.length];
        result.get(actual);
        assertArrayEquals(expected, actual, "Byte content must be identical");
        // AND later changes to the original's position/limit do NOT affect the result
        original.position(0);
        original.limit(original.capacity());
        assertEquals(1, result.position(), "Result position should stay as captured");
        assertEquals(3, result.limit(), "Result limit should stay as captured");
    }

    @Test
    void toByteBuffer_directBuffer_returnsReadOnlyDirectBuffer() {
        // GIVEN a direct ByteBuffer containing a single int (4 bytes)
        ByteBuffer original = ByteBuffer.allocateDirect(4).putInt(0x01020304);
        // position=0, limit=4
        original.flip();
        // WHEN converting
        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);
        // THEN the result is read‑only and still direct
        assertTrue(result.isReadOnly(), "Result should be read‑only");
        assertTrue(result.isDirect(), "Result should remain a direct buffer");
        assertEquals(original.capacity(), result.capacity(), "Capacity must match");
        assertEquals(original.position(), result.position(), "Position must match at conversion time");
        assertEquals(original.limit(), result.limit(), "Limit must match at conversion time");
        // AND the 4 bytes are exactly the same
        byte[] expected = new byte[4];
        original.get(expected);
        // reset original for comparison (result is a view of same data)
        original.rewind();
        byte[] actual = new byte[4];
        result.get(actual);
        assertArrayEquals(expected, actual, "Byte content of direct buffer must be identical");
    }

    @Test
    void toByteBuffer_alreadyReadOnly_returnsReadOnlyAndWriteFails() {
        // GIVEN an already read‑only buffer
        ByteBuffer original = ByteBuffer.wrap(new byte[] { 5, 6, 7, 8 }).asReadOnlyBuffer();
        // WHEN converting
        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);
        // THEN the result is still read‑only
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        // AND attempting to write throws ReadOnlyBufferException
        assertThrows(ReadOnlyBufferException.class, () -> result.put((byte) 0), "Writing to a read‑only buffer must throw ReadOnlyBufferException");
        // AND the visible bytes are unchanged
        byte[] expected = new byte[] { 5, 6, 7, 8 };
        byte[] actual = new byte[expected.length];
        result.get(actual);
        assertArrayEquals(expected, actual, "Content of read‑only buffer must match source");
    }

    @Test
    void toByteBuffer_nullSource_throwsNullPointerException() {
        // GIVEN a null source
        Object source = null;
        // WHEN & THEN a NullPointerException is thrown (cast on null)
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toByteBuffer(source, converter), "Passing null should result in NullPointerException");
    }

    @Test
    void toByteBuffer_wrongTypeString_throwsClassCastException() {
        // GIVEN a String (not a ByteBuffer)
        Object source = "some string";
        // WHEN & THEN a ClassCastException is thrown
        assertThrows(ClassCastException.class, () -> ByteBufferConversions.toByteBuffer(source, converter), "Converting a non‑ByteBuffer should throw ClassCastException");
    }

    @Test
    void toByteBuffer_wrongTypeArray_throwsClassCastException() {
        // GIVEN an int array (not a ByteBuffer)
        Object source = new int[] { 1, 2, 3 };
        // WHEN & THEN a ClassCastException is thrown
        assertThrows(ClassCastException.class, () -> ByteBufferConversions.toByteBuffer(source, converter), "Converting a non‑ByteBuffer (array) should throw ClassCastException");
    }
}
