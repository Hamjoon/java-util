package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import java.nio.ByteBuffer;
import java.util.function.Function;
import static org.mockito.ArgumentMatchers.*;

class CharBufferConversions_toString_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default options is sufficient
        // for the built‑in conversions exercised by the tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up; simply drop the reference.
        converter = null;
    }

    /**
     * Helper that creates a Mockito mock of {@link Converter} whose
     *  {@code convert(..., CharBuffer.class)} method returns the value
     *  supplied by {@code mapping}.
     */
    private static Converter mockConverter(Function<Object, CharBuffer> mapping) {
        Converter mock = mock(Converter.class);
        // The real Converter API is {@code <T> T convert(Object from, Class<T> toClass)}.
        // We only need to stub the CharBuffer conversion.
        when(mock.convert(any(), eq(CharBuffer.class))).thenAnswer(invocation -> {
            Object from = invocation.getArgument(0);
            return mapping.apply(from);
        });
        return mock;
    }

    @Test
    void toString_givenString_returnsSameString() {
        String input = "Hello, World!";
        String result = CharBufferConversions.toString(input, converter);
        assertEquals(input, result, "String input should be returned unchanged");
    }

    @Test
    void toString_givenCharArray_returnsCorrespondingString() {
        char[] input = new char[] { 'J', 'a', 'v', 'a' };
        String expected = new String(input);
        String result = CharBufferConversions.toString(input, converter);
        assertEquals(expected, result, "Char array should be converted to its String representation");
    }

    @Test
    void toString_givenCharBuffer_returnsItsContent() {
        CharBuffer input = CharBuffer.wrap("CharBuffer Content");
        // CharBuffer's toString returns the remaining characters
        String expected = input.toString();
        String result = CharBufferConversions.toString(input, converter);
        assertEquals(expected, result, "CharBuffer should be converted to its String content");
    }

    @Test
    void toString_givenByteArray_returnsStringViaDefaultConversion() {
        // The built‑in conversion treats a byte array as a UTF‑8 encoded string.
        byte[] input = "Byte array".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String expected = new String(input, java.nio.charset.StandardCharsets.UTF_8);
        String result = CharBufferConversions.toString(input, converter);
        assertEquals(expected, result, "Byte array should be converted to a UTF‑8 String");
    }

    @Test
    void toString_givenNull_returnsEmptyString() {
        // The conversion utilities treat null as an empty CharBuffer, whose toString is "".
        String result = CharBufferConversions.toString(null, converter);
        assertEquals("", result, "Null input should yield an empty String");
    }

    @Test
    void toString_givenString_withConverterThatWrapsString() {
        String input = "Hello World";
        Converter mock = mockConverter(from -> CharBuffer.wrap((String) from));
        String result = CharBufferConversions.toString(input, mock);
        assertEquals("Hello World", result);
    }

    @Test
    void toString_givenCharBuffer_withIdentityConverter() {
        CharBuffer input = CharBuffer.wrap("abc");
        // identity converter simply returns the supplied CharBuffer
        Converter mock = mockConverter(from -> (CharBuffer) from);
        String result = CharBufferConversions.toString(input, mock);
        assertEquals("abc", result);
    }

    @Test
    void toString_givenCharArray_withConverterThatCreatesCharBuffer() {
        char[] input = new char[] { 'J', 'a', 'v', 'a' };
        Converter mock = mockConverter(from -> CharBuffer.wrap((char[]) from));
        String result = CharBufferConversions.toString(input, mock);
        assertEquals("Java", result);
    }

    @Test
    void toString_givenByteBuffer_withConverterThatDecodesASCII() {
        // "ABC"
        ByteBuffer input = ByteBuffer.wrap(new byte[] { 65, 66, 67 });
        Converter mock = mockConverter(from -> {
            ByteBuffer bb = (ByteBuffer) from;
            // simple ASCII decoding
            CharBuffer cb = CharBuffer.allocate(bb.remaining());
            while (bb.hasRemaining()) {
                cb.put((char) (bb.get() & 0xFF));
            }
            cb.flip();
            return cb;
        });
        String result = CharBufferConversions.toString(input, mock);
        assertEquals("ABC", result);
    }

    @Test
    void toString_givenNullSource_converterReturnsNull_throwsNPE() {
        // returns null CharBuffer
        Converter mock = mockConverter(from -> null);
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toString(null, mock), "When the converter returns null, toString() should NPE");
    }

    @Test
    void toString_givenNullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toString("some text", null), "Passing a null Converter must cause NPE");
    }

    @Test
    void toString_givenEmptyString_withWrappingConverter() {
        String input = "";
        Converter mock = mockConverter(from -> CharBuffer.wrap((String) from));
        String result = CharBufferConversions.toString(input, mock);
        assertEquals("", result);
    }

    @Test
    void toString_givenEmptyCharBuffer_withIdentityConverter() {
        CharBuffer input = CharBuffer.allocate(0);
        Converter mock = mockConverter(from -> (CharBuffer) from);
        String result = CharBufferConversions.toString(input, mock);
        assertEquals("", result);
    }

    @Test
    void toString_givenEmptyCharArray_withConverter() {
        char[] input = new char[0];
        Converter mock = mockConverter(from -> CharBuffer.wrap((char[]) from));
        String result = CharBufferConversions.toString(input, mock);
        assertEquals("", result);
    }

    @Test
    void toString_givenEmptyByteBuffer_withConverter() {
        ByteBuffer input = ByteBuffer.allocate(0);
        Converter mock = mockConverter(from -> {
            ByteBuffer bb = (ByteBuffer) from;
            CharBuffer cb = CharBuffer.allocate(bb.remaining());
            cb.flip();
            return cb;
        });
        String result = CharBufferConversions.toString(input, mock);
        assertEquals("", result);
    }

    @Test
    void toString_givenByteBufferWithSlice_returnsSliceString() {
        // "ABCD"
        ByteBuffer bb = ByteBuffer.wrap(new byte[] { 65, 66, 67, 68 });
        // expose "BC"
        bb.position(1).limit(3);
        Converter mock = mockConverter(from -> {
            ByteBuffer src = (ByteBuffer) from;
            CharBuffer cb = CharBuffer.allocate(src.remaining());
            while (src.hasRemaining()) {
                cb.put((char) (src.get() & 0xFF));
            }
            cb.flip();
            return cb;
        });
        String result = CharBufferConversions.toString(bb, mock);
        assertEquals("BC", result);
    }

    @Test
    void toString_givenCharBufferWithSlice_returnsSliceString() {
        CharBuffer cb = CharBuffer.wrap("HelloWorld");
        // "llo"
        cb.position(2).limit(5);
        // identity
        Converter mock = mockConverter(from -> (CharBuffer) from);
        String result = CharBufferConversions.toString(cb, mock);
        assertEquals("llo", result);
    }

    @Test
    void toString_givenReadOnlyCharBuffer_identityConverter() {
        CharBuffer cb = CharBuffer.wrap("ReadOnly").asReadOnlyBuffer();
        Converter mock = mockConverter(from -> (CharBuffer) from);
        String result = CharBufferConversions.toString(cb, mock);
        assertEquals("ReadOnly", result);
    }

    @Test
    void toString_converterReturnsStringInsteadOfCharBuffer_throwsClassCast() {
        Converter mock = mock(Converter.class);
        // wrong type
        when(mock.convert(any(), eq(CharBuffer.class))).thenReturn("oops");
        assertThrows(ClassCastException.class, () -> CharBufferConversions.toString("oops", mock), "A ClassCastException is expected when the converter returns a non‑CharBuffer");
    }

    @Test
    void toString_converterReturnsNullCharBuffer_throwsNPE() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(CharBuffer.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toString("any", mock), "Null CharBuffer from converter should cause NPE");
    }

    @Test
    void toString_converterThrowsIllegalArgumentException_propagates() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(CharBuffer.class))).thenThrow(new IllegalArgumentException("bad arg"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toString("any", mock));
        assertEquals("bad arg", ex.getMessage());
    }

    @Test
    void toString_converterThrowsRuntimeException_propagates() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(CharBuffer.class))).thenThrow(new RuntimeException("runtime"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CharBufferConversions.toString("any", mock));
        assertEquals("runtime", ex.getMessage());
    }

    @Test
    void toString_converterThrowsNullPointerException_propagates() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(CharBuffer.class))).thenThrow(new NullPointerException("null inside"));
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CharBufferConversions.toString("any", mock));
        assertEquals("null inside", ex.getMessage());
    }
}
