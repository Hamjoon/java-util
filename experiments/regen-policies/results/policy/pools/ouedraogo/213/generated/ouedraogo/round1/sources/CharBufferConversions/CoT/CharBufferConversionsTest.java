package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Test suite for {@code CharBufferConversions}.
 * The test class lives in the same package so it can invoke the package‑private static methods.
 */
public class CharBufferConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is not part of the listed API, but it is required by the
        // Converter constructor.  We rely on the public no‑arg constructor that
        // supplies a default Charset (normally UTF‑8).
        ConverterOptions options = new ConverterOptions(); // default charset
        converter = new Converter(options);
    }

    /* -------------------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------------------- */

    @Test
    public void testToCharBuffer_returnsReadOnlyBufferWithSameContent() {
        CharBuffer src = CharBuffer.wrap("hello");
        CharBuffer result = CharBufferConversions.toCharBuffer(src, converter);

        assertNotNull(result);
        assertTrue(result.isReadOnly());
        assertEquals(src.toString(), result.toString());

        // original buffer must stay unchanged (position/limit)
        assertEquals(0, src.position());
        assertEquals(src.limit(), result.limit());
    }

    @Test
    public void testToString_returnsCorrectString() {
        CharBuffer src = CharBuffer.wrap("world");
        String s = CharBufferConversions.toString(src, converter);
        assertEquals("world", s);
    }

    @Test
    public void testToCharArray_nonEmptyBuffer() {
        CharBuffer src = CharBuffer.wrap("abcde");
        char[] chars = CharBufferConversions.toCharArray(src, converter);
        assertArrayEquals(new char[]{'a','b','c','d','e'}, chars);
    }

    @Test
    public void testToCharArray_emptyBuffer_returnsEmptyArray() {
        CharBuffer src = CharBuffer.allocate(0); // empty, hasRemaining() == false
        char[] chars = CharBufferConversions.toCharArray(src, converter);
        assertNotNull(chars);
        assertEquals(0, chars.length);
    }

    @Test
    public void testToStringBuffer() {
        CharBuffer src = CharBuffer.wrap("sb");
        StringBuffer sb = CharBufferConversions.toStringBuffer(src, converter);
        assertEquals("sb", sb.toString());
    }

    @Test
    public void testToStringBuilder() {
        CharBuffer src = CharBuffer.wrap("builder");
        StringBuilder sb = CharBufferConversions.toStringBuilder(src, converter);
        assertEquals("builder", sb.toString());
    }

    @Test
    public void testToByteBuffer_usesConverterCharset() {
        CharBuffer src = CharBuffer.wrap("ABC");
        ByteBuffer bb = CharBufferConversions.toByteBuffer(src, converter);
        assertNotNull(bb);
        // default charset is UTF‑8, so "ABC" -> 3 bytes 0x41 0x42 0x43
        byte[] expected = "ABC".getBytes(Charset.defaultCharset());
        byte[] actual = new byte[bb.remaining()];
        bb.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteArray_ascii() {
        CharBuffer src = CharBuffer.wrap("xyz");
        byte[] bytes = CharBufferConversions.toByteArray(src, converter);
        assertArrayEquals("xyz".getBytes(Charset.defaultCharset()), bytes);
    }

    @Test
    public void testToByteArray_utf8MultibyteCharacter() {
        // 'é' is two bytes in UTF‑8
        CharBuffer src = CharBuffer.wrap("café");
        byte[] bytes = CharBufferConversions.toByteArray(src, converter);
        assertArrayEquals("café".getBytes(Charset.defaultCharset()), bytes);
    }

    @Test
    public void testToMap_containsValueKey() {
        CharBuffer src = CharBuffer.wrap("mapValue");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) CharBufferConversions.toMap(src, converter);
        assertTrue(map.containsKey(VALUE));
        assertEquals("mapValue", map.get(VALUE));
    }

    /* -------------------------------------------------------------
     *  Edge‑case / exception tests
     * ------------------------------------------------------------- */

    @Test
    public void testToCharArray_respectsBufferPosition() {
        CharBuffer src = CharBuffer.wrap("12345");
        src.position(2); // now remaining = "345"
        char[] chars = CharBufferConversions.toCharArray(src, converter);
        assertArrayEquals(new char[]{'3','4','5'}, chars);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBuffer_nullSource_throwsNPE() {
        CharBufferConversions.toCharBuffer(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_nullSource_throwsNPE() {
        CharBufferConversions.toString(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArray_nullSource_throwsNPE() {
        CharBufferConversions.toCharArray(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuffer_nullSource_throwsNPE() {
        CharBufferConversions.toStringBuffer(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilder_nullSource_throwsNPE() {
        CharBufferConversions.toStringBuilder(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_nullSource_throwsNPE() {
        CharBufferConversions.toByteBuffer(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArray_nullSource_throwsNPE() {
        CharBufferConversions.toByteArray(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_nullSource_throwsNPE() {
        CharBufferConversions.toMap(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testMethods_nullConverter_throwsNPE() {
        CharBuffer src = CharBuffer.wrap("any");
        CharBufferConversions.toCharBuffer(src, null);
        // each method internally dereferences the converter, so the first NPE is enough.
    }
}
