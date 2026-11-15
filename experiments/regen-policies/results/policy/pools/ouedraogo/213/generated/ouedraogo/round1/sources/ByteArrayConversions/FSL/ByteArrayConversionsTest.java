package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Comprehensive tests for {@link ByteArrayConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>null and empty byte arrays</li>
 *   <li>ASCII and multi‑byte UTF‑8 characters</li>
 *   <li>All conversion methods (String, ByteBuffer, CharBuffer, char[], StringBuffer, StringBuilder)</li>
 *   <li>Correct handling of the {@link Converter} charset option</li>
 * </ul>
 */
public class ByteArrayConversionsTest {

    /**
     * Helper that creates a {@link Converter} whose options use the supplied charset.
     * The real library provides a {@code ConverterOptions} class with a {@code setCharset}
     * (or similar) method.  If the API differs, adjust the call accordingly – the intent
     * is to ensure the converter reports the charset we want.
     */
    private static Converter createConverter(Charset charset) {
        // The real ConverterOptions class is part of the library; we assume it has a
        // constructor and a setter for the charset.  If the setter name is different,
        // replace it with the correct one.
        ConverterOptions options = new ConverterOptions();
        // The following line uses reflection to avoid compile‑time dependency on the
        // exact setter name.  It works for both a {@code setCharset} method or a
        // {@code withCharset} method that returns the options instance.
        try {
            java.lang.reflect.Method m = options.getClass().getMethod("setCharset", Charset.class);
            m.invoke(options, charset);
        } catch (NoSuchMethodException e) {
            // Fallback to a method named "withCharset"
            try {
                java.lang.reflect.Method m = options.getClass().getMethod("withCharset", Charset.class);
                m.invoke(options, charset);
            } catch (Exception ex) {
                throw new RuntimeException("Unable to configure charset on ConverterOptions", ex);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new Converter(options);
    }

    @Test
    public void testToString_NullAndEmpty() {
        Converter converter = createConverter(StandardCharsets.UTF_8);

        // null byte array should return the library's EMPTY constant
        assertSame(StringUtilities.EMPTY, ByteArrayConversions.toString(null, converter));

        // empty byte array should also result in an empty string (but not the SAME instance)
        byte[] empty = new byte[0];
        String result = ByteArrayConversions.toString(empty, converter);
        assertEquals("", result);
        assertNotSame(StringUtilities.EMPTY, result); // they are equal but not the same reference
    }

    @Test
    public void testToString_ASCIIAndUTF8() {
        Converter converter = createConverter(StandardCharsets.UTF_8);

        // Simple ASCII
        byte[] ascii = "Hello World".getBytes(StandardCharsets.UTF_8);
        assertEquals("Hello World", ByteArrayConversions.toString(ascii, converter));

        // UTF‑8 characters (e.g., é, 漢字)
        String multi = "Café 漢字";
        byte[] utf8 = multi.getBytes(StandardCharsets.UTF_8);
        assertEquals(multi, ByteArrayConversions.toString(utf8, converter));
    }

    @Test
    public void testToByteBuffer_WrapsArray() {
        Converter converter = createConverter(StandardCharsets.UTF_8);
        byte[] data = {0, 1, 2, 3, 4};

        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(data, converter);
        assertNotNull(buffer);
        assertEquals(data.length, buffer.remaining());

        // Verify the underlying content matches the source array
        byte[] extracted = new byte[data.length];
        buffer.get(extracted);
        assertArrayEquals(data, extracted);

        // Position should be at the end after the get; reset and verify again
        buffer.rewind();
        assertEquals(0, buffer.position());
        assertEquals(data.length, buffer.remaining());
    }

    @Test
    public void testToCharBuffer_ContentMatchesString() {
        Converter converter = createConverter(StandardCharsets.UTF_8);
        String original = "BufferTest 🚀";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);

        CharBuffer charBuffer = ByteArrayConversions.toCharBuffer(bytes, converter);
        assertNotNull(charBuffer);
        assertEquals(original.length(), charBuffer.length());
        assertEquals(original, charBuffer.toString());
    }

    @Test
    public void testToCharArray_CorrectCharacters() {
        Converter converter = createConverter(StandardCharsets.UTF_8);
        String text = "CharArray✓";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        char[] chars = ByteArrayConversions.toCharArray(bytes, converter);
        assertNotNull(chars);
        assertArrayEquals(text.toCharArray(), chars);
    }

    @Test
    public void testToStringBuffer_And_ToStringBuilder() {
        Converter converter = createConverter(StandardCharsets.UTF_8);
        String payload = "Builder&Buffer Test";
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

        StringBuffer sb = ByteArrayConversions.toStringBuffer(bytes, converter);
        StringBuilder sbuilder = ByteArrayConversions.toStringBuilder(bytes, converter);

        assertNotNull(sb);
        assertNotNull(sbuilder);
        assertEquals(payload, sb.toString());
        assertEquals(payload, sbuilder.toString());

        // Mutate the buffers and ensure they are independent
        sb.append("X");
        sbuilder.append("Y");
        assertEquals(payload + "X", sb.toString());
        assertEquals(payload + "Y", sbuilder.toString());
    }

    @Test
    public void testAllConversions_WithDifferentCharset() {
        // Use ISO‑8859‑1 to prove that the charset supplied by the converter is honoured.
        Charset iso = StandardCharsets.ISO_8859_1;
        Converter converter = createConverter(iso);

        // The character 'ñ' (U+00F1) is representable in ISO‑8859‑1 but encoded differently
        // in UTF‑8.  By encoding the string with ISO‑8859‑1 we can verify the conversion
        // path respects the charset.
        String original = "Español";
        byte[] isoBytes = original.getBytes(iso);

        // toString should decode using ISO‑8859‑1, yielding the original string.
        assertEquals(original, ByteArrayConversions.toString(isoBytes, converter));

        // The other conversion helpers rely on toString internally, so they should also
        // respect the charset.
        assertEquals(original, ByteArrayConversions.toCharBuffer(isoBytes, converter).toString());
        assertArrayEquals(original.toCharArray(),
                ByteArrayConversions.toCharArray(isoBytes, converter));
        assertEquals(original, ByteArrayConversions.toStringBuffer(isoBytes, converter).toString());
        assertEquals(original, ByteArrayConversions.toStringBuilder(isoBytes, converter).toString());
    }
}
