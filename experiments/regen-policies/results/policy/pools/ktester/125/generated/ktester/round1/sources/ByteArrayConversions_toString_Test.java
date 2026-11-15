package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toString_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mockConverter to return a mockOptions instance
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // By default, use UTF‑8 charset unless a test overrides it
        when(mockOptions.getCharset()).thenReturn(StandardCharsets.UTF_8);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions after each test
        clearInvocations(mockConverter, mockOptions);
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// UTF‑16LE BOM → ZERO WIDTH NO‑BREAK SPACE (U+FEFF)
        Arguments.of(new byte[] { (byte) 0xFF, (byte) 0xFE }, StandardCharsets.UTF_16LE, "\uFEFF"), // US‑ASCII cannot represent 0x80 → replacement character '?'
        Arguments.of(new byte[] { (byte) 0x80 }, StandardCharsets.US_ASCII, "?"), // UTF‑16LE encoding of "AB"
        Arguments.of(new byte[] { 0x41, 0x00, 0x42, 0x00 }, StandardCharsets.UTF_16LE, "AB"), // UTF‑16BE encoding of "AB"
        Arguments.of(new byte[] { 0x00, 0x41, 0x00, 0x42 }, StandardCharsets.UTF_16BE, "AB"), // Euro sign in UTF‑8
        Arguments.of(new byte[] { (byte) 0xE2, (byte) 0x82, (byte) 0xAC }, StandardCharsets.UTF_8, "€"), // Same bytes interpreted as ISO‑8859‑1 → "â‚¬"
        Arguments.of(new byte[] { (byte) 0xE2, (byte) 0x82, (byte) 0xAC }, StandardCharsets.ISO_8859_1, "â‚¬"), // Simple ASCII "ABC"
        Arguments.of(new byte[] { 0x41, 0x42, 0x43 }, StandardCharsets.US_ASCII, "ABC"), // Mixed bytes in ISO‑8859‑1 → "Aÿ"
        Arguments.of(new byte[] { 0x41, (byte) 0xFF }, StandardCharsets.ISO_8859_1, "Aÿ"));
    }

    @Test
    void toString_NullInput_ReturnsEmptyString() {
        // given
        Object from = null;
        // when
        String result = ByteArrayConversions.toString(from, mockConverter);
        // then
        assertEquals(StringUtilities.EMPTY, result);
        // No charset lookup should be performed when the input is null
        verify(mockConverter, never()).getOptions();
    }

    @Test
    void toString_EmptyByteArray_ReturnsEmptyString() {
        // given
        byte[] empty = new byte[0];
        Object from = empty;
        // when
        String result = ByteArrayConversions.toString(from, mockConverter);
        // then
        assertEquals("", result);
        verify(mockConverter).getOptions();
        verify(mockOptions).getCharset();
    }

    @Test
    void toString_AsciiBytes_UsesDefaultCharset() {
        // given
        byte[] bytes = "Hello".getBytes(StandardCharsets.UTF_8);
        Object from = bytes;
        // when
        String result = ByteArrayConversions.toString(from, mockConverter);
        // then
        assertEquals("Hello", result);
        verify(mockConverter).getOptions();
        verify(mockOptions).getCharset();
    }

    @Test
    void toString_NonAsciiBytes_WithUtf8Charset() {
        // given
        String original = "Café 🌟";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        Object from = bytes;
        when(mockOptions.getCharset()).thenReturn(StandardCharsets.UTF_8);
        // when
        String result = ByteArrayConversions.toString(from, mockConverter);
        // then
        assertEquals(original, result);
        verify(mockConverter).getOptions();
        verify(mockOptions).getCharset();
    }

    @Test
    void toString_NonAsciiBytes_WithIsoCharset() {
        // given
        // The string contains characters that are representable in ISO‑8859‑1
        String original = "Café";
        byte[] bytes = original.getBytes(StandardCharsets.ISO_8859_1);
        Object from = bytes;
        when(mockOptions.getCharset()).thenReturn(StandardCharsets.ISO_8859_1);
        // when
        String result = ByteArrayConversions.toString(from, mockConverter);
        // then
        assertEquals(original, result);
        verify(mockConverter).getOptions();
        verify(mockOptions).getCharset();
    }

    @ParameterizedTest(name = "[{index}] charset={1}, expected=\"{2}\"")
    @MethodSource("provideSuccessfulConversions")
    void toString_SuccessfulConversions(byte[] bytes, Charset charset, String expected) {
        // Arrange
        when(mockOptions.getCharset()).thenReturn(charset);
        // Act
        String result = ByteArrayConversions.toString(bytes, mockConverter);
        // Assert
        assertEquals(expected, result);
        verify(mockConverter).getOptions();
        verify(mockOptions).getCharset();
    }

    @Test
    void toString_NonByteArray_ThrowsClassCastException() {
        // given a non‑byte[] object
        Object from = new String("test");
        // when / then
        assertThrows(ClassCastException.class, () -> ByteArrayConversions.toString(from, mockConverter));
        // Converter interaction is irrelevant – the cast fails first
        verify(mockConverter, never()).getOptions();
    }

    @Test
    void toString_NullConverter_ThrowsNullPointerException() {
        // given a valid byte array but a null converter
        byte[] bytes = new byte[] { 0x41 };
        // when / then
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toString(bytes, null));
        // No interaction with mocks because the argument itself is null
    }

    @Test
    void toString_ConverterOptionsNull_ThrowsNullPointerException() {
        // given a converter that returns null options
        when(mockConverter.getOptions()).thenReturn(null);
        byte[] bytes = new byte[] { 0x41 };
        // when / then
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toString(bytes, mockConverter));
        verify(mockConverter).getOptions();
    }

    @Test
    void toString_CharsetNull_ThrowsNullPointerException() {
        // given options that return a null charset
        when(mockOptions.getCharset()).thenReturn(null);
        byte[] bytes = new byte[] { 0x41 };
        // when / then
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toString(bytes, mockConverter));
        verify(mockConverter).getOptions();
        verify(mockOptions).getCharset();
    }
}
