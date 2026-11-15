package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toDouble_Test {

    /**
     * A mock Converter – the method under test does not use it, but the signature requires it.
     */
    @Mock
    Converter mockConverter;

    /**
     * A dummy Converter – does nothing; required only because the signature demands a non‑null instance.
     */
    private static class DummyConverter implements Converter {
        // No methods needed for the test; the real Converter interface methods are irrelevant.
    }

    private static Stream<Arguments> validCharacterProvider() {
        return Stream.of(// Regular ASCII
        arguments("Convert a regular ASCII character 'A' to its numeric double value (Unicode 65)", Character.valueOf('A'), null, 65.0), // Null character
        arguments("Convert the null character (code point 0) to double 0.0", Character.valueOf('\u0000'), null, 0.0), // Highest BMP character
        arguments("Convert the highest Unicode char (\\uffff) to its double representation (65535)", Character.valueOf('\uffff'), null, 65535.0), // Constructor‑created Character with a non‑null Converter (ignored)
        arguments("Convert a Character created with the constructor and pass a non‑null Converter (ignored)", new Character('z'), new DummyConverter(), 122.0), // Digit character '0'
        arguments("Convert the digit character '0' (code point 48) to its double representation", Character.valueOf('0'), null, 48.0), // Space character
        arguments("Convert a space character (code point 32) to double 32.0", Character.valueOf(' '), null, 32.0), // Newline character
        arguments("Convert a newline character (code point 10) to double 10.0", Character.valueOf('\n'), null, 10.0), // High‑surrogate (U+D800)
        arguments("Convert a high‑surrogate Unicode character (U+D800, code point 55296)", Character.valueOf('\uD800'), null, 55296.0), // Low‑surrogate (U+DC00)
        arguments("Convert a low‑surrogate Unicode character (U+DC00, code point 56320)", Character.valueOf('\uDC00'), null, 56320.0), // Primitive char literal (autoboxed)
        arguments("A primitive char literal is autoboxed to Character; the method should return its code point (66)", 'B', null, 66.0), // Converter that would throw if used – should be ignored
        arguments("Even when a Converter that throws on any method is supplied, toDouble ignores it", Character.valueOf('A'), throwingConverter(), 65.0), arguments("A Converter that would throw a RuntimeException if used is passed, but toDouble does not invoke it", Character.valueOf('z'), throwingRuntimeExceptionConverter(), 122.0));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(arguments("Passing a null source object should result in NullPointerException", null, null, NullPointerException.class), arguments("Passing a String should cause ClassCastException because the cast to char is invalid", "string", null, ClassCastException.class), arguments("Passing an Integer should also trigger a ClassCastException", Integer.valueOf(123), null, ClassCastException.class));
    }

    /**
     * Returns a Mockito mock that throws {@link UnsupportedOperationException}
     * on any method invocation.  The {@code toDouble} implementation never calls
     * the converter, so the exception should never be observed.
     */
    private static Converter throwingConverter() {
        return Mockito.mock(Converter.class, invocation -> {
            throw new UnsupportedOperationException("Converter should not be called");
        });
    }

    /**
     * Returns a Mockito mock that throws a {@link RuntimeException}
     * on any method invocation.  Again, the method under test does not use it.
     */
    private static Converter throwingRuntimeExceptionConverter() {
        return Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
    }

    @Test
    void toDouble_WithCharacter_returnsCorrectDouble() {
        // given
        // Unicode 65
        Character input = 'A';
        // when
        double result = CharacterConversions.toDouble(input, mockConverter);
        // then
        assertEquals(65.0d, result, "The double value should match the numeric value of the character.");
    }

    @Test
    void toDouble_WithZeroChar_returnsZero() {
        // given
        Character input = '\u0000';
        // when
        double result = CharacterConversions.toDouble(input, mockConverter);
        // then
        assertEquals(0.0d, result, "The double value of the null character should be 0.0.");
    }

    @Test
    void toDouble_WithNull_throwsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toDouble(input, mockConverter), "Casting null to char should throw NullPointerException.");
    }

    @Test
    void toDouble_WithNonCharacter_throwsClassCastException() {
        // given
        Object input = Integer.valueOf(123);
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toDouble(input, mockConverter), "Casting a non‑Character object to char should throw ClassCastException.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validCharacterProvider")
    void toDouble_ValidCharacters(String description, Object from, Converter converter, double expected) {
        // when
        double result = CharacterConversions.toDouble(from, converter);
        // then
        assertEquals(expected, result, 0.0, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionProvider")
    void toDouble_InvalidConversions(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> CharacterConversions.toDouble(from, converter), description);
    }
}
