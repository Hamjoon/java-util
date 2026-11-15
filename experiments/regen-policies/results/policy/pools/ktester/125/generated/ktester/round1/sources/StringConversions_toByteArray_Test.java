package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Stream;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toByteArray_Test {

    private static Converter converter;

    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Default converter uses UTF‑8 charset (the default in ConverterOptions)
        ConverterOptions options = new ConverterOptions();
        defaultConverter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference
        converter = null;
    }

    private static Stream<Arguments> utf8StringProvider() {
        return Stream.of(Arguments.of("ASCII string", "Hello World", "Hello World".getBytes(StandardCharsets.UTF_8)), Arguments.of("String with é", "Café", "Café".getBytes(StandardCharsets.UTF_8)), Arguments.of("Emoji 😀", "😀", "😀".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void toByteArray_returnsEmptyArrayWhenNullInput() {
        // given
        Object input = null;
        // when
        byte[] result = StringConversions.toByteArray(input, defaultConverter);
        // then
        assertArrayEquals(EMPTY_BYTE_ARRAY, result, "Converting a null value should return the constant EMPTY_BYTE_ARRAY");
    }

    @Test
    void toByteArray_returnsEmptyArrayWhenEmptyString() {
        // given
        Object input = "";
        // when
        byte[] result = StringConversions.toByteArray(input, defaultConverter);
        // then
        assertArrayEquals(EMPTY_BYTE_ARRAY, result, "Converting an empty string should return the constant EMPTY_BYTE_ARRAY");
    }

    @Test
    void toByteArray_convertsStringUsingConverterCharset() {
        // given
        // contains a non‑ASCII character to verify charset handling
        String text = "Café";
        Object input = text;
        // when
        byte[] result = StringConversions.toByteArray(input, converter);
        // then
        // The Converter's default charset is UTF‑8, so compare against text.getBytes(StandardCharsets.UTF_8)
        byte[] expected = text.getBytes(converter.getOptions().getCharset());
        assertArrayEquals(expected, result, "The byte array should be produced using the charset from ConverterOptions");
    }

    @ParameterizedTest(name = "[{index}] UTF‑8 conversion of {0}")
    @MethodSource("utf8StringProvider")
    void toByteArray_utf8StringConversion(String description, String source, byte[] expected) {
        byte[] result = StringConversions.toByteArray(source, defaultConverter);
        assertArrayEquals(expected, result, description);
    }

    @Test
    void toByteArray_iso88591Encoding_ofCafe() {
        // given a converter that uses ISO‑8859‑1
        Converter isoConverter = new Converter(new ConverterOptions(Charset.forName("ISO-8859-1")));
        String source = "Café";
        // when
        byte[] result = StringConversions.toByteArray(source, isoConverter);
        // then
        byte[] expected = source.getBytes(Charset.forName("ISO-8859-1"));
        assertArrayEquals(expected, result, "ISO‑8859‑1 should encode é as a single byte 0xE9");
    }

    @Test
    void toByteArray_iso88591Encoding_ofEuroSign_replacedByQuestionMark() {
        // given a converter that uses ISO‑8859‑1
        Converter isoConverter = new Converter(new ConverterOptions(Charset.forName("ISO-8859-1")));
        // Euro sign is not representable in ISO‑8859‑1
        String source = "€";
        // when
        byte[] result = StringConversions.toByteArray(source, isoConverter);
        // then
        // The default replacement character for unmappable characters is '?', i.e., 0x3F
        byte[] expected = source.getBytes(Charset.forName("ISO-8859-1"));
        assertArrayEquals(expected, result, "Unrepresentable characters should be replaced by '?' (0x3F) in ISO‑8859‑1");
    }

    @Test
    void toByteArray_integerConvertedToStringBytes() {
        Integer source = 12345;
        byte[] result = StringConversions.toByteArray(source, defaultConverter);
        byte[] expected = "12345".getBytes(defaultConverter.getOptions().getCharset());
        assertArrayEquals(expected, result, "Integer should be converted via its toString() representation");
    }

    @Test
    void toByteArray_booleanConvertedToStringBytes() {
        Boolean source = true;
        byte[] result = StringConversions.toByteArray(source, defaultConverter);
        byte[] expected = "true".getBytes(defaultConverter.getOptions().getCharset());
        assertArrayEquals(expected, result, "Boolean should be converted via its toString() representation");
    }

    @Test
    void toByteArray_uuidConvertedToStringBytes() {
        UUID source = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        byte[] result = StringConversions.toByteArray(source, defaultConverter);
        byte[] expected = source.toString().getBytes(defaultConverter.getOptions().getCharset());
        assertArrayEquals(expected, result, "UUID should be converted to its canonical string representation");
    }

    @Test
    void toByteArray_largeStringOf1000As() {
        StringBuilder sb = new StringBuilder(1000);
        for (int i = 0; i < 1000; i++) sb.append('a');
        String source = sb.toString();
        byte[] result = StringConversions.toByteArray(source, defaultConverter);
        assertEquals(1000, result.length, "Resulting byte array should have length 1000");
        byte expectedByte = (byte) 'a';
        for (byte b : result) {
            assertEquals(expectedByte, b, "Each byte should equal ASCII 'a' (0x61)");
        }
    }

    @Test
    void toByteArray_nullConverter_throwsNullPointerException() {
        Object source = "test";
        Converter nullConverter = null;
        assertThrows(NullPointerException.class, () -> StringConversions.toByteArray(source, nullConverter), "Passing a null Converter should result in NPE");
    }

    @Test
    void toByteArray_converterWithNullOptions_throwsNullPointerException() {
        // Mock a Converter whose getOptions() returns null
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(null);
        Object source = "test";
        assertThrows(NullPointerException.class, () -> StringConversions.toByteArray(source, mockConverter), "Converter with null options should cause NPE when charset is accessed");
    }
}
