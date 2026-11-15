package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.CharBuffer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCharacterArray_Test {

    /**
     * The Converter instance required by the method under test.
     */
    @Mock
    private Converter // Mockito will initialise this mock before each test
    converter;

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// regular String
        Arguments.of("Hello", new Character[] { 'H', 'e', 'l', 'l', 'o' }), // empty String
        Arguments.of("", new Character[] {}), // StringBuilder
        Arguments.of(new StringBuilder("AB"), new Character[] { 'A', 'B' }), // StringBuffer
        Arguments.of(new StringBuffer("12"), new Character[] { '1', '2' }), // CharBuffer
        Arguments.of(CharBuffer.wrap("XYZ"), new Character[] { 'X', 'Y', 'Z' }), // surrogate pair (emoji) – each UTF‑16 code unit becomes a separate Character
        Arguments.of("\uD83D\uDE00", new Character[] { '\uD83D', '\uDE00' }), // control characters
        Arguments.of("\u0000\u0001", new Character[] { '\u0000', '\u0001' }), // whitespace only
        Arguments.of("   ", new Character[] { ' ', ' ', ' ' }));
    }

    @Test
    void toCharacterArray_withSimpleString_returnsCorrectArray() {
        // given
        String source = "abc";
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        assertArrayEquals(new Character[] { 'a', 'b', 'c' }, result);
    }

    @Test
    void toCharacterArray_withStringBuilder_returnsCorrectArray() {
        // given
        StringBuilder source = new StringBuilder("Hello");
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        assertArrayEquals(new Character[] { 'H', 'e', 'l', 'l', 'o' }, result);
    }

    @Test
    void toCharacterArray_withEmptyString_returnsEmptyArray() {
        // given
        String source = "";
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        assertEquals(0, result.length);
    }

    @Test
    void toCharacterArray_withNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toCharacterArray(source, converter));
    }

    @Test
    void toCharacterArray_withNonCharSequence_throwsClassCastException() {
        // given
        Object source = Integer.valueOf(123);
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toCharacterArray(source, converter));
    }

    @ParameterizedTest(name = "{index}: toCharacterArray({0}) → {1}")
    @MethodSource("normalConversionProvider")
    void toCharacterArray_normalConversions(Object source, Character[] expected) {
        // when
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // then
        assertArrayEquals(expected, result, () -> "Expected conversion of " + source + " to produce " + java.util.Arrays.toString(expected));
    }

    @Test
    void toCharacterArray_singleCharacter() {
        Character[] result = StringConversions.toCharacterArray("A", converter);
        assertArrayEquals(new Character[] { 'A' }, result);
    }

    @Test
    void toCharacterArray_maximumCharValue() {
        Character[] result = StringConversions.toCharacterArray("\uFFFF", converter);
        assertArrayEquals(new Character[] { '\uFFFF' }, result);
    }

    @Test
    void toCharacterArray_customCharSequence() {
        CharSequence custom = new CharSequence() {

            @Override
            public int length() {
                return 2;
            }

            @Override
            public char charAt(int i) {
                return i == 0 ? 'X' : 'Y';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return null;
            }

            @Override
            public String toString() {
                return "XY";
            }
        };
        Character[] result = StringConversions.toCharacterArray(custom, converter);
        assertArrayEquals(new Character[] { 'X', 'Y' }, result);
    }

    @Test
    void toCharacterArray_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringConversions.toCharacterArray(null, converter));
    }

    @Test
    void toCharacterArray_invalidType_throwsClassCastException() {
        Object notACharSequence = Integer.valueOf(12345);
        assertThrows(ClassCastException.class, () -> StringConversions.toCharacterArray(notACharSequence, converter));
    }

    @Test
    void toCharacterArray_nonNullConverter_isIgnored() {
        // a stub Converter that would return something unexpected if used
        Converter stub = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return "should not be used";
            }
        };
        Character[] result = StringConversions.toCharacterArray("AB", stub);
        assertArrayEquals(new Character[] { 'A', 'B' }, result);
    }

    @Test
    void toCharacterArray_resultIsImmutableAfterSourceMutation() {
        StringBuilder source = new StringBuilder("AB");
        Character[] result = StringConversions.toCharacterArray(source, converter);
        // mutate the original source
        source.setCharAt(0, 'Z');
        source.append('C');
        // the previously created array must stay unchanged
        assertArrayEquals(new Character[] { 'A', 'B' }, result, "The Character[] returned by toCharacterArray must be independent of the original CharSequence");
    }

    @Test
    void toCharacterArray_emptyStringBuilder_returnsEmptyArray() {
        StringBuilder sb = new StringBuilder("");
        Character[] result = StringConversions.toCharacterArray(sb, converter);
        assertEquals(0, result.length);
    }
}
