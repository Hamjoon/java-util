package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toStringBuffer_Test {

    private static Stream<Arguments> defaultConversionProvider() {
        return Stream.of(// null input → "null"
        Arguments.of("null input yields \"null\"", null, "null"), // plain String
        Arguments.of("plain String is unchanged", "plain string", "plain string"), // byte[] → UTF‑8
        Arguments.of("byte[] {65,66,67} → \"ABC\"", new byte[] { 65, 66, 67 }, "ABC"), // ByteBuffer → remaining bytes as UTF‑8
        Arguments.of("ByteBuffer wrapping \"DEF\" → \"DEF\"", ByteBuffer.wrap(new byte[] { 68, 69, 70 }), "DEF"), // char[] → String
        Arguments.of("char[] {'G','H','I'} → \"GHI\"", new char[] { 'G', 'H', 'I' }, "GHI"), // CharBuffer → remaining chars
        Arguments.of("CharBuffer wrapping \"JKL\" → \"JKL\"", CharBuffer.wrap("JKL"), "JKL"), // arbitrary object with custom toString()
        Arguments.of("Object with overridden toString() → \"custom\"", new Object() {

            @Override
            public String toString() {
                return "custom";
            }
        }, "custom"), // empty String
        Arguments.of("empty String → \"\"", "", ""), // empty byte[]
        Arguments.of("empty byte[] → \"\"", new byte[0], ""), // ByteBuffer with non‑zero position (skip first byte)
        Arguments.of("ByteBuffer positioned at 1 → \"YZ\"", ByteBuffer.wrap(new byte[] { 'X', 'Y', 'Z' }).position(1), "YZ"), // CharBuffer positioned at 2 → only last char
        Arguments.of("CharBuffer positioned at 2 → \"O\"", CharBuffer.wrap("MNO").position(2), "O"), // StringBuilder implements CharSequence
        Arguments.of("StringBuilder \"PQR\" → \"PQR\"", new StringBuilder("PQR"), "PQR"), // Integer → its toString()
        Arguments.of("Integer 12345 → \"12345\"", Integer.valueOf(12345), "12345"), // Boolean → \"true\"
        Arguments.of("Boolean.TRUE → \"true\"", Boolean.TRUE, "true"), // empty char[]
        Arguments.of("empty char[] → \"\"", new char[0], ""), // Unicode surrogate pair (emoji)
        Arguments.of("String with emoji → same emoji", "unicode \uD83D\uDE00", "unicode 😀"), // empty ByteBuffer
        Arguments.of("empty ByteBuffer → \"\"", ByteBuffer.allocate(0), ""), // CharBuffer filled via put/flip
        Arguments.of("CharBuffer put/flip → \"XYZ\"", CharBuffer.allocate(5).put("XYZ").flip(), "XYZ"), // multi‑byte UTF‑8 character (é)
        Arguments.of("byte[] {0xC3,0xA9} → \"é\"", new byte[] { (byte) 0xC3, (byte) 0xA9 }, "é"), // non‑printable bytes (control characters)
        Arguments.of("byte[] {0x01,0x02,0x03} → control chars", new byte[] { 0x01, 0x02, 0x03 }, new String(new byte[] { 0x01, 0x02, 0x03 }, StandardCharsets.UTF_8)), // empty CharBuffer (capacity 0)
        Arguments.of("empty CharBuffer (capacity 0) → \"\"", CharBuffer.allocate(0).flip(), ""));
    }

    /**
     * Verifies that {@code toStringBuffer} returns a {@link StringBuffer}
     * whose content equals the string returned by {@code toString}.
     */
    @Test
    void toStringBuffer_returnsStringBufferWithConvertedString() {
        // GIVEN
        // arbitrary input
        Object input = new byte[] { 0x41, 0x42, 0x43 };
        Converter mockConverter = mock(Converter.class);
        // Simulate the behaviour of ByteArrayConversions.toString(...):
        // it will ask the converter to produce a String representation.
        when(mockConverter.convert(eq(input), eq(String.class))).thenReturn("ABC");
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(input, mockConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer should not be null");
        assertEquals("ABC", result.toString(), "StringBuffer should contain the string returned by the converter");
        // Verify that the converter was asked to convert the original object to a String.
        verify(mockConverter, times(1)).convert(eq(input), eq(String.class));
    }

    /**
     * Ensures that a {@code null} input yields a {@link StringBuffer} containing {@code "null"}
     * (the default behaviour of {@code String.valueOf(null)} used inside the conversion).
     */
    @Test
    void toStringBuffer_withNullInput_returnsStringBufferContainingNullString() {
        // GIVEN
        Object input = null;
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(isNull(), eq(String.class))).thenReturn("null");
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(input, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals("null", result.toString());
        verify(mockConverter, times(1)).convert(isNull(), eq(String.class));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("defaultConversionProvider")
    void toStringBuffer_defaultConversion(String description, Object input, String expected) {
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(input, null);
        // THEN
        assertNotNull(result, description);
        assertEquals(expected, result.toString(), description);
    }

    @Test
    void toStringBuffer_converterOverridesDefault() {
        // GIVEN
        Object input = "any";
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(input), eq(String.class))).thenReturn("converted");
        // WHEN
        StringBuffer result = ByteArrayConversions.toStringBuffer(input, mockConverter);
        // THEN
        assertEquals("converted", result.toString());
        verify(mockConverter, times(1)).convert(eq(input), eq(String.class));
    }

    @Test
    void toStringBuffer_converterThrowsIllegalArgumentException() {
        // GIVEN
        Object input = new byte[] { 0, (byte) 0xFF };
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(input), eq(String.class))).thenThrow(new IllegalArgumentException("invalid"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ByteArrayConversions.toStringBuffer(input, mockConverter));
        assertEquals("invalid", ex.getMessage());
    }

    @Test
    void toStringBuffer_converterThrowsRuntimeException() {
        // GIVEN
        Object input = new Object();
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(input), eq(String.class))).thenThrow(new RuntimeException("boom"));
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ByteArrayConversions.toStringBuffer(input, mockConverter));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toStringBuffer_converterReturnsNull_causesNPE() {
        // GIVEN
        Object input = "any";
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(input), eq(String.class))).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toStringBuffer(input, mockConverter));
    }

    @Test
    void toStringBuffer_converterReturnsNonString_causesClassCastException() {
        // GIVEN
        Object input = "any";
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(input), eq(String.class))).thenReturn(Integer.valueOf(5));
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ByteArrayConversions.toStringBuffer(input, mockConverter));
    }

    @Test
    void toStringBuffer_sourceToStringThrowsException() {
        // GIVEN
        Object badObject = new Object() {

            @Override
            public String toString() {
                throw new IllegalStateException("bad");
            }
        };
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ByteArrayConversions.toStringBuffer(badObject, null));
        assertEquals("bad", ex.getMessage());
    }

    @Test
    void toStringBuffer_nullInput_converterReturnsNull_causesNPE() {
        // GIVEN
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(isNull(), eq(String.class))).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toStringBuffer(null, mockConverter));
    }
}
