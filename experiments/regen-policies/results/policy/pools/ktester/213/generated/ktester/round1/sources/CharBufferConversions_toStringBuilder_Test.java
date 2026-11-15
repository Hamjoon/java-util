package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toStringBuilder_Test {

    /**
     * Converter used for all test cases.
     */
    private static Converter converter;

    /**
     * A real Converter instance for the cases that do not need a mock.
     */
    private static Converter realConverter;

    // ---------- Custom object with a mock Converter ----------
    static final class MyCustomObject {

        private final String value;

        MyCustomObject(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter with default options is sufficient for simple conversions
        // (String, CharBuffer, char[], StringBuilder, etc.).
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long‑running test suites.
        converter = null;
    }

    @Test
    @DisplayName("toStringBuilder returns a StringBuilder with the same characters when the source is a CharBuffer")
    void toStringBuilder_givenCharBuffer_returnsStringBuilderWithSameContent() {
        // given
        CharBuffer source = CharBuffer.wrap("hello");
        // when
        StringBuilder result = CharBufferConversions.toStringBuilder(source, converter);
        // then
        assertNotNull(result, "Resulting StringBuilder should not be null");
        assertEquals("hello", result.toString(), "StringBuilder content should match the CharBuffer source");
    }

    @Test
    @DisplayName("toStringBuilder converts a String to CharBuffer and builds the expected StringBuilder")
    void toStringBuilder_givenString_convertsAndBuildsStringBuilder() {
        // given
        String source = "world";
        // when
        StringBuilder result = CharBufferConversions.toStringBuilder(source, converter);
        // then
        assertNotNull(result, "Resulting StringBuilder should not be null");
        assertEquals("world", result.toString(), "StringBuilder content should match the original String");
    }

    @Test
    @DisplayName("toStringBuilder returns an empty StringBuilder when the source is null")
    void toStringBuilder_givenNull_returnsEmptyStringBuilder() {
        // given
        Object source = null;
        // when
        StringBuilder result = CharBufferConversions.toStringBuilder(source, converter);
        // then
        assertNotNull(result, "Resulting StringBuilder should not be null even for null input");
        assertEquals("", result.toString(), "StringBuilder should be empty for null input");
    }

    // ---------- Char array ----------
    @Test
    @DisplayName("Char array is correctly converted to a StringBuilder")
    void toStringBuilder_givenCharArray_returnsStringBuilder() {
        char[] source = new char[] { 'J', 'a', 'v', 'a' };
        StringBuilder result = CharBufferConversions.toStringBuilder(source, realConverter);
        assertEquals("Java", result.toString());
    }

    // ---------- Empty inputs ----------
    @Test
    @DisplayName("Empty CharBuffer yields an empty StringBuilder")
    void toStringBuilder_givenEmptyCharBuffer_returnsEmptyStringBuilder() {
        CharBuffer source = CharBuffer.allocate(0);
        StringBuilder result = CharBufferConversions.toStringBuilder(source, realConverter);
        assertEquals("", result.toString());
    }

    @Test
    @DisplayName("Empty String yields an empty StringBuilder")
    void toStringBuilder_givenEmptyString_returnsEmptyStringBuilder() {
        String source = "";
        StringBuilder result = CharBufferConversions.toStringBuilder(source, realConverter);
        assertEquals("", result.toString());
    }

    @Test
    @DisplayName("Empty char[] yields an empty StringBuilder")
    void toStringBuilder_givenEmptyCharArray_returnsEmptyStringBuilder() {
        char[] source = new char[0];
        StringBuilder result = CharBufferConversions.toStringBuilder(source, realConverter);
        assertEquals("", result.toString());
    }

    // ---------- Unicode characters ----------
    @Test
    @DisplayName("Unicode surrogate pairs are preserved")
    void toStringBuilder_givenUnicodeString_preservesCharacters() {
        // 😀🎉
        String source = "\uD83D\uDE00\uD83C\uDF89";
        StringBuilder result = CharBufferConversions.toStringBuilder(source, realConverter);
        assertEquals("😀🎉", result.toString());
    }

    // ---------- StringBuilder input ----------
    @Test
    @DisplayName("StringBuilder input is treated like a CharSequence")
    void toStringBuilder_givenStringBuilder_returnsEquivalentStringBuilder() {
        StringBuilder source = new StringBuilder("BuilderText");
        StringBuilder result = CharBufferConversions.toStringBuilder(source, realConverter);
        assertEquals("BuilderText", result.toString());
    }

    // ---------- Large input ----------
    @Test
    @DisplayName("Large String (10,000 characters) is processed correctly")
    void toStringBuilder_givenLargeString_returnsLargeStringBuilder() {
        String source = String.join("", java.util.Collections.nCopies(10_000, "x"));
        StringBuilder result = CharBufferConversions.toStringBuilder(source, realConverter);
        assertEquals(10_000, result.length());
        assertTrue(result.toString().chars().allMatch(ch -> ch == 'x'));
    }

    // ---------- Byte array with a mock Converter ----------
    @Test
    @DisplayName("Byte[] conversion uses provided Converter (UTF‑8) to produce expected StringBuilder")
    void toStringBuilder_givenByteArrayWithConverter_returnsStringBuilder(@Mock Converter mockConverter) {
        // "Hello"
        byte[] source = new byte[] { 72, 101, 108, 108, 111 };
        when(mockConverter.convert(eq(source), eq(CharBuffer.class))).thenReturn(CharBuffer.wrap("Hello"));
        StringBuilder result = CharBufferConversions.toStringBuilder(source, mockConverter);
        assertEquals("Hello", result.toString());
        verify(mockConverter).convert(eq(source), eq(CharBuffer.class));
    }

    // ---------- Byte array without a Converter (should fail) ----------
    @Test
    @DisplayName("Byte[] without a Converter throws IllegalArgumentException")
    void toStringBuilder_givenByteArrayWithoutConverter_throws() {
        byte[] source = new byte[] { 65, 66, 67 };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toStringBuilder(source, null));
        assertTrue(ex.getMessage().toLowerCase().contains("converter"));
    }

    // ---------- ByteBuffer with a mock Converter ----------
    @Test
    @DisplayName("ByteBuffer conversion uses provided Converter (UTF‑8) to produce expected StringBuilder")
    void toStringBuilder_givenByteBufferWithConverter_returnsStringBuilder(@Mock Converter mockConverter) {
        // "☃"
        ByteBuffer source = ByteBuffer.wrap(new byte[] { (byte) 0xE2, (byte) 0x98, (byte) 0x83 });
        when(mockConverter.convert(eq(source), eq(CharBuffer.class))).thenReturn(CharBuffer.wrap("☃"));
        StringBuilder result = CharBufferConversions.toStringBuilder(source, mockConverter);
        assertEquals("☃", result.toString());
        verify(mockConverter).convert(eq(source), eq(CharBuffer.class));
    }

    // ---------- ByteBuffer without a Converter (should fail) ----------
    @Test
    @DisplayName("ByteBuffer without a Converter throws IllegalArgumentException")
    void toStringBuilder_givenByteBufferWithoutConverter_throws() {
        ByteBuffer source = ByteBuffer.wrap(new byte[] { 0x41, 0x42, 0x43 });
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toStringBuilder(source, null));
        assertTrue(ex.getMessage().toLowerCase().contains("converter"));
    }

    @Test
    @DisplayName("Custom object conversion uses provided Converter")
    void toStringBuilder_givenCustomObjectWithConverter_returnsStringBuilder(@Mock Converter mockConverter) {
        MyCustomObject source = new MyCustomObject("Custom");
        when(mockConverter.convert(eq(source), eq(CharBuffer.class))).thenReturn(CharBuffer.wrap(source.getValue()));
        StringBuilder result = CharBufferConversions.toStringBuilder(source, mockConverter);
        assertEquals("Custom", result.toString());
        verify(mockConverter).convert(eq(source), eq(CharBuffer.class));
    }

    // ---------- Converter returns null ----------
    @Test
    @DisplayName("Converter returning null leads to IllegalArgumentException")
    void toStringBuilder_converterReturnsNull_throws() {
        byte[] source = new byte[] { 65, 66, 67 };
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(source), eq(CharBuffer.class))).thenReturn(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toStringBuilder(source, mockConverter));
        assertTrue(ex.getMessage().toLowerCase().contains("null"));
    }

    // ---------- Unsupported source type ----------
    @Test
    @DisplayName("Unsupported source type throws IllegalArgumentException")
    void toStringBuilder_givenUnsupportedSource_throws() {
        java.util.Date source = new java.util.Date();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toStringBuilder(source, realConverter));
        assertTrue(ex.getMessage().toLowerCase().contains("unsupported"));
    }

    // ---------- Null source object ----------
    @Test
    @DisplayName("Null source object throws NullPointerException")
    void toStringBuilder_givenNullSource_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toStringBuilder(null, realConverter));
    }
}
