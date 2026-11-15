package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toString_Test {

    /**
     * Mocked {@link Converter} used in all tests.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Mocked {@link ConverterOptions} returned by the converter.
     */
    @Mock
    private ConverterOptions mockOptions;

    /**
     * Charset that will be used by the mocked options.
     */
    private static final Charset TEST_CHARSET = StandardCharsets.UTF_8;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy:
        // converter.getOptions().getCharset() -> TEST_CHARSET
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getCharset()).thenReturn(TEST_CHARSET);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        clearInvocations(mockConverter, mockOptions);
    }

    /**
     * Builds a mock {@link Converter} according to the supplied label.
     */
    private Converter createConverterForLabel(String label) {
        switch(label) {
            case // converter argument is null
            "null":
                return null;
            case "validUTF8":
                return mockConverterWithCharset(StandardCharsets.UTF_8);
            case "validISO8859_1":
                return mockConverterWithCharset(StandardCharsets.ISO_8859_1);
            case "validUS_ASCII":
                return mockConverterWithCharset(StandardCharsets.US_ASCII);
            case "validUTF16BE":
                return mockConverterWithCharset(StandardCharsets.UTF_16BE);
            case "nullOptions":
                // Converter returns null for getOptions()
                Converter cNullOpts = mock(Converter.class);
                when(cNullOpts.getOptions()).thenReturn(null);
                return cNullOpts;
            case "nullCharset":
                // Options exist but return null charset
                ConverterOptions optsNullCharset = mock(ConverterOptions.class);
                when(optsNullCharset.getCharset()).thenReturn(null);
                Converter cNullCharset = mock(Converter.class);
                when(cNullCharset.getOptions()).thenReturn(optsNullCharset);
                return cNullCharset;
            case "throwGetOptions":
                // getOptions() throws RuntimeException
                Converter cThrowOpts = mock(Converter.class);
                when(cThrowOpts.getOptions()).thenThrow(new RuntimeException("boom"));
                return cThrowOpts;
            case "throwGetCharset":
                // getCharset() throws RuntimeException
                ConverterOptions optsThrow = mock(ConverterOptions.class);
                when(optsThrow.getCharset()).thenThrow(new RuntimeException("boom"));
                Converter cThrowCharset = mock(Converter.class);
                when(cThrowCharset.getOptions()).thenReturn(optsThrow);
                return cThrowCharset;
            default:
                throw new IllegalArgumentException("Unknown converter label: " + label);
        }
    }

    /**
     * Helper that creates a mock Converter returning the supplied charset.
     */
    private Converter mockConverterWithCharset(Charset charset) {
        ConverterOptions opts = mock(ConverterOptions.class);
        when(opts.getCharset()).thenReturn(charset);
        Converter conv = mock(Converter.class);
        when(conv.getOptions()).thenReturn(opts);
        return conv;
    }

    /**
     * Supplies all test cases defined in the JSON matrix.
     */
    private static Stream<Arguments> toStringTestCases() {
        return Stream.of(// 1. null byte array – already covered, but kept for completeness (expects empty string)
        Arguments.of(null, "validUTF8", StringUtilities.EMPTY, "When the input byte array is null, return empty string (StringUtilities.EMPTY)"), // 2. Simple "Hello" in UTF‑8
        Arguments.of(new byte[] { 72, 101, 108, 108, 111 }, "validUTF8", "Hello", "UTF‑8 decoding of byte array representing \"Hello\""), // 3. Multi‑byte character 'é' in UTF‑8 (0xC3 0xA9)
        Arguments.of(new byte[] { (byte) 0xC3, (byte) 0xA9 }, "validUTF8", "é", "UTF‑8 multi‑byte character 'é'"), // 4. Single byte 0xE9 interpreted with ISO‑8859‑1 -> 'é'
        Arguments.of(new byte[] { (byte) 0xE9 }, "validISO8859_1", "é", "ISO‑8859‑1 decoding of 0xE9 yields 'é'"), // 5. Empty byte array → empty string
        Arguments.of(new byte[] {}, "validUTF8", "", "Empty byte array should result in empty string"), // 6. Null characters (0x00) → string with three NUL chars
        Arguments.of(new byte[] { 0, 0, 0 }, "validUTF8", "\u0000\u0000\u0000", "Byte array of three NUL bytes should become three NUL characters"), // 7. Null converter → NPE
        Arguments.of(new byte[] { 65, 66, 67 }, "null", NullPointerException.class, "Passing null converter should throw NullPointerException"), // 8. Non‑byte[] input (String) → ClassCastException
        // Since the method signature forces Object, we pass a String and expect a cast failure.
        // We cannot pass a String through the byte[] parameter, so we use a raw Object cast.
        Arguments.of((byte[]) (Object) "NotAByteArray", "validUTF8", ClassCastException.class, "Supplying a non‑byte[] argument should trigger ClassCastException"), // 9. Converter with null options → NPE
        Arguments.of(new byte[] { 72, 101, 108, 108, 111 }, "nullOptions", NullPointerException.class, "Converter.getOptions() returns null → NullPointerException"), //10. Converter with null charset → NPE
        Arguments.of(new byte[] { 72, 101, 108, 108, 111 }, "nullCharset", NullPointerException.class, "ConverterOptions.getCharset() returns null → NullPointerException"), //11. US‑ASCII cannot represent bytes > 0x7F → replacement '?' characters
        Arguments.of(new byte[] { (byte) 0xFF, (byte) 0xFE }, "validUS_ASCII", "??", "US‑ASCII decoding of non‑ASCII bytes yields \"??\""), //12. UTF‑16BE decoding of 0x00 0x48 0x00 0x69 → \"Hi\"
        Arguments.of(new byte[] { 0x00, 0x48, 0x00, 0x69 }, "validUTF16BE", "Hi", "UTF‑16BE decoding of byte sequence 0x00 0x48 0x00 0x69 yields \"Hi\""), //13. Invalid UTF‑8 start byte 0xC3 alone → replacement character
        Arguments.of(new byte[] { (byte) 0xC3 }, "validUTF8", "\uFFFD", "Single continuation byte 0xC3 in UTF‑8 yields Unicode replacement character"), //14. Euro sign € (UTF‑8 0xE2 0x82 0xAC)
        Arguments.of(new byte[] { (byte) 0xE2, (byte) 0x82, (byte) 0xAC }, "validUTF8", "€", "Three‑byte UTF‑8 sequence for Euro sign should decode to \"€\""), //15. Large zero‑filled array (1000 bytes) → 1000 NUL characters
        Arguments.of(new byte[1000], "validISO8859_1", "\u0000".repeat(1000), "Large zero‑filled array should produce string of 1000 NUL characters"), //16. Converter.getOptions() throws RuntimeException → propagate
        Arguments.of(new byte[] { 65, 66 }, "throwGetOptions", RuntimeException.class, "Exception thrown from Converter.getOptions() should propagate"), //17. Options.getCharset() throws RuntimeException → propagate
        Arguments.of(new byte[] { 65 }, "throwGetCharset", RuntimeException.class, "Exception thrown from ConverterOptions.getCharset() should propagate"));
    }

    @Test
    void toString_WithNullByteArray_ReturnsEmptyString() {
        // GIVEN
        byte[] source = null;
        // WHEN
        String result = ByteArrayConversions.toString(source, mockConverter);
        // THEN
        assertEquals(StringUtilities.EMPTY, result, "When the source byte array is null the method should return StringUtilities.EMPTY");
        // Verify that charset lookup was not performed (no need to call getCharset)
        verify(mockConverter, never()).getOptions();
    }

    @Test
    void toString_WithNonNullByteArray_ReturnsStringUsingCharset() {
        // GIVEN
        String original = "Cedar™ Test – 文字列";
        // encode using the test charset
        byte[] source = original.getBytes(TEST_CHARSET);
        // WHEN
        String result = ByteArrayConversions.toString(source, mockConverter);
        // THEN
        assertEquals(original, result, "The returned string must be decoded with the charset supplied by ConverterOptions");
        // Ensure the charset was consulted exactly once.
        verify(mockConverter, times(1)).getOptions();
        verify(mockOptions, times(1)).getCharset();
    }

    /**
     * Each argument consists of:
     * <ul>
     *   <li>byte[] input (or {@code null})</li>
     *   <li>String label describing the converter configuration</li>
     *   <li>Object expected – either the expected String result or the expected exception class</li>
     *   <li>String description (used as the test name)</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("toStringTestCases")
    void toString_VariousScenarios(byte[] input, String converterLabel, Object expected, String description) {
        Converter converter = createConverterForLabel(converterLabel);
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> ByteArrayConversions.toString(input, converter), description);
        } else {
            // Expected a normal string result
            String expectedString = (String) expected;
            String actual = ByteArrayConversions.toString(input, converter);
            assertEquals(expectedString, actual, description);
        }
    }
}
