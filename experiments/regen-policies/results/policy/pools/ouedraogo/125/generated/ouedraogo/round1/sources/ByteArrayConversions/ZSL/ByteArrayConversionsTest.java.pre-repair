package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * JUnit 4 tests for {@link ByteArrayConversions}.
 *
 * The class under test is package‑private, therefore the test class resides in the same
 * package to gain access to its static methods.
 */
public class ByteArrayConversionsTest {

    /**
     * Helper that creates a {@link Converter} with default options.
     * The default {@link ConverterOptions} is expected to use {@link StandardCharsets#UTF_8}
     * as its charset, which is sufficient for the test scenarios.
     */
    private static Converter createDefaultConverter() {
        // The library provides a public ConverterOptions class (not shown in the excerpt)
        // with a no‑arg constructor that defaults to UTF‑8.
        ConverterOptions options = new ConverterOptions();
        return new Converter(options);
    }

    @Test
    public void testToString_WithNullByteArray_ReturnsEmpty() {
        Converter converter = createDefaultConverter();
        String result = ByteArrayConversions.toString(null, converter);
        Assert.assertEquals("Result should be the library's EMPTY constant",
                StringUtilities.EMPTY, result);
    }

    @Test
    public void testToString_WithEmptyByteArray_ReturnsEmptyString() {
        Converter converter = createDefaultConverter();
        byte[] empty = new byte[0];
        String result = ByteArrayConversions.toString(empty, converter);
        Assert.assertEquals("Empty byte array should produce empty string",
                "", result);
    }

    @Test
    public void testToString_WithAsciiBytes_ReturnsCorrectString() {
        Converter converter = createDefaultConverter();
        byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(data, converter);
        Assert.assertEquals("ASCII conversion failed", "Hello, World!", result);
    }

    @Test
    public void testToString_WithUnicodeBytes_ReturnsCorrectString() {
        Converter converter = createDefaultConverter();
        // String containing characters that need more than one byte in UTF‑8
        String original = "Café 🌟 漢字";
        byte[] data = original.getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(data, converter);
        Assert.assertEquals("Unicode conversion failed", original, result);
    }

    @Test
    public void testToByteBuffer_WithValidBytes_WrapsCorrectly() {
        Converter converter = createDefaultConverter();
        byte[] data = {1, 2, 3, 4, 5};
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(data, converter);
        Assert.assertNotNull("ByteBuffer should not be null", buffer);
        Assert.assertEquals("Buffer limit should match array length", data.length, buffer.limit());
        // Verify content
        byte[] extracted = new byte[buffer.remaining()];
        buffer.get(extracted);
        Assert.assertArrayEquals("ByteBuffer content mismatch", data, extracted);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_WithNull_ThrowsNPE() {
        Converter converter = createDefaultConverter();
        ByteArrayConversions.toByteBuffer(null, converter);
    }

    @Test
    public void testToCharBuffer_WithNullByteArray_ReturnsEmptyCharBuffer() {
        Converter converter = createDefaultConverter();
        CharBuffer cb = ByteArrayConversions.toCharBuffer(null, converter);
        Assert.assertNotNull(cb);
        Assert.assertEquals(0, cb.length());
    }

    @Test
    public void testToCharBuffer_WithData_ReturnsCorrectCharBuffer() {
        Converter converter = createDefaultConverter();
        String text = "JUnit Test";
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        CharBuffer cb = ByteArrayConversions.toCharBuffer(data, converter);
        Assert.assertEquals("CharBuffer length mismatch", text.length(), cb.length());
        Assert.assertEquals("CharBuffer content mismatch", text, cb.toString());
    }

    @Test
    public void testToCharArray_WithNull_ReturnsEmptyArray() {
        Converter converter = createDefaultConverter();
        char[] result = ByteArrayConversions.toCharArray(null, converter);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void testToCharArray_WithData_ReturnsCorrectArray() {
        Converter converter = createDefaultConverter();
        String sample = "abcXYZ123";
        byte[] data = sample.getBytes(StandardCharsets.UTF_8);
        char[] result = ByteArrayConversions.toCharArray(data, converter);
        Assert.assertArrayEquals("Char array does not match expected characters",
                sample.toCharArray(), result);
    }

    @Test
    public void testToStringBuffer_WithNull_ReturnsEmptyStringBuffer() {
        Converter converter = createDefaultConverter();
        StringBuffer sb = ByteArrayConversions.toStringBuffer(null, converter);
        Assert.assertNotNull(sb);
        Assert.assertEquals(0, sb.length());
    }

    @Test
    public void testToStringBuffer_WithData_ReturnsCorrectStringBuffer() {
        Converter converter = createDefaultConverter();
        String expected = "StringBuffer content";
        byte[] data = expected.getBytes(StandardCharsets.UTF_8);
        StringBuffer sb = ByteArrayConversions.toStringBuffer(data, converter);
        Assert.assertEquals("StringBuffer content mismatch", expected, sb.toString());
    }

    @Test
    public void testToStringBuilder_WithNull_ReturnsEmptyStringBuilder() {
        Converter converter = createDefaultConverter();
        StringBuilder sb = ByteArrayConversions.toStringBuilder(null, converter);
        Assert.assertNotNull(sb);
        Assert.assertEquals(0, sb.length());
    }

    @Test
    public void testToStringBuilder_WithData_ReturnsCorrectStringBuilder() {
        Converter converter = createDefaultConverter();
        String expected = "Builder 🚀";
        byte[] data = expected.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = ByteArrayConversions.toStringBuilder(data, converter);
        Assert.assertEquals("StringBuilder content mismatch", expected, sb.toString());
    }

    @Test
    public void testAllConversionsAreConsistent_ForSameInput() {
        Converter converter = createDefaultConverter();
        String original = "Consistency Check 123";
        byte[] data = original.getBytes(StandardCharsets.UTF_8);

        // Verify that every conversion path yields the same textual representation
        String fromToString = ByteArrayConversions.toString(data, converter);
        CharBuffer fromToCharBuffer = ByteArrayConversions.toCharBuffer(data, converter);
        char[] fromToCharArray = ByteArrayConversions.toCharArray(data, converter);
        StringBuffer fromToStringBuffer = ByteArrayConversions.toStringBuffer(data, converter);
        StringBuilder fromToStringBuilder = ByteArrayConversions.toStringBuilder(data, converter);

        Assert.assertEquals(original, fromToString);
        Assert.assertEquals(original, fromToCharBuffer.toString());
        Assert.assertArrayEquals(original.toCharArray(), fromToCharArray);
        Assert.assertEquals(original, fromToStringBuffer.toString());
        Assert.assertEquals(original, fromToStringBuilder.toString());
    }

    @Test
    public void testCharsetUsedByConverter_IsRespected() {
        // Create a ConverterOptions that explicitly requests ISO‑8859‑1
        ConverterOptions options = new ConverterOptions();
        Charset iso = StandardCharsets.ISO_8859_1;
        // Assuming ConverterOptions has a setter for charset (common in such APIs)
        // If the real API differs, this test will need to be adapted accordingly.
        options.setCharset(iso);
        Converter converter = new Converter(options);

        // The string contains characters that differ between UTF‑8 and ISO‑8859‑1
        String original = "Åß∂"; // characters that have a direct ISO‑8859‑1 representation
        byte[] data = original.getBytes(iso);
        String result = ByteArrayConversions.toString(data, converter);
        Assert.assertEquals("String should be decoded using ISO‑8859‑1 charset", original, result);
    }
}
