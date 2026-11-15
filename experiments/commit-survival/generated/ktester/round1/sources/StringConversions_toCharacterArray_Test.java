package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCharacterArray_Test {

    @Mock
    private ConverterOptions // used only to construct a Converter
    mockOptions;

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Stub the options to return an empty overrides map – the Converter does not need any real data
        Map<String, Object> emptyMap = Collections.emptyMap();
        // Mockito's default behaviour for unstubbed methods returns null, so we explicitly stub the needed call
        org.mockito.Mockito.when(mockOptions.getConverterOverrides()).thenReturn(emptyMap);
        converter = new Converter(mockOptions);
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// Regular non‑empty String
        arguments("Hello", new Character[] { 'H', 'e', 'l', 'l', 'o' }, "Convert a regular non‑empty String to a Character[]"), // Empty String
        arguments("", new Character[] {}, "Convert an empty String; expects an empty Character array"), // StringBuilder (already covered above, but included for completeness)
        arguments(new StringBuilder("AB"), new Character[] { 'A', 'B' }, "Pass a StringBuilder to ensure casting works for any CharSequence"), // CharSequence derived from a char array (using String constructor)
        arguments(new String(new char[] { 'x', 'y', 'z' }), new Character[] { 'x', 'y', 'z' }, "CharSequence derived from a char array"), // Surrogate pair (emoji 😀) – UTF‑16 code units are returned separately
        arguments("\uD83D\uDE00", new Character[] { '\uD83D', '\uDE00' }, "String containing a surrogate pair (emoji)"), // Newline and tab characters are preserved
        arguments("Line1\nLine2\tEnd", new Character[] { 'L', 'i', 'n', 'e', '1', '\n', 'L', 'i', 'n', 'e', '2', '\t', 'E', 'n', 'd' }, "String with newline and tab characters"), // Embedded null character (\\0)
        arguments("a\u0000b", new Character[] { 'a', '\u0000', 'b' }, "String containing an embedded null character"), // Single‑character string with a non‑null Converter instance
        arguments("Z", new Character[] { 'Z' }, "Single‑character string with a non‑null Converter (converter ignored)"), // Anonymous CharSequence implementation returning "XYZ"
        arguments(new CharSequence() {

            private final String data = "XYZ";

            @Override
            public int length() {
                return data.length();
            }

            @Override
            public char charAt(int index) {
                return data.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return data.subSequence(start, end);
            }

            @Override
            public String toString() {
                return data;
            }
        }, new Character[] { 'X', 'Y', 'Z' }, "Anonymous CharSequence implementation returning \"XYZ\""), // Very long string (1000 'a' characters)
        arguments(generateLongString('a', 1000), generateLongArray('a', 1000), "Very long string (1000 'a' characters)"));
    }

    private static String generateLongString(char ch, int length) {
        char[] arr = new char[length];
        java.util.Arrays.fill(arr, ch);
        return new String(arr);
    }

    private static Character[] generateLongArray(char ch, int length) {
        Character[] arr = new Character[length];
        java.util.Arrays.fill(arr, ch);
        return arr;
    }

    @Test
    void toCharacterArray_givenSimpleString_returnsCorrectArray() {
        // given
        String source = "abc";
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        Character[] expected = new Character[] { 'a', 'b', 'c' };
        assertArrayEquals(expected, result);
    }

    @Test
    void toCharacterArray_givenEmptyString_returnsEmptyArray() {
        // given
        String source = "";
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void toCharacterArray_givenStringBuilder_returnsCorrectArray() {
        // given
        StringBuilder sb = new StringBuilder("xyz");
        // when
        Character[] result = StringConversions.toCharacterArray(sb, converter);
        // then
        Character[] expected = new Character[] { 'x', 'y', 'z' };
        assertArrayEquals(expected, result);
    }

    @Test
    void toCharacterArray_givenNullCharSequence_throwsNullPointerException() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toCharacterArray(nullSource, converter));
    }

    @Test
    void toCharacterArray_converterArgumentIsIgnored() {
        // given
        String source = "test";
        // when
        Character[] resultWithNullConverter = StringConversions.toCharacterArray(source, null);
        Character[] resultWithRealConverter = StringConversions.toCharacterArray(source, converter);
        // then
        Character[] expected = new Character[] { 't', 'e', 's', 't' };
        assertArrayEquals(expected, resultWithNullConverter);
        assertArrayEquals(expected, resultWithRealConverter);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("normalConversionProvider")
    void toCharacterArray_normalConversions(Object from, Character[] expected, String description) {
        // when
        Character[] result = StringConversions.toCharacterArray(from, converter);
        // then
        assertArrayEquals(expected, result, description);
    }

    @Test
    void toCharacterArray_givenNonCharSequence_throwsClassCastException() {
        // given – an object that does NOT implement CharSequence
        Object notACharSequence = new Object();
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toCharacterArray(notACharSequence, converter));
    }

    @Test
    void toCharacterArray_charSequenceLengthThrowsException_propagatesIt() {
        // given – CharSequence whose length() throws IllegalStateException
        CharSequence faultyLength = new CharSequence() {

            @Override
            public int length() {
                throw new IllegalStateException("length failure");
            }

            @Override
            public char charAt(int index) {
                return 'x';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return this;
            }

            @Override
            public String toString() {
                return "";
            }
        };
        // when / then
        assertThrows(IllegalStateException.class, () -> StringConversions.toCharacterArray(faultyLength, converter));
    }

    @Test
    void toCharacterArray_charAtThrowsException_propagatesIt() {
        // given – CharSequence whose charAt(1) throws IndexOutOfBoundsException
        CharSequence faultyCharAt = new CharSequence() {

            private final String data = "AB";

            @Override
            public int length() {
                return data.length();
            }

            @Override
            public char charAt(int index) {
                if (index == 1) {
                    throw new IndexOutOfBoundsException("charAt failure");
                }
                return data.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return data.subSequence(start, end);
            }

            @Override
            public String toString() {
                return data;
            }
        };
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> StringConversions.toCharacterArray(faultyCharAt, converter));
    }
}
