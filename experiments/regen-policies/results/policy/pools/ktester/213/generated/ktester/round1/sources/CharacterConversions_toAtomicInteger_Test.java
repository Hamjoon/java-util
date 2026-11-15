package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toAtomicInteger_Test {

    @Mock
    private Converter // Converter is not used by the method, but we provide a mock
    mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock was not interacted with – the method under test should ignore it
        verifyNoInteractions(mockConverter);
    }

    private static Stream<Arguments> validCharacterProvider() {
        return Stream.of(// Basic ASCII
        Arguments.of(Character.valueOf('A'), 65), // Greek capital Omega (U+03A9)
        Arguments.of(Character.valueOf('\u03A9'), 937), // Minimum char value (null character)
        Arguments.of(Character.valueOf('\u0000'), 0), // Maximum char value (U+FFFF)
        Arguments.of(Character.valueOf('\uFFFF'), 65535), // Private use area character (U+1234)
        Arguments.of(Character.valueOf('\u1234'), 4660), // High surrogate (U+D83D)
        Arguments.of(Character.valueOf('\uD83D'), 55357), // Low surrogate (U+DC00)
        Arguments.of(Character.valueOf('\uDC00'), 56320), // Character with highest bit set (U+8000)
        Arguments.of(new Character('\u8000'), 32768));
    }

    private static Stream<Arguments> errorProvider() {
        return Stream.of(// Null argument – casting null to char triggers NullPointerException
        Arguments.of(null, NullPointerException.class), // Wrong type – String cannot be cast to char
        Arguments.of("String", ClassCastException.class));
    }

    @Test
    void toAtomicInteger_WithCharacter_ReturnsCorrectAtomicInteger() {
        // GIVEN
        // Unicode value 90
        Character input = 'Z';
        // WHEN
        AtomicInteger result = CharacterConversions.toAtomicInteger(input, mockConverter);
        // THEN
        assertNotNull(result, "Resulting AtomicInteger should not be null");
        assertEquals((int) input.charValue(), result.get(), "AtomicInteger should contain the int value of the character");
    }

    @Test
    void toAtomicInteger_WithInvalidType_ThrowsClassCastException() {
        // GIVEN
        String invalidInput = "not a char";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toAtomicInteger(invalidInput, mockConverter), "Converting a non‑character object should throw ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Valid character conversions – parameterized test
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("validCharacterProvider")
    void toAtomicInteger_ValidCharacters_ReturnsExpectedAtomicInteger(Character from, int expected) {
        // WHEN
        AtomicInteger result = CharacterConversions.toAtomicInteger(from, mockConverter);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertEquals(expected, result.get(), () -> "AtomicInteger should contain the int value of the character '" + from + "'");
    }

    /* -------------------------------------------------------------
     *  Error cases – null and non‑character inputs
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("errorProvider")
    void toAtomicInteger_InvalidInputs_ThrowsException(Object from, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> CharacterConversions.toAtomicInteger(from, mockConverter), () -> "Converting " + (from == null ? "null" : from.getClass().getSimpleName()) + " should throw " + expectedException.getSimpleName());
    }
}
