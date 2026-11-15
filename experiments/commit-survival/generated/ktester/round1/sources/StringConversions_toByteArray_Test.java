package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toByteArray_Test {

    @Mock
    private Converter // mocked Converter
    mockConverter;

    @Mock
    private ConverterOptions // mocked ConverterOptions
    mockOptions;

    /**
     * Minimal {@link ConverterOptions} implementation exposing only the charset.
     */
    private static class SimpleOptions implements ConverterOptions {

        private final Charset charset;

        SimpleOptions(Charset charset) {
            this.charset = charset;
        }

        @Override
        public Charset getCharset() {
            return charset;
        }
        // All other methods of ConverterOptions (if any) are left with default
        // implementations or can be added as needed for compilation.
    }

    /**
     * Minimal {@link Converter} implementation that returns the supplied
     * {@link ConverterOptions}.  This is sufficient for the tests that need a
     * concrete instance rather than a Mockito mock.
     */
    private static class SimpleConverter implements Converter {

        private final ConverterOptions options;

        SimpleConverter(ConverterOptions options) {
            this.options = options;
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }
        // Other methods of Converter are not required for the toByteArray tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy so that converter.getOptions().getCharset()
        // returns a known charset (UTF‑8) for all test cases.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getCharset()).thenReturn(StandardCharsets.UTF_8);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        reset(mockConverter, mockOptions);
    }

    /**
     * Provides a mixture of non‑String inputs together with the expected byte
     * representation when encoded with UTF‑8 (the charset supplied by the mock
     * converter).
     */
    private static Stream<Arguments> nonStringInputs() {
        return Stream.of(// "123"
        Arguments.of(123, new byte[] { 49, 50, 51 }), Arguments.of(true, "true".getBytes(StandardCharsets.UTF_8)), Arguments.of(new StringBuilder("abc"), "abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void toByteArray_NullInput_ReturnsEmptyArray() {
        byte[] result = StringConversions.toByteArray(null, mockConverter);
        assertArrayEquals(EMPTY_BYTE_ARRAY, result, "Converting a null value should yield the constant EMPTY_BYTE_ARRAY");
    }

    @Test
    void toByteArray_EmptyString_ReturnsEmptyArray() {
        byte[] result = StringConversions.toByteArray("", mockConverter);
        assertArrayEquals(EMPTY_BYTE_ARRAY, result, "Converting an empty string should yield the constant EMPTY_BYTE_ARRAY");
    }

    @Test
    void toByteArray_NonEmptyString_ReturnsBytesInConverterCharset() {
        String source = "Cedar";
        // matches the stubbed charset
        Charset charset = StandardCharsets.UTF_8;
        byte[] expected = source.getBytes(charset);
        byte[] actual = StringConversions.toByteArray(source, mockConverter);
        assertArrayEquals(expected, actual, "The byte array should be the UTF‑8 representation of the source string");
    }

    @Test
    void toByteArray_ObjectThatConvertsToString_ReturnsBytes() {
        // The implementation of asString(Object) treats non‑String objects via
        // String.valueOf(...). We can verify that behaviour with a simple Integer.
        Integer source = 12345;
        Charset charset = StandardCharsets.UTF_8;
        byte[] expected = source.toString().getBytes(charset);
        byte[] actual = StringConversions.toByteArray(source, mockConverter);
        assertArrayEquals(expected, actual, "Non‑String objects should be converted via asString and then encoded");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("nonStringInputs")
    void toByteArray_NonStringObjects_ReturnsCorrectBytes(Object from, byte[] expected) {
        byte[] actual = StringConversions.toByteArray(from, mockConverter);
        assertArrayEquals(expected, actual, () -> "Expected UTF‑8 bytes for input [" + from + "]");
    }

    @Test
    void toByteArray_NullConverter_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringConversions.toByteArray("sample", null), "Passing a null Converter should result in NPE when getOptions() is invoked");
    }

    @Test
    void toByteArray_ConverterWithNullOptions_ThrowsNullPointerException() {
        Converter converterWithNullOptions = new SimpleConverter(null);
        assertThrows(NullPointerException.class, () -> StringConversions.toByteArray("sample", converterWithNullOptions), "A Converter that returns null for getOptions() should cause NPE");
    }

    @Test
    void toByteArray_ConverterWithNullCharset_ThrowsNullPointerException() {
        ConverterOptions optionsWithNullCharset = new SimpleOptions(null);
        Converter converter = new SimpleConverter(optionsWithNullCharset);
        assertThrows(NullPointerException.class, () -> StringConversions.toByteArray("sample", converter), "A Converter whose options return null charset should cause NPE when getBytes(null) is called");
    }
}
