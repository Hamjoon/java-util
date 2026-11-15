package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class CharacterArrayConversions_toString_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is required as a parameter but the method under test does not use it.
        // Initialise it with default options.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        converter = null;
    }

    static Stream<Arguments> normalBehaviorProvider() {
        return Stream.of(Arguments.of(new Character[] { 'a', 'b', 'c' }, "abc", "Converts a typical Character[] containing three letters into the corresponding String."), Arguments.of(new Character[] {}, "", "Handles an empty Character[] and returns an empty String."), Arguments.of(new Character[] { null, 'x', null }, "nullxnull", "Array contains null elements; StringBuilder.append(null) yields the literal \"null\" for each null entry."));
    }

    static Stream<Arguments> exceptionProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "The source object is null; casting to Character[] succeeds (null) but accessing length triggers NPE."), Arguments.of(new String[] { "not", "a", "char", "array" }, ClassCastException.class, "The source object is a String[] rather than a Character[]; the explicit cast causes a ClassCastException."), Arguments.of(new int[] { 1, 2, 3 }, ClassCastException.class, "The source object is an int[] (primitive array); casting to Character[] is illegal and throws ClassCastException."));
    }

    @Test
    void toString_normalArray_returnsConcatenatedString() {
        // given
        Character[] chars = new Character[] { 'J', 'a', 'v', 'a' };
        // when
        String result = CharacterArrayConversions.toString(chars, converter);
        // then
        assertEquals("Java", result);
    }

    @Test
    void toString_emptyArray_returnsEmptyString() {
        // given
        Character[] chars = new Character[0];
        // when
        String result = CharacterArrayConversions.toString(chars, converter);
        // then
        assertEquals("", result);
    }

    @Test
    void toString_nullArray_throwsNullPointerException() {
        // given
        Object nullArray = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toString(nullArray, converter));
    }

    @Test
    void toString_arrayContainingNullElement_throwsNullPointerException() {
        // given
        Character[] chars = new Character[] { 'A', null, 'B' };
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toString(chars, converter));
    }

    @Test
    void toString_wrongSourceType_throwsClassCastException() {
        // given
        Object notACharArray = new String[] { "not", "a", "char", "array" };
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterArrayConversions.toString(notACharArray, converter));
    }

    /* -------------------------------------------------------------
     *  Normal behaviour
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{2}")
    @MethodSource("normalBehaviorProvider")
    void toString_normalBehavior(Character[] from, String expected, String description) {
        // when
        String result = CharacterArrayConversions.toString(from, converter);
        // then
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Exception scenarios
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{2}")
    @MethodSource("exceptionProvider")
    void toString_exceptionScenarios(Object from, Class<? extends Throwable> expectedException, String description) {
        // then
        assertThrows(expectedException, () -> CharacterArrayConversions.toString(from, converter), description);
    }
}
