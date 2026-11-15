package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toString_Test {

    @Mock
    private Converter // not used by the method but required by signature
    converter;

    /**
     * Supplies normal char[] test data.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> normalCharArrayProvider() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of(new char[] { 'a', 'b', 'c' }, "abc"), org.junit.jupiter.params.provider.Arguments.of(new char[] { 'J', 'a', 'v', 'a' }, "Java"), org.junit.jupiter.params.provider.Arguments.of(new char[] {}, ""));
    }

    @Test
    @DisplayName("toString converts a non‑empty char[] to the expected String")
    void toString_convertsCharArrayToString() {
        // given
        char[] source = { 'J', 'a', 'v', 'a' };
        // when
        String result = CharArrayConversions.toString(source, converter);
        // then
        assertEquals("Java", result);
    }

    @Test
    @DisplayName("toString returns an empty String for an empty char[]")
    void toString_emptyCharArrayReturnsEmptyString() {
        // given
        char[] source = {};
        // when
        String result = CharArrayConversions.toString(source, converter);
        // then
        assertEquals("", result);
    }

    @Test
    @DisplayName("toString throws NullPointerException when source is null")
    void toString_nullInputThrowsNPE() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toString(source, converter));
    }

    /**
     * Explicit test for a non‑empty array (kept for readability alongside the
     * parameterized test).
     */
    @Test
    @DisplayName("toString converts a non‑empty char[] to the expected String")
    void toString_convertsNonEmptyCharArray() {
        char[] source = { 'J', 'a', 'v', 'a' };
        String result = CharArrayConversions.toString(source, converter);
        assertEquals("Java", result);
    }

    @Test
    @DisplayName("toString throws ClassCastException when source is a String")
    void toString_invalidFromType_String() {
        Object source = "not a char array";
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toString(source, converter));
    }

    @Test
    @DisplayName("toString throws ClassCastException when source is an int[]")
    void toString_invalidFromType_IntArray() {
        Object source = new int[] { 1, 2, 3 };
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toString(source, converter));
    }
}
