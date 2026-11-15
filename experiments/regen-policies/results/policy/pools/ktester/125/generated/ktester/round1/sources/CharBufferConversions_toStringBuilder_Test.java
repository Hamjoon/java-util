package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toStringBuilder_Test {

    /**
     * The {@link Converter} used for all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the real Converter with default options – no mocking needed for the
        // majority of the test cases.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
        converter = null;
    }

    private static Stream<Arguments> charBufferProvider() {
        return Stream.of(Arguments.of(CharBuffer.wrap("hello"), "hello"), Arguments.of(CharBuffer.allocate(0), ""));
    }

    private static Stream<Arguments> stringProvider() {
        return Stream.of(Arguments.of("Java", "Java"), Arguments.of("", ""));
    }

    private static Stream<Arguments> byteArrayProvider() {
        return Stream.of(// ASCII "ABC"
        Arguments.of(new byte[] { 65, 66, 67 }, "ABC"), Arguments.of(new byte[] {}, ""));
    }

    @Test
    void toStringBuilder_fromCharBuffer_returnsMatchingBuilder() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("hello");
        // WHEN
        StringBuilder result = CharBufferConversions.toStringBuilder(source, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuilder should not be null");
        assertEquals("hello", result.toString(), "StringBuilder content should match the CharBuffer source");
    }

    @Test
    void toStringBuilder_fromString_returnsMatchingBuilder() {
        // GIVEN
        String source = "world";
        // WHEN
        StringBuilder result = CharBufferConversions.toStringBuilder(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals("world", result.toString());
    }

    @Test
    void toStringBuilder_fromCharArray_returnsMatchingBuilder() {
        // GIVEN
        char[] source = new char[] { 'a', 'b', 'c' };
        // WHEN
        StringBuilder result = CharBufferConversions.toStringBuilder(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals("abc", result.toString());
    }

    @Test
    void toStringBuilder_fromNull_returnsEmptyBuilder() {
        // GIVEN
        Object source = null;
        // WHEN
        StringBuilder result = CharBufferConversions.toStringBuilder(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(0, result.length(), "Builder should be empty when source is null");
    }

    @Test
    void toStringBuilder_fromNull_throwsNullPointerException() {
        // GIVEN a null source object and a real converter
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toStringBuilder(source, converter), "Calling toStringBuilder with a null source should throw NPE");
    }

    @Test
    void toStringBuilder_nullConverter_throwsNullPointerException() {
        // GIVEN a non‑null source but a null converter
        Object source = "sample";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toStringBuilder(source, null), "Calling toStringBuilder with a null converter should throw NPE");
    }

    @ParameterizedTest(name = "[{index}] CharBuffer ''{0}'' → ''{1}''")
    @MethodSource("charBufferProvider")
    void toStringBuilder_fromCharBuffer_returnsExpectedBuilder(CharBuffer source, String expected) {
        // WHEN
        StringBuilder result = CharBufferConversions.toStringBuilder(source, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuilder must not be null");
        assertEquals(expected, result.toString(), "StringBuilder content should match the CharBuffer source");
    }

    @ParameterizedTest(name = "[{index}] String ''{0}'' → ''{1}''")
    @MethodSource("stringProvider")
    void toStringBuilder_fromString_returnsExpectedBuilder(String source, String expected) {
        // WHEN
        StringBuilder result = CharBufferConversions.toStringBuilder(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.toString());
    }

    @ParameterizedTest(name = "[{index}] byte[] {0} → ''{1}''")
    @MethodSource("byteArrayProvider")
    void toStringBuilder_fromByteArray_returnsExpectedBuilder(byte[] source, String expected) {
        // Mock a Converter that knows how to turn a byte[] into a CharBuffer.
        Converter mockConverter = Mockito.mock(Converter.class);
        // The conversion logic used by CharBufferConversions is not public, but
        // we assume it eventually calls Converter.convert(source, CharBuffer.class).
        CharBuffer cb = CharBuffer.wrap(expected);
        Mockito.when(mockConverter.convert(eq(source), eq(CharBuffer.class))).thenReturn(cb);
        // WHEN
        StringBuilder result = CharBufferConversions.toStringBuilder(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.toString(), "StringBuilder should contain characters decoded from the byte array");
    }

    @Test
    void toStringBuilder_fromUnsupportedType_throwsIllegalArgumentException() {
        // GIVEN an Integer which the conversion routine does not support
        Integer source = 12345;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toStringBuilder(source, converter), "Unsupported source type should cause IllegalArgumentException");
    }

    @Test
    void toStringBuilder_converterReturnsNullCharBuffer_throwsNullPointerException() {
        // Mock a Converter that returns null for any CharBuffer conversion.
        Converter mockConverter = Mockito.mock(Converter.class);
        Mockito.when(mockConverter.convert(any(), eq(CharBuffer.class))).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toStringBuilder("any", mockConverter), "If the Converter returns null CharBuffer, StringBuilder ctor should NPE");
    }

    @Test
    void toStringBuilder_converterThrowsException_propagatesIllegalArgumentException() {
        // Mock a Converter that throws IllegalArgumentException during conversion.
        Converter mockConverter = Mockito.mock(Converter.class);
        Mockito.when(mockConverter.convert(any(), eq(CharBuffer.class))).thenThrow(new IllegalArgumentException("Conversion failed"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toStringBuilder("sample", mockConverter), "Exception thrown by the Converter should propagate");
        assertEquals("Conversion failed", ex.getMessage());
    }
}
