package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toCharBuffer_Test {

    /**
     * Converter instance used for all tests.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – they provide the built‑in conversions needed for the tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no stray state leaks between tests.
        converter = null;
    }

    private static Stream<Arguments> nonStringInputs() {
        return Stream.of(// byte[] → "Hello"
        Arguments.of(new byte[] { 72, 101, 108, 108, 111 }, "Hello"), // char[] → "abc"
        Arguments.of(new char[] { 'a', 'b', 'c' }, "abc"), // StringBuilder → "builder"
        Arguments.of(new StringBuilder("builder"), "builder"), // StringBuffer → "buffer"
        Arguments.of(new StringBuffer("buffer"), "buffer"), // Integer → "12345"
        Arguments.of(12345, "12345"), // empty byte[] → ""
        Arguments.of(new byte[] {}, ""), // empty char[] → ""
        Arguments.of(new char[] {}, ""));
    }

    @Test
    void toCharBuffer_givenString_returnsWrappedCharBuffer() {
        // GIVEN
        String source = "Hello, World!";
        // WHEN
        CharBuffer result = ByteArrayConversions.toCharBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals(source, result.toString(), "CharBuffer should contain the original string");
    }

    @Test
    void toCharBuffer_givenByteArray_returnsWrappedCharBuffer() {
        // GIVEN
        byte[] bytes = "byte array".getBytes(StandardCharsets.UTF_8);
        // WHEN
        CharBuffer result = ByteArrayConversions.toCharBuffer(bytes, converter);
        // THEN
        assertNotNull(result);
        assertEquals("byte array", result.toString());
    }

    @Test
    void toCharBuffer_givenByteBuffer_returnsWrappedCharBuffer() {
        // GIVEN
        ByteBuffer byteBuffer = ByteBuffer.wrap("buffered".getBytes(StandardCharsets.UTF_8));
        // WHEN
        CharBuffer result = ByteArrayConversions.toCharBuffer(byteBuffer, converter);
        // THEN
        assertNotNull(result);
        assertEquals("buffered", result.toString());
    }

    @Test
    void toCharBuffer_givenNull_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toCharBuffer(source, converter), "Passing null should result in NullPointerException because CharBuffer.wrap(null) is illegal");
    }

    /**
     * When both arguments are {@code null} the internal {@code toString(null, null)}
     * contract of the library returns the literal string {@code "null"}.
     * The resulting {@link CharBuffer} must wrap that string.
     */
    @Test
    void toCharBuffer_givenBothNull_returnsCharBufferWithLiteralNull() {
        CharBuffer cb = ByteArrayConversions.toCharBuffer(null, null);
        assertNotNull(cb, "CharBuffer must not be null");
        assertEquals("null", cb.toString(), "CharBuffer should contain the literal string \"null\"");
        assertEquals(4, cb.remaining(), "Remaining characters should be 4 for \"null\"");
        // Verify the character sequence
        assertEquals('n', cb.get(0));
        assertEquals('u', cb.get(1));
        assertEquals('l', cb.get(2));
        assertEquals('l', cb.get(3));
    }

    /**
     * Empty source string should result in an empty {@link CharBuffer}.
     */
    @Test
    void toCharBuffer_givenEmptyString_returnsEmptyCharBuffer() {
        CharBuffer cb = ByteArrayConversions.toCharBuffer("", converter);
        assertNotNull(cb);
        assertEquals(0, cb.remaining(), "Empty string must produce CharBuffer with remaining() == 0");
        assertEquals("", cb.toString());
    }

    /**
     * Normal non‑null string should be wrapped unchanged.
     */
    @Test
    void toCharBuffer_givenNormalString_returnsWrappedCharBuffer() {
        String src = "hello world";
        CharBuffer cb = ByteArrayConversions.toCharBuffer(src, converter);
        assertNotNull(cb);
        assertEquals(src.length(), cb.remaining());
        assertEquals(src, cb.toString());
    }

    @ParameterizedTest(name = "[{index}] from={0} expects=\"{1}\"")
    @MethodSource("nonStringInputs")
    void toCharBuffer_givenVariousNonStringInputs_returnsExpectedCharBuffer(Object from, String expected) {
        CharBuffer cb = ByteArrayConversions.toCharBuffer(from, converter);
        assertNotNull(cb);
        assertEquals(expected, cb.toString(), "CharBuffer content mismatch");
        assertEquals(expected.length(), cb.remaining(), "Remaining count mismatch");
    }

    /**
     * A custom {@link Converter} that throws an {@link IllegalArgumentException}
     * for any conversion request. The exception must propagate unchanged.
     */
    @Test
    void toCharBuffer_converterThrowsException_propagatesException() {
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), eq(String.class))).thenThrow(new IllegalArgumentException("converter failure"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ByteArrayConversions.toCharBuffer("any value", mockConverter));
        assertEquals("converter failure", ex.getMessage());
    }

    /**
     * When a converter returns {@code null}, {@link CharBuffer#wrap(CharSequence)} will
     * throw a {@link NullPointerException}. The test verifies that this NPE is observed.
     */
    @Test
    void toCharBuffer_converterReturnsNull_resultsInNPE() {
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), eq(String.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toCharBuffer("does not matter", mockConverter));
    }

    /**
     * If no converter is supplied and the source object type is unsupported,
     * the library is expected to throw an {@link IllegalArgumentException}.
     */
    @Test
    void toCharBuffer_unsupportedInputWithoutConverter_throwsIllegalArgumentException() {
        // plain Object has no default conversion
        Object unsupported = new Object();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ByteArrayConversions.toCharBuffer(unsupported, null));
        assertTrue(ex.getMessage().toLowerCase().contains("unsupported"), "Exception message should indicate unsupported source type");
    }

    /**
     * A custom {@link Converter} that returns a non‑String (e.g., {@link Integer}).
     * The subsequent {@link CharBuffer#wrap(CharSequence)} call will attempt to treat
     * the result as a {@link CharSequence} and therefore throw a {@link ClassCastException}.
     */
    @Test
    void toCharBuffer_converterReturnsNonString_throwsClassCastException() {
        Converter mockConverter = mock(Converter.class);
        // Integer, not CharSequence
        when(mockConverter.convert(any(), eq(String.class))).thenReturn(123);
        assertThrows(ClassCastException.class, () -> ByteArrayConversions.toCharBuffer("irrelevant", mockConverter));
    }

    /**
     * When a non‑null converter is supplied but the source object itself is {@code null},
     * the converter should be invoked. This mock returns the literal string "converted".
     */
    @Test
    void toCharBuffer_nullSourceWithCustomConverter_returnsConvertedString() {
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(isNull(), eq(String.class))).thenReturn("converted");
        CharBuffer cb = ByteArrayConversions.toCharBuffer(null, mockConverter);
        assertNotNull(cb);
        assertEquals("converted", cb.toString());
        assertEquals(9, cb.remaining());
    }

    /**
     * Custom converter that transforms any input string to upper case.
     */
    @Test
    void toCharBuffer_stringWithUpperCaseConverter_returnsUpperCase() {
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), eq(String.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            return arg == null ? null : arg.toString().toUpperCase();
        });
        CharBuffer cb = ByteArrayConversions.toCharBuffer("test", mockConverter);
        assertNotNull(cb);
        assertEquals("TEST", cb.toString());
        assertEquals(4, cb.remaining());
    }
}
