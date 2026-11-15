package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toLong_Test {

    /**
     * Mocked {@link Converter} – not used by the method under test.
     */
    @Mock
    private Converter mockConverter;

    /* -------------------------------------------------------------
     *  Helper mock converter used for the “converter argument ignored”
     *  test cases. It does not implement any behaviour – the test only
     *  needs a non‑null instance.
     * ------------------------------------------------------------- */
    private static class MockConverter implements Converter {
        // No implementation required for the tests.
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validConversionsProvider() {
        return Stream.of(// Standard ASCII characters
        arguments(new Character('A'), null, 65L, "Convert a standard ASCII character 'A' (Unicode 65) to its long representation."), arguments(new Character('\u0000'), null, 0L, "Convert the null character (Unicode 0) to long; edge case for the lowest possible char value."), arguments(new Character('\uFFFF'), null, 65535L, "Convert the highest char value (Unicode 0xFFFF) to long; tests upper bound handling."), // Additional valid Unicode characters
        arguments(new Character('\u1234'), null, 4660L, "Convert a non‑ASCII Unicode character (U+1234) to its long representation."), arguments(Character.valueOf('\uD800'), null, 55296L, "Convert the lowest high‑surrogate code unit (U+D800) to long."), arguments(Character.valueOf('\uDFFF'), null, 57343L, "Convert the highest low‑surrogate code unit (U+DFFF) to long."), // Converter argument supplied (non‑null) – should be ignored
        arguments(new Character('Z'), new MockConverter(), 90L, "Non‑null converter does not affect conversion of character 'Z' (Unicode 90)."), arguments(new Character('\u00A9'), new MockConverter(), 169L, "Non‑null converter does not affect conversion of © (Unicode 169)."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidInputsProvider() {
        return Stream.of(arguments(null, null, NullPointerException.class, "Passing null as the source object should trigger a NullPointerException."), arguments(Integer.valueOf(65), null, ClassCastException.class, "Providing an Integer instead of a Character should cause a ClassCastException."), arguments("A", null, ClassCastException.class, "Supplying a String (even if it contains a single character) must result in a ClassCastException."));
    }

    @Test
    void toLong_WithCharacter_ReturnsCorrectLong() {
        // GIVEN
        Character input = 'Z';
        // WHEN
        long result = CharacterConversions.toLong(input, mockConverter);
        // THEN
        assertEquals('Z', result, "The long value should equal the numeric value of the character");
        // Verify that the converter was never used.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toLong_WithPrimitiveCharBoxed_ReturnsCorrectLong() {
        // GIVEN
        // Autoboxing a primitive char to Character
        Object input = 'a';
        // WHEN
        long result = CharacterConversions.toLong(input, mockConverter);
        // THEN
        assertEquals('a', result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toLong_WithInvalidType_ThrowsClassCastException() {
        // GIVEN
        Integer invalid = 123;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toLong(invalid, mockConverter), "Casting a non‑character object should throw ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toLong_WithNull_ThrowsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterConversions.toLong(nullInput, mockConverter), "Passing null should result in NullPointerException because of unboxing");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Valid conversion test cases (including edge‑cases and extra
     *  Unicode values). The converter argument is either {@code null}
     *  or a mock – in both situations the method must ignore it.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionsProvider")
    void toLong_ValidConversions_ReturnsExpectedLong(Object from, Converter converter, long expected, String description) {
        // WHEN
        long result = CharacterConversions.toLong(from, converter);
        // THEN
        assertEquals(expected, result, () -> "Expected long value " + expected + " for input " + description);
        // The method must never touch the converter (if a mock was supplied).
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }

    /* -------------------------------------------------------------
     *  Invalid input test cases – expect the method to throw the
     *  appropriate runtime exception. The converter argument is not
     *  relevant for these failures.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidInputsProvider")
    void toLong_InvalidInputs_ThrowsException(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> CharacterConversions.toLong(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
        // No interaction with a mock converter (if one was supplied)
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }
}
