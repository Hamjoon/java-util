package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import static org.mockito.ArgumentMatchers.*;

class CharBufferConversions_toStringBuffer_Test {

    private Converter converter;

    /**
     * Simple {@link CharSequence} implementation used in tests.
     */
    private static final class CustomCharSequence implements CharSequence {

        private final String data;

        CustomCharSequence(String data) {
            this.data = data;
        }

        @Override
        public int length() {
            return data.length();
        }

        @Override
        public char charAt(int index) {
            return data.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return data.subSequence(start, end);
        }

        @Override
        public String toString() {
            return data;
        }
    }

    /**
     * CharSequence whose {@code length()} method always throws.
     */
    private static final class ThrowingCharSequence implements CharSequence {

        @Override
        public int length() {
            throw new RuntimeException("length() failure");
        }

        @Override
        public char charAt(int index) {
            throw new RuntimeException("charAt() failure");
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            throw new RuntimeException("subSequence() failure");
        }

        @Override
        public String toString() {
            throw new RuntimeException("toString() failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // ConverterOptions has a public no‑arg constructor that creates the default options.
        // Using the real Converter keeps the test simple and avoids mocking static methods.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    void toStringBuffer_convertsStringCorrectly() {
        // GIVEN
        String source = "Hello, World!";
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(new StringBuffer(source), result, "StringBuffer content should match the source string");
    }

    @Test
    void toStringBuffer_convertsCharBufferCorrectly() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("CharBufferContent");
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(new StringBuffer(source.toString()), result, "StringBuffer content should match the CharBuffer's string representation");
    }

    @Test
    void toStringBuffer_handlesEmptyString() {
        // GIVEN
        String source = "";
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(0, result.length(), "StringBuffer should be empty for an empty source string");
    }

    @Test
    void toStringBuffer_returnsEmptyBufferForNullInput() {
        // GIVEN
        Object source = null;
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result, "Result should not be null even when source is null");
        assertEquals(0, result.length(), "StringBuffer should be empty when source is null");
    }

    @Test
    void toStringBuffer_nullConverterWithCharBuffer() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("hello");
        Converter nullConverter = null;
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, nullConverter);
        // THEN
        assertEquals(new StringBuffer("hello"), result);
    }

    @Test
    void toStringBuffer_nullConverterWithString() {
        // GIVEN
        String source = "world";
        Converter nullConverter = null;
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, nullConverter);
        // THEN
        assertEquals(new StringBuffer("world"), result);
    }

    @Test
    void toStringBuffer_nullConverterWithCharArray() {
        // GIVEN
        char[] source = new char[] { 'a', 'b', 'c' };
        Converter nullConverter = null;
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, nullConverter);
        // THEN
        assertEquals(new StringBuffer("abc"), result);
    }

    @Test
    void toStringBuffer_nullConverterWithAsciiByteBuffer() {
        // GIVEN
        // "ABC"
        ByteBuffer source = ByteBuffer.wrap(new byte[] { 65, 66, 67 });
        Converter nullConverter = null;
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, nullConverter);
        // THEN
        assertEquals(new StringBuffer("ABC"), result);
    }

    @Test
    void toStringBuffer_nullConverterWithUnsupportedType() {
        // GIVEN
        Object source = new Object();
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toStringBuffer(source, nullConverter), "Unsupported source type should cause IllegalArgumentException");
    }

    @Test
    void toStringBuffer_upperCaseConverterTransformsString() {
        // GIVEN
        String source = "custom";
        Converter upperCaseConverter = mock(Converter.class);
        // Simulate a converter that returns the upper‑cased string
        when(upperCaseConverter.convert(any(), eq(CharBuffer.class))).thenAnswer(invocation -> CharBuffer.wrap(((String) invocation.getArgument(0)).toUpperCase()));
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, upperCaseConverter);
        // THEN
        assertEquals(new StringBuffer("CUSTOM"), result);
    }

    @Test
    void toStringBuffer_nullSourceWithUpperCaseConverter() {
        // GIVEN
        Converter upperCaseConverter = mock(Converter.class);
        when(upperCaseConverter.convert(isNull(), any())).thenReturn(// converter returns null
        null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toStringBuffer(null, upperCaseConverter), "Null source with non‑null converter should still throw NPE");
    }

    @Test
    void toStringBuffer_utf8ByteBufferProducesUnicodeCharacter() {
        // GIVEN
        // UTF‑8 bytes for 'é' (0xC3 0xA9)
        ByteBuffer source = ByteBuffer.wrap(new byte[] { (byte) 0xC3, (byte) 0xA9 });
        Converter nullConverter = null;
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, nullConverter);
        // THEN
        assertEquals(new StringBuffer("é"), result);
    }

    @Test
    void toStringBuffer_charBufferWithNonZeroPosition() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("hello");
        // now points at "llo"
        source.position(2);
        Converter nullConverter = null;
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, nullConverter);
        // THEN
        assertEquals(new StringBuffer("llo"), result);
    }

    @Test
    void toStringBuffer_readOnlyCharBuffer() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("world").asReadOnlyBuffer();
        Converter nullConverter = null;
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, nullConverter);
        // THEN
        assertEquals(new StringBuffer("world"), result);
    }

    @Test
    void toStringBuffer_stringBuilderInput() {
        // GIVEN
        StringBuilder source = new StringBuilder("builder");
        Converter nullConverter = null;
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, nullConverter);
        // THEN
        assertEquals(new StringBuffer("builder"), result);
    }

    @Test
    void toStringBuffer_stringBufferInput() {
        // GIVEN
        java.lang.StringBuffer source = new java.lang.StringBuffer("buf");
        Converter nullConverter = null;
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, nullConverter);
        // THEN
        assertEquals(new StringBuffer("buf"), result);
    }

    @Test
    void toStringBuffer_rawByteArrayIsDecodedAsUtf8() {
        // GIVEN
        // "Hi"
        byte[] source = new byte[] { 72, 105 };
        Converter nullConverter = null;
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, nullConverter);
        // THEN
        assertEquals(new StringBuffer("Hi"), result);
    }

    @Test
    void toStringBuffer_customCharSequence() {
        // GIVEN
        CharSequence source = new CustomCharSequence("customSeq");
        Converter nullConverter = null;
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, nullConverter);
        // THEN
        assertEquals(new StringBuffer("customSeq"), result);
    }

    @Test
    void toStringBuffer_converterReturnsNullLeadsToNPE() {
        // GIVEN
        String source = "data";
        Converter nullReturningConverter = mock(Converter.class);
        when(nullReturningConverter.convert(any(), any())).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toStringBuffer(source, nullReturningConverter), "Converter returning null should cause NPE inside toStringBuffer");
    }

    @Test
    void toStringBuffer_converterThrowsRuntimeException() {
        // GIVEN
        String source = "trigger";
        Converter throwingConverter = mock(Converter.class);
        when(throwingConverter.convert(any(), any())).thenThrow(new RuntimeException("Converter failure"));
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CharBufferConversions.toStringBuffer(source, throwingConverter));
        assertEquals("Converter failure", ex.getMessage());
    }

    @Test
    void toStringBuffer_converterReturnsUnsupportedType() {
        // GIVEN
        String source = "any";
        Converter unsupportedReturnConverter = mock(Converter.class);
        // Integer
        when(unsupportedReturnConverter.convert(any(), any())).thenReturn(42);
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toStringBuffer(source, unsupportedReturnConverter), "Converter returning unsupported type should cause IllegalArgumentException");
    }

    @Test
    void toStringBuffer_converterReturnsCharSequenceThatThrowsOnLength() {
        // GIVEN
        String source = "test";
        Converter throwingCharSeqConverter = mock(Converter.class);
        when(throwingCharSeqConverter.convert(any(), any())).thenReturn(new ThrowingCharSequence());
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CharBufferConversions.toStringBuffer(source, throwingCharSeqConverter));
        assertEquals("length() failure", ex.getMessage());
    }
}
