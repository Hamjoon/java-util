package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toByteArray_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private static final Charset TEST_CHARSET = StandardCharsets.UTF_8;

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock Converter to return a mock ConverterOptions
        // that supplies a deterministic charset.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getCharset()).thenReturn(TEST_CHARSET);
    }

    /**
     * Provides the test matrix described in the JSON specification.
     */
    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(// Null source – already covered, but kept for completeness (no exception)
        Arguments.of(null, StandardCharsets.UTF_8, EMPTY_BYTE_ARRAY, null, "Null source should yield EMPTY_BYTE_ARRAY (UTF‑8)"), // Empty string – any charset yields empty array
        Arguments.of("", StandardCharsets.UTF_8, EMPTY_BYTE_ARRAY, null, "Empty string should always yield EMPTY_BYTE_ARRAY (UTF‑8)"), // Simple ASCII string – UTF‑8
        Arguments.of("hello", StandardCharsets.UTF_8, new byte[] { 104, 101, 108, 108, 111 }, null, "ASCII string \"hello\" encoded with UTF‑8"), // Non‑ASCII é – UTF‑8
        Arguments.of("café", StandardCharsets.UTF_8, new byte[] { 99, 97, 102, (byte) 0xC3, (byte) 0xA9 }, null, "String \"café\" encoded with UTF‑8"), // Non‑ASCII é – ISO‑8859‑1
        Arguments.of("café", StandardCharsets.ISO_8859_1, new byte[] { 99, 97, 102, (byte) 0xE9 }, null, "String \"café\" encoded with ISO‑8859‑1"), // Integer 123 – UTF‑8
        Arguments.of(123, StandardCharsets.UTF_8, new byte[] { 49, 50, 51 }, null, "Integer 123 converted to \"123\" and encoded with UTF‑8"), // Boolean true – UTF‑8
        Arguments.of(true, StandardCharsets.UTF_8, new byte[] { 116, 114, 117, 101 }, null, "Boolean true becomes \"true\" and encoded with UTF‑8"), // ASCII‑only string – US‑ASCII
        Arguments.of("test", StandardCharsets.US_ASCII, new byte[] { 116, 101, 115, 116 }, null, "ASCII string \"test\" encoded with US‑ASCII"), // Emoji – UTF‑8 (4‑byte sequence)
        Arguments.of("😀", StandardCharsets.UTF_8, new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 }, null, "Emoji \"😀\" encoded with UTF‑8"), // Emoji – UTF‑16LE (surrogate pair, little‑endian)
        Arguments.of("😀", Charset.forName("UTF-16LE"), new byte[] { 0x3D, (byte) 0xD8, 0x00, (byte) 0xDE }, null, "Emoji \"😀\" encoded with UTF‑16LE"), // Euro sign – ISO‑8859‑1 (unrepresentable, becomes '?')
        Arguments.of("€", StandardCharsets.ISO_8859_1, new byte[] { 63 }, null, "Euro sign encoded with ISO‑8859‑1 becomes '?' (0x3F)"), // Euro sign – Windows‑1252 (0x80)
        Arguments.of("€", Charset.forName("windows-1252"), new byte[] { (byte) 0x80 }, null, "Euro sign encoded with Windows‑1252 becomes 0x80"), // Empty string – UTF‑16LE (still empty)
        Arguments.of("", Charset.forName("UTF-16LE"), EMPTY_BYTE_ARRAY, null, "Empty string always yields EMPTY_BYTE_ARRAY, even with UTF‑16LE"));
    }

    @Test
    void toByteArray_ReturnsEmptyArray_WhenSourceIsNull() {
        byte[] result = StringConversions.toByteArray(null, mockConverter);
        assertArrayEquals(EMPTY_BYTE_ARRAY, result, "When the source object is null the method should return EMPTY_BYTE_ARRAY");
    }

    @Test
    void toByteArray_ReturnsEmptyArray_WhenSourceIsEmptyString() {
        byte[] result = StringConversions.toByteArray("", mockConverter);
        assertArrayEquals(EMPTY_BYTE_ARRAY, result, "When the source string is empty the method should return EMPTY_BYTE_ARRAY");
    }

    @Test
    void toByteArray_ConvertsStringToBytes_UsingProvidedCharset() {
        // contains a non‑ASCII character to verify charset handling
        String source = "Café";
        byte[] expected = source.getBytes(DEFAULT_CHARSET);
        byte[] actual = StringConversions.toByteArray(source, mockConverter);
        assertArrayEquals(expected, actual, "The byte array should be the UTF‑8 representation of the source string");
    }

    @Test
    void toByteArray_ConvertsNonStringObjectViaAsString() {
        // The static asString method will turn the Integer into its String representation.
        Integer source = 42;
        String asString = StringConversions.asString(source);
        assertNotNull(asString, "asString should not return null for a non‑null Integer");
        byte[] expected = asString.getBytes(TEST_CHARSET);
        byte[] actual = StringConversions.toByteArray(source, mockConverter);
        assertArrayEquals(expected, actual, "The method should first convert the object to a String via asString and then to bytes");
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("normalCasesProvider")
    void toByteArray_ConvertsVariousInputs(Object from, Charset charset, byte[] expected, Class<? extends Throwable> expectedException, String description) {
        // Arrange – configure the mock according to the test data
        if (expectedException == null) {
            when(mockConverter.getOptions()).thenReturn(mockOptions);
            when(mockOptions.getCharset()).thenReturn(charset);
        }
        // Act & Assert
        if (expectedException != null) {
            assertThrows(expectedException, () -> StringConversions.toByteArray(from, mockConverter), description);
        } else {
            byte[] actual = StringConversions.toByteArray(from, mockConverter);
            assertArrayEquals(expected, actual, description);
        }
    }

    @Test
    void toByteArray_ThrowsNullPointerException_WhenConverterIsNull() {
        assertThrows(NullPointerException.class, () -> StringConversions.toByteArray("test", null), "Passing a null Converter should cause a NullPointerException");
    }

    @Test
    void toByteArray_ThrowsNullPointerException_WhenConverterOptionsAreNull() {
        // Configure the mock converter to return null for getOptions()
        when(mockConverter.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> StringConversions.toByteArray("test", mockConverter), "If converter.getOptions() returns null, a NullPointerException is expected");
    }

    @Test
    void toByteArray_ThrowsNullPointerException_WhenCharsetIsNull() {
        // Options exist but charset is null
        when(mockOptions.getCharset()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> StringConversions.toByteArray("test", mockConverter), "If converter.getOptions().getCharset() returns null, a NullPointerException is expected");
    }

    @Test
    void toByteArray_PropagatesRuntimeException_FromBadToStringObject() {
        Object badObject = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("Bad toString");
            }
        };
        // The mock is irrelevant – any charset works
        assertThrows(RuntimeException.class, () -> StringConversions.toByteArray(badObject, mockConverter), "An object whose toString() throws should propagate the RuntimeException");
    }
}
