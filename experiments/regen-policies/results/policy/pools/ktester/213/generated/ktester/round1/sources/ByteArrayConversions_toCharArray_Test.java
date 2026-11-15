package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toCharArray_Test {

    /**
     * Shared Converter used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The library's Converter needs a ConverterOptions instance – the default constructor creates an empty options set.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Explicitly release reference – not strictly required but mirrors typical teardown pattern.
        converter = null;
    }

    private static Stream<Arguments> basicConversionProvider() {
        return Stream.of(// String input
        Arguments.of("Hello World", new char[] { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' }), // StringBuilder input
        Arguments.of(new StringBuilder("abc"), new char[] { 'a', 'b', 'c' }), // StringBuffer input
        Arguments.of(new StringBuffer("xyz"), new char[] { 'x', 'y', 'z' }), // char[] input
        Arguments.of(new char[] { 'a', 'b', 'c' }, new char[] { 'a', 'b', 'c' }));
    }

    private static Stream<Arguments> emptyAndPrimitiveProvider() {
        return Stream.of(// Empty String
        Arguments.of("", new char[] {}), // Empty char[]
        Arguments.of(new char[0], new char[] {}), // Integer value
        Arguments.of(Integer.valueOf(12345), new char[] { '1', '2', '3', '4', '5' }), // Boolean true
        Arguments.of(Boolean.TRUE, new char[] { 't', 'r', 'u', 'e' }), // Custom object with overridden toString()
        Arguments.of(new Object() {

            @Override
            public String toString() {
                return "CustomObj";
            }
        }, new char[] { 'C', 'u', 's', 't', 'o', 'm', 'O', 'b', 'j' }));
    }

    @Test
    void toCharArray_givenString_returnsCharArray() {
        String source = "hello world";
        char[] expected = source.toCharArray();
        char[] actual = ByteArrayConversions.toCharArray(source, converter);
        assertArrayEquals(expected, actual, "Converting a String should yield its exact char[] representation");
    }

    @Test
    void toCharArray_givenByteArray_returnsCharArray() {
        // UTF‑8 bytes for the word "bytes"
        byte[] source = "bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        char[] expected = "bytes".toCharArray();
        char[] actual = ByteArrayConversions.toCharArray(source, converter);
        assertArrayEquals(expected, actual, "Converting a byte[] (UTF‑8) should produce the correct char[]");
    }

    @Test
    void toCharArray_givenByteBuffer_returnsCharArray() {
        ByteBuffer buffer = ByteBuffer.wrap("buffer".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        char[] expected = "buffer".toCharArray();
        char[] actual = ByteArrayConversions.toCharArray(buffer, converter);
        assertArrayEquals(expected, actual, "Converting a ByteBuffer should produce the correct char[]");
    }

    @Test
    void toCharArray_givenCharBuffer_returnsCharArray() {
        CharBuffer charBuffer = CharBuffer.wrap("charbuf");
        char[] expected = "charbuf".toCharArray();
        char[] actual = ByteArrayConversions.toCharArray(charBuffer, converter);
        assertArrayEquals(expected, actual, "Converting a CharBuffer should produce the correct char[]");
    }

    @Test
    void toCharArray_givenCharArray_returnsSameArray() {
        char[] source = new char[] { 'a', 'b', 'c' };
        // The conversion path will turn the char[] into a String and back,
        // resulting in a new array with the same contents.
        char[] expected = new char[] { 'a', 'b', 'c' };
        char[] actual = ByteArrayConversions.toCharArray(source, converter);
        assertArrayEquals(expected, actual, "Converting a char[] should yield an equivalent char[]");
    }

    @Test
    void toCharArray_givenNull_throwsNullPointerException() {
        // The implementation delegates to toString(...).toCharArray(),
        // which will NPE when the intermediate String is null.
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toCharArray(null, converter), "Passing null should result in a NullPointerException");
    }

    @ParameterizedTest(name = "[basic] from={0}")
    @MethodSource("basicConversionProvider")
    void toCharArray_basicConversions(Object from, char[] expected) {
        // The JSON specifies a null converter for these cases – the library is expected to handle a null
        // Converter by using its default conversion logic.
        char[] actual = ByteArrayConversions.toCharArray(from, null);
        assertArrayEquals(expected, actual, () -> "Conversion of " + from.getClass().getSimpleName() + " should produce the expected char[]");
    }

    @ParameterizedTest(name = "[empty/primitive] from={0}")
    @MethodSource("emptyAndPrimitiveProvider")
    void toCharArray_emptyAndPrimitiveInputs(Object from, char[] expected) {
        char[] actual = ByteArrayConversions.toCharArray(from, null);
        assertArrayEquals(expected, actual, () -> "Conversion of " + (from == null ? "null" : from.getClass().getSimpleName()) + " should yield the expected char[]");
    }

    @Test
    void toCharArray_converterThrowsNullPointerException_propagates() {
        // Mock a Converter that throws NPE when asked to convert to String.
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), eq(String.class))).thenThrow(new NullPointerException("converter NPE"));
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toCharArray("anyString", mockConverter), "A Converter that throws NPE should have the exception propagate");
    }

    @Test
    void toCharArray_converterThrowsIllegalArgumentException_propagates() {
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), eq(String.class))).thenThrow(new IllegalArgumentException("converter illegal arg"));
        assertThrows(IllegalArgumentException.class, () -> ByteArrayConversions.toCharArray("anyString", mockConverter), "A Converter that throws IllegalArgumentException should have the exception propagate");
    }

    @Test
    void toCharArray_converterReturnsNull_causesNullPointerException() {
        // Converter returns null instead of a String – the subsequent toCharArray() call on null should NPE.
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), eq(String.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toCharArray("test", mockConverter), "If the Converter returns null, a NullPointerException should be thrown");
    }

    @Test
    void toCharArray_unsupportedList_withoutConverter_throwsIllegalArgumentException() {
        // List is not a type that the internal toString method knows how to handle without a custom Converter.
        assertThrows(IllegalArgumentException.class, () -> ByteArrayConversions.toCharArray(java.util.Arrays.asList(1, 2, 3), null), "Passing a List with a null Converter should throw IllegalArgumentException");
    }

    @Test
    void toCharArray_rawByteArray_withoutConverter_throwsIllegalArgumentException() {
        // The library does not support raw byte[] without an explicit Converter.
        assertThrows(IllegalArgumentException.class, () -> ByteArrayConversions.toCharArray(new byte[] { 65, 66, 67 }, null), "Passing a byte[] with a null Converter should throw IllegalArgumentException");
    }
}
