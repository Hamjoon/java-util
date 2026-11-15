package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CharacterArrayConversions_toString_Test {

    /**
     * A (mocked) {@link Converter} – the method under test does not use it, but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Provides arguments for successful conversion tests.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        // 1. Normal conversion – non‑empty array
        Character[] normal = new Character[] { 'a', 'b', 'c' };
        // 2. Empty array
        Character[] empty = new Character[0];
        // 3. Array containing null elements
        Character[] withNulls = new Character[] { null, 'x', null };
        // 4. Large array (100 'a' characters)
        Character[] large = new Character[100];
        for (int i = 0; i < large.length; i++) {
            large[i] = 'a';
        }
        // 5. Unicode / non‑ASCII characters
        Character[] unicode = new Character[] { '©', '€', 'Ω', '漢' };
        // 6. Control and whitespace characters
        Character[] control = new Character[] { '\n', '\t', '\r', ' ' };
        // 7. Converter argument ignored – using a distinct mock
        Converter dummyConverter = MockitoExtension.mock(Converter.class);
        Character[] xyz = new Character[] { 'x', 'y', 'z' };
        return Stream.of(arguments(normal, null, "abc"), // same case with explicit null converter
        arguments(normal, null, "abc"), arguments(empty, null, ""), arguments(withNulls, null, "nullxnull"), arguments(large, null, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"), arguments(unicode, null, "©€Ω漢"), arguments(control, null, "\n\t\r "), arguments(xyz, dummyConverter, "xyz"));
    }

    /**
     * Provides arguments for conversion scenarios that must throw an exception.
     */
    private static Stream<Arguments> exceptionConversionProvider() {
        // 1. Null source array
        Object nullSource = null;
        // 2. Invalid type – Integer[]
        Integer[] intArray = new Integer[] { 1, 2, 3 };
        // 3. Primitive char[]
        char[] primitiveCharArray = new char[] { 'a', 'b', 'c' };
        // 4. Non‑array object (String)
        String notAnArray = "abc";
        return Stream.of(arguments(nullSource, null, NullPointerException.class), arguments(intArray, null, ClassCastException.class), arguments(primitiveCharArray, null, ClassCastException.class), arguments(notAnArray, null, ClassCastException.class));
    }

    @Test
    void toString_convertsTypicalCharacterArray() {
        // given
        Character[] source = new Character[] { 'J', 'a', 'v', 'a' };
        // when
        String result = CharacterArrayConversions.toString(source, mockConverter);
        // then
        assertEquals("Java", result);
    }

    @Test
    void toString_convertsEmptyArrayToEmptyString() {
        // given
        Character[] source = new Character[0];
        // when
        String result = CharacterArrayConversions.toString(source, mockConverter);
        // then
        assertEquals("", result);
    }

    @Test
    void toString_handlesNullElementsWithinArray() {
        // given
        Character[] source = new Character[] { 'X', null, 'Y' };
        // when
        String result = CharacterArrayConversions.toString(source, mockConverter);
        // then
        // StringBuilder.append(Object) uses String.valueOf(Object) which yields "null" for a null reference.
        assertEquals("XnullY", result);
    }

    @Test
    void toString_nullSourceArray_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toString(source, mockConverter));
    }

    /**
     * Successful conversions – the method should return the expected {@link String}.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successfulConversionProvider")
    void toString_successfulConversions(Object from, Converter converter, String expected) {
        String result = CharacterArrayConversions.toString(from, converter);
        assertEquals(expected, result);
    }

    /**
     * Cases that must raise an exception (either {@link NullPointerException}
     * or {@link ClassCastException}).
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionConversionProvider")
    void toString_exceptionScenarios(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CharacterArrayConversions.toString(from, converter));
    }
}
