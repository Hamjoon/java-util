package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toAtomicLong_Test {

    /**
     * Mocked Converter – the method under test does not call any methods on it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A {@link Converter} implementation that throws if its {@code convert}
     * method is ever called.  The test verifies that {@code toAtomicLong}
     * does not invoke the converter.
     */
    private static final class ThrowingConverter implements Converter {

        @Override
        public Object convert(Object src, Class<?> tgt) {
            throw new RuntimeException("Converter should not be called");
        }
    }

    /**
     * Provides a set of valid {@code Character} inputs together with the
     * expected {@code long} value that should be stored inside the returned
     * {@link AtomicLong}.  The {@link Converter} argument is always the
     * {@code mockConverter} – the implementation must ignore it.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// normal conversion
        Arguments.of(Character.valueOf('Z'), 90L), // edge: null character
        Arguments.of(Character.valueOf('\u0000'), 0L), // maximum char value (0xFFFF)
        Arguments.of(Character.valueOf('\uFFFF'), 65535L), // digit character '0'
        Arguments.of(Character.valueOf('0'), 48L), // DEL control character (0x7F)
        Arguments.of(Character.valueOf('\u007F'), 127L), // high‑surrogate code unit (0xD800)
        Arguments.of(Character.valueOf('\uD800'), 55296L), // Greek capital omega (Ω)
        Arguments.of(Character.valueOf('Ω'), 937L), // smallest non‑zero character (code point 1)
        Arguments.of(Character.valueOf('\u0001'), 1L));
    }

    /**
     * Supplies inputs that are not {@link Character} (or are {@code null})
     * and the exception type that the method is expected to throw.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(65), ClassCastException.class), Arguments.of("A", ClassCastException.class), Arguments.of(null, NullPointerException.class), Arguments.of(Boolean.TRUE, ClassCastException.class), Arguments.of(new Object(), ClassCastException.class));
    }

    @Test
    void toAtomicLong_givenCharacter_returnsCorrectAtomicLong() {
        // GIVEN
        // Unicode value 65
        Character input = 'A';
        // WHEN
        AtomicLong result = CharacterConversions.toAtomicLong(input, mockConverter);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertEquals(65L, result.get(), "AtomicLong should contain the char's numeric value");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicLong_givenPrimitiveCharAutoboxed_returnsCorrectAtomicLong() {
        // GIVEN
        // Unicode value 90; autoboxed to Character when passed as Object
        char input = 'Z';
        // WHEN
        AtomicLong result = CharacterConversions.toAtomicLong(input, mockConverter);
        // THEN
        assertEquals(90L, result.get(), "AtomicLong should contain the numeric value of the char");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicLong_givenNull_throwsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterConversions.toAtomicLong(input, mockConverter), "Passing null should result in NPE due to unboxing");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicLong_givenIncompatibleType_throwsClassCastException() {
        // GIVEN
        String input = "not a char";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toAtomicLong(input, mockConverter), "Passing a non‑character type should cause a ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "toAtomicLong({0}) → AtomicLong[{1}]")
    @MethodSource("validConversionProvider")
    void toAtomicLong_givenValidCharacter_returnsCorrectAtomicLong(Character from, long expected) {
        // WHEN
        AtomicLong result = CharacterConversions.toAtomicLong(from, mockConverter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), () -> "AtomicLong should contain the numeric value of the character '" + from + "'");
        // Verify that the converter was never touched
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "toAtomicLong({0}) → throws {1}")
    @MethodSource("exceptionProvider")
    void toAtomicLong_givenInvalidInput_throwsExpectedException(Object from, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        Throwable thrown = assertThrows(expectedException, () -> CharacterConversions.toAtomicLong(from, mockConverter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
        // No interaction with the converter should have happened
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicLong_givenThrowingConverter_ignoresConverterAndReturnsAtomicLong() {
        // GIVEN
        // Unicode 97
        Character input = Character.valueOf('a');
        Converter throwingConverter = new ThrowingConverter();
        // WHEN
        AtomicLong result = CharacterConversions.toAtomicLong(input, throwingConverter);
        // THEN
        assertNotNull(result);
        assertEquals(97L, result.get(), "AtomicLong should contain the numeric value of the character 'a' (97)");
        // No exception means the converter was not used.
    }

    @Test
    void toAtomicLong_givenSameCharacterTwice_returnsTwoDistinctAtomicLongs() {
        // GIVEN
        // Unicode 98
        Character input = Character.valueOf('b');
        Converter throwingConverter = new ThrowingConverter();
        // WHEN
        AtomicLong first = CharacterConversions.toAtomicLong(input, throwingConverter);
        AtomicLong second = CharacterConversions.toAtomicLong(input, throwingConverter);
        // THEN
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(98L, first.get());
        assertEquals(98L, second.get());
        assertNotSame(first, second, "Each invocation should produce a new AtomicLong instance");
        // The converter must not have been called (otherwise an exception would be thrown)
    }
}
