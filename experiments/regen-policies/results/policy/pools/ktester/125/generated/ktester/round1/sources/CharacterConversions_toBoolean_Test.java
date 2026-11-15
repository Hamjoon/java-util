package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toBoolean_Test {

    /**
     * A real {@code Converter} instance – the implementation does not depend on it.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The method under test does not use the converter, but we create a
        // legitimate instance to satisfy the signature in normal scenarios.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Provides every character that the implementation treats as “true”.
     */
    static Stream<Arguments> trueCases() {
        return Stream.of(// control character with numeric value 1
        arguments((char) 1, null), arguments('t', null), arguments('T', null), arguments('1', null), arguments('y', null), arguments('Y', null));
    }

    /**
     * Characters that must evaluate to {@code false}.
     */
    static Stream<Arguments> falseCases() {
        return Stream.of(arguments('0', null), arguments('f', null), arguments('F', null), arguments('n', null), arguments('N', null), // null character (0x0000)
        arguments((char) 0, null), // control character with numeric value 2
        arguments((char) 2, null));
    }

    /**
     * Objects that cannot be cast to {@code char}.
     */
    static Stream<Arguments> unsupportedTypeCases() {
        return Stream.of(arguments(Integer.valueOf(5)), arguments("true"), arguments(Boolean.TRUE));
    }

    @Test
    void shouldReturnTrueForRecognisedTrueCharacters() {
        // Characters that must evaluate to true according to the implementation.
        Object[] trueValues = { // control character with numeric value 1
        (char) 1, 't', 'T', '1', 'y', 'Y' };
        for (Object val : trueValues) {
            boolean result = CharacterConversions.toBoolean(val, converter);
            assertTrue(result, () -> "Expected true for character [" + val + "] but got false");
        }
    }

    @Test
    void shouldReturnFalseForOtherCharacters() {
        // A selection of characters that are *not* considered true.
        Object[] falseValues = { (char) 0, '0', 'f', 'F', 'n', 'N', 'a', 'b', 'c', 'z', 'Z', ' ', '\n', '\r' };
        for (Object val : falseValues) {
            boolean result = CharacterConversions.toBoolean(val, converter);
            assertFalse(result, () -> "Expected false for character [" + val + "] but got true");
        }
    }

    @Test
    void shouldThrowClassCastExceptionWhenObjectIsNotACharacter() {
        // The implementation casts the argument to char; any non‑character type
        // should cause a ClassCastException.
        Object[] nonCharObjects = { // String
        "t", // Integer
        1, // Boolean
        true, // Plain Object
        new Object() };
        for (Object obj : nonCharObjects) {
            assertThrows(ClassCastException.class, () -> CharacterConversions.toBoolean(obj, converter), () -> "Expected ClassCastException for object of type " + obj.getClass());
        }
    }

    @Test
    void shouldThrowNullPointerExceptionWhenArgumentIsNull() {
        // Casting null to char triggers a NullPointerException.
        assertThrows(NullPointerException.class, () -> CharacterConversions.toBoolean(null, converter));
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("trueCases")
    void shouldReturnTrueForAcceptedCharacters(Object from, Converter conv) {
        // The method casts the first argument to {@code char} and returns true
        // for the defined set of characters.
        boolean result = CharacterConversions.toBoolean(from, conv);
        assertTrue(result, () -> "Expected true for character [" + from + "]");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenFromIsNull() {
        // Casting a null reference to {@code char} triggers a NPE.
        assertThrows(NullPointerException.class, () -> CharacterConversions.toBoolean(null, null), "Expected NullPointerException when 'from' is null");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("unsupportedTypeCases")
    void shouldThrowClassCastExceptionForUnsupportedFrom(Object from) {
        // Any non‑character type causes a {@code ClassCastException} during the cast.
        assertThrows(ClassCastException.class, () -> CharacterConversions.toBoolean(from, null), () -> "Expected ClassCastException for from type " + (from == null ? "null" : from.getClass()));
    }
}
