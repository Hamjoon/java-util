package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.util.Arrays;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toStringBuilder_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared default {@link Converter} instance used for the majority of tests.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no overrides needed for these simple conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    private static Stream<Arguments> positiveCasesProvider() {
        return Stream.of(// null source – the internal toString returns the literal "null"
        arguments(null, "null"), // byte array – ASCII/UTF‑8 decoding
        arguments(new byte[] { 65, 66, 67 }, "ABC"), // char array
        arguments(new char[] { 'x', 'y', 'z' }, "xyz"), // ByteBuffer
        arguments(ByteBuffer.wrap(new byte[] { 72, 105 }), "Hi"), // CharBuffer
        arguments(CharBuffer.wrap("Hello"), "Hello"), // plain String
        arguments("plain string", "plain string"), // Object with overridden toString()
        arguments(new Object() {

            @Override
            public String toString() {
                return "custom";
            }
        }, "custom"), // Empty byte array
        arguments(new byte[] {}, ""), // Empty char array
        arguments(new char[] {}, ""), // Empty String
        arguments("", ""), // StringBuilder (CharSequence)
        arguments(new StringBuilder("builder"), "builder"), // StringBuffer
        arguments(new StringBuffer("buffer"), "buffer"), // Integer wrapper
        arguments(Integer.valueOf(12345), "12345"), // CharBuffer with non‑zero position (slice from position to limit)
        arguments(CharBuffer.wrap("HelloWorld").position(5), "World"), // ByteBuffer with altered position/limit (slice)
        arguments(ByteBuffer.wrap(new byte[] { 65, 66, 67, 68 }).position(1).limit(3), "BC"), // Multi‑byte UTF‑8 character (é)
        arguments(new byte[] { (byte) 0xC3, (byte) 0xA9 }, "é"), // Empty ByteBuffer
        arguments(ByteBuffer.allocate(0), ""), // boolean array – falls back to Arrays.toString()
        arguments(new boolean[] { true, false }, Arrays.toString(new boolean[] { true, false })));
    }

    @Test
    void toStringBuilder_fromString_returnsBuilderWithSameContent() {
        String source = "sample string";
        StringBuilder sb = ByteArrayConversions.toStringBuilder(source, converter);
        assertNotNull(sb);
        assertEquals(source, sb.toString());
    }

    @Test
    void toStringBuilder_fromByteArray_returnsBuilderWithDecodedString() {
        byte[] bytes = "byte array".getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = ByteArrayConversions.toStringBuilder(bytes, converter);
        assertNotNull(sb);
        assertEquals("byte array", sb.toString());
    }

    @Test
    void toStringBuilder_fromByteBuffer_returnsBuilderWithDecodedString() {
        byte[] bytes = "byte buffer".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        StringBuilder sb = ByteArrayConversions.toStringBuilder(buffer, converter);
        assertNotNull(sb);
        assertEquals("byte buffer", sb.toString());
    }

    @Test
    void toStringBuilder_fromCharBuffer_returnsBuilderWithString() {
        CharBuffer charBuffer = CharBuffer.wrap("char buffer".toCharArray());
        StringBuilder sb = ByteArrayConversions.toStringBuilder(charBuffer, converter);
        assertNotNull(sb);
        assertEquals("char buffer", sb.toString());
    }

    @Test
    void toStringBuilder_fromInteger_returnsBuilderWithStringRepresentation() {
        Integer source = 12345;
        StringBuilder sb = ByteArrayConversions.toStringBuilder(source, converter);
        assertNotNull(sb);
        assertEquals("12345", sb.toString());
    }

    @Test
    void toStringBuilder_fromNull_throwsNullPointerException() {
        // The implementation of ByteArrayConversions.toString eventually returns null for a null source,
        // and StringBuilder's constructor does not accept null, so a NPE is expected.
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toStringBuilder(null, converter));
    }

    @ParameterizedTest(name = "{index}: from={0} → expected=\"{1}\"")
    @MethodSource("positiveCasesProvider")
    void toStringBuilder_positiveCases(Object from, String expected) {
        StringBuilder sb = ByteArrayConversions.toStringBuilder(from, null);
        assertNotNull(sb, "StringBuilder must not be null");
        assertEquals(expected, sb.toString(), () -> "Unexpected conversion result for input: " + from);
    }

    @Test
    void toStringBuilder_malformedUtf8_returnsReplacementChars() {
        // 0xF0 0x28 0x8C 0x28 is an illegal UTF‑8 sequence.
        byte[] malformed = new byte[] { (byte) 0xF0, 0x28, (byte) 0x8C, 0x28 };
        StringBuilder sb = ByteArrayConversions.toStringBuilder(malformed, null);
        assertNotNull(sb);
        // The exact representation depends on the charset's error handling;
        // the standard decoder substitutes U+FFFD for each malformed byte.
        String expected = "\uFFFD\uFFFD\uFFFD\uFFFD";
        assertEquals(expected, sb.toString(), "Malformed UTF‑8 should be replaced with the Unicode replacement character");
    }

    @Test
    void toStringBuilder_withMockConverter_returnsConvertedString() {
        Converter mock = mock(Converter.class);
        // The library’s Converter typically has a method `convert(Object, Class<T>)`.
        // We stub any conversion to return the constant "converted".
        when(mock.convert(any(), eq(String.class))).thenReturn("converted");
        StringBuilder sb = ByteArrayConversions.toStringBuilder("ignored", mock);
        assertNotNull(sb);
        assertEquals("converted", sb.toString(), "When a custom Converter is supplied, its result must be wrapped");
    }

    @Test
    void toStringBuilder_mockConverterReturnsNull_throwsNullPointerException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(String.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toStringBuilder("any", mock), "A null result from the Converter should cause NPE when StringBuilder is constructed");
    }

    @Test
    void toStringBuilder_mockConverterThrowsIllegalArgumentException_propagates() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(String.class))).thenThrow(new IllegalArgumentException("bad conversion"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ByteArrayConversions.toStringBuilder("trigger", mock));
        assertEquals("bad conversion", ex.getMessage(), "The original exception message must be preserved");
    }

    @Test
    void toStringBuilder_mockConverterReturnsNonString_throwsClassCastException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(String.class))).thenReturn(Integer.valueOf(123));
        assertThrows(ClassCastException.class, () -> ByteArrayConversions.toStringBuilder("any", mock), "Returning a non‑String should cause a ClassCastException before StringBuilder construction");
    }

    @Test
    void toStringBuilder_mockConverterReturnsStringBuilder_throwsClassCastException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(String.class))).thenReturn(new StringBuilder("sb"));
        assertThrows(ClassCastException.class, () -> ByteArrayConversions.toStringBuilder("any", mock), "Returning a StringBuilder instead of a String should cause a ClassCastException");
    }

    @Test
    void toStringBuilder_mockConverterThrowsNullPointerException_propagates() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(String.class))).thenThrow(new NullPointerException("null conv"));
        NullPointerException ex = assertThrows(NullPointerException.class, () -> ByteArrayConversions.toStringBuilder("any", mock));
        assertEquals("null conv", ex.getMessage(), "The original NullPointerException message must be preserved");
    }

    @Test
    void toStringBuilder_objectToStringThrowsException_propagates() {
        Object bad = new Object() {

            @Override
            public String toString() {
                throw new IllegalStateException("bad toString");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ByteArrayConversions.toStringBuilder(bad, null));
        assertEquals("bad toString", ex.getMessage(), "Exception from object's toString() must propagate unchanged");
    }

    @Test
    void toStringBuilder_objectToStringReturnsNull_throwsNullPointerException() {
        Object bad = new Object() {

            @Override
            public String toString() {
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toStringBuilder(bad, null), "A null result from toString() should cause NPE when constructing StringBuilder");
    }

    @Test
    void toStringBuilder_byteBufferPositionBeyondLimit_throwsIllegalArgumentException() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 65, 66, 67 });
        // set position > limit (limit defaults to capacity)
        buf.position(3).limit(2);
        assertThrows(IllegalArgumentException.class, () -> ByteArrayConversions.toStringBuilder(buf, null), "Invalid ByteBuffer state (position > limit) must cause IllegalArgumentException");
    }

    @Test
    void toStringBuilder_charBufferPositionBeyondLimit_throwsIllegalArgumentException() {
        CharBuffer cb = CharBuffer.wrap("abc");
        cb.position(3).limit(2);
        assertThrows(IllegalArgumentException.class, () -> ByteArrayConversions.toStringBuilder(cb, null), "Invalid CharBuffer state (position > limit) must cause IllegalArgumentException");
    }

    @Test
    void toStringBuilder_bothArgumentsNull_returnsBuilderWithLiteralNull() {
        // According to the specification, toString(null, null) yields the literal "null".
        StringBuilder sb = ByteArrayConversions.toStringBuilder(null, null);
        assertNotNull(sb);
        assertEquals("null", sb.toString(), "Both arguments null should result in a StringBuilder containing the literal \"null\"");
    }
}
