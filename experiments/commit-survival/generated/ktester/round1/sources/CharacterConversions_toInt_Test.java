package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toInt_Test {

    @Mock
    Converter // Converter is not used by toInt, but we provide a mock
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already initialises @Mock fields
        // Ensure the mock has no previous interactions
        clearInvocations(mockConverter);
    }

    private static Stream<Arguments> successfulConversionProvider() {
        // a reusable no‑op Converter instance (the method under test ignores it)
        Converter noopConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dst) {
                return src;
            }
        };
        return Stream.of(Arguments.of(Character.valueOf('A'), null, 65, "Convert a Character containing 'A' to its Unicode code point (65)"), Arguments.of(Character.valueOf('z'), null, 122, "Convert a Character containing lower‑case 'z' to its Unicode code point (122)"), Arguments.of(Character.valueOf('\u0000'), null, 0, "Convert the null character (code point 0)"), Arguments.of(Character.valueOf('5'), null, 53, "Convert a Character containing the digit '5' (Unicode 53)"), Arguments.of(Character.valueOf('\uFFFF'), null, 65535, "Convert the maximum char value (\\uFFFF) to int (65535)"), Arguments.of(Character.valueOf('\u0001'), null, 1, "Convert the char with code point 1 (\\u0001)"), Arguments.of(Character.valueOf(' '), null, 32, "Convert a space character to its Unicode code point (32)"), Arguments.of(Character.valueOf('\n'), null, 10, "Convert a newline character to its int value (10)"), Arguments.of(Character.valueOf('\t'), null, 9, "Convert a tab character to its int value (9)"), // primitive char literal – autoboxed to Character by the compiler
        Arguments.of('c', null, 99, "Pass a primitive char literal 'c' (autoboxed) and verify conversion (99)"), // custom non‑null Converter – should be ignored
        Arguments.of(Character.valueOf('X'), noopConverter, 88, "Provide a non‑null Converter; method should ignore it and return 88 for 'X'"));
    }

    private static Stream<Arguments> exceptionProvider() {
        // a reusable non‑null Converter instance for the relevant test case
        Converter noopConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dst) {
                return src;
            }
        };
        return Stream.of(Arguments.of(null, null, NullPointerException.class, "Passing a null source object should cause NullPointerException"), Arguments.of(Integer.valueOf(5), null, ClassCastException.class, "An Integer cannot be cast to char; expect ClassCastException"), Arguments.of(new Object(), null, ClassCastException.class, "A generic Object is not a Character; expect ClassCastException"), Arguments.of("A", null, ClassCastException.class, "Passing a String (even length‑1) cannot be cast to char"), Arguments.of(Byte.valueOf((byte) 1), null, ClassCastException.class, "A Byte object is not a Character; expect ClassCastException"), Arguments.of(Short.valueOf((short) 2), null, ClassCastException.class, "A Short object cannot be cast to char"), Arguments.of(Long.valueOf(3L), null, ClassCastException.class, "A Long object is not a Character; expect ClassCastException"), Arguments.of(Boolean.TRUE, null, ClassCastException.class, "A Boolean object cannot be cast to char"), // null source with a non‑null Converter – still NPE from the cast
        Arguments.of(null, noopConverter, NullPointerException.class, "Null source with a non‑null Converter should still throw NullPointerException"), Arguments.of(new char[] { 'a' }, null, ClassCastException.class, "A char array cannot be cast to a single char primitive"), Arguments.of(java.math.BigInteger.ONE, null, ClassCastException.class, "BigInteger is not a Character; expect ClassCastException"));
    }

    @Test
    void toInt_givenCharacter_returnsItsUnicodeCodePoint() {
        // given
        Character input = 'Z';
        // when
        int result = CharacterConversions.toInt(input, mockConverter);
        // then
        assertEquals((int) 'Z', result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInt_givenPrimitiveChar_autoboxedToCharacter_returnsItsUnicodeCodePoint() {
        // given
        char input = 'a';
        // when
        int result = CharacterConversions.toInt(input, mockConverter);
        // then
        assertEquals((int) 'a', result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInt_givenNull_throwsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toInt(input, mockConverter));
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInt_givenNonCharacterObject_throwsClassCastException() {
        // given
        Object input = Integer.valueOf(123);
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toInt(input, mockConverter));
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successfulConversionProvider")
    void toInt_successfulConversion(Object from, Converter converter, int expected, String description) {
        // when
        int result = CharacterConversions.toInt(from, converter);
        // then
        assertEquals(expected, result, "Unexpected int value for: " + description);
        // If a mock converter was supplied, ensure it was never used
        if (converter == mockConverter) {
            verifyNoInteractions(mockConverter);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionProvider")
    void toInt_expectedException(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> CharacterConversions.toInt(from, converter), "Expected " + expectedException.getSimpleName() + " for: " + description);
        // Verify that a mock converter, when supplied, is never interacted with
        if (converter == mockConverter) {
            verifyNoInteractions(mockConverter);
        }
    }
}
