package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;

/**
 * JUnit‑4 test suite for {@link CharBufferConversions}.
 *
 * The tests are written in the same package as {@code CharBufferConversions}
 * so that the package‑private static methods are directly accessible.
 *
 * The suite covers:
 *   1. Basic functionality of every conversion method.
 *   2. Edge‑case scenarios such as empty buffers, non‑zero positions,
 *      and {@code null} arguments.
 *   3. Verification that the underlying {@link Converter} charset is
 *      respected.
 */
public class CharBufferConversionsTest {

    /** Helper that creates a default {@link Converter}.  
     *  The library provides a public {@code ConverterOptions} class with a
     *  no‑arg constructor that defaults to UTF‑8 charset. */
    private Converter createDefaultConverter() {
        return new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------------- */
    /* 1. toCharBuffer() – basic & read‑only behaviour                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharBufferBasic() {
        Converter converter = createDefaultConverter();
        CharBuffer source = CharBuffer.wrap("Hello World");
        CharBuffer result = CharBufferConversions.toCharBuffer(source, converter);

        // content must be identical
        Assert.assertEquals(source.toString(), result.toString());

        // result must be a read‑only view
        Assert.assertTrue("Resulting CharBuffer should be read‑only", result.isReadOnly());

        // modifying the source position must not affect the result
        source.position(5);
        Assert.assertEquals("Hello World".length(), result.position());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBufferNullInput() {
        Converter converter = createDefaultConverter();
        CharBufferConversions.toCharBuffer(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /* 2. toByteArray() – conversion via ByteBufferConversions                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByteArrayBasic() {
        Converter converter = createDefaultConverter();
        CharBuffer source = CharBuffer.wrap("ABCé"); // includes a non‑ASCII char
        byte[] expected = converter.getOptions()
                                   .getCharset()
                                   .encode(source)
                                   .array();

        byte[] actual = CharBufferConversions.toByteArray(source, converter);
        Assert.assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteArrayEmpty() {
        Converter converter = createDefaultConverter();
        CharBuffer source = CharBuffer.wrap("");
        byte[] actual = CharBufferConversions.toByteArray(source, converter);
        Assert.assertEquals(0, actual.length);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArrayNullInput() {
        Converter converter = createDefaultConverter();
        CharBufferConversions.toByteArray(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /* 3. toByteBuffer() – direct charset encoding                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByteBufferMatchesCharsetEncoding() {
        Converter converter = createDefaultConverter();
        CharBuffer source = CharBuffer.wrap("Sample Text");
        ByteBuffer expected = converter.getOptions()
                                       .getCharset()
                                       .encode(source);

        ByteBuffer actual = CharBufferConversions.toByteBuffer(source, converter);
        // compare remaining bytes
        Assert.assertEquals(expected.remaining(), actual.remaining());
        while (expected.hasRemaining()) {
            Assert.assertEquals(expected.get(), actual.get());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBufferNullInput() {
        Converter converter = createDefaultConverter();
        CharBufferConversions.toByteBuffer(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /* 4. toString() – simple delegation to CharBuffer.toString()            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringMatchesOriginal() {
        Converter converter = createDefaultConverter();
        CharBuffer source = CharBuffer.wrap("Just a String");
        String result = CharBufferConversions.toString(source, converter);
        Assert.assertEquals(source.toString(), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullInput() {
        Converter converter = createDefaultConverter();
        CharBufferConversions.toString(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /* 5. toCharArray() – normal case and empty‑buffer handling              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharArrayNormal() {
        Converter converter = createDefaultConverter();
        CharBuffer source = CharBuffer.wrap("charArray");
        char[] result = CharBufferConversions.toCharArray(source, converter);
        Assert.assertArrayEquals("charArray".toCharArray(), result);
    }

    @Test
    public void testToCharArrayEmptyBuffer() {
        Converter converter = createDefaultConverter();
        CharBuffer source = CharBuffer.wrap("");
        char[] result = CharBufferConversions.toCharArray(source, converter);
        // The implementation returns the library constant EMPTY_CHAR_ARRAY
        Assert.assertSame("Empty buffer should return EMPTY_CHAR_ARRAY",
                EMPTY_CHAR_ARRAY, result);
    }

    @Test
    public void testToCharArrayWithNonZeroPosition() {
        Converter converter = createDefaultConverter();
        CharBuffer source = CharBuffer.wrap("prefixXYZsuffix");
        // Move position to after "prefix"
        source.position(6);
        source.limit(9); // limit at the end of "XYZ"
        char[] result = CharBufferConversions.toCharArray(source, converter);
        Assert.assertArrayEquals(new char[]{'X', 'Y', 'Z'}, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArrayNullInput() {
        Converter converter = createDefaultConverter();
        CharBufferConversions.toCharArray(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /* 6. toStringBuffer() – conversion to mutable StringBuffer               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBufferContent() {
        Converter converter = createDefaultConverter();
        CharBuffer source = CharBuffer.wrap("StringBufferContent");
        StringBuffer sb = CharBufferConversions.toStringBuffer(source, converter);
        Assert.assertEquals(source.toString(), sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBufferNullInput() {
        Converter converter = createDefaultConverter();
        CharBufferConversions.toStringBuffer(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /* 7. toStringBuilder() – conversion to mutable StringBuilder            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBuilderContent() {
        Converter converter = createDefaultConverter();
        CharBuffer source = CharBuffer.wrap("StringBuilderContent");
        StringBuilder sb = CharBufferConversions.toStringBuilder(source, converter);
        Assert.assertEquals(source.toString(), sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilderNullInput() {
        Converter converter = createDefaultConverter();
        CharBufferConversions.toStringBuilder(null, converter);
    }
}
