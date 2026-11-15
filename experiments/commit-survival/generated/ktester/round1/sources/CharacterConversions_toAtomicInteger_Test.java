package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicInteger;
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
class CharacterConversions_toAtomicInteger_Test {

    @Mock
    private Converter // the method does not use it, but it must be supplied
    mockConverter;

    /**
     * Supplies all valid conversion scenarios (both the original “Valid conversions”
     * group and the “Additional valid conversions” group).
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// group: Valid conversions
        // 'A' → 65
        Arguments.of('A', null, 65), // null character → 0
        Arguments.of('\u0000', null, 0), // group: Additional valid conversions
        // 'Z' → 90
        Arguments.of('Z', null, 90), // newline → 10
        Arguments.of(Character.valueOf('\n'), null, 10), // max char → 65535
        Arguments.of(Character.valueOf('\uFFFF'), null, 65535), Arguments.of('0', // non‑null converter (ignored)
        new Converter() {
        }, // '0' → 48
        48));
    }

    /**
     * Supplies all invalid conversion scenarios (both the “Invalid conversions”
     * and “Invalid conversions - additional types” groups).
     */
    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// group: Invalid conversions
        Arguments.of(Integer.valueOf(123), null, ClassCastException.class), Arguments.of(null, null, NullPointerException.class), // group: Invalid conversions - additional types
        Arguments.of("A", null, ClassCastException.class), Arguments.of(new Object(), null, ClassCastException.class));
    }

    @Test
    void toAtomicInteger_givenCharacter_returnsAtomicIntegerWithCharCode() {
        // given
        // Unicode code point 90
        Character input = 'Z';
        // when
        AtomicInteger result = CharacterConversions.toAtomicInteger(input, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals((int) input.charValue(), result.get(), "AtomicInteger should contain the numeric value of the character");
    }

    @Test
    void toAtomicInteger_givenNonCharacter_throwsClassCastException() {
        // given
        Object input = Integer.valueOf(123);
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toAtomicInteger(input, mockConverter), "Casting a non‑Character object to char should throw ClassCastException");
    }

    @Test
    void toAtomicInteger_givenNull_throwsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toAtomicInteger(input, mockConverter), "Casting null to char should throw NullPointerException");
    }

    /**
     * Parameterised test that verifies successful conversion of various {@code Character}
     * (or {@code char}) inputs to {@link AtomicInteger}. The expected integer value is the
     * Unicode code point of the supplied character.
     */
    @ParameterizedTest(name = "[{index}] from={0} (converter={1}) → expected={2}")
    @MethodSource("validConversionProvider")
    void toAtomicInteger_validConversions(Object from, Converter converter, int expectedValue) {
        // when
        AtomicInteger result = CharacterConversions.toAtomicInteger(from, converter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedValue, result.get(), () -> "AtomicInteger should contain the numeric value of the character");
    }

    /**
     * Parameterised test that verifies the method throws the expected exception
     * when the supplied {@code from} argument cannot be cast to {@code char}.
     */
    @ParameterizedTest(name = "[{index}] from={0} (converter={1}) → throws {2}")
    @MethodSource("invalidConversionProvider")
    void toAtomicInteger_invalidConversions(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CharacterConversions.toAtomicInteger(from, converter), () -> "Expected " + expectedException.getSimpleName() + " when converting " + from);
    }
}
