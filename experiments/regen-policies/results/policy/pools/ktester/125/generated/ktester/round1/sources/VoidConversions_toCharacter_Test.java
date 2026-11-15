package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class VoidConversions_toCharacter_Test {

    // the dependency required by the method under test
    private Converter converter;

    class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }
    }

    class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The Converter can be created with default options; it is not used by the method,
        // but we provide a real instance to avoid dealing with a null check inside the method.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
        converter = null;
    }

    /**
     * Supplies all test scenarios extracted from the JSON definition.
     *
     * @return a stream of arguments: from‑object, converter‑instance, description
     */
    private static Stream<Arguments> provideToCharacterScenarios() {
        // Helper dummy converter – a minimal concrete subclass of {@link Converter}
        class DummyConverter extends Converter {

            DummyConverter() {
                super(new ConverterOptions());
            }
        }
        return Stream.of(// Both parameters are null
        Arguments.of(null, null, "Both parameters are null – method should ignore inputs and always return Character.MIN_VALUE."), // Non‑null `from` (String) with a null converter
        Arguments.of("any string", null, "Non‑null `from` (String) with a null converter – still returns Character.MIN_VALUE."), // Numeric `from` with a dummy converter
        Arguments.of(12345, new DummyConverter(), "Numeric `from` and a dummy Converter – method does not use the converter and returns MIN_VALUE."), // Arbitrary object with a dummy converter
        Arguments.of(new Object(), new DummyConverter(), "Arbitrary object as `from` with a valid Converter – result must still be Character.MIN_VALUE."), // `from` already a Character with a dummy converter
        Arguments.of(Character.valueOf('A'), new DummyConverter(), "`from` already a Character; method does not forward it, returns MIN_VALUE."));
    }

    @Test
    void toCharacter_returnsMinValue_whenFromIsNull() {
        // given
        Object from = null;
        // when
        Character result = VoidConversions.toCharacter(from, converter);
        // then
        assertEquals(Character.MIN_VALUE, result, "toCharacter should return Character.MIN_VALUE for null input");
    }

    @Test
    void toCharacter_returnsMinValue_whenFromIsString() {
        // given
        Object from = "any string";
        // when
        Character result = VoidConversions.toCharacter(from, converter);
        // then
        assertEquals(Character.MIN_VALUE, result, "toCharacter should return Character.MIN_VALUE for a String input");
    }

    @Test
    void toCharacter_returnsMinValue_whenFromIsNumber() {
        // given
        Object from = 12345;
        // when
        Character result = VoidConversions.toCharacter(from, converter);
        // then
        assertEquals(Character.MIN_VALUE, result, "toCharacter should return Character.MIN_VALUE for a numeric input");
    }

    @Test
    void toCharacter_returnsMinValue_whenConverterIsNull() {
        // given
        Object from = "doesn't matter";
        // when
        Character result = VoidConversions.toCharacter(from, null);
        // then
        assertEquals(Character.MIN_VALUE, result, "toCharacter should return Character.MIN_VALUE even when the Converter is null");
    }

    /**
     * Parameterized test that iterates over the different scenarios described in the
     * supplied JSON test cases. Every combination should still result in {@link Character#MIN_VALUE}.
     *
     * @param from        the input object passed to {@code toCharacter}
     * @param conv        the {@link Converter} instance (may be {@code null})
     * @param description textual description of the scenario (used as the test name)
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideToCharacterScenarios")
    void toCharacter_alwaysReturnsMinValue(Object from, Converter conv, String description) {
        // Act
        Character result = VoidConversions.toCharacter(from, conv);
        // Assert
        assertEquals(Character.MIN_VALUE, result, () -> "Expected MIN_VALUE for scenario: " + description);
    }
}
