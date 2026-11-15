package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.charset.CharacterCodingException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toByteBuffer_Test {

    @Mock
    private Converter // mock of the Converter passed to the method
    mockConverter;

    @Mock
    private ConverterOptions // mock of the options returned by Converter#getOptions()
    mockOptions;

    // deterministic charset for the test
    private Charset testCharset = StandardCharsets.UTF_8;

    /**
     * Holds the mocked static class {@link CharBufferConversions}.
     */
    private MockedStatic<CharBufferConversions> charBufferConversionsMockedStatic;

    @BeforeEach
    void setupBeforeEach() {
        // Mock the chain: converter.getOptions() -> mockOptions
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // mockOptions.getCharset() -> testCharset
        when(mockOptions.getCharset()).thenReturn(testCharset);
        // result of the package‑private static method toCharBuffer(...).
        charBufferConversionsMockedStatic = mockStatic(CharBufferConversions.class);
        // Default stubbing – individual tests override as needed
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getCharset()).thenReturn(StandardCharsets.UTF_8);
        // Prepare a static mock for CharBufferConversions so we can control the
        // Mock the static class to control the result of toCharBuffer(...)
        charBufferConversionsMockedStatic = mockStatic(CharBufferConversions.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock after each test to avoid cross‑test pollution.
        if (charBufferConversionsMockedStatic != null) {
            charBufferConversionsMockedStatic.close();
        }
        if (charBufferConversionsMockedStatic != null) {
            charBufferConversionsMockedStatic.close();
        }
    }

    /**
     * Parameter source for the "charset encoding branches" group.
     * Each argument consists of:
     *   - source object passed as {@code from}
     *   - the Charset that the mock ConverterOptions should return
     *   - the String that the mocked {@code toCharBuffer} will produce
     *   - a short description (used only for readability)
     */
    private static Stream<Arguments> charsetEncodingBranchProvider() {
        return Stream.of(// simple String → UTF‑8
        Arguments.of("simple string", StandardCharsets.UTF_8, "simple string", "Encode a regular Java String using UTF‑8"), // StringBuilder → UTF‑16
        Arguments.of(new StringBuilder("builder content"), StandardCharsets.UTF_16, "builder content", "StringBuilder with UTF‑16"), // char[] → ISO‑8859‑1
        Arguments.of(new char[] { 'a', 'b', 'c' }, StandardCharsets.ISO_8859_1, "abc", "char[] with ISO‑8859‑1"), // null → UTF‑8 (treated as empty)
        Arguments.of(null, StandardCharsets.UTF_8, "", "null input should become empty CharBuffer"), // generic Object → UTF‑8 (fallback to toString())
        Arguments.of(new Object(), StandardCharsets.UTF_8, new Object().toString(), "unsupported type falls back to toString()"), // non‑ASCII character with US‑ASCII charset (replacement character expected)
        Arguments.of("€", StandardCharsets.US_ASCII, "€", "character not representable in US‑ASCII"));
    }

    /**
     * Parameter source for the "empty and zero‑length inputs" group.
     */
    private static Stream<Arguments> emptyInputProvider() {
        return Stream.of(Arguments.of("", StandardCharsets.UTF_8, "empty String"), Arguments.of(new char[0], StandardCharsets.ISO_8859_1, "empty char array"), Arguments.of(new StringBuilder(), StandardCharsets.UTF_16, "empty StringBuilder"));
    }

    @Test
    void toByteBuffer_encodesReturnedCharBuffer_usingConverterCharset() {
        // GIVEN
        // the actual value is irrelevant because we mock toCharBuffer
        String source = "any input object";
        // the CharBuffer we want the method to encode
        CharBuffer expectedCharBuffer = CharBuffer.wrap("Hello, world!");
        // Mock the static call to return the prepared CharBuffer
        charBufferConversionsMockedStatic.when(() -> CharBufferConversions.toCharBuffer(source, mockConverter)).thenReturn(expectedCharBuffer);
        // WHEN
        ByteBuffer result = CharBufferConversions.toByteBuffer(source, mockConverter);
        // THEN
        // The expected ByteBuffer is the UTF‑8 encoding of the CharBuffer content.
        ByteBuffer expected = testCharset.encode(expectedCharBuffer);
        assertEquals(expected, result, "ByteBuffer should contain the UTF‑8 encoded bytes of the mocked CharBuffer");
        // Verify interactions
        // converter.getOptions() must be called
        verify(mockConverter).getOptions();
        // options.getCharset() must be called
        verify(mockOptions).getCharset();
        // Verify the static method was invoked exactly once with the given arguments
        charBufferConversionsMockedStatic.verify(() -> CharBufferConversions.toCharBuffer(source, mockConverter), times(1));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("charsetEncodingBranchProvider")
    void toByteBuffer_encodesVariousInputs(Object from, Charset charset, String charBufferContent, String _description) {
        // -----------------------------------------------------------------
        // GIVEN – configure mocks for this iteration
        // -----------------------------------------------------------------
        when(mockOptions.getCharset()).thenReturn(charset);
        CharBuffer mockedCharBuffer = CharBuffer.wrap(charBufferContent);
        // static method should return the CharBuffer we prepared
        charBufferConversionsMockedStatic.when(() -> CharBufferConversions.toCharBuffer(from, mockConverter)).thenReturn(mockedCharBuffer);
        // -----------------------------------------------------------------
        // WHEN
        // -----------------------------------------------------------------
        ByteBuffer result = CharBufferConversions.toByteBuffer(from, mockConverter);
        // -----------------------------------------------------------------
        // THEN – compare the actual bytes with the expected encoding
        // -----------------------------------------------------------------
        ByteBuffer expected = charset.encode(CharBuffer.wrap(charBufferContent));
        // Normalise both buffers for comparison
        expected.rewind();
        result.rewind();
        assertEquals(expected, result, "ByteBuffer should contain the bytes produced by the charset encoding");
        // Verify the interaction chain
        verify(mockConverter).getOptions();
        verify(mockOptions).getCharset();
        charBufferConversionsMockedStatic.verify(() -> CharBufferConversions.toCharBuffer(from, mockConverter), times(1));
    }

    @ParameterizedTest(name = "[{index}] Empty input: {2}")
    @MethodSource("emptyInputProvider")
    void toByteBuffer_emptyInputsProduceEmptyByteBuffer(Object from, Charset charset, String _desc) {
        // GIVEN
        when(mockOptions.getCharset()).thenReturn(charset);
        CharBuffer emptyCharBuffer = CharBuffer.wrap("");
        charBufferConversionsMockedStatic.when(() -> CharBufferConversions.toCharBuffer(from, mockConverter)).thenReturn(emptyCharBuffer);
        // WHEN
        ByteBuffer result = CharBufferConversions.toByteBuffer(from, mockConverter);
        // THEN – the resulting buffer must be empty (limit == 0)
        assertEquals(0, result.remaining(), "ByteBuffer should be empty for zero‑length input");
        // Verify interactions
        verify(mockConverter).getOptions();
        verify(mockOptions).getCharset();
        charBufferConversionsMockedStatic.verify(() -> CharBufferConversions.toCharBuffer(from, mockConverter), times(1));
    }

    @Test
    void toByteBuffer_nullConverter_throwsNullPointerException() {
        // GIVEN a null converter
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteBuffer("any", nullConverter), "Calling with a null Converter must throw NPE");
    }

    @Test
    void toByteBuffer_nullCharsetInOptions_throwsNullPointerException() {
        // GIVEN – options return a null charset
        when(mockOptions.getCharset()).thenReturn(null);
        CharBuffer anyCharBuffer = CharBuffer.wrap("any");
        charBufferConversionsMockedStatic.when(() -> CharBufferConversions.toCharBuffer("any", mockConverter)).thenReturn(anyCharBuffer);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteBuffer("any", mockConverter), "A null Charset in ConverterOptions should cause NPE when encode() is invoked");
    }

    @Test
    void toByteBuffer_underlyingToCharBufferThrowsRuntimeException_propagates() {
        // GIVEN – static method throws an unchecked exception
        RuntimeException boom = new RuntimeException("boom");
        charBufferConversionsMockedStatic.when(() -> CharBufferConversions.toCharBuffer("trigger", mockConverter)).thenThrow(boom);
        when(mockOptions.getCharset()).thenReturn(StandardCharsets.UTF_8);
        // WHEN / THEN
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> CharBufferConversions.toByteBuffer("trigger", mockConverter), "RuntimeException from toCharBuffer should be propagated");
        assertSame(boom, thrown, "The same exception instance should be re‑thrown");
    }

    @Test
    void toByteBuffer_charsetEncodeThrowsCharacterCodingException_propagates() throws Exception {
        // GIVEN – a mock Charset whose encode method throws CharacterCodingException
        Charset mockCharset = mock(Charset.class);
        when(mockCharset.encode(any(CharBuffer.class))).thenThrow(new CharacterCodingException());
        when(mockOptions.getCharset()).thenReturn(mockCharset);
        CharBuffer dummy = CharBuffer.wrap("dummy");
        charBufferConversionsMockedStatic.when(() -> CharBufferConversions.toCharBuffer("any", mockConverter)).thenReturn(dummy);
        // WHEN / THEN
        assertThrows(CharacterCodingException.class, () -> CharBufferConversions.toByteBuffer("any", mockConverter), "CharacterCodingException from Charset.encode should be propagated");
    }
}
