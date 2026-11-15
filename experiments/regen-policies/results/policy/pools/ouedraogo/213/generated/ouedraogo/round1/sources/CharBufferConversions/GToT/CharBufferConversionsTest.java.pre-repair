package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.ArrayUtilities;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Map;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;
import static com.cedarsoftware.util.convert.MapConversions.VALUE;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CharBufferConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal (happy‑path) usage of every public static method.</li>
 *   <li>Edge‑case handling such as empty buffers and buffers with no remaining characters.</li>
 *   <li>Exception scenarios – passing {@code null} arguments or objects that are not {@link CharBuffer}.</li>
 * </ul>
 *
 * The test class uses only the public API that is visible in the supplied source
 * (no mocking, no subclassing of library types).
 */
public class CharBufferConversionsTest {

    private Converter converter;          // a real Converter instance with default options
    private Charset charset;               // the charset used by the converter (UTF‑8 by default)

    @Before
    public void setUp() {
        // The library supplies a default‑construction path for ConverterOptions.
        // The concrete class is not shown, but the public API guarantees a
        // no‑arg constructor is available for the default configuration.
        ConverterOptions options = new ConverterOptions(); // default options (UTF‑8 charset)
        this.converter = new Converter(options);
        this.charset = options.getCharset(); // expected to be UTF‑8
        assertNotNull("Converter options must provide a charset", charset);
    }

    /* ---------------------------------------------------------------------- */
    /*  1. toCharBuffer(Object, Converter)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCharBuffer_HappyPath() {
        CharBuffer original = CharBuffer.wrap("Hello World");
        CharBuffer result = CharBufferConversions.toCharBuffer(original, converter);

        // The method returns a read‑only view – verify it is read‑only
        assertTrue("Result should be read‑only", result.isReadOnly());

        // The content must be identical
        assertEquals("Content must match original", original.toString(), result.toString());

        // Changing the original's position must not affect the returned buffer
        original.position(5);
        assertEquals("Returned buffer must retain original position (0)", 0, result.position());
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBuffer_WrongType() {
        // Passing an object that is not a CharBuffer should cause a ClassCastException
        CharBufferConversions.toCharBuffer("Not a CharBuffer", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBuffer_NullFrom() {
        CharBufferConversions.toCharBuffer(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBuffer_NullConverter() {
        CharBuffer original = CharBuffer.wrap("test");
        CharBufferConversions.toCharBuffer(original, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  2. toByteArray(Object, Converter)                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByteArray_HappyPath() {
        CharBuffer source = CharBuffer.wrap("ABC");
        byte[] bytes = CharBufferConversions.toByteArray(source, converter);
        assertNotNull(bytes);
        assertArrayEquals("Bytes must be UTF‑8 representation of \"ABC\"",
                "ABC".getBytes(charset), bytes);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteArray_WrongType() {
        CharBufferConversions.toByteArray(12345, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArray_NullFrom() {
        CharBufferConversions.toByteArray(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  3. toByteBuffer(Object, Converter)                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByteBuffer_HappyPath() {
        CharBuffer source = CharBuffer.wrap("Δειγμα"); // contains non‑ASCII characters
        ByteBuffer buffer = CharBufferConversions.toByteBuffer(source, converter);
        assertNotNull(buffer);
        // Encode the same string with the charset directly and compare
        ByteBuffer expected = charset.encode(source);
        assertEquals("ByteBuffer position must be 0", 0, buffer.position());
        assertEquals("ByteBuffer limit must match encoded length",
                expected.limit(), buffer.limit());
        assertArrayEquals("Underlying byte arrays must be identical",
                expected.array(), buffer.array());
    }

    @Test(expected = ClassCastException.class)
    public void testToByteBuffer_WrongType() {
        CharBufferConversions.toByteBuffer(new Object(), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_NullFrom() {
        CharBufferConversions.toByteBuffer(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  4. toString(Object, Converter)                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToString_HappyPath() {
        CharBuffer source = CharBuffer.wrap("JUnit Test");
        String result = CharBufferConversions.toString(source, converter);
        assertEquals("Result string must equal source content", "JUnit Test", result);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongType() {
        CharBufferConversions.toString(Boolean.TRUE, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullFrom() {
        CharBufferConversions.toString(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  5. toCharArray(Object, Converter)                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCharArray_HappyPath() {
        CharBuffer source = CharBuffer.wrap("charArray");
        char[] chars = CharBufferConversions.toCharArray(source, converter);
        assertArrayEquals("Char array must contain the same characters",
                "charArray".toCharArray(), chars);
    }

    @Test
    public void testToCharArray_EmptyBuffer() {
        CharBuffer empty = CharBuffer.wrap("");
        char[] result = CharBufferConversions.toCharArray(empty, converter);
        // The implementation returns the shared EMPTY_CHAR_ARRAY constant
        assertSame("Empty buffer should return EMPTY_CHAR_ARRAY constant",
                EMPTY_CHAR_ARRAY, result);
        assertEquals("Length must be zero", 0, result.length);
    }

    @Test
    public void testToCharArray_BufferWithNoRemaining() {
        CharBuffer buffer = CharBuffer.wrap("abc");
        // Move position to limit – no characters remain
        buffer.position(buffer.limit());
        char[] result = CharBufferConversions.toCharArray(buffer, converter);
        assertSame("When no chars remain, EMPTY_CHAR_ARRAY must be returned",
                EMPTY_CHAR_ARRAY, result);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharArray_WrongType() {
        CharBufferConversions.toCharArray(42, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArray_NullFrom() {
        CharBufferConversions.toCharArray(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  6. toStringBuffer(Object, Converter)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringBuffer_HappyPath() {
        CharBuffer source = CharBuffer.wrap("StringBufferTest");
        StringBuffer sb = CharBufferConversions.toStringBuffer(source, converter);
        assertNotNull(sb);
        assertEquals("StringBuffer content must match source", "StringBufferTest", sb.toString());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuffer_WrongType() {
        CharBufferConversions.toStringBuffer(3.14, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuffer_NullFrom() {
        CharBufferConversions.toStringBuffer(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  7. toStringBuilder(Object, Converter)                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringBuilder_HappyPath() {
        CharBuffer source = CharBuffer.wrap("StringBuilderTest");
        StringBuilder sb = CharBufferConversions.toStringBuilder(source, converter);
        assertNotNull(sb);
        assertEquals("StringBuilder content must match source", "StringBuilderTest", sb.toString());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilder_WrongType() {
        CharBufferConversions.toStringBuilder(new Object(), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilder_NullFrom() {
        CharBufferConversions.toStringBuilder(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  8. toMap(Object, Converter)                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMap_HappyPath() {
        CharBuffer source = CharBuffer.wrap("mapValue");
        Map<?, ?> map = CharBufferConversions.toMap(source, converter);
        assertNotNull(map);
        assertTrue("Map must contain exactly one entry", map.size() == 1);
        assertTrue("Map must contain the key defined by MapConversions.VALUE",
                map.containsKey(VALUE));
        assertEquals("Map value must be the string representation of the CharBuffer",
                "mapValue", map.get(VALUE));
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongType() {
        CharBufferConversions.toMap(Boolean.FALSE, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullFrom() {
        CharBufferConversions.toMap(null, converter);
    }
}
