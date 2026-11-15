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
    private Converter // Converter is not used by the method but required for the signature
    mockConverter;

    /**
     * Supplies a variety of {@link ByteBuffer} instances covering the
     * successful conversion scenarios described in the JSON test cases.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideSuccessfulBuffers() {
        // 1. Writable buffer with explicit put+flip
        ByteBuffer buf1 = ByteBuffer.allocate(10);
        buf1.put(new byte[] { 1, 2, 3, 4, 5 });
        // position=0, limit=5
        buf1.flip();
        // 2. Buffer created via wrap()
        ByteBuffer buf2 = ByteBuffer.wrap(new byte[] { 9, 8, 7 });
        // 3. Direct buffer containing an int (0x01020304)
        ByteBuffer buf3 = ByteBuffer.allocateDirect(4);
        buf3.putInt(0x01020304);
        buf3.flip();
        // 4. Already read‑only buffer
        ByteBuffer buf4 = ByteBuffer.allocate(5).put(new byte[] { 1, 2, 3, 4, 5 }).asReadOnlyBuffer().flip();
        // 5. Empty buffer (capacity 0)
        ByteBuffer buf5 = ByteBuffer.allocate(0);
        // 6. Buffer with a mark set before conversion
        ByteBuffer buf6 = ByteBuffer.allocate(8).put(new byte[] { 10, 20, 30, 40, 50, 60, 70, 80 }).flip();
        // mark at position 0
        buf6.mark();
        // 7. Slice of a larger buffer (position 2, limit 8, then slice)
        ByteBuffer larger = ByteBuffer.allocate(10);
        larger.put(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        larger.position(2).limit(8);
        // contains bytes 2‑7, position=0, limit=6
        ByteBuffer buf7 = larger.slice();
        return Stream.of(arguments("Writable buffer with put+flip", buf1), arguments("Wrapped buffer via ByteBuffer.wrap()", buf2), arguments("Direct buffer with an int value", buf3), arguments("Already read‑only source buffer", buf4), arguments("Empty buffer (capacity 0)", buf5), arguments("Buffer with a mark set before conversion", buf6), arguments("Sliced buffer (position 2, limit 8, then slice)", buf7));
    }

    @Test
    void toByteBuffer_returnsReadOnlyBufferWithSameContent() {
        // GIVEN: a writable ByteBuffer containing known data
        byte[] data = new byte[] { 10, 20, 30, 40 };
        ByteBuffer writable = ByteBuffer.allocate(data.length);
        writable.put(data);
        // prepare for reading (position = 0, limit = data.length)
        writable.flip();
        // WHEN: the static conversion method is invoked
        ByteBuffer result = ByteBufferConversions.toByteBuffer(writable, mockConverter);
        // THEN: the returned buffer is a read‑only view
        assertTrue(result.isReadOnly(), "Resulting ByteBuffer should be read‑only");
        // The view should have the same remaining bytes as the original
        assertEquals(writable.remaining(), result.remaining(), "Read‑only buffer should have the same remaining bytes as the source");
        // The content must match
        for (int i = 0; i < data.length; i++) {
            assertEquals(data[i], result.get(i), "Byte at index " + i + " should match the source");
        }
        // Attempting to modify the read‑only buffer must throw ReadOnlyBufferException
        assertThrows(ReadOnlyBufferException.class, () -> result.put((byte) 99), "Modifying a read‑only buffer should throw ReadOnlyBufferException");
    }

    /* -------------------------------------------------------------
     *  Successful conversion cases – parameterised to avoid duplication
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideSuccessfulBuffers")
    void toByteBuffer_successfulConversions(String description, ByteBuffer source) {
        // Preserve original state for later comparison
        int originalPosition = source.position();
        int originalLimit = source.limit();
        // we will set a mark explicitly where needed
        int originalMark = source.markSupported() ? source.position() : -1;
        // WHEN
        ByteBuffer result = ByteBufferConversions.toByteBuffer(source, mockConverter);
        // THEN – the result must be a read‑only view
        assertTrue(result.isReadOnly(), "Resulting ByteBuffer should be read‑only for case: " + description);
        // The view must share the same remaining bytes
        assertEquals(source.remaining(), result.remaining(), "Remaining bytes must match for case: " + description);
        // Content equality (byte‑by‑byte)
        for (int i = 0; i < source.remaining(); i++) {
            assertEquals(source.get(source.position() + i), result.get(result.position() + i), "Byte at relative index " + i + " differs for case: " + description);
        }
        // Original buffer's position/limit must stay unchanged
        assertEquals(originalPosition, source.position(), "Original position must remain unchanged for case: " + description);
        assertEquals(originalLimit, source.limit(), "Original limit must remain unchanged for case: " + description);
        // The view must also preserve the mark (if any)
        if (source.isMarkSupported()) {
            // set a mark on the source before conversion in the provider where needed
            // after conversion, reset on the view should bring it back to the same position
            // ensure mark is set (no‑op if not set)
            source.reset();
            result.reset();
            assertEquals(source.position(), result.position(), "Mark position should be preserved for case: " + description);
        }
        // Verify that attempts to write through the read‑only view fail
        assertThrows(ReadOnlyBufferException.class, () -> result.put((byte) 0xFF), "Modifying a read‑only buffer must throw ReadOnlyBufferException for case: " + description);
    }

    /* -------------------------------------------------------------
     *  Exception handling cases
     * ------------------------------------------------------------- */
    @Test
    void toByteBuffer_nullFrom_throwsNullPointerException() {
        // GIVEN a null source
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toByteBuffer(source, mockConverter), "Passing null as 'from' must throw NullPointerException");
    }

    @Test
    void toByteBuffer_nonByteBuffer_throwsClassCastException() {
        // GIVEN an object that is not a ByteBuffer
        Object source = "Not a ByteBuffer";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ByteBufferConversions.toByteBuffer(source, mockConverter), "Passing a non‑ByteBuffer object must throw ClassCastException");
    }

    @Test
    void toByteBuffer_originalBufferUnchangedAfterConversion() {
        // GIVEN a writable buffer that is flipped before conversion
        ByteBuffer original = ByteBuffer.allocate(8);
        original.put(new byte[] { 10, 20, 30, 40, 50, 60, 70, 80 });
        // position=0, limit=8
        original.flip();
        int posBefore = original.position();
        int limBefore = original.limit();
        // WHEN
        ByteBuffer view = ByteBufferConversions.toByteBuffer(original, mockConverter);
        // THEN – original state must be untouched
        assertEquals(posBefore, original.position(), "Original position should stay unchanged");
        assertEquals(limBefore, original.limit(), "Original limit should stay unchanged");
        assertTrue(view.isReadOnly(), "Returned buffer must be read‑only");
    }

    @Test
    void toByteBuffer_modifyingOriginalReflectsInReadOnlyView() {
        // GIVEN a writable buffer
        ByteBuffer original = ByteBuffer.allocate(3);
        original.put(new byte[] { 5, 6, 7 });
        original.flip();
        // Create read‑only view
        ByteBuffer view = ByteBufferConversions.toByteBuffer(original, mockConverter);
        // WHEN: modify the original's underlying array via absolute put
        original.put(0, (byte) 99);
        // THEN: the view must see the change (shared backing array)
        assertEquals(99, view.get(0), "Read‑only view should reflect modifications to the original buffer");
    }

    @Test
    void toByteBuffer_markPreservedInReadOnlyView() {
        // GIVEN a buffer with a mark set at position 2
        ByteBuffer original = ByteBuffer.allocate(5);
        original.put(new byte[] { 10, 20, 30, 40, 50 });
        // position=0, limit=5
        original.flip();
        original.position(2);
        // mark at position 2
        original.mark();
        // WHEN
        ByteBuffer view = ByteBufferConversions.toByteBuffer(original, mockConverter);
        // THEN: reset on the view should bring it back to the same marked position
        // should reset to position 2
        view.reset();
        assertEquals(2, view.position(), "Read‑only view must preserve the mark set on the original buffer");
    }

    @Test
    void toByteBuffer_sliceBufferProperties() {
        // GIVEN a sliced buffer (as described in the JSON case)
        ByteBuffer source = ByteBuffer.allocate(10);
        source.put(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        source.position(2).limit(8);
        // contains bytes 2‑7, position=0, limit=6
        ByteBuffer slice = source.slice();
        // WHEN
        ByteBuffer result = ByteBufferConversions.toByteBuffer(slice, mockConverter);
        // THEN
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        assertEquals(0, result.position(), "Result position should be 0");
        assertEquals(6, result.limit(), "Result limit should be 6 (slice length)");
        byte[] expected = new byte[] { 2, 3, 4, 5, 6, 7 };
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result.get(i), "Byte at index " + i + " should match expected slice content");
        }
    }
}
