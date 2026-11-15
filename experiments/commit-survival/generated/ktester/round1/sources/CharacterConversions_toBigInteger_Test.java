package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toBigInteger_Test {

    /**
     * The Converter instance is required by the method signature but is not used
     *  by {@code CharacterConversions.toBigInteger}.  A mock is sufficient.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A minimal stub implementation of {@link Converter}. It does nothing
     * because {@code CharacterConversions.toBigInteger} never calls any
     * method on the converter.
     */
    private static class DummyConverter implements Converter {
        // No implementation needed.
    }

    /**
     * A converter that throws an exception if any method is invoked.
     * Used to verify that {@code toBigInteger} truly ignores the converter.
     */
    private static class ExplodingConverter implements Converter {
        // If the real Converter interface had methods, they would throw here.
        // Since the interface is empty for this test, the class remains empty.
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Simple letters
        Arguments.of("Convert Character('A') (Unicode 65)", Character.valueOf('A'), null, BigInteger.valueOf(65)), Arguments.of("Convert Character('z') (Unicode 122)", Character.valueOf('z'), null, BigInteger.valueOf(122)), // Boundary values
        Arguments.of("Convert null character '\\u0000' (Unicode 0)", Character.valueOf('\u0000'), null, BigInteger.ZERO), Arguments.of("Convert max char '\\uFFFF' (Unicode 65535)", Character.valueOf('\uFFFF'), null, BigInteger.valueOf(65535)), // Non‑ASCII character with a non‑null converter (ignored)
        Arguments.of("Convert Greek capital omega '\\u03A9' (Unicode 937) with DummyConverter", Character.valueOf('Ω'), new DummyConverter(), BigInteger.valueOf(937)), // Additional characters
        Arguments.of("Convert space character ' ' (Unicode 32)", Character.valueOf(' '), null, BigInteger.valueOf(32)), Arguments.of("Convert newline character '\\n' (Unicode 10)", Character.valueOf('\n'), null, BigInteger.valueOf(10)), Arguments.of("Convert high‑surrogate '\\uD800' (Unicode 55296)", Character.valueOf('\uD800'), null, BigInteger.valueOf(55296)), Arguments.of("Convert low‑surrogate '\\uDC00' (Unicode 56320)", Character.valueOf('\uDC00'), null, BigInteger.valueOf(56320)), Arguments.of("Convert digit character '5' (Unicode 53)", Character.valueOf('5'), null, BigInteger.valueOf(53)), // Primitive char literal – autoboxed to Character
        Arguments.of("Convert primitive char literal 'B' (Unicode 66)", 'B', null, BigInteger.valueOf(66)), // ExplodingConverter – should be ignored
        Arguments.of("Convert Greek capital omega with ExplodingConverter (should ignore)", Character.valueOf('Ω'), new ExplodingConverter(), BigInteger.valueOf(937)));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// Null input
        Arguments.of("Null 'from' reference should throw NullPointerException", null, null, NullPointerException.class), // Wrong wrapper types
        Arguments.of("Integer cannot be cast to char – expect ClassCastException", Integer.valueOf(5), null, ClassCastException.class), Arguments.of("String cannot be cast to char – expect ClassCastException", "5", null, ClassCastException.class), Arguments.of("Arbitrary Object cannot be cast to char – expect ClassCastException", new Object(), null, ClassCastException.class), // Primitive wrapper types other than Character
        Arguments.of("Byte cannot be cast to char – expect ClassCastException", Byte.valueOf((byte) 7), null, ClassCastException.class), Arguments.of("Short cannot be cast to char – expect ClassCastException", Short.valueOf((short) 10), null, ClassCastException.class), Arguments.of("Long cannot be cast to char – expect ClassCastException", Long.valueOf(100L), null, ClassCastException.class), Arguments.of("Float cannot be cast to char – expect ClassCastException", Float.valueOf(1.2f), null, ClassCastException.class), Arguments.of("Double cannot be cast to char – expect ClassCastException", Double.valueOf(3.14), null, ClassCastException.class), Arguments.of("Boolean cannot be cast to char – expect ClassCastException", Boolean.TRUE, null, ClassCastException.class), // Array of char – not a Character
        Arguments.of("char[] cannot be cast to char – expect ClassCastException", new char[] { 'a' }, null, ClassCastException.class));
    }

    @Test
    void toBigInteger_convertsSimpleCharacter() {
        // given
        // Unicode code point 65
        Character input = 'A';
        // when
        BigInteger result = CharacterConversions.toBigInteger(input, mockConverter);
        // then
        assertEquals(BigInteger.valueOf(65), result, "The BigInteger value should correspond to the char's numeric value.");
    }

    @Test
    void toBigInteger_convertsZeroCharacter() {
        // given
        // Unicode code point 0
        Character input = '\0';
        // when
        BigInteger result = CharacterConversions.toBigInteger(input, mockConverter);
        // then
        assertEquals(BigInteger.ZERO, result, "The BigInteger value for the null character should be zero.");
    }

    @Test
    void toBigInteger_convertsMaxChar() {
        // given
        // 0xFFFF = 65535
        Character input = Character.MAX_VALUE;
        // when
        BigInteger result = CharacterConversions.toBigInteger(input, mockConverter);
        // then
        assertEquals(BigInteger.valueOf(65535), result, "The BigInteger value should match Character.MAX_VALUE.");
    }

    @Test
    void toBigInteger_nullInputThrowsNPE() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toBigInteger(input, mockConverter), "Passing null should result in a NullPointerException because of the cast to char.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toBigInteger_validConversions(String description, Object from, Converter converter, BigInteger expected) {
        // when
        BigInteger result = CharacterConversions.toBigInteger(from, converter);
        // then
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionProvider")
    void toBigInteger_invalidConversions(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> CharacterConversions.toBigInteger(from, converter), description);
    }
}
