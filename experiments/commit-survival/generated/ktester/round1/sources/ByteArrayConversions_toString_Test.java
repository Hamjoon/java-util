package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.StringUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.Charset;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toString_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
        // Default behaviour: UTF‑8 charset for all tests unless overridden.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getCharset()).thenReturn(Charset.forName("UTF-8"));
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito open mocks to avoid memory leaks.
        mocks.close();
    }

    /**
     * Supplies arguments that are not {@code byte[]} to provoke a {@link ClassCastException}.
     */
    private static Stream<Object> nonByteArrayInputs() {
        return Stream.of(// a plain String
        "Hello", // an int[] array
        new int[] { 1, 2, 3 });
    }

    @Test
    @DisplayName("toString returns empty string when input byte array is null")
    void toString_NullByteArray_ReturnsEmptyString() {
        // GIVEN
        byte[] input = null;
        // WHEN
        String result = ByteArrayConversions.toString(input, mockConverter);
        // THEN
        assertEquals(StringUtilities.EMPTY, result, "Expected an empty string when the source byte array is null");
    }

    @Test
    @DisplayName("toString converts a UTF‑8 encoded byte array to the correct String")
    void toString_ValidUtf8Bytes_ReturnsExpectedString() {
        // GIVEN
        String expected = "Hello, world!";
        byte[] input = expected.getBytes(Charset.forName("UTF-8"));
        // WHEN
        String result = ByteArrayConversions.toString(input, mockConverter);
        // THEN
        assertEquals(expected, result, "The resulting string should match the original UTF‑8 content");
    }

    @Test
    @DisplayName("toString respects the charset supplied by ConverterOptions")
    void toString_DifferentCharset_RespectsCharset() {
        // GIVEN
        // Use ISO‑8859‑1 charset which maps the byte 0xE9 to 'é'
        Charset iso = Charset.forName("ISO-8859-1");
        when(mockOptions.getCharset()).thenReturn(iso);
        // 'é' in ISO‑8859‑1
        byte[] input = { (byte) 0xE9 };
        String expected = new String(input, iso);
        // WHEN
        String result = ByteArrayConversions.toString(input, mockConverter);
        // THEN
        assertEquals(expected, result, "The conversion should use the charset provided by ConverterOptions");
    }

    @Test
    @DisplayName("toString returns empty string for an empty byte array")
    void toString_EmptyByteArray_ReturnsEmptyString() {
        // GIVEN
        // empty array
        byte[] input = new byte[0];
        // WHEN
        String result = ByteArrayConversions.toString(input, mockConverter);
        // THEN
        // An empty array should produce an empty string (not null).
        assertEquals(StringUtilities.EMPTY, result, "Empty byte array must result in an empty string");
    }

    @Test
    @DisplayName("toString correctly decodes UTF‑16BE multibyte characters")
    void toString_Utf16BeMultibyte_ReturnsExpectedString() {
        // GIVEN
        Charset utf16be = Charset.forName("UTF-16BE");
        when(mockOptions.getCharset()).thenReturn(utf16be);
        // Bytes 0x00 0x41 0x00 0x42 represent "AB" in UTF‑16BE.
        byte[] input = new byte[] { 0x00, 0x41, 0x00, 0x42 };
        String expected = new String(input, utf16be);
        // WHEN
        String result = ByteArrayConversions.toString(input, mockConverter);
        // THEN
        assertEquals(expected, result, "UTF‑16BE charset should correctly produce the string \"AB\"");
    }

    @ParameterizedTest(name = "ClassCastException when from is {0}")
    @MethodSource("nonByteArrayInputs")
    @DisplayName("toString throws ClassCastException for non‑byte[] inputs")
    void toString_NonByteArray_ThrowsClassCastException(Object invalidFrom) {
        // GIVEN
        // mockConverter already configured in @BeforeEach
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ByteArrayConversions.toString(invalidFrom, mockConverter), "Expected ClassCastException when 'from' is not a byte[]");
    }

    @Test
    @DisplayName("toString throws NullPointerException when converter is null")
    void toString_NullConverter_ThrowsNullPointerException() {
        // GIVEN
        byte[] input = new byte[] { 65, 66 };
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toString(input, null), "A null converter should cause NullPointerException");
    }

    @Test
    @DisplayName("toString throws NullPointerException when converter.getOptions() returns null")
    void toString_NullOptions_ThrowsNullPointerException() {
        // GIVEN
        byte[] input = new byte[] { 65, 66 };
        // options are null
        when(mockConverter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toString(input, mockConverter), "Null options should cause NullPointerException when charset is accessed");
    }

    @Test
    @DisplayName("toString throws NullPointerException when options.getCharset() returns null")
    void toString_NullCharset_ThrowsNullPointerException() {
        // GIVEN
        byte[] input = new byte[] { 65, 66 };
        // charset is null
        when(mockOptions.getCharset()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toString(input, mockConverter), "Null charset should cause NullPointerException in the String constructor");
    }
}
