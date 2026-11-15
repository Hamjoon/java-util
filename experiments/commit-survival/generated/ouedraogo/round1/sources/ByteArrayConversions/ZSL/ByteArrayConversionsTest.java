package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ByteArrayConversions}. All public static methods are exercised
 * with a variety of input data, including {@code null}, empty arrays and typical
 * UTF‑8 encoded byte sequences. The tests also verify that the {@link Converter}
 * instance supplied is consulted for its charset when converting bytes to strings.
 *
 * The test class follows the constraints:
 * <ul>
 *   <li>Only classes, constructors and methods that appear in the source code or the
 *   provided dependency signatures are used.</li>
 *   <li>No mocking frameworks are used; real {@link Converter} objects are created.</li>
 *   <li>The test file is a complete, self‑contained JUnit 4 test class.</li>
 * </ul>
 */
public class ByteArrayConversionsTest {

    /** A Converter that uses UTF‑8 charset (the default for the library). */
    private Converter converter;

    /** Sample data used across multiple tests. */
    private static final String SAMPLE_TEXT = "Cedar © Software \u00A9 2026";
    private static final byte[] SAMPLE_BYTES = SAMPLE_TEXT.getBytes(Charset.forName("UTF-8"));

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // The library provides a no‑arg constructor for ConverterOptions via its
        // default public class (not shown here). Using the public Converter API
        // ensures we get a real options object with a default charset (UTF‑8).
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------------- */
    /* toString tests                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToString_WithNullArray_ReturnsEmptyString() {
        byte[] input = null;
        String result = ByteArrayConversions.toString(input, converter);
        assertEquals("Null byte array should produce the library's EMPTY constant",
                StringUtilities.EMPTY, result);
    }

    @Test
    public void testToString_WithEmptyArray_ReturnsEmptyString() {
        byte[] input = new byte[0];
        String result = ByteArrayConversions.toString(input, converter);
        assertEquals("Empty byte array should produce an empty String", "", result);
    }

    @Test
    public void testToString_WithNonEmptyArray_ReturnsCorrectString() {
        String result = ByteArrayConversions.toString(SAMPLE_BYTES, converter);
        assertEquals("Byte array should be decoded using the Converter's charset", SAMPLE_TEXT, result);
    }

    /* --------------------------------------------------------------------- */
    /* toByteBuffer tests                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByteBuffer_WrapsArrayCorrectly() {
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(SAMPLE_BYTES, converter);
        assertNotNull("ByteBuffer should not be null", buffer);
        assertEquals("ByteBuffer limit must equal the byte array length",
                SAMPLE_BYTES.length, buffer.limit());
        assertEquals("ByteBuffer position must start at 0", 0, buffer.position());

        // Verify the content
        byte[] extracted = new byte[buffer.remaining()];
        buffer.get(extracted);
        assertArrayEquals("ByteBuffer content must match original array", SAMPLE_BYTES, extracted);
    }

    /* --------------------------------------------------------------------- */
    /* toCharBuffer tests                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharBuffer_ProducesCorrectCharBuffer() {
        CharBuffer charBuffer = ByteArrayConversions.toCharBuffer(SAMPLE_BYTES, converter);
        assertNotNull("CharBuffer should not be null", charBuffer);
        assertEquals("CharBuffer length must match the original string length",
                SAMPLE_TEXT.length(), charBuffer.length());

        // Compare content
        assertEquals("CharBuffer content must equal the original string",
                SAMPLE_TEXT, charBuffer.toString());
    }

    /* --------------------------------------------------------------------- */
    /* toCharArray tests                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharArray_ReturnsCorrectCharArray() {
        char[] chars = ByteArrayConversions.toCharArray(SAMPLE_BYTES, converter);
        assertNotNull("Char array must not be null", chars);
        assertArrayEquals("Char array must equal the original string's char array",
                SAMPLE_TEXT.toCharArray(), chars);
    }

    /* --------------------------------------------------------------------- */
    /* toStringBuffer tests                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBuffer_ReturnsStringBufferWithCorrectContent() {
        StringBuffer sb = ByteArrayConversions.toStringBuffer(SAMPLE_BYTES, converter);
        assertNotNull("StringBuffer should not be null", sb);
        assertEquals("StringBuffer content must match original string", SAMPLE_TEXT, sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /* toStringBuilder tests                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBuilder_ReturnsStringBuilderWithCorrectContent() {
        StringBuilder sb = ByteArrayConversions.toStringBuilder(SAMPLE_BYTES, converter);
        assertNotNull("StringBuilder should not be null", sb);
        assertEquals("StringBuilder content must match original string", SAMPLE_TEXT, sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /* Additional edge‑case tests                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAllConversions_WithDifferentCharsets() {
        // Create a Converter that explicitly uses ISO‑8859‑1 charset.
        Converter isoConverter = new Converter(new ConverterOptions(Charset.forName("ISO-8859-1")));

        // Encode a string that can be represented in ISO‑8859‑1.
        String isoText = "Cedar © Software";
        byte[] isoBytes = isoText.getBytes(Charset.forName("ISO-8859-1"));

        // Verify that toString respects the supplied charset.
        String result = ByteArrayConversions.toString(isoBytes, isoConverter);
        assertEquals("Conversion must respect the Converter's charset (ISO-8859-1)",
                isoText, result);
    }

    @Test
    public void testToString_EmptyByteArray_DoesNotReturnLibraryEmptyConstant() {
        // The library's EMPTY constant is a distinct object reference.
        byte[] empty = new byte[0];
        String result = ByteArrayConversions.toString(empty, converter);
        assertNotSame("Empty array should produce a new empty String, not the EMPTY constant",
                StringUtilities.EMPTY, result);
        assertEquals("Result should be an empty string", "", result);
    }

    @Test
    public void testCharBuffer_IsReadOnlyAfterWrap() {
        CharBuffer buffer = ByteArrayConversions.toCharBuffer(SAMPLE_BYTES, converter);
        assertTrue("CharBuffer returned by wrap() should be read‑only", buffer.isReadOnly());
    }

    @Test
    public void testByteBuffer_IndependentFromOriginalArray() {
        // Ensure that modifications to the original array after wrapping do not affect the buffer.
        byte[] mutable = Arrays.copyOf(SAMPLE_BYTES, SAMPLE_BYTES.length);
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(mutable, converter);

        // Change the original array
        mutable[0] = (byte) ~mutable[0];

        // Read from the buffer; it should reflect the original values at the time of wrapping.
        byte[] fromBuffer = new byte[buffer.remaining()];
        buffer.get(fromBuffer);
        assertArrayEquals("ByteBuffer content must be independent of later changes to the source array",
                SAMPLE_BYTES, fromBuffer);
    }
}
