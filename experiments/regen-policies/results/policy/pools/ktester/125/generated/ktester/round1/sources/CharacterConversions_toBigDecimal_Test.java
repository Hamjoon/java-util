package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toBigDecimal_Test {

    /**
     * A mock {@link Converter} – the method under test does not call any of its behaviour.
     */
    @Mock
    private Converter mockConverter;

    /* -------------------------------------------------------------
     *  Helper dummy converter – used to prove the method truly ignores the argument
     * ------------------------------------------------------------- */
    private static class DummyConverter implements Converter {
        // No implementation needed; the interface is empty for the purpose of this test.
    }

    /**
     * Provides a stream of arguments for successful character → BigDecimal conversions.
     * Each argument consists of:
     *   - the {@code Object} to be passed as the {@code from} parameter,
     *   - the expected {@link BigDecimal} result,
     *   - a short description used only for readability.
     */
    private static Stream<Arguments> validCharacterProvider() {
        return Stream.of(// Standard alphabetic character
        Arguments.of(Character.valueOf('A'), BigDecimal.valueOf(65L), "Character 'A' (Unicode 65)"), // Null character (U+0000)
        Arguments.of(Character.valueOf('\u0000'), BigDecimal.valueOf(0L), "Null character '\\u0000'"), // Numeric character '9'
        Arguments.of(Character.valueOf('9'), BigDecimal.valueOf(57L), "Character '9' (Unicode 57)"), // Maximum valid char value (U+FFFF)
        Arguments.of(Character.valueOf('\uFFFF'), BigDecimal.valueOf(65535L), "Maximum char '\\uFFFF'"), // Minimum non‑zero char (U+0001)
        Arguments.of(Character.valueOf('\u0001'), BigDecimal.valueOf(1L), "Minimum non‑zero char '\\u0001'"), // Primitive char literal – autoboxed to Character
        Arguments.of('Z', BigDecimal.valueOf(90L), "Primitive char literal 'Z' (Unicode 90)"), // Non‑null converter argument – method should ignore it
        Arguments.of(Character.valueOf('a'), BigDecimal.valueOf(97L), "Character 'a' with non‑null converter"));
    }

    @Test
    void toBigDecimal_givenCharacter_returnsCorrectBigDecimal() {
        // GIVEN
        // Unicode 65
        Character input = 'A';
        // WHEN
        BigDecimal result = CharacterConversions.toBigDecimal(input, mockConverter);
        // THEN
        assertEquals(BigDecimal.valueOf(65L), result, "BigDecimal value should correspond to the numeric value of the character");
    }

    @Test
    void toBigDecimal_givenCharPrimitiveWrappedAsObject_returnsCorrectBigDecimal() {
        // GIVEN
        // Unicode 122
        Object input = Character.valueOf('z');
        // WHEN
        BigDecimal result = CharacterConversions.toBigDecimal(input, mockConverter);
        // THEN
        assertEquals(BigDecimal.valueOf(122L), result, "BigDecimal value should correspond to the numeric value of the character");
    }

    @Test
    void toBigDecimal_givenNonCharacterObject_throwsClassCastException() {
        // GIVEN
        Object input = "not a character";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toBigDecimal(input, mockConverter), "A non‑Character object should cause a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validCharacterProvider")
    void toBigDecimal_validCharacters_returnsExpectedBigDecimal(Object input, BigDecimal expected, String _description) {
        // GIVEN
        Converter converterToUse = (input == Character.valueOf('a')) ? new DummyConverter() : mockConverter;
        // WHEN
        BigDecimal result = CharacterConversions.toBigDecimal(input, converterToUse);
        // THEN
        assertEquals(expected, result, () -> "Expected BigDecimal " + expected + " for input " + input);
        // Verify that the method never interacts with the converter (mock or dummy)
        verifyNoInteractions(converterToUse);
    }

    @Test
    void toBigDecimal_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterConversions.toBigDecimal(input, mockConverter), "Passing a null `from` value should trigger a NullPointerException");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toBigDecimal_nonCharacterObject_throwsClassCastException() {
        // GIVEN
        Object input = Integer.valueOf(123);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toBigDecimal(input, mockConverter), "Providing a non‑Character object should cause a ClassCastException");
        verifyNoInteractions(mockConverter);
    }
}
