package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toString_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * Shared default {@link Converter} for the happy‑path tests.
     */
    private static Converter defaultConverter;

    /**
     * --------------------------------------------------------------------
     *  Initialise a shared {@link Converter} before any test runs.
     *  -----------------------------------------------------------------
     */
    @BeforeAll
    static void setupBeforeAll() {
        // The library’s default converter works for plain Strings, CharBuffer,
        // ByteBuffer and the special Map‑structure used by MapConversions.
        ConverterOptions options = new ConverterOptions();
        defaultConverter = new Converter(options);
    }

    /**
     * Provides a stream of arguments for the null‑input handling group.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>the source object ({@code from})</li>
     *   <li>the expected string result</li>
     * </ul>
     */
    private static Stream<Arguments> nullInputProvider() {
        return Stream.of(// null source – should become the literal "null"
        Arguments.of(null, "null"), // empty string – stays empty
        Arguments.of("", ""), // regular string
        Arguments.of("test", "test"), // Unicode characters
        Arguments.of("こんにちは", "こんにちは"), // Leading/trailing spaces
        Arguments.of("   leading and trailing spaces   ", "   leading and trailing spaces   "), // Boolean true
        Arguments.of(true, "true"), // Boolean false
        Arguments.of(false, "false"), // String containing an embedded NUL character
        Arguments.of("a\u0000b", "a\u0000b"), // String with line‑breaks
        Arguments.of("a\nb\r\nc", "a\nb\r\nc"), // Emoji (surrogate pair)
        Arguments.of("😀", "😀"));
    }

    @Test
    void toString_withString_returnsSameString() {
        // GIVEN
        String source = "Hello, World!";
        // WHEN
        String result = ByteBufferConversions.toString(source, defaultConverter);
        // THEN
        assertEquals(source, result);
    }

    @Test
    void toString_withCharBuffer_returnsSameString() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("CharBuffer content");
        // WHEN
        String result = ByteBufferConversions.toString(source, defaultConverter);
        // THEN
        assertEquals(source.toString(), result);
    }

    @Test
    void toString_withByteBuffer_returnsDecodedString() {
        // GIVEN
        byte[] bytes = "ByteBuffer content".getBytes(StandardCharsets.UTF_8);
        ByteBuffer source = ByteBuffer.wrap(bytes);
        // WHEN
        String result = ByteBufferConversions.toString(source, defaultConverter);
        // THEN
        assertEquals(new String(bytes, StandardCharsets.UTF_8), result);
    }

    @Test
    void toString_withMapContainingValueKey_returnsValueString() {
        // GIVEN – a map that mimics the structure used by MapConversions
        // The constant VALUE is imported from MapConversions and represents the key
        // that the conversion logic looks for when turning a map into a CharBuffer.
        // (The actual constant is not needed for the test; we use the literal key.)
        var source = Collections.singletonMap("value", "Map‑based string");
        // WHEN
        String result = ByteBufferConversions.toString(source, converter);
        // THEN
        assertEquals("Map‑based string", result);
    }

    @Test
    void toString_withNull_returnsNullString() {
        // GIVEN
        Object source = null;
        // WHEN
        String result = ByteBufferConversions.toString(source, converter);
        // THEN
        // The Converter returns the literal string "null" for a null source when
        // converting to a CharBuffer, which then yields the same string.
        assertEquals("null", result);
    }

    @Test
    void toString_withNull_returnsLiteralNullString() {
        // GIVEN
        Object source = null;
        // WHEN
        String result = ByteBufferConversions.toString(source, defaultConverter);
        // THEN
        // The library’s default converter represents a null source as the literal
        // string "null".
        assertEquals("null", result);
    }

    /**
     * Executes the {@code null‑input handling} test cases using a mock
     * {@link Converter} that simply returns a {@link CharBuffer} wrapping the
     * {@code from} object's {@code toString()} value (or the literal "null"
     * when the source itself is {@code null}).
     *
     * <p>This mirrors the behaviour of the library’s {@code IdentityConverter}
     * used in the original test data.
     */
    @ParameterizedTest(name = "[{index}] from={0} ⇒ expected=\"{1}\"")
    @MethodSource("nullInputProvider")
    void toString_nullInputHandling(Object from, String expected) {
        // GIVEN – a mock converter that behaves like an identity conversion
        Converter identityMock = mock(Converter.class);
        // The conversion contract used by ByteBufferConversions is assumed to be:
        //   CharBuffer toCharBuffer(Object source, Converter converter)
        // which internally calls converter.convert(source, CharBuffer.class).
        // We therefore stub that method.
        when(identityMock.convert(any(), eq(CharBuffer.class))).thenAnswer(invocation -> {
            Object src = invocation.getArgument(0);
            // null → literal "null"
            String text = (src == null) ? "null" : src.toString();
            return CharBuffer.wrap(text);
        });
        // WHEN
        String result = ByteBufferConversions.toString(from, identityMock);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void toString_numberToStringConverter_handlesStringNumber() {
        // GIVEN
        String source = "12345";
        Converter numberMock = mock(Converter.class);
        when(numberMock.convert(eq(source), eq(CharBuffer.class))).thenReturn(CharBuffer.wrap(source));
        // WHEN
        String result = ByteBufferConversions.toString(source, numberMock);
        // THEN
        assertEquals("12345", result);
    }

    @Test
    void toString_numberToStringConverter_handlesInteger() {
        // GIVEN
        Integer source = 12345;
        Converter numberMock = mock(Converter.class);
        when(numberMock.convert(eq(source), eq(CharBuffer.class))).thenReturn(CharBuffer.wrap(source.toString()));
        // WHEN
        String result = ByteBufferConversions.toString(source, numberMock);
        // THEN
        assertEquals("12345", result);
    }

    @Test
    void toString_numberToStringConverter_handlesFloatingPoint() {
        // GIVEN
        Double source = 3.14159;
        Converter numberMock = mock(Converter.class);
        when(numberMock.convert(eq(source), eq(CharBuffer.class))).thenReturn(CharBuffer.wrap(source.toString()));
        // WHEN
        String result = ByteBufferConversions.toString(source, numberMock);
        // THEN
        assertEquals("3.14159", result);
    }

    @Test
    void toString_nullConverter_throwsNullPointerException() {
        // GIVEN
        String source = "test";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toString(source, null));
    }

    @Test
    void toString_unsupportedType_throwsIllegalArgumentException() {
        // GIVEN – a plain Object that has no conversion rule
        Object source = new Object();
        // Mock a converter that throws the expected exception for this type
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(source), eq(CharBuffer.class))).thenThrow(new IllegalArgumentException("Unsupported type"));
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> ByteBufferConversions.toString(source, mock));
    }

    @Test
    void toString_converterThrowsRuntimeException_propagatesException() {
        // GIVEN
        String source = "test";
        Converter exceptionMock = mock(Converter.class);
        when(exceptionMock.convert(eq(source), eq(CharBuffer.class))).thenThrow(new RuntimeException("Conversion failed"));
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ByteBufferConversions.toString(source, exceptionMock));
        assertEquals("Conversion failed", ex.getMessage());
    }
}
