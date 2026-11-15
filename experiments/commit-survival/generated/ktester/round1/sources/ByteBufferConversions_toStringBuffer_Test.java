package com.cedarsoftware.util.convert;

import java.nio.CharBuffer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toStringBuffer_Test {

    /**
     * Mocked Converter used in each test.
     */
    @Mock
    private Converter mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Verify that no unexpected interactions occurred with the mock.
        verifyNoMoreInteractions(mockConverter);
    }

    private static Stream<Arguments> emptyInputProvider() {
        return Stream.of(Arguments.of("CharBuffer", CharBuffer.wrap("")), Arguments.of("byte array", new byte[] {}), Arguments.of("String", ""));
    }

    /**
     * Verify that a non‑null source object is converted to a {@link StringBuffer}
     * containing the characters supplied by the mocked {@link Converter}.
     */
    @Test
    void toStringBuffer_returnsStringBufferWithConvertedChars() {
        // GIVEN
        String source = "Hello, world!";
        CharBuffer expectedCharBuffer = CharBuffer.wrap(source);
        when(mockConverter.convert(eq(source), eq(CharBuffer.class))).thenReturn(expectedCharBuffer);
        // WHEN
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer should not be null");
        assertEquals(source, result.toString(), "StringBuffer content must match the converted CharBuffer");
        verify(mockConverter).convert(eq(source), eq(CharBuffer.class));
    }

    /**
     * Verify that a {@code null} source yields an empty {@link StringBuffer}.
     * The mock is configured to return an empty {@link CharBuffer} for a null input.
     */
    @Test
    void toStringBuffer_withNullSource_returnsEmptyStringBuffer() {
        // GIVEN
        Object source = null;
        CharBuffer emptyCharBuffer = CharBuffer.allocate(0);
        when(mockConverter.convert(isNull(), eq(CharBuffer.class))).thenReturn(emptyCharBuffer);
        // WHEN
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer should not be null even for null source");
        assertEquals("", result.toString(), "StringBuffer should be empty when source is null");
        verify(mockConverter).convert(isNull(), eq(CharBuffer.class));
    }

    /**
     * Verify that the method works with a byte array source.
     * The mock converts the byte array to a CharBuffer containing the expected string.
     */
    @Test
    void toStringBuffer_withByteArraySource_convertsCorrectly() {
        // GIVEN
        // default charset
        byte[] source = "bytes".getBytes();
        String expected = "bytes";
        CharBuffer expectedCharBuffer = CharBuffer.wrap(expected);
        when(mockConverter.convert(eq(source), eq(CharBuffer.class))).thenReturn(expectedCharBuffer);
        // WHEN
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, mockConverter);
        // THEN
        assertEquals(expected, result.toString(), "StringBuffer content must match conversion of byte array to CharBuffer");
        verify(mockConverter).convert(eq(source), eq(CharBuffer.class));
    }

    /* -------------------------------------------------------------
     *  1. Null Input – both parameters null → NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_nullFromAndNullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toStringBuffer(null, null), "Both arguments null should cause NullPointerException");
    }

    /* -------------------------------------------------------------
     *  2. CharBuffer Input (converter == null)
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_charBufferInput_withoutConverter_returnsMatchingStringBuffer() {
        CharBuffer source = CharBuffer.wrap("test");
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, null);
        assertNotNull(result);
        assertEquals("test", result.toString(), "StringBuffer content must match the original CharBuffer");
    }

    /* -------------------------------------------------------------
     *  3. ByteArray Input (converter == null)
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_byteArrayInput_withoutConverter_returnsMatchingStringBuffer() {
        // "test"
        byte[] source = new byte[] { 116, 101, 115, 116 };
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, null);
        assertEquals("test", result.toString(), "Byte array should be converted to a StringBuffer containing \"test\"");
    }

    /* -------------------------------------------------------------
     *  4. ByteBuffer Input (converter == null)
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_byteBufferInput_withoutConverter_returnsMatchingStringBuffer() {
        // "test"
        ByteBuffer source = ByteBuffer.wrap(new byte[] { 116, 101, 115, 116 });
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, null);
        assertEquals("test", result.toString(), "ByteBuffer should be converted to a StringBuffer containing \"test\"");
    }

    /* -------------------------------------------------------------
     *  5. String Input (converter == null)
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_stringInput_withoutConverter_returnsMatchingStringBuffer() {
        String source = "test";
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, null);
        assertEquals("test", result.toString(), "String input should be wrapped in a StringBuffer via internal conversion");
    }

    /* -------------------------------------------------------------
     *  6. Unsupported Type → IllegalArgumentException
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_unsupportedType_withoutConverter_throwsIllegalArgumentException() {
        Integer source = 12345;
        assertThrows(IllegalArgumentException.class, () -> ByteBufferConversions.toStringBuffer(source, null), "Unsupported source type should cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     *  7. Empty Inputs – parameterized (covers CharBuffer, byte[], String)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: empty {0}")
    @MethodSource("emptyInputProvider")
    void toStringBuffer_emptyInputs_withoutConverter(String description, Object source) {
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, null);
        assertNotNull(result);
        assertEquals("", result.toString(), "Empty " + description + " should produce an empty StringBuffer");
    }

    /* -------------------------------------------------------------
     *  8. CharArray Input – both with and without a Converter
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_charArrayInput_withoutConverter_returnsMatchingStringBuffer() {
        char[] source = new char[] { 't', 'e', 's', 't' };
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, null);
        assertEquals("test", result.toString(), "char[] should be converted to a StringBuffer containing \"test\"");
    }

    @Test
    void toStringBuffer_charArrayInput_withMockConverter_returnsMatchingStringBuffer() {
        char[] source = new char[] { 't', 'e', 's', 't' };
        CharBuffer expected = CharBuffer.wrap("test");
        when(mockConverter.convert(eq(source), eq(CharBuffer.class))).thenReturn(expected);
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, mockConverter);
        assertEquals("test", result.toString(), "Mocked conversion of char[] should produce expected StringBuffer");
        verify(mockConverter).convert(eq(source), eq(CharBuffer.class));
    }

    /* -------------------------------------------------------------
     *  9. StringBuilder Input (converter == null)
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_stringBuilderInput_withoutConverter_returnsMatchingStringBuffer() {
        StringBuilder source = new StringBuilder("test");
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, null);
        assertEquals("test", result.toString(), "StringBuilder should be converted to a matching StringBuffer");
    }

    /* -------------------------------------------------------------
     * 10. ByteBuffer with Non‑Zero Position (converter == null)
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_byteBufferWithPosition_withoutConverter_ignoresLeadingBytes() {
        // 0 + "test"
        ByteBuffer source = ByteBuffer.wrap(new byte[] { 0, 116, 101, 115, 116 });
        // skip the leading zero
        source.position(1);
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, null);
        assertEquals("test", result.toString(), "ByteBuffer position should cause conversion of the remaining bytes only");
    }

    /* -------------------------------------------------------------
     * 11. CharBuffer with Offset (position/limit) (converter == null)
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_charBufferWithOffset_withoutConverter_exposesSubsequence() {
        CharBuffer source = CharBuffer.wrap("prefixtestsuffix");
        // after "prefix"
        source.position(6);
        // up to "test"
        source.limit(10);
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, null);
        assertEquals("test", result.toString(), "CharBuffer position/limit should limit conversion to the subsequence");
    }

    /* -------------------------------------------------------------
     * 12. Custom Converter handling an otherwise unsupported type (Integer)
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_customConverter_convertsIntegerToStringBuffer() {
        // A simple Converter that knows how to turn an Integer into a CharBuffer.
        Converter customConverter = mock(Converter.class);
        Integer source = Integer.valueOf(12345);
        CharBuffer expected = CharBuffer.wrap(String.valueOf(source));
        when(customConverter.convert(eq(source), eq(CharBuffer.class))).thenReturn(expected);
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, customConverter);
        assertEquals("12345", result.toString(), "Custom converter should produce a StringBuffer containing the integer's string value");
        verify(customConverter).convert(eq(source), eq(CharBuffer.class));
    }

    /* -------------------------------------------------------------
     * 13. Large Input – 10 000 'a' characters (converter == null)
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_largeInput_withoutConverter_handlesBigData() {
        char[] manyAs = new char[10_000];
        java.util.Arrays.fill(manyAs, 'a');
        String largeString = new String(manyAs);
        StringBuffer result = ByteBufferConversions.toStringBuffer(largeString, null);
        assertEquals(10_000, result.length(), "Resulting StringBuffer should have length 10 000");
        assertTrue(result.chars().allMatch(ch -> ch == 'a'), "All characters in the result should be 'a'");
    }

    /* -------------------------------------------------------------
     * 14. Read‑Only ByteBuffer (converter == null)
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_readOnlyByteBuffer_withoutConverter_returnsMatchingStringBuffer() {
        // "test"
        ByteBuffer source = ByteBuffer.wrap(new byte[] { 116, 101, 115, 116 }).asReadOnlyBuffer();
        StringBuffer result = ByteBufferConversions.toStringBuffer(source, null);
        assertEquals("test", result.toString(), "Read‑only ByteBuffer should be treated the same as a regular ByteBuffer");
    }
}
