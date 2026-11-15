package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toBigInteger_Test {

    private static Stream<Arguments> validCharacterProvider() {
        return Stream.of(// regular characters
        // 65
        Arguments.of('A', null, BigInteger.valueOf('A')), // 0
        Arguments.of('\u0000', null, BigInteger.valueOf('\u0000')), // 65535
        Arguments.of('\uffff', null, BigInteger.valueOf('\uffff')), // 90
        Arguments.of('Z', null, BigInteger.valueOf('Z')), // additional edge cases
        // 32
        Arguments.of(' ', null, BigInteger.valueOf(' ')), // 55296 (high surrogate)
        Arguments.of('\uD800', null, BigInteger.valueOf('\uD800')), // 56320 (low surrogate)
        Arguments.of('\uDC00', null, BigInteger.valueOf('\uDC00')), // 4660
        Arguments.of('\u1234', null, BigInteger.valueOf('\u1234')), // 48
        Arguments.of('0', null, BigInteger.valueOf('0')), // 65534
        Arguments.of('\ufffe', null, BigInteger.valueOf('\ufffe')));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// null source leads to NPE because of unboxing a null Character
        Arguments.of(null, null, NullPointerException.class), // non‑Character objects cause ClassCastException when cast to char
        Arguments.of(123, null, ClassCastException.class), Arguments.of(new Object(), null, ClassCastException.class), Arguments.of("", null, ClassCastException.class), Arguments.of("123", null, ClassCastException.class), Arguments.of(true, null, ClassCastException.class));
    }

    private static Stream<Arguments> converterNotUsedProvider() {
        return Stream.of(// 65
        Arguments.of('A', BigInteger.valueOf('A')), // 90
        Arguments.of('Z', BigInteger.valueOf('Z')));
    }

    @Test
    void toBigInteger_givenCharacter_returnsBigIntegerOfCharCode() {
        // GIVEN
        // char code 90
        Character input = 'Z';
        // not used by the method
        Converter mockConverter = mock(Converter.class);
        // WHEN
        BigInteger result = CharacterConversions.toBigInteger(input, mockConverter);
        // THEN
        assertEquals(BigInteger.valueOf('Z'), result, "The BigInteger value should correspond to the char code of the input character");
    }

    @Test
    void toBigInteger_givenNull_throwsNullPointerException() {
        // GIVEN
        Object input = null;
        Converter mockConverter = mock(Converter.class);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterConversions.toBigInteger(input, mockConverter), "Passing null should result in a NullPointerException because of unboxing");
    }

    @Test
    void toBigInteger_givenNonCharacter_throwsClassCastException() {
        // GIVEN
        // not a Character
        Integer input = 123;
        Converter mockConverter = mock(Converter.class);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toBigInteger(input, mockConverter), "Passing a non‑Character should trigger a ClassCastException");
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] from=''{0}'' → {2}")
    @MethodSource("validCharacterProvider")
    void toBigInteger_validCharacters(Object from, Converter converter, BigInteger expected) {
        // WHEN
        BigInteger result = CharacterConversions.toBigInteger(from, converter);
        // THEN
        assertEquals(expected, result, () -> "Expected BigInteger value of the char code for '" + from + "'");
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] from={0} → {2}")
    @MethodSource("invalidInputProvider")
    void toBigInteger_invalidInputs(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> CharacterConversions.toBigInteger(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] from=''{0}'' with misbehaving Converter → {2}")
    @MethodSource("converterNotUsedProvider")
    void toBigInteger_converterIgnored(Character from, BigInteger expected) {
        // GIVEN a mock Converter that throws on any interaction
        Converter badConverter = mock(Converter.class, invocation -> {
            throw new AssertionError("Converter should not be used by toBigInteger");
        });
        // WHEN
        BigInteger result = CharacterConversions.toBigInteger(from, badConverter);
        // THEN
        assertEquals(expected, result, () -> "Result should be the char code for '" + from + "' even when a bad converter is supplied");
        // Verify that no method on the mock was invoked
        verifyNoInteractions(badConverter);
    }
}
