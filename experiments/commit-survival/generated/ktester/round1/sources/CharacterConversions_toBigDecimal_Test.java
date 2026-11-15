package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toBigDecimal_Test {

    /**
     * Mocked Converter – the implementation of {@code toBigDecimal} does not use it,
     *  but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /* --------------------------------------------------------------------- *
     *  Dummy Converter implementation used for the "Converter influence" tests.
     * --------------------------------------------------------------------- */
    private static class DummyConverter implements Converter {
        // No methods needed for the current tests – the conversion logic ignores this instance.
    }

    /**
     * Supplies the successful conversion arguments.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        // a dummy non‑null converter to verify that the parameter is ignored
        Converter dummyConverter = new DummyConverter();
        return Stream.of(// group: Successful conversion from Character
        Arguments.of(new Character('A'), null, BigDecimal.valueOf(65), "Converts Character('A') → 65"), Arguments.of(new Character('5'), null, BigDecimal.valueOf(53), "Converts Character('5') → 53"), // group: Additional successful conversions
        Arguments.of(new Character('\u0000'), null, BigDecimal.valueOf(0), "Converts null character '\\u0000' → 0"), Arguments.of(new Character('\uFFFF'), null, BigDecimal.valueOf(65535), "Converts max char '\\uFFFF' → 65535"), Arguments.of('Z', null, BigDecimal.valueOf(90), "Converts primitive char literal 'Z' → 90"), Arguments.of(new Character('\n'), null, BigDecimal.valueOf(10), "Converts newline character '\\n' → 10"), Arguments.of(new Character('\uD800'), null, BigDecimal.valueOf(55296), "Converts high‑surrogate '\\uD800' → 55296"), Arguments.of(new Character('\uDC00'), null, BigDecimal.valueOf(56320), "Converts low‑surrogate '\\uDC00' → 56320"), // group: Converter influence (no effect) – using a non‑null dummy converter
        Arguments.of(new Character('A'), dummyConverter, BigDecimal.valueOf(65), "Non‑null Converter does not affect conversion of 'A'"), Arguments.of(new Character('5'), dummyConverter, BigDecimal.valueOf(53), "Non‑null Converter does not affect conversion of '5'"));
    }

    /**
     * Supplies the error scenario arguments.
     */
    private static Stream<Arguments> errorScenarioProvider() {
        return Stream.of(// group: Error scenarios – invalid source types
        Arguments.of(Integer.valueOf(65), null, ClassCastException.class, "Integer source should cause ClassCastException"), Arguments.of("A", null, ClassCastException.class, "String source should cause ClassCastException"), Arguments.of(Boolean.TRUE, null, ClassCastException.class, "Boolean source should cause ClassCastException"), Arguments.of(Double.valueOf(65.0), null, ClassCastException.class, "Double source should cause ClassCastException"), Arguments.of(Float.valueOf(65.0f), null, ClassCastException.class, "Float source should cause ClassCastException"), Arguments.of(Byte.valueOf((byte) 65), null, ClassCastException.class, "Byte source should cause ClassCastException"), Arguments.of(Short.valueOf((short) 65), null, ClassCastException.class, "Short source should cause ClassCastException"), Arguments.of(Long.valueOf(65L), null, ClassCastException.class, "Long source should cause ClassCastException"), Arguments.of(new int[] { 65 }, null, ClassCastException.class, "int[] source should cause ClassCastException"), Arguments.of(new char[] { 'A' }, null, ClassCastException.class, "char[] source should cause ClassCastException"), Arguments.of(new Character[] { new Character('A') }, null, ClassCastException.class, "Character[] source should cause ClassCastException"), Arguments.of(new Object() {
        }, null, ClassCastException.class, "Anonymous Object source should cause ClassCastException"), Arguments.of(new java.util.Date(), null, ClassCastException.class, "java.util.Date source should cause ClassCastException"), // group: Error scenarios – other invalid source types (null source)
        Arguments.of(null, null, NullPointerException.class, "Null source should cause NullPointerException"));
    }

    @Test
    void toBigDecimal_WithUpperCaseLetter_ReturnsCorrectValue() {
        // given
        // Unicode code point 65
        Character input = 'A';
        // when
        BigDecimal result = CharacterConversions.toBigDecimal(input, mockConverter);
        // then
        assertEquals(BigDecimal.valueOf((int) input), result, "BigDecimal value should match the numeric code point of the character");
    }

    @Test
    void toBigDecimal_WithDigitCharacter_ReturnsCorrectValue() {
        // given
        // Unicode code point 53
        Character input = '5';
        // when
        BigDecimal result = CharacterConversions.toBigDecimal(input, mockConverter);
        // then
        assertEquals(BigDecimal.valueOf((int) input), result, "BigDecimal value should match the numeric code point of the digit character");
    }

    @Test
    void toBigDecimal_WithNullInput_ThrowsClassCastException() {
        // given
        // casting null to char triggers ClassCastException
        Object input = null;
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toBigDecimal(input, mockConverter), "Passing null (or a non‑character) should result in a ClassCastException");
    }

    @Test
    void toBigDecimal_WithNonCharacterObject_ThrowsClassCastException() {
        // given
        Object input = "not a char";
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toBigDecimal(input, mockConverter), "Passing an object that is not a Character should throw ClassCastException");
    }

    /**
     * Parameterized test covering all successful conversion scenarios.
     *
     * @param from        the source object to be converted (must be a {@link Character} or a primitive {@code char})
     * @param converter   a {@link Converter} instance – may be {@code null}, a mock, or a dummy implementation
     * @param expected    the expected {@link BigDecimal} result
     * @param description textual description of the test case (used in assertion messages)
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successfulConversionProvider")
    void toBigDecimal_SuccessfulConversions(Object from, Converter converter, BigDecimal expected, String description) {
        // when
        BigDecimal result = CharacterConversions.toBigDecimal(from, converter);
        // then
        assertEquals(expected, result, () -> "Failed: " + description + " – expected " + expected + " but got " + result);
    }

    /**
     * Parameterized test covering all error scenarios where the source object
     * cannot be cast to {@code char}.
     *
     * @param from               the invalid source object
     * @param converter          a {@link Converter} instance (always {@code null} for these tests)
     * @param expectedException  the exception class that should be thrown
     * @param description        description of the scenario (used in assertion messages)
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("errorScenarioProvider")
    void toBigDecimal_ErrorScenarios(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> CharacterConversions.toBigDecimal(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for: " + description);
    }
}
