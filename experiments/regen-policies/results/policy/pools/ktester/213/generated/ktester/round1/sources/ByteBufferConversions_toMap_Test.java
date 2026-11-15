package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static com.cedarsoftware.util.convert.MapConversions.VALUE;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toMap_Test {

    /**
     * Converter instance – not used by the method but required by the signature.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not participate in the logic of toMap, but we provide a real instance
        // to avoid null‑pointer surprises in future changes.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> arrayBackedProvider() {
        // Case 1: slice with bytes 0x01,0x02 → Base64 "AQI="
        ByteBuffer buf1 = ByteBuffer.wrap(new byte[] { 0x00, 0x01, 0x02, 0x03 });
        buf1.position(1);
        // remaining: 0x01,0x02
        buf1.limit(3);
        // Case 2: empty heap buffer → empty Base64 string
        ByteBuffer buf2 = ByteBuffer.wrap(new byte[] {});
        buf2.position(0);
        buf2.limit(0);
        return Stream.of(arguments(buf1, "AQI="), arguments(buf2, ""));
    }

    private static Stream<Arguments> directProvider() {
        // 1. Heap buffer (allocate) but treated as a generic slice – bytes 0x0C,0x0D → "DA0="
        ByteBuffer buf1 = ByteBuffer.allocate(4);
        buf1.put(new byte[] { 0x0A, 0x0B, 0x0C, 0x0D });
        // position = 0, limit = 4
        buf1.flip();
        // start at 0x0C
        buf1.position(2);
        // end after 0x0D
        buf1.limit(4);
        // 2. Direct buffer – bytes 0x7F,0x80,0xFF → "f4D/"
        ByteBuffer buf2 = ByteBuffer.allocateDirect(3);
        buf2.put(new byte[] { 0x7F, (byte) 0x80, (byte) 0xFF });
        // position = 0, limit = 3
        buf2.flip();
        // 3. Zero‑length slice on a heap buffer – should yield empty string
        ByteBuffer buf3 = ByteBuffer.allocate(2);
        buf3.position(2);
        // remaining == 0
        buf3.limit(2);
        // 4. Slice of a slice – underlying array exists but after slice()
        //    the buffer may no longer be array‑backed. Expected bytes 0x02,0x03 → "AgM="
        ByteBuffer base = ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 });
        base.position(0);
        base.limit(5);
        // shares the same backing array
        ByteBuffer buf4 = base.slice();
        buf4.position(1);
        // remaining: 0x02,0x03
        buf4.limit(3);
        return Stream.of(arguments(buf1, "DA0="), arguments(buf2, "f4D/"), arguments(buf3, ""), arguments(buf4, "AgM="));
    }

    @Test
    void testToMap_ArrayBackedBuffer() {
        // GIVEN – a heap‑backed ByteBuffer with a slice (position/limit) set
        // [h, e, l, l, o]
        byte[] source = "hello".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(source);
        // start at 'e'
        buffer.position(1);
        // end before 'o' → remaining bytes are "ell"
        buffer.limit(4);
        // WHEN
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) ByteBufferConversions.toMap(buffer, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        // "ell" in Base64 is "ZWxs"
        assertEquals("ZWxs", result.get(VALUE), "Encoded Base64 value is incorrect");
    }

    @Test
    void testToMap_NonArrayBackedBuffer() {
        // GIVEN – a direct (non‑array‑backed) ByteBuffer with a slice
        // [w, o, r, l, d]
        byte[] source = "world".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocateDirect(source.length);
        buffer.put(source);
        // position = 0, limit = length
        buffer.flip();
        // slice starts at 'o'
        buffer.position(1);
        // slice ends before 'd' → remaining bytes are "orl"
        buffer.limit(4);
        int originalPosition = buffer.position();
        // WHEN
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) ByteBufferConversions.toMap(buffer, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        // "orl" in Base64 is "b3Js"
        assertEquals("b3Js", result.get(VALUE), "Encoded Base64 value is incorrect");
        // Verify that the buffer's position was restored after the call
        assertEquals(originalPosition, buffer.position(), "ByteBuffer position must be restored to its original value");
    }

    @ParameterizedTest(name = "[{index}] array‑backed slice → expected Base64: {1}")
    @MethodSource("arrayBackedProvider")
    void testToMap_ArrayBacked(ByteBuffer buffer, String expectedBase64) {
        // Preserve the original position so we can verify that toMap does not modify it.
        int originalPos = buffer.position();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) ByteBufferConversions.toMap(buffer, null);
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals(expectedBase64, result.get(VALUE), "Encoded Base64 value is incorrect");
        assertEquals(originalPos, buffer.position(), "ByteBuffer position must remain unchanged for array‑backed buffers");
    }

    @ParameterizedTest(name = "[{index}] direct slice → expected Base64: {1}")
    @MethodSource("directProvider")
    void testToMap_Direct(ByteBuffer buffer, String expectedBase64) {
        // Store original position to verify restoration after the call.
        int originalPos = buffer.position();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) ByteBufferConversions.toMap(buffer, null);
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals(expectedBase64, result.get(VALUE), "Encoded Base64 value is incorrect");
        assertEquals(originalPos, buffer.position(), "ByteBuffer position must be restored after processing a non‑array‑backed buffer");
    }

    @Test
    void testToMap_ArrayBackedBuffer_Explicit() {
        // GIVEN – a heap‑backed ByteBuffer with a slice (position/limit) set
        // [h, e, l, l, o]
        byte[] source = "hello".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(source);
        // start at 'e'
        buffer.position(1);
        // remaining bytes are "ell"
        buffer.limit(4);
        // WHEN
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) ByteBufferConversions.toMap(buffer, converter);
        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        // "ell" in Base64 is "ZWxs"
        assertEquals("ZWxs", result.get(VALUE));
    }

    @Test
    void testToMap_NonArrayBackedBuffer_Explicit() {
        // GIVEN – a direct (non‑array‑backed) ByteBuffer with a slice
        byte[] source = "world".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocateDirect(source.length);
        buffer.put(source);
        // position = 0, limit = length
        buffer.flip();
        // slice starts at 'o'
        buffer.position(1);
        // remaining bytes are "orl"
        buffer.limit(4);
        int originalPosition = buffer.position();
        // WHEN
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) ByteBufferConversions.toMap(buffer, converter);
        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        // "orl" in Base64 is "b3Js"
        assertEquals("b3Js", result.get(VALUE));
        // Verify that the buffer's position was restored after the call
        assertEquals(originalPosition, buffer.position(), "ByteBuffer position must be restored to its original value");
    }
}
