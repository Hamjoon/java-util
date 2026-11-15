package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toAtomicLong_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    mockConverter;

    private static Stream<Arguments> validCharacterProvider() {
        return Stream.of(Arguments.of("Standard case: Character 'A' -> 65", Character.valueOf('A'), 65L), Arguments.of("Boundary case: minimum char '\\u0000' -> 0", Character.valueOf('\u0000'), 0L), Arguments.of("Boundary case: maximum char '\\uFFFF' -> 65535", Character.valueOf('\uFFFF'), 65535L), Arguments.of("Typical numeric character: '0' -> 48 (converter ignored)", Character.valueOf('0'), 48L));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Null input
        Arguments.of("Null source object should cause NullPointerException", null, NullPointerException.class), // Wrong type – String (not a Character)
        Arguments.of("String input should cause ClassCastException", "A", ClassCastException.class), // Wrong type – Integer
        Arguments.of("Integer input should cause ClassCastException", Integer.valueOf(123), ClassCastException.class), // Boolean
        Arguments.of("Boolean input should cause ClassCastException", Boolean.TRUE, ClassCastException.class), // Byte
        Arguments.of("Byte input should cause ClassCastException", Byte.valueOf((byte) 1), ClassCastException.class), // Short
        Arguments.of("Short input should cause ClassCastException", Short.valueOf((short) 2), ClassCastException.class), // Long
        Arguments.of("Long input should cause ClassCastException", Long.valueOf(123L), ClassCastException.class), // Float
        Arguments.of("Float input should cause ClassCastException", Float.valueOf(12.34f), ClassCastException.class), // Double
        Arguments.of("Double input should cause ClassCastException", Double.valueOf(56.78), ClassCastException.class), // BigInteger
        Arguments.of("BigInteger input should cause ClassCastException", new java.math.BigInteger("12345"), ClassCastException.class), // Arbitrary Object
        Arguments.of("Object input should cause ClassCastException", new Object(), ClassCastException.class));
    }

    @Test
    @DisplayName("toAtomicLong converts a Character to the corresponding AtomicLong")
    void testToAtomicLongWithCharacter() {
        // given
        // Unicode 65
        Character input = 'A';
        // when
        AtomicLong result = CharacterConversions.toAtomicLong(input, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(65L, result.get(), "AtomicLong should contain the numeric value of the character");
    }

    @Test
    @DisplayName("toAtomicLong converts a primitive char (autoboxed) to the corresponding AtomicLong")
    void testToAtomicLongWithPrimitiveChar() {
        // given
        // 255
        char input = '\u00FF';
        // when
        AtomicLong result = CharacterConversions.toAtomicLong(input, mockConverter);
        // then
        assertEquals(255L, result.get(), "AtomicLong should contain the numeric value of the primitive char");
    }

    @Test
    @DisplayName("toAtomicLong handles the maximum char value")
    void testToAtomicLongWithMaxChar() {
        // given
        // 0xFFFF = 65535
        char maxChar = Character.MAX_VALUE;
        // when
        AtomicLong result = CharacterConversions.toAtomicLong(maxChar, mockConverter);
        // then
        assertEquals((long) maxChar, result.get(), "AtomicLong should contain the numeric value of Character.MAX_VALUE");
    }

    @Test
    @DisplayName("toAtomicLong throws NullPointerException when null is supplied")
    void testToAtomicLongWithNull() {
        // given
        Object nullInput = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toAtomicLong(nullInput, mockConverter), "Casting null to char should result in NullPointerException");
    }

    /* -----------------------------------------------------------
     *  Valid character inputs – parameterized to cover several cases
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validCharacterProvider")
    @DisplayName("toAtomicLong converts valid character inputs")
    void testToAtomicLong_ValidInputs(String description, Object input, long expectedValue) {
        // when
        AtomicLong result = CharacterConversions.toAtomicLong(input, mockConverter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedValue, result.get(), "AtomicLong should contain the numeric value of the character");
    }

    /* -----------------------------------------------------------
     *  Invalid inputs – null and non‑character types
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidInputProvider")
    @DisplayName("toAtomicLong throws expected exceptions for invalid inputs")
    void testToAtomicLong_InvalidInputs(String description, Object input, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> CharacterConversions.toAtomicLong(input, mockConverter), description);
    }

    /* -----------------------------------------------------------
     *  Additional explicit tests for clarity (non‑parameterized)
     * ----------------------------------------------------------- */
    @Test
    @DisplayName("toAtomicLong converts a primitive char (autoboxed) correctly")
    void testToAtomicLong_WithPrimitiveChar() {
        // given
        // 255
        char input = '\u00FF';
        // when
        AtomicLong result = CharacterConversions.toAtomicLong(input, mockConverter);
        // then
        assertEquals(255L, result.get(), "AtomicLong should contain the numeric value of the primitive char");
    }

    @Test
    @DisplayName("toAtomicLong handles the maximum char value")
    void testToAtomicLong_WithMaxChar() {
        // given
        // 0xFFFF = 65535
        char maxChar = Character.MAX_VALUE;
        // when
        AtomicLong result = CharacterConversions.toAtomicLong(maxChar, mockConverter);
        // then
        assertEquals((long) maxChar, result.get(), "AtomicLong should contain the numeric value of Character.MAX_VALUE");
    }
}
