package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.VALUE;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ByteBufferConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical use cases for each conversion method.</li>
 *   <li>Edge cases such as empty buffers, read‑only buffers and direct (non‑array‑backed) buffers.</li>
 *   <li>Error scenarios – e.g. passing {@code null} as the source object.</li>
 *   <li>Side‑effect checks – especially that {@code toMap} does not modify the original buffer position.</li>
 * </ul>
 *
 * The test class uses only the public API of {@link Converter} and the classes that appear in the
 * source file of {@code ByteBufferConversions}.  No mocking frameworks are used; all objects are
 * created via their real constructors or factory methods.
 */
public class ByteBufferConversionsTest {

    private Converter converter;
    private Charset charset;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // ConverterOptions is not shown in the prompt, but it is part of the library.
        // It provides a Charset via getCharset().  The default constructor supplies a
        // reasonable default (UTF‑8) which is sufficient for the tests.
        ConverterOptions options = new ConverterOptions();   // assumes a public no‑arg ctor
        converter = new Converter(options);
        charset = options.getCharset();                     // the charset used by the converter
    }

    /** Helper to create a read‑only view of a buffer (the class under test does this internally). */
    private static ByteBuffer readOnly(ByteBuffer buffer) {
        return buffer.asReadOnlyBuffer();
    }

    @Test
    public void testToByteBuffer_ReturnsReadOnlySlice() {
        byte[] data = {1, 2, 3, 4, 5};
        ByteBuffer original = ByteBuffer.wrap(data);
        original.position(1).limit(4); // slice = {2,3,4}
        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);

        // Result must be a read‑only buffer with the same content slice
        assertTrue("Resulting buffer must be read‑only", result.isReadOnly());
        assertEquals("Remaining bytes must match slice length", 3, result.remaining());

        byte[] expected = {2, 3, 4};
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteArray_NonEmptyBuffer() {
        byte[] data = {10, 20, 30, 40};
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.position(1).limit(3); // slice = {20,30}
        byte[] result = ByteBufferConversions.toByteArray(buf, converter);
        assertArrayEquals(new byte[]{20, 30}, result);
    }

    @Test
    public void testToByteArray_EmptyBuffer() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        byte[] result = ByteBufferConversions.toByteArray(empty, converter);
        assertSame("Empty buffer should return the shared EMPTY_BYTE_ARRAY constant",
                com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY, result);
    }

    @Test
    public void testToCharBuffer_TypicalString() {
        String original = "Hello, 世界";
        byte[] bytes = original.getBytes(charset);
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        CharBuffer cb = ByteBufferConversions.toCharBuffer(buf, converter);
        assertEquals(original, cb.toString());
    }

    @Test
    public void testToString_TypicalString() {
        String original = "JUnit‑4 Test";
        ByteBuffer buf = ByteBuffer.wrap(original.getBytes(charset));
        String result = ByteBufferConversions.toString(buf, converter);
        assertEquals(original, result);
    }

    @Test
    public void testToCharArray_TypicalString() {
        String original = "CharArray";
        ByteBuffer buf = ByteBuffer.wrap(original.getBytes(charset));
        char[] chars = ByteBufferConversions.toCharArray(buf, converter);
        assertArrayEquals(original.toCharArray(), chars);
    }

    @Test
    public void testToStringBuffer_TypicalString() {
        String original = "StringBuffer";
        ByteBuffer buf = ByteBuffer.wrap(original.getBytes(charset));
        StringBuffer sb = ByteBufferConversions.toStringBuffer(buf, converter);
        assertEquals(original, sb.toString());
    }

    @Test
    public void testToStringBuilder_TypicalString() {
        String original = "StringBuilder";
        ByteBuffer buf = ByteBuffer.wrap(original.getBytes(charset));
        StringBuilder sb = ByteBufferConversions.toStringBuilder(buf, converter);
        assertEquals(original, sb.toString());
    }

    @Test
    public void testToMap_ArrayBackedBuffer() {
        // Buffer with backing array → the fast path should be exercised
        byte[] data = {1, 2, 3, 4, 5};
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.position(1).limit(4); // slice = {2,3,4}
        Map<?, ?> map = ByteBufferConversions.toMap(buf, converter);

        // Verify map content
        assertTrue("Map must contain the VALUE key", map.containsKey(VALUE));
        String encoded = (String) map.get(VALUE);
        String expected = Base64.getEncoder().encodeToString(new byte[]{2, 3, 4});
        assertEquals(expected, encoded);

        // Verify that the original buffer position is unchanged
        assertEquals("Position must be restored after toMap", 1, buf.position());
        assertEquals("Limit must be unchanged after toMap", 4, buf.limit());
    }

    @Test
    public void testToMap_DirectNonArrayBackedBuffer() {
        // Direct buffer does not have a backing array → the copy path is exercised
        ByteBuffer direct = ByteBuffer.allocateDirect(6);
        direct.put(new byte[]{9, 8, 7, 6, 5, 4});
        direct.flip(); // position=0, limit=6
        direct.position(2).limit(5); // slice = {7,6,5}
        Map<?, ?> map = ByteBufferConversions.toMap(direct, converter);

        assertTrue(map.containsKey(VALUE));
        String encoded = (String) map.get(VALUE);
        String expected = Base64.getEncoder().encodeToString(new byte[]{7, 6, 5});
        assertEquals(expected, encoded);

        // Ensure the original position is restored
        assertEquals(2, direct.position());
        assertEquals(5, direct.limit());
    }

    @Test
    public void testToMap_EmptyBuffer() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        Map<?, ?> map = ByteBufferConversions.toMap(empty, converter);
        assertTrue(map.containsKey(VALUE));
        String encoded = (String) map.get(VALUE);
        assertEquals("", encoded); // Base64 of empty byte array is empty string
    }

    @Test
    public void testNullInputThrowsNPE() {
        thrown.expect(NullPointerException.class);
        ByteBufferConversions.toByteBuffer(null, converter);
    }

    @Test
    public void testToCharBuffer_NullInputThrowsNPE() {
        thrown.expect(NullPointerException.class);
        ByteBufferConversions.toCharBuffer(null, converter);
    }

    @Test
    public void testToString_NullInputThrowsNPE() {
        thrown.expect(NullPointerException.class);
        ByteBufferConversions.toString(null, converter);
    }

    @Test
    public void testToCharArray_NullInputThrowsNPE() {
        thrown.expect(NullPointerException.class);
        ByteBufferConversions.toCharArray(null, converter);
    }

    @Test
    public void testToStringBuffer_NullInputThrowsNPE() {
        thrown.expect(NullPointerException.class);
        ByteBufferConversions.toStringBuffer(null, converter);
    }

    @Test
    public void testToStringBuilder_NullInputThrowsNPE() {
        thrown.expect(NullPointerException.class);
        ByteBufferConversions.toStringBuilder(null, converter);
    }

    @Test
    public void testToMap_NullInputThrowsNPE() {
        thrown.expect(NullPointerException.class);
        ByteBufferConversions.toMap(null, converter);
    }

    // -------------------------------------------------------------------------
    // Additional sanity test: verify that the read‑only view returned by
    // toByteBuffer does not affect the original buffer's mark/position.
    // -------------------------------------------------------------------------
    @Test
    public void testToByteBuffer_DoesNotModifyOriginalMarkOrPosition() {
        byte[] data = {0, 1, 2, 3, 4};
        ByteBuffer original = ByteBuffer.wrap(data);
        original.position(1);
        original.mark(); // mark at position 1
        original.position(3);
        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);

        // The original buffer's position and mark must stay where they were before the call
        assertEquals("Original position must be unchanged", 3, original.position());
        original.reset(); // should reset to the previously marked position (1)
        assertEquals("Original mark must be unchanged", 1, original.position());

        // The returned buffer should be a slice of the original (position = 3, limit = 5)
        assertEquals(2, result.remaining());
    }
}
