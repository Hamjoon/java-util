package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toString_Test {

    static Stream<Arguments> successfulCasesProvider() {
        // a dummy (non‑null) Converter – the method must ignore it
        Converter dummyConverter = Mockito.mock(Converter.class);
        // a Converter that throws on any method call – still must be ignored
        Converter throwingConverter = Mockito.mock(Converter.class, (Answer<Object>) invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
        // large character set (100+ characters)
        String largeString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_=+[]{}|;':,.<>?`~";
        char[] largeCharArray = largeString.toCharArray();
        return Stream.of(// normal usage
        arguments(new char[] { 'h', 'e', 'l', 'l', 'o' }, null, "hello", "Converts a typical char[] containing the word \"hello\" to a String. The converter argument is ignored."), arguments(new char[0], null, "", "Converts an empty char[] to an empty String."), arguments(new char[] { 'é', '€', '\ud83d', '\ude00' }, null, "é€😀", "Converts a char[] containing Unicode characters (é, €, and a smiley surrogate pair) to the corresponding String."), arguments(null, null, NullPointerException.class, "Passing null as the source object results in a NullPointerException when the method attempts to create a new String from a null char[]."), arguments("not a char array", null, ClassCastException.class, "If the source object is not a char[], a ClassCastException is thrown during the cast."), arguments(largeCharArray, null, largeString, "Converts a large char[] (100+ characters) to verify that the method correctly handles longer inputs."), arguments(new char[] { 'x', 'y', 'z' }, dummyConverter, "xyz", "Ensures that the method does not depend on the Converter argument; even a non‑null dummy object yields the same result."), // edge cases
        arguments(new char[] { 'Z' }, null, "Z", "Single‑character array should be converted to a one‑character String."), arguments(new char[] { 'a', '\u0000', 'b' }, null, "a\u0000b", "Array containing a NUL (\\u0000) character should preserve the NUL in the resulting String."), arguments(new char[] { '\ud83d' }, null, "\ud83d", "Array with an isolated high surrogate should be turned into a String that contains that lone surrogate code unit."), arguments(new char[] { '\ude00' }, null, "\ude00", "Array with an isolated low surrogate should be turned into a String that contains that lone surrogate code unit."), arguments(new char[] { '\ude00', '\ud83d' }, null, "\ude00\ud83d", "Array with low‑surrogate followed by high‑surrogate (invalid order) should be preserved exactly in the output String."), arguments(new char[] { '\ud83d', '\ude00', '\ud83d', '\ude80' }, null, "😀🚀", "Array containing two valid surrogate pairs (😀 and 🚀) should be converted to the corresponding two‑character String."), arguments(new char[] { ' ', '\t', '\n', 'A' }, null, " \t\nA", "Whitespace characters (space, tab, newline) must be retained unchanged in the resulting String."), arguments(new char[] { '\u0001', '\uffff' }, null, "\u0001\uffff", "Control characters and the highest BMP code point should survive the conversion unchanged."), arguments(new char[] { 'h', 'i' }, throwingConverter, "hi", "Even when a Converter implementation throws on any method call, CharArrayConversions.toString must not invoke it and should still return the correct String."));
    }

    static Stream<Arguments> exceptionCasesProvider() {
        return Stream.of(arguments(null, null, NullPointerException.class, "Passing null as the source object results in a NullPointerException."), arguments("not a char array", null, ClassCastException.class, "If the source object is not a char[], a ClassCastException is thrown."));
    }

    @Test
    void toString_convertsCharArrayToString() {
        // given
        char[] input = new char[] { 'h', 'e', 'l', 'l', 'o' };
        // not used by the method
        Converter converter = null;
        // when
        String result = CharArrayConversions.toString(input, converter);
        // then
        assertEquals("hello", result);
    }

    @Test
    void toString_convertsEmptyCharArrayToEmptyString() {
        // given
        char[] input = new char[0];
        Converter converter = null;
        // when
        String result = CharArrayConversions.toString(input, converter);
        // then
        assertEquals("", result);
    }

    @Test
    void toString_nullCharArray_throwsNullPointerException() {
        // given
        char[] input = null;
        Converter converter = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toString(input, converter));
    }

    @Test
    void toString_invalidObjectType_throwsClassCastException() {
        // given
        Object notACharArray = "not a char array";
        Converter converter = null;
        // when / then
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toString(notACharArray, converter));
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("successfulCasesProvider")
    void toString_success(Object from, Converter converter, String expected, String description) {
        // when
        String result = CharArrayConversions.toString(from, converter);
        // then
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("exceptionCasesProvider")
    void toString_exceptions(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CharArrayConversions.toString(from, converter), description);
    }
}
