package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;

class CharacterConversions_toBigDecimal_Test {

    /**
     * Converter instance – not used by the method but required for the signature.
     */
    private Converter converter;

    /**
     * Simple holder used in the parameterized source to indicate that the
     * payload should be passed together with a *mock* Converter.
     */
    private static final class MockInfo {

        final Object payload;

        MockInfo(Object payload) {
            this.payload = payload;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The Converter constructor requires a ConverterOptions instance.
        // Use the default options – the method under test does not depend on it.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        converter = null;
    }

    private static Stream<Arguments> validCharacterProvider() {
        return Stream.of(// Regular uppercase character 'A' (Unicode 65)
        Arguments.of("Convert uppercase 'A'", 'A', "65"), // Null character (Unicode 0)
        Arguments.of("Convert null character '\\u0000'", '\u0000', "0"), // Maximum char value (Unicode 65535)
        Arguments.of("Convert max char '\\uffff'", '\uffff', "65535"), // Uppercase 'Z' with a mock Converter (should be ignored)
        Arguments.of("Convert 'Z' with mock Converter", new MockInfo('Z'), "90"), // Digit character '0' (Unicode 48)
        Arguments.of("Convert digit '0'", '0', "48"), // Space character (Unicode 32)
        Arguments.of("Convert space character", ' ', "32"), // Copyright symbol '©' (Unicode 169)
        Arguments.of("Convert © symbol", '©', "169"), // Latin small letter e with acute 'é' (Unicode 233)
        Arguments.of("Convert é character", 'é', "233"), // High‑surrogate char (Unicode 55296)
        Arguments.of("Convert high‑surrogate '\\ud800'", '\ud800', "55296"), // Low‑surrogate char (Unicode 56320)
        Arguments.of("Convert low‑surrogate '\\udc00'", '\udc00', "56320"), // Uppercase 'G' with a mock Converter that would throw if used
        Arguments.of("Convert 'G' with throwing mock Converter", new MockInfo('G'), "71"));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Integer instead of Character
        Arguments.of("Integer input 5", 5, ClassCastException.class), // Null input
        Arguments.of("Null input", null, NullPointerException.class), // Character wrapper object (explicit Character instance) – cast to char fails
        Arguments.of("Character wrapper object", new Character('A'), ClassCastException.class), // String containing a single character – not a Character
        Arguments.of("String 'A'", "A", ClassCastException.class), // Boolean value
        Arguments.of("Boolean true", true, ClassCastException.class), // int array
        Arguments.of("int array", new int[] { 65 }, ClassCastException.class), // Generic Object instance
        Arguments.of("Plain Object", new Object(), ClassCastException.class));
    }

    @Test
    void toBigDecimal_WithUpperCaseCharacter_ReturnsCorrectBigDecimal() {
        // given
        // Unicode code point 65
        Character input = 'A';
        // when
        BigDecimal result = CharacterConversions.toBigDecimal(input, converter);
        // then
        assertEquals(BigDecimal.valueOf(65L), result, "BigDecimal value should match the numeric value of the character 'A'");
    }

    @Test
    void toBigDecimal_WithDigitCharacter_ReturnsCorrectBigDecimal() {
        // given
        // Unicode code point 48
        Character input = '0';
        // when
        BigDecimal result = CharacterConversions.toBigDecimal(input, converter);
        // then
        assertEquals(BigDecimal.valueOf(48L), result, "BigDecimal value should match the numeric value of the character '0'");
    }

    @Test
    void toBigDecimal_WithNullInput_ThrowsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toBigDecimal(input, converter), "Passing null should result in a NullPointerException");
    }

    @Test
    void toBigDecimal_WithNonCharacterObject_ThrowsClassCastException() {
        // given
        Object input = "not a char";
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toBigDecimal(input, converter), "Passing a non‑character object should cause a ClassCastException");
    }

    @Test
    void toBigDecimal_WithPrimitiveCharWrappedInObject_ReturnsCorrectBigDecimal() {
        // given
        // Autoboxed to Character when stored as Object
        Object input = (char) 'z';
        // when
        BigDecimal result = CharacterConversions.toBigDecimal(input, converter);
        // then
        assertEquals(BigDecimal.valueOf((int) 'z'), result, "BigDecimal value should match the numeric value of the character 'z'");
    }

    /* -------------------------------------------------------------
     *  Parameterized test for all *valid* character conversion cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validCharacterProvider")
    void toBigDecimal_ValidCharacters(String description, Object input, String expectedValue) {
        // given
        // input may be a Character, primitive char autoboxed, or a mock Converter
        // The test framework already creates a real Converter for us; for the
        // case where a mock Converter is required we replace it below.
        if (input instanceof MockInfo) {
            MockInfo mockInfo = (MockInfo) input;
            // Create a mock that throws on any method call to prove it is ignored.
            Converter mockConverter = mock(Converter.class, invocation -> {
                throw new IllegalStateException("Converter should not be used");
            });
            converter = mockConverter;
            input = mockInfo.payload;
        }
        // when
        BigDecimal result = CharacterConversions.toBigDecimal(input, converter);
        // then
        assertEquals(new BigDecimal(expectedValue), result, () -> "Unexpected BigDecimal for case: " + description);
    }

    /* -------------------------------------------------------------
     *  Parameterized test for invalid input handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidInputProvider")
    void toBigDecimal_InvalidInputs(String description, Object input, Class<? extends Throwable> expectedException) {
        // given
        // No special setup required; the real converter is fine.
        // when / then
        assertThrows(expectedException, () -> CharacterConversions.toBigDecimal(input, converter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
