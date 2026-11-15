package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toDouble_Test {

    @Mock
    private Converter // not used by the method but required for the signature
    converter;

    /**
     * Provides a stream of valid character inputs together with the expected double value.
     * The {@code converter} argument may be {@code null} or a mock – the method ignores it.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> validCharacterProvider() {
        // a mock converter that does nothing (used for two specific cases)
        Converter dummyConverter = mock(Converter.class);
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be invoked");
        });
        return Stream.of(// simple literals
        arguments('A', 65.0, "Convert a Character literal 'A' (Unicode 65) to double", null), arguments('\u0000', 0.0, "Convert the null character (code point 0) to double", null), arguments('\uFFFF', 65535.0, "Convert the highest char value (Unicode 0xFFFF) to double", null), // Character objects
        arguments(new Character('z'), 122.0, "Convert a Character object representing 'z' (code point 122) to double", null), // whitespace / control chars
        arguments(' ', 32.0, "Convert a space character (Unicode 32) to double", null), arguments('\n', 10.0, "Convert a newline character (Unicode 10) to double", null), arguments('\t', 9.0, "Convert a tab character (Unicode 9) to double", null), // surrogate characters
        arguments('\uD800', 55296.0, "Convert a high‑surrogate character (Unicode 0xD800) to double", null), arguments('\uDC00', 56320.0, "Convert a low‑surrogate character (Unicode 0xDC00) to double", null), // arbitrary Unicode point via Character object
        arguments(new Character('\u1234'), 4660.0, "Convert a Character object with code point 0x1234 (4660) to double", null), // non‑null converters (method should ignore them)
        arguments('A', 65.0, "Pass a non‑null Converter (ignored) and a character literal 'A'", dummyConverter), arguments('z', 122.0, "Provide a Converter that would throw if used; conversion still succeeds", throwingConverter));
    }

    /**
     * Provides inputs that are *not* characters and the exception type we expect.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> exceptionProvider() {
        return Stream.of(arguments("test", ClassCastException.class, "Pass a String object; casting to char should throw ClassCastException."), arguments(Boolean.TRUE, ClassCastException.class, "Pass a Boolean object; casting to char should throw ClassCastException."), arguments(new Double(3.14), ClassCastException.class, "Pass a Double wrapper; casting to char should throw ClassCastException."), arguments(new Long(123L), ClassCastException.class, "Pass a Long wrapper; casting to char should throw ClassCastException."), arguments(new Short((short) 5), ClassCastException.class, "Pass a Short wrapper; casting to char should throw ClassCastException."), arguments(new Byte((byte) 1), ClassCastException.class, "Pass a Byte wrapper; casting to char should throw ClassCastException."), arguments(new int[] { 65 }, ClassCastException.class, "Pass an int array; casting to char should throw ClassCastException."), arguments(new Object(), ClassCastException.class, "Pass a generic Object instance; casting to char should throw ClassCastException."));
    }

    @Test
    void toDouble_WithCharacter_ReturnsCorrectDouble() {
        // GIVEN
        // Unicode 90
        Character input = 'Z';
        // WHEN
        double result = CharacterConversions.toDouble(input, converter);
        // THEN
        assertEquals((double) input.charValue(), result, "The double value should equal the numeric value of the character");
    }

    @Test
    void toDouble_WithNonCharacter_ThrowsClassCastException() {
        // GIVEN
        String invalidInput = "A";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toDouble(invalidInput, converter), "Casting a non‑character object to char should throw ClassCastException");
    }

    @Test
    void toDouble_WithNull_ThrowsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterConversions.toDouble(nullInput, converter), "Casting null to char should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validCharacterProvider")
    void toDouble_ValidCharacters_ReturnsExpectedDouble(Object from, double expected, String description, Converter customConverter) {
        // GIVEN
        // fall back to the @Mock
        Converter usedConverter = (customConverter != null) ? customConverter : converter;
        // WHEN
        double result = CharacterConversions.toDouble(from, usedConverter);
        // THEN
        assertEquals(expected, result, 0.000001, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionProvider")
    void toDouble_NonCharacterInputs_ThrowExpectedException(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> CharacterConversions.toDouble(from, converter), description);
    }

    @Test
    void toDouble_NullInput_ThrowsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterConversions.toDouble(nullInput, converter), "Casting null to char should throw NullPointerException");
    }
}
