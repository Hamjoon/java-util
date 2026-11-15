package com.cedarsoftware.util.convert;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;
import static com.cedarsoftware.util.convert.MapConversions.VALUE;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite that exercises every public static method in
 * {@link CharBufferConversions}.  The tests use only the API that is
 * visible in the source file and in the supplied {@link Converter}
 * signature – no additional third‑party libraries or mocking frameworks
 * are required.
 */
public class CharBufferConversionsTest {

    /** A {@link Converter} with default options (UTF‑8 charset). */
    private Converter converter;

    /** Sample data used by many tests. */
    private static final String SAMPLE_TEXT = "Hello, 世界";

    /** A read‑only CharBuffer that wraps {@link #SAMPLE_TEXT}. */
    private CharBuffer sourceBuffer;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor (as implied by the API).
        // The default charset is UTF‑8, which is sufficient for our verification.
        ConverterOptions options = new ConverterOptions();   // uses default charset
        converter = new Converter(options);

        sourceBuffer = CharBuffer.wrap(SAMPLE_TEXT);
    }

    /* --------------------------------------------------------------------- *
     *  toCharBuffer
     * --------------------------------------------------------------------- */

    @Test
    public void testToCharBufferReturnsReadOnlyCopy() {
        CharBuffer result = CharBufferConversions.toCharBuffer(sourceBuffer, converter);

        // The result must contain the same characters.
        assertEquals(SAMPLE_TEXT, result.toString());

        // The returned buffer must be read‑only.
        assertTrue("Resulting CharBuffer should be read‑only", result.isReadOnly());

        // Changing the position of the original buffer must NOT affect the copy.
        sourceBuffer.position(5);
        assertEquals("Position of original buffer should not influence copy",
                SAMPLE_TEXT, result.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBufferWithNullThrowsNPE() {
        CharBufferConversions.toCharBuffer(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toString
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringProducesExactString() {
        String result = CharBufferConversions.toString(sourceBuffer, converter);
        assertEquals(SAMPLE_TEXT, result);
    }

    /* --------------------------------------------------------------------- *
     *  toCharArray
     * --------------------------------------------------------------------- */

    @Test
    public void testToCharArrayCopiesAllCharacters() {
        char[] chars = CharBufferConversions.toCharArray(sourceBuffer, converter);
        assertArrayEquals(SAMPLE_TEXT.toCharArray(), chars);
    }

    @Test
    public void testToCharArrayEmptyBufferReturnsConstant() {
        CharBuffer empty = CharBuffer.wrap("");
        char[] chars = CharBufferConversions.toCharArray(empty, converter);
        assertSame("Empty CharBuffer should return the shared EMPTY_CHAR_ARRAY constant",
                EMPTY_CHAR_ARRAY, chars);
    }

    /* --------------------------------------------------------------------- *
     *  toStringBuffer
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringBufferContainsSameContent() {
        StringBuffer sb = CharBufferConversions.toStringBuffer(sourceBuffer, converter);
        assertEquals(SAMPLE_TEXT, sb.toString());
    }

    /* --------------------------------------------------------------------- *
     *  toStringBuilder
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringBuilderContainsSameContent() {
        StringBuilder sb = CharBufferConversions.toStringBuilder(sourceBuffer, converter);
        assertEquals(SAMPLE_TEXT, sb.toString());
    }

    /* --------------------------------------------------------------------- *
     *  toByteBuffer
     * --------------------------------------------------------------------- */

    @Test
    public void testToByteBufferIsNotNullAndEncodesContent() {
        ByteBuffer byteBuf = CharBufferConversions.toByteBuffer(sourceBuffer, converter);
        assertNotNull("ByteBuffer must not be null", byteBuf);
        assertTrue("ByteBuffer should have remaining bytes", byteBuf.remaining() > 0);

        // Decode the bytes using the same charset from the ConverterOptions
        String decoded = converter.getOptions().getCharset().decode(byteBuf).toString();
        assertEquals("Decoded bytes must equal original string", SAMPLE_TEXT, decoded);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBufferWithNullThrowsNPE() {
        CharBufferConversions.toByteBuffer(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toByteArray
     * --------------------------------------------------------------------- */

    @Test
    public void testToByteArrayMatchesEncodedBytes() {
        byte[] bytes = CharBufferConversions.toByteArray(sourceBuffer, converter);
        assertNotNull("Byte array must not be null", bytes);
        assertTrue("Byte array should not be empty", bytes.length > 0);

        // Verify against the charset‑encoded version of the original string.
        byte[] expected = SAMPLE_TEXT.getBytes(converter.getOptions().getCharset());
        assertArrayEquals("Byte array must match charset‑encoded string", expected, bytes);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArrayWithNullThrowsNPE() {
        CharBufferConversions.toByteArray(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toMap
     * --------------------------------------------------------------------- */

    @Test
    public void testToMapContainsSingleEntryWithValueKey() {
        Map<?, ?> map = CharBufferConversions.toMap(sourceBuffer, converter);
        assertNotNull("Resulting map must not be null", map);
        assertEquals("Map must contain exactly one entry", 1, map.size());
        assertTrue("Map must contain the VALUE key", map.containsKey(VALUE));
        assertEquals("Map value must be the original string", SAMPLE_TEXT, map.get(VALUE));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNullThrowsNPE() {
        CharBufferConversions.toMap(null, converter);
    }
}
