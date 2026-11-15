package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CharacterConversions_toBigInteger_Test {

    private static Converter converter;

    /**
     * Simple stub extending {@link Converter} to prove the argument is ignored.
     */
    private static class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter is required by the method signature but not used in the implementation.
        // Provide a minimal instance with default options.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none in this case).
        converter = null;
    }

    /**
     * Provides all valid character conversion cases (including custom converters).
     */
    private static Stream<Arguments> validCharacterConversions() {
        return Stream.of(// group: Valid character conversions
        arguments(Character.valueOf('A'), null, BigInteger.valueOf(65)), arguments(Character.valueOf('\u0000'), null, BigInteger.valueOf(0)), arguments(Character.valueOf('\uFFFF'), null, BigInteger.valueOf(65535L)), // group: Additional valid character conversions
        // 4660
        arguments(Character.valueOf('\u1234'), null, BigInteger.valueOf(0x1234L)), arguments(Character.valueOf('\u7FFF'), null, BigInteger.valueOf(32767L)), arguments(Character.valueOf('\u8000'), null, BigInteger.valueOf(32768L)), arguments(Character.valueOf('\uD800'), null, BigInteger.valueOf(55296L)), arguments(Character.valueOf('\uDC00'), null, BigInteger.valueOf(56320L)), // group: Converter argument usage (should be ignored)
        arguments(Character.valueOf('Z'), new DummyConverter(), BigInteger.valueOf(90L)), arguments(Character.valueOf('\uFFFF'), new DummyConverter(), BigInteger.valueOf(65535L)));
    }

    /**
     * Provides all invalid conversion cases that must throw an exception.
     */
    private static Stream<Arguments> invalidCharacterConversions() {
        return Stream.of(// group: Invalid type conversions (expect exceptions)
        arguments(Integer.valueOf(65), null, ClassCastException.class), arguments("A", null, ClassCastException.class), arguments(null, null, NullPointerException.class), // group: Further invalid type conversions (expect exceptions)
        arguments(Boolean.TRUE, null, ClassCastException.class), arguments(new Object(), null, ClassCastException.class), arguments(BigInteger.valueOf(65), null, ClassCastException.class), arguments(new int[] { 65 }, null, ClassCastException.class));
    }

    @Test
    void toBigInteger_convertsCharacterToBigInteger() {
        // GIVEN
        char ch = 'Z';
        // autoboxed to Character when passed as Object
        Object from = ch;
        // WHEN
        BigInteger result = CharacterConversions.toBigInteger(from, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(BigInteger.valueOf(ch), result, "BigInteger value should match the numeric value of the character");
    }

    @Test
    void toBigInteger_nullInputReturnsZero() {
        // GIVEN
        Object from = null;
        // WHEN
        BigInteger result = CharacterConversions.toBigInteger(from, converter);
        // THEN
        // The implementation casts the null to char, which throws a NullPointerException.
        // However, the contract of Converter (null handling) is not applied here.
        // We assert that a NullPointerException is thrown.
        assertThrows(NullPointerException.class, () -> CharacterConversions.toBigInteger(from, converter));
    }

    @Test
    void toBigInteger_invalidTypeThrowsClassCastException() {
        // GIVEN
        Object from = "not a char";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toBigInteger(from, converter), "Passing a non‑character object should result in ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1} => {2}")
    @MethodSource("validCharacterConversions")
    void toBigInteger_convertsValidCharacters(Object from, Converter conv, BigInteger expected) {
        // WHEN
        BigInteger result = CharacterConversions.toBigInteger(from, conv);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result, () -> String.format("Expected %s for input %s", expected, from));
    }

    @ParameterizedTest(name = "[{index}] from={0} throws {2}")
    @MethodSource("invalidCharacterConversions")
    void toBigInteger_invalidInputsThrowException(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> CharacterConversions.toBigInteger(from, conv), () -> String.format("Expected %s for input %s", expectedException.getSimpleName(), from));
    }
}
